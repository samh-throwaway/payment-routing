package com.example.paymentrouting.graph;

/** A branch (vertex) of the payment network:
 * its id and the fee it charges to send a payment onward.
 *
 * <p>Raw input only. Nothing is validated here; {@link BranchNetwork#of} does that,
 * so that all problems can be reported together.</p>
 *
 * @param id the branch id, e.g. "A"
 * @param fee the fee, in whole currency units, charged when a payment leaves this branch
 */
public record Branch(String id, long fee) {}
