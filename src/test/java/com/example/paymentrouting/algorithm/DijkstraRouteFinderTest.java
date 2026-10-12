package com.example.paymentrouting.algorithm;

import com.example.paymentrouting.TestNetworks;
import com.example.paymentrouting.graph.Branch;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.graph.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DijkstraRouteFinderTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "A | D | A,C,E,D | 35",
            "A | F | A,C,E,F | 35",
            "C | E | C,E | 10",
            "C | D | C,E,D | 30",
            "A | A | A | 0"
    })
    void givenSpecNetwork_whenFindingRoute_thenReturnsCheapestRouteAndCost(String origin, String destination,
                                                                           String expectedRoute, long expectedCost) {
        // given
        BranchNetwork network = TestNetworks.spec();
        DijkstraRouteFinder dijkstraRouteFinder = new DijkstraRouteFinder(network);

        // when
        Route route = dijkstraRouteFinder.find(origin, destination);

        // then
        assertThat(route).isEqualTo(new Route(Arrays.asList(expectedRoute.split(",")), expectedCost));
    }

    @Test
    void givenSpecNetwork_whenFindingUnfinishableRoute_thenReturnsNull() {
        // given
        BranchNetwork network = TestNetworks.spec();
        DijkstraRouteFinder dijkstraRouteFinder = new DijkstraRouteFinder(network);

        // when - then
        assertThat(dijkstraRouteFinder.find("E", "B")).isNull();
    }

    @Test
    void givenSpecNetwork_whenFindingAnUnknownOrigin_thenThrowsIllegalArgumentException() {
        // given
        BranchNetwork network = TestNetworks.spec();
        DijkstraRouteFinder routeFinder = new DijkstraRouteFinder(network);

        // when - then
        assertThatThrownBy(() -> routeFinder.find("X", "B"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origin")
                .hasMessageContaining("X");
    }

    @Test
    void givenSpecNetwork_whenFindingAnUnknownDestination_thenThrowsIllegalArgumentException() {
        // given
        BranchNetwork network = TestNetworks.spec();
        DijkstraRouteFinder routeFinder = new DijkstraRouteFinder(network);

        // when - then
        assertThatThrownBy(() -> routeFinder.find("A", "Y"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destination")
                .hasMessageContaining("Y");
    }

    @Test
    void givenTwoEqualCostRoutes_whenFindingRoute_thenFewestHopsWins() {
        // given
        List<Branch> branches = List.of(new Branch("A", 1), new Branch("X", 1),
                new Branch("S", 1), new Branch("L1", 0), new Branch("L2", 0),
                new Branch("L3", 2), new Branch("D", 0));
        List<Link> links = List.of(new Link("A", "X"), new Link("X", "S"), new Link("S", "D"),
                new Link("A", "L1"), new Link("L1", "L2"), new Link("L2", "L3"), new Link("L3", "D"));
        BranchNetwork network = BranchNetwork.of(branches, links);
        DijkstraRouteFinder dijkstraRouteFinder = new DijkstraRouteFinder(network);

        // when
        Route route = dijkstraRouteFinder.find("A", "D");

        // then
        assertThat(route).isEqualTo(new Route(List.of("A", "X", "S", "D"), 3));
    }

    @Test
    void givenLinksDeclaredOutOfOrder_whenFindingRoute_thenSameRouteAsInOrder() {
        // given
        List<Branch> branches = List.of(new Branch("A", 1), new Branch("B", 1),
                new Branch("C", 1), new Branch("D", 0));
        List<Link> links = List.of(new Link("A", "C"), new Link("C", "D"), new Link("A", "B"),
                new Link("B", "D"));
        BranchNetwork network = BranchNetwork.of(branches, links);
        DijkstraRouteFinder dijkstraRouteFinder = new DijkstraRouteFinder(network);

        // when
        Route route = dijkstraRouteFinder.find("A", "D");

        // then
        assertThat(route).isEqualTo(new Route(List.of("A", "B", "D"), 2));
    }

    @Test
    void givenTwoEqualCostAndHopsRoutes_whenFindingRoute_thenAlphabeticallyFirstBranchWins() {
        // given
        List<Branch> branches = List.of(new Branch("A", 1), new Branch("Y", 1),
                new Branch("Z", 1), new Branch("M", 1), new Branch("B", 1),
                new Branch("D", 0));
        List<Link> links = List.of(new Link("A", "Y"), new Link("A", "Z"), new Link("Y", "M"),
                new Link("Z", "B"), new Link("M", "D"), new Link("B", "D"));
        BranchNetwork network = BranchNetwork.of(branches, links);
        DijkstraRouteFinder dijkstraRouteFinder = new DijkstraRouteFinder(network);

        // when
        Route route = dijkstraRouteFinder.find("A", "D");

        // then
        assertThat(route).isEqualTo(new Route(List.of("A", "Z", "B", "D"), 3));
    }
}
