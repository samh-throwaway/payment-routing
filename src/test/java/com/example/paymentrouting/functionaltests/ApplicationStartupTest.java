package com.example.paymentrouting.functionaltests;

import com.example.paymentrouting.PaymentRoutingApplication;
import com.example.paymentrouting.config.NetworkLoadException;
import com.example.paymentrouting.graph.InvalidNetworkException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ApplicationStartupTest {

    @Test
    void givenNetworkFileBreakingRules_whenStartingApplication_thenStartupFailsWithEveryError() {
        // when
        Throwable failure = catchThrowable(() -> start("classpath:networks/invalid-network.json"));

        // then
        assertThat(causeChain(failure))
                .filteredOn(InvalidNetworkException.class::isInstance)
                .singleElement()
                .extracting(Throwable::getMessage)
                .asString()
                .contains("2 errors");
    }

    @Test
    void givenNetworkFileWithUnknownField_whenStartingApplication_thenStartupFailsWithNetworkLoadException() {
        // when
        Throwable failure = catchThrowable(() -> start("classpath:networks/unknown-field-network.json"));

        // then
        assertThat(causeChain(failure)).hasAtLeastOneElementOfType(NetworkLoadException.class);
    }

    /** Starts the whole application on the given network file, on a random free port. */
    private static void start(String networkLocation) {
        SpringApplication.run(PaymentRoutingApplication.class,
                "--payment.network.location=" + networkLocation, "--server.port=0").close();
    }

    /** The exception followed by its cause, its cause's cause, and so on. Empty if nothing was thrown. */
    private static List<Throwable> causeChain(Throwable failure) {
        return Stream.iterate(failure, Objects::nonNull, Throwable::getCause).toList();
    }
}
