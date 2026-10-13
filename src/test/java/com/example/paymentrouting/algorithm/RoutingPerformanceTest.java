package com.example.paymentrouting.algorithm;

import com.example.paymentrouting.graph.Branch;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.graph.Link;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("performance")
class RoutingPerformanceTest {

    private static final int EXTRA_LINKS_PER_BRANCH = 5;
    private static final long MAX_QUERY_MILLIS = 5_000;
    private static final String UNREACHABLE = "NOWHERE";

    @ParameterizedTest
    @ValueSource(ints = {1_000, 10_000, 100_000})
    void givenLargeNetwork_whenNoRouteExists_thenSearchingWholeNetworkCompletesWithinCeiling(int size) {
        // given
        long buildStart = System.nanoTime();
        BranchNetwork network = generate(size);
        long buildMillis = (System.nanoTime() - buildStart) / 1_000_000;
        DijkstraRouteFinder finder = new DijkstraRouteFinder(network);
        assertThat(finder.find(id(0), id(size - 1)))
                .as("every branch is reachable from the first").isNotNull();
        for (int warmUp = 0; warmUp < 3; warmUp++) {
            assertThat(finder.find(id(0), UNREACHABLE)).isNull(); // also warms up the JIT
        }

        // when
        long bestNanos = Long.MAX_VALUE;
        for (int run = 0; run < 5; run++) {
            long start = System.nanoTime();
            finder.find(id(0), UNREACHABLE);
            bestNanos = Math.min(bestNanos, System.nanoTime() - start);
        }

        // then
        double queryMillis = bestNanos / 1_000_000.0;
        System.out.printf("size=%d build=%dms full search(best of 5)=%.2fms%n",
                size, buildMillis, queryMillis);
        assertThat(queryMillis).isLessThan(MAX_QUERY_MILLIS);
    }

    private static BranchNetwork generate(int size) {
        Random random = new Random(42L);
        List<Branch> branches = new ArrayList<>(size);
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            branches.add(new Branch(id(i), random.nextInt(100)));
            Set<Integer> targets = new TreeSet<>();
            int wanted = EXTRA_LINKS_PER_BRANCH;
            if (i + 1 < size) {
                targets.add(i + 1); // chain: guarantees the last branch is reachable
                wanted++;
            }
            wanted = Math.min(wanted, size - 1);
            while (targets.size() < wanted) {
                int target = random.nextInt(size);
                if (target != i) {
                    targets.add(target);
                }
            }
            for (int target : targets) {
                links.add(new Link(id(i), id(target)));
            }
        }
        branches.add(new Branch(UNREACHABLE, 0)); // isolated: no links in or out, so it can never be reached
        return BranchNetwork.of(branches, links);
    }

    private static String id(int index) {
        return "B" + index;
    }
}
