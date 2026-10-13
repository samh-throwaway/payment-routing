package com.example.paymentrouting.rest;

import com.example.paymentrouting.service.InvalidBranchException;
import com.example.paymentrouting.service.PaymentService;
import com.example.paymentrouting.service.UnknownBranchException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void givenReachableBranches_whenGettingRoute_thenReturnsRoute() throws Exception {
        // given
        when(paymentService.processPayment("A", "D")).thenReturn("A,C,E,D");

        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "A").param("destination", "D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value("A,C,E,D"));
    }

    @Test
    void givenNoRoute_whenGettingRoute_thenReturnsOkWithNullRoute() throws Exception {
        // given
        when(paymentService.processPayment("E", "B")).thenReturn(null);

        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "E").param("destination", "B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value(nullValue()));
    }

    @Test
    void givenInvalidBranchId_whenGettingRoute_thenReturnsBadRequestProblem() throws Exception {
        // given
        when(paymentService.processPayment("!", "D"))
                .thenThrow(new InvalidBranchException("branch id must be 1-20 characters of A-Z, 0-9 or '-'"));

        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "!").param("destination", "D"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid branch id"));
    }

    @Test
    void givenUnknownBranch_whenGettingRoute_thenReturnsNotFoundProblem() throws Exception {
        // given
        when(paymentService.processPayment("X", "D")).thenThrow(new UnknownBranchException("X"));

        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "X").param("destination", "D"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Unknown branch: X"));
    }

    @Test
    void givenMissingDestination_whenGettingRoute_thenReturnsBadRequest() throws Exception {
        // when - then
        mockMvc.perform(get("/api/v1/routes").param("origin", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void givenPostRequest_whenCallingRoutes_thenReturnsMethodNotAllowed() throws Exception {
        // when - then
        mockMvc.perform(post("/api/v1/routes").param("origin", "A").param("destination", "D"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
