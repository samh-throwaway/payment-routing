package com.example.paymentrouting.service;

/**
 * The payment operation offered to callers: given an origin and a destination,
 * return the cheapest route.
 *
 * <p>Implementations must be thread safe.
 */
public interface PaymentService {

    /**
     * Finds the cheapest route for a payment between two branches.
     *
     * @return the branch ids in order, comma separated (for example "A,C,E,D"),
     *         or null if the destination cannot be reached
     * @throws InvalidBranchException if an id is missing or malformed
     * @throws UnknownBranchException if an id is well-formed but not in the network
     */
    String processPayment(String origin, String destination);
}
