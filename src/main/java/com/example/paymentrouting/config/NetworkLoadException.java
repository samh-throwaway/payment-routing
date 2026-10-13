package com.example.paymentrouting.config;

/**
 * Thrown when the network definition file cannot be read.
 *
 * <p>This means the file itself is the problem: it cannot be opened, it is not
 * valid JSON, or its structure does not match what the loader expects. It is
 * different from {@link com.example.paymentrouting.graph.InvalidNetworkException},
 * which is thrown when the file was read successfully but the network it
 * describes breaks the rules (for example a negative fee or a link to an
 * unknown branch).
 *
 * <p>Unchecked, and it always carries the underlying cause (a parser error,
 * or the failure to open the file).
 */
public final class NetworkLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message and the underlying cause.
     *
     * @param message the custom message related to the error
     * @param cause the actual cause of the exception thrown
     */
    public NetworkLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
