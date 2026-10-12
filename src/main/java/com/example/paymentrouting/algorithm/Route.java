package com.example.paymentrouting.algorithm;

import java.util.List;

/**
 * The cheapest route found between two branches.
 *
 * <p>Immutable. {@code branches} lists the branch ids in travel order, starting
 * with origin and ending with the destination. It is never empty. {@code cost}
 * is the sum of fees of every branch on the route except the destination
 * (receiving is free).</p>
 *
 * @param branches the branch ids from origin to destination, in order
 * @param cost the total fees paid along the route, never negative
 */
public record Route(List<String> branches, long cost) {

    public Route {
        branches = List.copyOf(branches);
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("a route must contain at least one branch");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative");
        }
    }
}
