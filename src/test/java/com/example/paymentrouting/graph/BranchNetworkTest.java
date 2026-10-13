package com.example.paymentrouting.graph;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

class BranchNetworkTest {

    private static final List<Branch> SPEC_BRANCHES = List.of(
            new Branch("A", 5), new Branch("B", 50), new Branch("C", 10),
            new Branch("D", 10), new Branch("E", 20), new Branch("F", 5));

    private static final List<Link> SPEC_LINKS = List.of(
            new Link("A", "B"), new Link("A", "C"), new Link("C", "B"),
            new Link("B", "D"), new Link("C", "E"), new Link("D", "E"),
            new Link("E", "D"), new Link("D", "F"), new Link("E", "F"));

    /** Builds a network that must be rejected, and returns the errors it was rejected with. */
    private static List<String> errorsFor(List<Branch> branches, List<Link> links) {
        try {
            BranchNetwork.of(branches, links);
        } catch (InvalidNetworkException e) {
            return e.errors();
        }

        throw new AssertionError("expected the network to be rejected");
    }

    @Nested
    class ValidNetwork {

        @Test
        void givenSpecNetwork_whenFetchingFeeOfB_thenReturns50() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when
            long fee = network.fee("B");

            // then
            assertThat(fee).isEqualTo(50);
        }

        @Test
        void givenSpecNetwork_whenFetchingOutgoingLinks_thenEachBranchListsItsTargetsSortedById() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when
            List<String> outgoingA = network.outgoing("A");
            List<String> outgoingB = network.outgoing("B");
            List<String> outgoingC = network.outgoing("C");
            List<String> outgoingD = network.outgoing("D");
            List<String> outgoingE = network.outgoing("E");
            List<String> outgoingF = network.outgoing("F");

