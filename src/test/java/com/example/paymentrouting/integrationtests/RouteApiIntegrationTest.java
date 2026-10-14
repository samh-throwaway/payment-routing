package com.example.paymentrouting.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RouteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "A | D | A,C,E,D",
            "A | F | A,C,E,F",
            "C | E | C,E",
            "C | D | C,E,D",
            "A | A | A",
            "' a ' | d | A,C,E,D"
    })
    void givenSpecNetwork_whenGettingRoute_thenReturnsCheapestRoute(
            String origin, String destination, String expected) throws Exception {
        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", origin).param("destination", destination))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value(expected));
    }

    @Test
    void givenUnreachableDestination_whenGettingRoute_thenReturnsNullRoute() throws Exception {
        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "E").param("destination", "B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value(nullValue()));
    }

    @Test
    void givenUnknownBranch_whenGettingRoute_thenReturnsNotFound() throws Exception {
        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "X").param("destination", "D"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A B", "A,B", "ABCDEFGHIJKLMNOPQRSTU"})
    void givenInvalidOrigin_whenGettingRoute_thenReturnsBadRequest(String origin) throws Exception {
        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", origin).param("destination", "D"))
                .andExpect(status().isBadRequest());
    }
}
