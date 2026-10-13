package com.example.paymentrouting.service;

/**
 * Thrown when a branch id is missing or malformed, before the network is
 * consulted. Maps to HTTP 400.
 */
public final class InvalidBranchException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an InvalidBranchException with the reasoning.
     *
     * @param message what is wrong with the id; must not repeat the raw input
     */
    public InvalidBranchException(String message) {
        super(message);
    }
}
