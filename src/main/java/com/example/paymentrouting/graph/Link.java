package com.example.paymentrouting.graph;

/**
 * A directed link of the payment network: a payment may be sent from one
 * branch to another. A link in the opposite direction must be declared
 * separately.
 *
 * <p>Raw input only. Validated by {@link BranchNetwork#of}, not here.</p>
 *
 * @param from the id of the sending branch
 * @param to the id of the receiving branch
 */
public record Link(String from, String to) {}
