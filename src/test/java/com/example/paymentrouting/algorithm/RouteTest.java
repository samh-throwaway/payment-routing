package com.example.paymentrouting.algorithm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteTest {

    @Test
    void givenNoBranches_whenCreatingRoute_thenThrowsIllegalArgumentException() {
        // when - then
        assertThatThrownBy(() -> new Route(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one branch");
    }

    @Test
    void givenNegativeCost_whenCreatingRoute_thenThrowsIllegalArgumentException() {
        // when - then
        assertThatThrownBy(() -> new Route(List.of("A"), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void givenCallerChangesOriginalList_whenReadingRoute_thenRouteIsUnchanged() {
        // given
        List<String> branches = new ArrayList<>(List.of("A", "B"));
        Route route = new Route(branches, 5);

        // when
        branches.add("C");

        // then
        assertThat(route.branches()).containsExactly("A", "B");
    }

    @Test
    void givenRoute_whenModifyingItsBranches_thenThrowsUnsupportedOperationException() {
        // given
        Route route = new Route(List.of("A"), 0);

        // when - then
        assertThatThrownBy(() -> route.branches().add("B"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
