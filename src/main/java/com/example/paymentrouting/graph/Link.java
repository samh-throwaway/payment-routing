package com.example.paymentrouting.graph;

/** A one directed edge of the payment network:
 * a payment may go from one branch to another. */
public record Link(String from, String to) {}
