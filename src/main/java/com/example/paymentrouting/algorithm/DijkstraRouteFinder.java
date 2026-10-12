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
     * algorithm.*/
    private final BranchNetwork network;

    /**
     * Public constructor for initializing Dijkstra's algorithm. Takes
     * a non-null BranchNetwork object that contains the nodes and all
     * their costs.
     *
     * @param branchNetwork the validated, immutable BranchNetwork
     */
    public DijkstraRouteFinder(BranchNetwork branchNetwork) {
        Objects.requireNonNull(branchNetwork, "branchNetwork must not be null");
        this.network = branchNetwork;
    }

    /**
     * The main method called to traverse through the validated BranchNetwork
     * from origin to destination.
     *
     * @param origin the branch the payment starts at; must exist in the network
     * @param destination the branch the payment must reach; must exist in the network
     * @return the cheapest Route from origin to destination, or null if destination
     * never settled
     * @throws IllegalArgumentException if the passed in origin or destination
     * doesn't exist in the shared BranchNetwork
     */
    @Override
    public Route find(String origin, String destination) {
        if (!network.contains(origin)) {
            throw new IllegalArgumentException("Unknown origin branch: " + origin);
        }
        if (!network.contains(destination)) {
            throw new IllegalArgumentException("Unknown destination branch: " + destination);
        }

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingLong(QueueEntry::cost));
        Map<String, Long> cost = new HashMap<>();
        Set<String> settled = new HashSet<>();
        Map<String, String> cameFrom = new HashMap<>();

        queue.add(new QueueEntry(origin, 0));
        cost.put(origin, 0L);

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
            for (String neighbour : currentNeighbours) {
                if (settled.contains(neighbour)) {
                    continue;
                }
                Long known = cost.get(neighbour);
                if (known == null || newCost < known) {
                    cost.put(neighbour, newCost);
                    cameFrom.put(neighbour, current.branch());
                    queue.add(new QueueEntry(neighbour, newCost));
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
     * some route found so far. It is a snapshot. If a cheaper route to the
     * branch is found later, a new entry is queued and this one becomes
     * stale; it is skipped when polled.*/
    private record QueueEntry (String branch, long cost) {}
}
