package com.example.paymentrouting.rest;

/** Body of a route lookup. {@code route} is null when no route exists.
 *
 * @param route the branch ids in order, comma separated, or null if unreachable. */
public record RouteResponse(String route) {}