            // then
            assertThat(outgoingA).containsExactly("B", "C");
            assertThat(outgoingB).containsExactly("D");
            assertThat(outgoingC).containsExactly("B", "E");
            assertThat(outgoingD).containsExactly("E", "F");
            assertThat(outgoingE).containsExactly("D", "F");
            assertThat(outgoingF).isEmpty();
        }

        @Test
        void givenBranchWithNoLinks_whenFetchingOutgoing_thenEmptyListIsReturned() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, List.of());

            // when
            List<String> outgoingF = network.outgoing("F");

            // then
            assertThat(outgoingF).isNotNull().isEmpty();
        }

        @Test
        void givenUnorderedSpecNetwork_whenFetchingOutgoingLinks_thenLinksAreReturnedSorted() {
            // given
            List<Branch> branches = List.of(
                    new Branch("A", 5), new Branch("B", 50), new Branch("C", 10));
            List<Link> unsortedLinks = List.of(
                    new Link("A", "C"), new Link("A", "B"));
            BranchNetwork network = BranchNetwork.of(branches, unsortedLinks);

            // when
            List<String> outgoingA = network.outgoing("A");

            // then
            assertThat(outgoingA).containsExactly("B", "C");
        }

        @Test
        void givenSpecNetwork_whenFetchingOutgoingLinks_thenLinksAreDirected() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when
            List<String> outgoingA = network.outgoing("A");
            List<String> outgoingB = network.outgoing("B");

            // then
            assertThat(outgoingA).contains("B");
            assertThat(outgoingB).doesNotContain("A");
        }

        @ParameterizedTest
        @CsvSource({
                "A, true", // a declared branch
                "F, true", // another declared branch, no links
                "Z, false", // not in the network
                "a, false", // exact match only; normalising is the service's job
                ", false" // null branch id
        })
        void givenSpecNetwork_whenCheckingContains_thenReturnsWhetherBranchExists(String id, boolean expected) {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when
            boolean result = network.contains(id);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        void givenSingleBranchAndNoLinks_whenBuildingNetwork_thenNetworkIsCreated() {
            // given
            BranchNetwork network = BranchNetwork.of(List.of(new Branch("A", 5)), List.of());

            // when
            List<String> outgoingA = network.outgoing("A");

            // then
            assertThat(outgoingA).isEmpty();
            assertThat(network.contains("A")).isTrue();
        }

        @Test
        void givenSingleBranch_whenBranchHasFeeOf0_thenReturnsFeeOf0() {
            // given
            BranchNetwork network = BranchNetwork.of(List.of(new Branch("H", 0)),
                    List.of());

            // when
            long fee = network.fee("H");

            // then
            assertThat(fee).isZero();
        }
    }

    @Nested
    class Immutability {

        @Test
        void givenSpecNetwork_whenAddingToAnOutgoingList_thenItIsRejected() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when - then
            List<String> outgoingA = network.outgoing("A");
            assertThatThrownBy(() -> outgoingA.add("X"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void givenNetworkBuiltFromMutableLists_whenTheListsAreChangedAfterwards_thenNetworkIsUnaffected() {
            // given
            List<Branch> branches = new ArrayList<>();
            branches.add(new Branch("A", 5));
            branches.add(new Branch("B", 10));
            branches.add(new Branch("C", 20));
            List<Link> links = new ArrayList<>();
            links.add(new Link("A", "C"));
            links.add(new Link("B", "C"));

            BranchNetwork network = BranchNetwork.of(branches, links);

            // when
            branches.add(new Branch("E", 10));
            links.add(new Link("A", "E"));

            // then
            assertThat(network.outgoing("A")).containsExactly("C");
        }
    }

    @Nested
    class Lookups {

        @Test
        void givenSpecNetwork_whenFetchingFeeOfUnknownBranch_thenIllegalArgumentException() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when - then
            assertThatThrownBy(() -> network.fee("Z"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Z");
        }

        @Test
        void givenSpecNetwork_whenFetchingFeeOfNullBranch_thenNullPointerException() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when - then
            assertThatThrownBy(() -> network.fee(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id must not be null");
        }

        @Test
        void givenSpecNetwork_whenFetchingLinksOfUnknownBranch_thenIllegalArgumentException() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when - then
            assertThatThrownBy(() -> network.outgoing("Z"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Z");
        }

        @Test
        void givenSpecNetwork_whenFetchingLinksOfNullBranch_thenNullPointerException() {
            // given
            BranchNetwork network = BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);

            // when - then
            assertThatThrownBy(() -> network.outgoing(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("id must not be null");
        }
    }

    @Nested
    class BranchValidation {

        @Test
        void givenInvalidNetwork_whenBranchesIsNull_thenReportsNoBranches() {
            // given
            List<Branch> branches = null;

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).containsExactly("branches must contain at least one branch");
        }

        @Test
        void givenInvalidNetwork_whenBranchesIsEmpty_thenReportsNoBranches() {
            // given
            List<Branch> branches = List.of();

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).containsExactly("branches must contain at least one branch");
        }

        @Test
        void givenInvalidNetwork_whenBranchIsMissingFromNetwork_thenReportsMissingBranchId() {
            // given
            List<Branch> branches = Arrays.asList(new Branch("A", 1), null);

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).containsExactly("branches[1]: branch is missing");
        }

        @Test
        void givenInvalidNetwork_whenBranchIdIsNull_thenReportsMissingBranchId() {
            // given
            List<Branch> branches = List.of(new Branch(null, 1));

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).containsExactly("branches[0]: id is missing");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "a", "A B", " A", "A_B", "A.B", "À", "ABCDEFGHIJKLMNOPQRSTU"})
        void givenInvalidBranchIds_whenBranchIdsAreMalformedInvalid_thenReportsInvalidBranchIds(String branchId) {
            // given
            List<Branch> branches = List.of(new Branch(branchId, 1));

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).singleElement(STRING).startsWith("branches[0]: id '" + branchId + "' is invalid");
        }

        @ParameterizedTest
        @ValueSource(strings = {"A", "Z9", "BRANCH-01", "ABCDEFGHIJKLMNOPQRST"})
        void givenValidBranchIds_whenBranchIdsAreValid_thenContainsIsTrue(String branchId) {
            // given
            Branch branch = new Branch(branchId, 1);

            // when
            BranchNetwork network = BranchNetwork.of(List.of(branch), List.of());

            // then
            assertThat(network.contains(branchId)).isTrue();
        }

        @Test
        void givenInvalidNetwork_whenBranchesHaveDuplicateBranchIdEntries_thenReportsDuplicateBranchIds() {
            // given
            List<Branch> branches = List.of(new Branch("A", 1), new Branch("A", 2));

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).containsExactly("branches[1]: duplicate branch id 'A'");
        }

        @Test
        void givenInvalidNetwork_whenBranchHasNegativeFee_thenReportsNegativeFee() {
            // given
            List<Branch> branches = List.of(new Branch("A", -1));

            // when
            List<String> errors = errorsFor(branches, List.of());

            // then
            assertThat(errors).containsExactly("branches[0]: fee -1 of 'A' must not be negative");
        }
    }

    @Nested
    class LinksValidation {

        @Test
        void givenInvalidLinks_whenLinksAreNull_thenReportsNoLinks() {
            // given
            List<Link> links = null;

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5)), links);

            // then
            assertThat(errors).containsExactly("links are missing; use an empty list" +
                    " for a network without links");
        }

        @Test
        void givenInvalidLinks_whenLinkIsMissing_thenReportsMissingLink() {
            // given
            List<Link> links = Arrays.asList(new Link("A", "B"), null);

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[1]: link is missing");
        }

        @Test
        void givenInvalidLinks_whenMissingFromBranchId_thenReportsMissingLink() {
            // given
            List<Link> links = List.of(new Link(null, "B"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[0]: from is missing");
        }

        @Test
        void givenInvalidLinks_whenMissingToBranchId_thenReportsMissingLink() {
            // given
            List<Link> links = List.of(new Link("A", null));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5)), links);

            // then
            assertThat(errors).containsExactly("links[0]: to is missing");
        }

        @Test
        void givenInvalidLinks_whenFromBranchIdIsNotKnown_thenReportsUnknownLink() {
            // given
            List<Link> links = List.of(new Link("X", "B"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[0]: from 'X' is not known");
        }

        @Test
        void givenInvalidLinks_whenToBranchIdIsNotKnown_thenReportsUnknownLink() {
            // given
            List<Link> links = List.of(new Link("A", "Y"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5)), links);

            // then
            assertThat(errors).containsExactly("links[0]: to 'Y' is not known");
        }

        @Test
        void givenInvalidLinks_whenSelfLink_thenReportsSelfLink() {
            // given
            List<Link> links = List.of(new Link("A", "A"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5)), links);

            // then
            assertThat(errors).containsExactly("links[0]: branch 'A' cannot link to itself");
        }

        @Test
        void givenInvalidLinks_whenDuplicateLinkPairs_thenReportsDuplicateLink() {
            // given
            List<Link> links = List.of(new Link("A", "B"), new Link("A", "B"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[1]: duplicate link 'A' -> 'B'");
        }

        @Test
        void givenBranchWithMalformedId_whenLinkReferencesIt_thenOnlyTheBranchErrorIsReported() {
            // given
            List<Link> links = List.of(new Link("a", "B"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("a", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).singleElement(STRING).startsWith("branches[0]: id 'a' is invalid");
        }

        @Test
        void givenInvalidLinks_whenNullFromAndTo_thenReportsBothMissingBranches() {
            // given
            List<Link> links = List.of(new Link(null, null));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[0]: from is missing",
                    "links[0]: to is missing");
        }

        @Test
        void givenInvalidLinks_whenUnknownFromAndTo_thenReportsBothUnknownBranches() {
            // given
            List<Link> links = List.of(new Link("X", "Y"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[0]: from 'X' is not known",
                    "links[0]: to 'Y' is not known");
        }

        @Test
        void givenInvalidLinks_whenMissingFromAndUnknownTo_thenReportsMissingFromAndUnknownTo() {
            // given
            List<Link> links = List.of(new Link(null, "Y"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[0]: from is missing",
                    "links[0]: to 'Y' is not known");
        }

        @Test
        void givenInvalidLinks_whenSelfLinkOnUnknownBranch_thenReportsOnlyUnknownBranch() {
            // given
            List<Link> links = List.of(new Link("X", "X"));

            // when
            List<String> errors = errorsFor(List.of(new Branch("A", 5),
                    new Branch("B", 10)), links);

            // then
            assertThat(errors).containsExactly("links[0]: from 'X' is not known",
                    "links[0]: to 'X' is not known");
        }
    }

    @Nested
    class ErrorReporting {

        @Test
        void givenInvalidNetwork_whenNetworkContainsInvalidFeesAndLinks_thenMultipleErrorsReported() {
            // given
            List<Branch> branches = List.of(new Branch("A", 1), new Branch("A", 2),
                    new Branch("B", -5));
            List<Link> links = List.of(new Link("A", "Z"), new Link("B", "B"), new Link("A", "B"),
                    new Link("A", "B"));

            // when
            List<String> errors = errorsFor(branches, links);

            // then
            assertThat(errors).containsExactly(
                    "branches[1]: duplicate branch id 'A'",
                    "branches[2]: fee -5 of 'B' must not be negative",
                    "links[0]: to 'Z' is not known",
                    "links[1]: branch 'B' cannot link to itself",
                    "links[3]: duplicate link 'A' -> 'B'"
            );
        }

        @Test
        void givenTwoProblems_whenBuildingNetwork_thenMessageCountsErrorsInPlural() {
            // given
            List<Branch> branches = List.of(new Branch("A", -5));
            List<Link> links = List.of(new Link("A", "B"));

            // when
            InvalidNetworkException exception = catchThrowableOfType(
                    InvalidNetworkException.class,
                    () -> BranchNetwork.of(branches, links));

            // then
            assertThat(exception.getMessage())
                    .startsWith("Invalid branch network (2 errors):")
                    .contains("branches[0]: fee -5 of 'A' must not be negative")
                    .contains("links[0]: to 'B' is not known");
        }

        @Test
        void givenOneProblem_whenBuildingNetwork_thenMessageUsesSingular() {
            // given
            List<Branch> branches = List.of(new Branch("A", -5), new Branch("B", 1));
            List<Link> links = List.of(new Link("A", "B"));

            // when
            InvalidNetworkException exception = catchThrowableOfType(
                    InvalidNetworkException.class,
                    () -> BranchNetwork.of(branches, links));

            // then
            assertThat(exception.getMessage())
                    .startsWith("Invalid branch network (1 error):")
                    .contains("branches[0]: fee -5 of 'A' must not be negative");
        }
    }

    @Nested
    class IdFormat {

        @ParameterizedTest
        @ValueSource(strings = {"A", "Z9", "BRANCH-01", "ABCDEFGHIJKLMNOPQRST"})
        void givenWellFormedId_whenCheckingFormat_thenIsValid(String id) {
            // when - then
            assertThat(BranchNetwork.isValidId(id)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "a", "A B", "A_B", "\u00C4", "ABCDEFGHIJKLMNOPQRSTU"})
        void givenMalformedId_whenCheckingFormat_thenIsNotValid(String id) {
            // when - then
            assertThat(BranchNetwork.isValidId(id)).isFalse();
        }
    }
}
