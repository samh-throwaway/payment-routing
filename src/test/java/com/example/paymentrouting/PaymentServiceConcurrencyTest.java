package com.example.paymentrouting;

import com.example.paymentrouting.algorithm.DijkstraRouteFinder;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.service.CheapestRoutePaymentService;
import com.example.paymentrouting.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceConcurrencyTest {

    private static final int THREADS = 16;
    private static final int CALLS_PER_THREAD = 2_000;

    private record Query(String origin, String destination, String expected) {
    }

    @Test
    void givenManyThreads_whenProcessingPaymentsConcurrently_thenEveryResultIsCorrect() throws Exception {
        // given
        BranchNetwork network = TestNetworks.spec();
        PaymentService service = new CheapestRoutePaymentService(network, new DijkstraRouteFinder(network));
        List<Query> queries = List.of(
                new Query("A", "D", "A,C,E,D"),
                new Query("A", "F", "A,C,E,F"),
                new Query("C", "E", "C,E"),
                new Query("C", "D", "C,E,D"),
                new Query("A", "A", "A"),
                new Query("E", "B", null));
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<String>>> futures = new ArrayList<>();

        // when
        for (int t = 0; t < THREADS; t++) {
            int offset = t;
            futures.add(pool.submit(() -> {
                List<String> mismatches = new ArrayList<>();
                start.await();
                for (int i = 0; i < CALLS_PER_THREAD; i++) {
                    Query q = queries.get((i + offset) % queries.size());
                    String actual = service.processPayment(q.origin(), q.destination());
                    if (!Objects.equals(q.expected(), actual)) {
                        mismatches.add(q + " -> " + actual);
                    }
                }
                return mismatches;
            }));
        }
        start.countDown();

        // then
        try {
            for (Future<List<String>> future : futures) {
                assertThat(future.get(30, TimeUnit.SECONDS)).isEmpty();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
