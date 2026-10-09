package com.example.paymentrouting.graph;

/** A branch (vertex) of the payment network:
 * its id and the fee it sends to send a payment onward. */
public record Branch(String id, long fee) {}
