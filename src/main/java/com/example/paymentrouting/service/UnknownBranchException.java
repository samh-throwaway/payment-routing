package com.example.paymentrouting.service;

/**
 * Thrown when a branch id is well-formed but does not exist in the network.
 * Maps to HTTP 404.
 */
public final class UnknownBranchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an UnknownBranchException with the branch ID not found.
     *
     * @param branchId the well-formed id that was not found in the network
     */
    public UnknownBranchException(String branchId) {
        super("Unknown branch: " + branchId);
    }
}
