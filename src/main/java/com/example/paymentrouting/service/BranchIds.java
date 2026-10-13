package com.example.paymentrouting.service;

import com.example.paymentrouting.graph.BranchNetwork;

import java.util.Locale;

/**
 * Turns a raw branch id from outside the application into the recognized form
 * used inside the network, or rejects it.
 *
 * <p>Trims, upper-cases (locale independent) and checks the id format. The
 * format rule itself lives in {@link BranchNetwork#isValidId}, so there is one
 * definition of a valid id. Error messages do not repeat the input, because it
 * is untrusted and can be arbitrarily long.
 *
 * <p>Stateless utility class. Used by {@link CheapestRoutePaymentService}.
 */
public final class BranchIds {

    private BranchIds() {
    }

    /**
     * Converts a raw branch id from outside the application to the recognized
     * form used inside the network.
     *
     * @param raw the id as received, possibly padded or in lower case
     * @return the trimmed, upper-cased id, guaranteed to have a valid format
     * @throws InvalidBranchException if the id is null, blank, or not 1-20
     *         characters of A-Z, 0-9 or '-'
     */
    public static String normalise(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidBranchException("branch id is required");
        }
        String id = raw.strip().toUpperCase(Locale.ROOT);
        if (!BranchNetwork.isValidId(id)) {
            throw new InvalidBranchException(
                    "branch id must be 1-20 characters of A-Z, 0-9 or '-'");
        }
        return id;
    }
}
