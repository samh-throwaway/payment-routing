package com.example.paymentrouting.algorithm;

import com.example.paymentrouting.graph.BranchNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Finds the cheapest route between two branches using Dijkstra's algorithm.
 *
 * <p>The result is the cheapest route because fees are never negative,
 * which {@link BranchNetwork} enforces. Returns {@code null} if the
 * destination cannot be reached from the origin.
 *
 * <p>Thread safe: all working state (the queue, costs, settled set and
 * the predecessor map) is local to each call to {@code find}, and the
 * shared network is immutable.
 */
public final class DijkstraRouteFinder implements RouteFinder {

    /** The immutable, shared, valid graph being worked on in Dijkstra's
     * algorithm. */
    private final BranchNetwork network;

    /**
     * Creates a route finder that searches the given network. The network
     * must be immutable, because it is shared by every call.
     *
     * @param branchNetwork the validated, immutable BranchNetwork
     */
    public DijkstraRouteFinder(BranchNetwork branchNetwork) {
        Objects.requireNonNull(branchNetwork, "branchNetwork must not be null");
        this.network = branchNetwork;
    }

    @Override
    public Route find(String origin, String destination) {
        if (!network.contains(origin)) {
            throw new IllegalArgumentException("Unknown origin branch: " + origin);
        }
        if (!network.contains(destination)) {
            throw new IllegalArgumentException("Unknown destination branch: " + destination);
        }

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(
                Comparator.comparingLong(QueueEntry::cost)
                .thenComparingInt(QueueEntry::hops)
                .thenComparing(QueueEntry::branch));
        Map<String, Long> cost = new HashMap<>();
        Set<String> settled = new HashSet<>();
        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Integer> hops = new HashMap<>();

        queue.add(new QueueEntry(origin, 0, 0));
        cost.put(origin, 0L);
        hops.put(origin, 0);

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.branch())) {
                // stale entry
                continue;
            }
            if (current.branch().equals(destination)) {
                break;
            }

            long newCost = current.cost() + network.fee(current.branch());

            List<String> currentNeighbours = network.outgoing(current.branch());
            int newHops = current.hops() + 1;
            for (String neighbour : currentNeighbours) {
                if (settled.contains(neighbour)) {
                    continue;
                }
                Long known = cost.get(neighbour);
                // equal cost: prefer fewer hops
                if (known == null || newCost < known || (newCost == known &&
                        newHops < hops.get(neighbour))) {
                    cost.put(neighbour, newCost);
                    cameFrom.put(neighbour, current.branch());
                    hops.put(neighbour, newHops);
                    queue.add(new QueueEntry(neighbour, newCost, newHops));
                }
            }
        }

        if (!settled.contains(destination)) {
            return null;
        }

        List<String> finalPath = new ArrayList<>();
        for (String step = destination; step != null; step = cameFrom.get(step)) {
            finalPath.add(step);
        }
        Collections.reverse(finalPath);
        return new Route(finalPath, cost.get(destination));
    }

    /** One item in the priority queue: the cost of reaching {@code branch} by
     * some route found so far. It is a snapshot. If a better route to the
     * branch is found later (cheaper, or equal cost with fewer hops), a new
     * entry is queued and this one becomes stale; it is skipped when polled.
     * {@code hops} is the number of links used to reach the branch; it breaks
     * ties between equal costs. */
    private record QueueEntry(String branch, long cost, int hops) {}
}
