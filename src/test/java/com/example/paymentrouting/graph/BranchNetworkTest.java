package com.example.paymentrouting.graph;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BranchNetworkTest {

    private static final List<Branch> SPEC_BRANCHES = List.of(
            new Branch("A", 5), new Branch("B", 50), new Branch("C", 10),
            new Branch("D", 10), new Branch("E", 20), new Branch("F", 5));

    private static final List<Link> SPEC_LINKS = List.of(
            new Link("A", "B"), new Link("A", "C"), new Link("C", "B"),
            new Link("B", "D"), new Link("C", "E"), new Link("D", "E"),
            new Link("E", "D"), new Link("D", "F"), new Link("E", "F"));

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
}
