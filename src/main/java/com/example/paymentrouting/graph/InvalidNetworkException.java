package com.example.paymentrouting.graph;

import java.util.List;
import java.util.stream.Collectors;

/** Thrown when the branches and links used to build a {@link BranchNetwork}
 * are invalid.
 *
 * <p>Carries every problem found rather than only the first, so that
 * whoever fixes the data can correct all of them in one pass. This is
 * a startup failure, unrelated to individual payment requests.
 */
public final class InvalidNetworkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    /**
     * Creates an exception listing all the given validation errors.
     *
     * <p>The list is copied so future changes to it do not affect this exception.</p>
     *
     * @param errors the individual list of validation errors in the order they were
     *               found. Must not be null or contain nulls.
     */
    public InvalidNetworkException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    /**
     * Returns the validation errors, in the order they were found.
     *
     * @return an unmodifiable list of error messages.
     */
    public List<String> errors() {
        return errors;
    }

    /** Formats the errors as a header line followed by one indented line per error. */
    private static String buildMessage(List<String> errors) {
        String header = "Invalid branch network (" + errors.size()
                + (errors.size() == 1 ? " error):" : " errors):");
        return errors.stream()
                .map(error -> System.lineSeparator() + "  -  " + error)
                .collect(Collectors.joining("", header, ""));
    }
}
