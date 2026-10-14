package com.example.paymentrouting.functionaltests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RouteApiFunctionalTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void givenRunningApplication_whenGettingRouteFromAToD_thenReturnsCheapestRouteAsJson() throws Exception {
        // when
        HttpResponse<String> response = getRoute("origin=A&destination=D");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).get().asString().startsWith("application/json");
        assertThat(response.body()).isEqualTo("{\"route\":\"A,C,E,D\"}");
    }

    @Test
    void givenRunningApplication_whenGettingRouteToUnreachableBranch_thenReturnsNullRoute() throws Exception {
        // when
        HttpResponse<String> response = getRoute("origin=E&destination=B");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"route\":null}");
    }

    @Test
    void givenRunningApplication_whenGettingRouteFromUnknownBranch_thenReturnsNotFoundProblem() throws Exception {
        // when
        HttpResponse<String> response = getRoute("origin=X&destination=D");

        // then
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).get().asString()
                .startsWith("application/problem+json");
        assertThat(response.body()).contains(
                "\"title\":\"Unknown branch\"",
                "\"status\":404",
                "\"detail\":\"Unknown branch: X\"",
                "\"instance\":\"/api/v1/routes\"");
    }

    @Test
    void givenRunningApplication_whenGettingRouteWithUrlEncodedNonAsciiId_thenReturnsBadRequestProblem()
            throws Exception {
        // when
        HttpResponse<String> response = getRoute("origin=%C3%9F&destination=D");

        // then
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).get().asString()
                .startsWith("application/problem+json");
        assertThat(response.body()).contains("\"title\":\"Invalid branch id\"");
    }

    /** Sends a real HTTP GET to the running server and returns the response with its body as text. */
    private HttpResponse<String> getRoute(String query) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/routes?" + query))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
