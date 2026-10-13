package com.example.paymentrouting.config;

import com.example.paymentrouting.algorithm.DijkstraRouteFinder;
import com.example.paymentrouting.algorithm.RouteFinder;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.service.CheapestRoutePaymentService;
import com.example.paymentrouting.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration(proxyBeanMethods = false)
public class NetworkConfiguration {

    /**
     * Spring wiring: builds the network from the configured JSON file at startup
     * and exposes the route finder and payment service as beans.
     *
     * <p>The file location comes from {@code payment.network.location}, defaulting
     * to {@code classpath:network.json}. A bad file stops the application starting.
     */
    @Bean
    public BranchNetwork branchNetwork(
            @Value("${payment.network.location:classpath:network.json}") Resource location) {
        try (InputStream in = location.getInputStream()) {
            return NetworkLoader.strict().load(in);
        } catch (IOException e) {
            throw new NetworkLoadException(
                    "Network file could not be opened: " + location, e);
        }
    }

    @Bean
    public RouteFinder routeFinder(BranchNetwork network) {
        return new DijkstraRouteFinder(network);
    }

    @Bean
    public PaymentService paymentService(BranchNetwork network, RouteFinder routeFinder) {
        return new CheapestRoutePaymentService(network, routeFinder);
    }
}
