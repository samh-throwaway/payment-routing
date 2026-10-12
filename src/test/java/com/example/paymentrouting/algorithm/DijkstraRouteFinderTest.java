package com.example.paymentrouting.algorithm;

import com.example.paymentrouting.TestNetworks;
import com.example.paymentrouting.graph.BranchNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

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
}
