package com.example.paymentrouting.service;

import com.example.paymentrouting.algorithm.Route;
import com.example.paymentrouting.algorithm.RouteFinder;
import com.example.paymentrouting.graph.BranchNetwork;

import java.util.Objects;

/**
 * Finds the cheapest payment route using a {@link RouteFinder} over a
 * {@link BranchNetwork}.
 *
 * <p>Validates both ids (format first, then existence), so callers get a
 * meaningful {@link InvalidBranchException} or {@link UnknownBranchException}
 * instead of a failure from deeper layers. Thread safe: holds only two final,
 * immutable-or-stateless collaborators and keeps no per-request state.
 *
 * <p>Used by the REST controller.
 */
public final class CheapestRoutePaymentService implements PaymentService {

    private final BranchNetwork network;
    private final RouteFinder routeFinder;

    /**
     * Creates the service from the network it checks ids against and the finder it delegates to.
     *
     * @param network the branch network, used to check that ids exist
     * @param routeFinder the algorithm that finds the cheapest route
     */
    public CheapestRoutePaymentService(BranchNetwork network, RouteFinder routeFinder) {
        this.network = Objects.requireNonNull(network, "network must not be null");
        this.routeFinder = Objects.requireNonNull(routeFinder, "routeFinder must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Both ids are normalised and format-checked before either is looked
     * up, so a malformed id is always reported ahead of an unknown one.
     */
    @Override
    public String processPayment(String origin, String destination) {
        String from = BranchIds.normalise(origin);
        String to = BranchIds.normalise(destination);
        requireKnown(from);
        requireKnown(to);

        Route route = routeFinder.find(from, to);
        return route == null ? null : String.join(",", route.branches());
    }

    /** Fails with {@link UnknownBranchException} if {@code id} is not in the network. */
    private void requireKnown(String id) {
        if (!network.contains(id)) {
            throw new UnknownBranchException(id);
        }
    }
}