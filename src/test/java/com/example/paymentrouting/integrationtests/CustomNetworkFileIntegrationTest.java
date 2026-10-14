package com.example.paymentrouting.integrationtests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "payment.network.location=classpath:networks/extended-network.json")
@AutoConfigureMockMvc
class CustomNetworkFileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenNetworkFileWithNewBranch_whenGettingRoute_thenRouteUsesNewBranch() throws Exception {
        // given

        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "A").param("destination", "D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value("A,G,D"));
    }
}
