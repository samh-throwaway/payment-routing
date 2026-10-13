package com.example.paymentrouting.config;

import com.example.paymentrouting.algorithm.DijkstraRouteFinder;
import com.example.paymentrouting.algorithm.RouteFinder;
import com.example.paymentrouting.graph.BranchNetwork;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration(proxyBeanMethods = false)
public class NetworkConfiguration {

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
}
