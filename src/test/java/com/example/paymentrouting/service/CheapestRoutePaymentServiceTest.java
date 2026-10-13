package com.example.paymentrouting.service;

import com.example.paymentrouting.TestNetworks;
import com.example.paymentrouting.algorithm.DijkstraRouteFinder;
import com.example.paymentrouting.graph.BranchNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheapestRoutePaymentServiceTest {

    private final BranchNetwork network = TestNetworks.spec();
    private final PaymentService service =
            new CheapestRoutePaymentService(network, new DijkstraRouteFinder(network));

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "A | D | A,C,E,D",
            "A | F | A,C,E,F",
            "C | E | C,E",
            "A | A | A",
            "' a ' | d | A,C,E,D"
    })
    void givenReachableBranches_whenProcessingPayment_thenReturnsCheapestRouteAsCsv(
            String origin, String destination, String expected) {
        // when
        String route = service.processPayment(origin, destination);

        // then
        assertThat(route).isEqualTo(expected);
    }

    @Test
    void givenUnreachableDestination_whenProcessingPayment_thenReturnsNull() {
        // when - then
        assertThat(service.processPayment("E", "B")).isNull();
    }

    @Test
    void givenUnknownOrigin_whenProcessingPayment_thenThrowsUnknownBranchException() {
        // when - then
        assertThatThrownBy(() -> service.processPayment("X", "D"))
                .isInstanceOf(UnknownBranchException.class)
                .hasMessageContaining("X");
    }

    @Test
    void givenUnknownDestination_whenProcessingPayment_thenThrowsUnknownBranchException() {
        // when - then
        assertThatThrownBy(() -> service.processPayment("A", "Y"))
                .isInstanceOf(UnknownBranchException.class)
                .hasMessageContaining("Y");
    }

    @Test
    void givenMalformedDestinationAndUnknownOrigin_whenProcessingPayment_thenReportsMalformedFirst() {
        // when - then
        assertThatThrownBy(() -> service.processPayment("X", "not valid!"))
                .isInstanceOf(InvalidBranchException.class);
    }
}
