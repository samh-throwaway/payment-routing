package com.example.paymentrouting.config;

import com.example.paymentrouting.algorithm.RouteFinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NetworkConfigurationTest {

    @Autowired
    RouteFinder routeFinder;

    @Test
    void givenApplicationContext_whenFindingRoute_thenUsesPackagedNetwork() {
        // when - then
        assertThat(routeFinder.find("A", "D").cost()).isEqualTo(35);
    }

    @Test
    void givenMissingNetworkFile_whenBuildingNetwork_thenThrowsNetworkLoadException() {
        // given
        NetworkConfiguration config = new NetworkConfiguration();

        // when - then
        assertThatThrownBy(() -> config.branchNetwork(new ClassPathResource("missing.json")))
                .isInstanceOf(NetworkLoadException.class)
                .hasMessageContaining("missing.json");
    }
}
