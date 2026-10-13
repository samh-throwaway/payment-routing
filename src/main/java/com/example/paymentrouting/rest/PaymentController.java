package com.example.paymentrouting.rest;

import com.example.paymentrouting.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST entry point for route lookups.
 *
 * <p>Contains no routing logic: it passes the request parameters to the
 * {@link PaymentService} and wraps the answer. Validation and error mapping
 * are the service's and {@link ApiExceptionHandler}'s job.
 */
@RestController
@RequestMapping("/api/v1/routes")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Public constructor for PaymentController.
     *
     * @param paymentService the service that finds the cheapest route
     */
    public PaymentController(PaymentService paymentService) {
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService must not be null");
    }

    /**
     * Finds the cheapest route between two branches, for example
     * {@code GET /api/v1/routes?origin=A&destination=D}.
     *
     * @param origin the branch the payment starts at
     * @param destination the branch the payment must reach
     * @return the route as comma-separated branch ids, or a null route if the
     *         destination cannot be reached (still HTTP 200: it is a valid answer)
     */
    @GetMapping
    public RouteResponse findRoute(@RequestParam String origin,
                                   @RequestParam String destination) {
        return new RouteResponse(paymentService.processPayment(origin, destination));
    }
}
