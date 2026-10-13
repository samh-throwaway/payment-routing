package com.example.paymentrouting.rest;

import com.example.paymentrouting.service.InvalidBranchException;
import com.example.paymentrouting.service.UnknownBranchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions thrown by the service layer into JSON error responses.
 *
 * <p>An invalid branch id gives HTTP 400 and an unknown branch gives HTTP 404.
 * Responses use Spring's {@code ProblemDetail}, a standard error format with a
 * title, a status and a detail message.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** A missing or malformed branch id is the caller's mistake: HTTP 400. */
    @ExceptionHandler(InvalidBranchException.class)
    public ProblemDetail handleInvalidBranch(InvalidBranchException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid branch id");
        return problem;
    }

    /** A well-formed id that is not in the network does not exist: HTTP 404. */
    @ExceptionHandler(UnknownBranchException.class)
    public ProblemDetail handleUnknownBranch(UnknownBranchException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Unknown branch");
        return problem;
    }
}
