package com.example.paymentrouting.service;

/**
 * Thrown when a branch id is well-formed but does not exist in the network.
 * Maps to HTTP 404. Carries the offending id (already validated, so safe to
 * show) via {@link #branchId()}.
 */
public final class UnknownBranchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String branchId;

    /**
     * Creates an UnknownBranchException with the branch ID not found.
     *
     * @param branchId the well-formed id that was not found in the network
     */
    public UnknownBranchException(String branchId) {
        super("Unknown branch: " + branchId);
        this.branchId = branchId;
    }

    /** @return the id that was not found in the network. */
    public String branchId() {
        return branchId;
    }
}
