package com.example.paymentrouting.algorithm;

/**
 * Finds the cheapest route between two branches of a network.
 *
 * <p>Implementation must be safe to call from many threads at once.
 */
public interface RouteFinder {

    /**
     * Finds the cheapest route from {@code origin} to {@code destination}.
     *
     * <p>Among equal-cost routes, the one with fewer hops wins; remaining ties
     * are broken by branch name.
     *
     * @param origin the branch the payment starts at; must exist in the network
     * @param destination the branch the payment must reach; must exist in the network
     * @return the cheapest route, or {@code null} if destination cannot be reached
     * @throws IllegalArgumentException if either branch is not in the network
     */
    Route find(String origin, String destination);
}
