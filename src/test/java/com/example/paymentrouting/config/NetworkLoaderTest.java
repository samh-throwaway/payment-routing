package com.example.paymentrouting.config;

import com.example.paymentrouting.algorithm.DijkstraRouteFinder;
import com.example.paymentrouting.algorithm.Route;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.graph.InvalidNetworkException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkLoaderTest {

    @Test
    void givenValidNetworkJson_whenLoading_thenBuildsNetworkWithFeesAndLinks() {
        // given
        NetworkLoader loader = new NetworkLoader(JsonMapper.builder().build());
        String json = """
                {
                  "branches": [
                    { "id": "A", "fee": 5 },
                    { "id": "B", "fee": 50 },
                    { "id": "C", "fee": 10 },
                    { "id": "D", "fee": 10 },
                    { "id": "E", "fee": 20 },
                    { "id": "F", "fee": 5 }
                  ],
                  "links": [
                    { "from": "A", "to": "B" }, { "from": "A", "to": "C" },
                    { "from": "C", "to": "B" }, { "from": "B", "to": "D" },
                    { "from": "C", "to": "E" }, { "from": "D", "to": "E" },
                    { "from": "E", "to": "D" }, { "from": "D", "to": "F" },
                    { "from": "E", "to": "F" }
                  ]
                }""";

        // when
        BranchNetwork network = loader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // then
        assertThat(network.fee("A")).isEqualTo(5);
        assertThat(network.outgoing("A")).containsExactly("B", "C");
        assertThat(network.contains("F")).isTrue();
        assertThat(network.outgoing("E")).containsExactly("D", "F");
        assertThat(network.fee("B")).isEqualTo(50);
    }

    @Test
    void givenMalformedJson_whenLoading_thenThrowsNetworkLoadException() {
        // given
        NetworkLoader loader = new NetworkLoader(JsonMapper.builder().build());
        String json = """
                {
                  "branches": [
        """;

        // when - then
        assertThatThrownBy(() ->
                loader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(NetworkLoadException.class)
                .hasMessageContaining("JSON")
                .hasCauseInstanceOf(JacksonException.class);
    }

    @Test
    void givenValidJsonDescribingInvalidNetwork_whenLoading_thenThrowsInvalidNetworkExceptionWithAllErrors() {
        // given
        NetworkLoader loader = NetworkLoader.strict();
        String json = """
            {
              "branches": [ { "id": "A", "fee": -1 } ],
              "links": [ { "from": "A", "to": "X" } ]
            }""";

        // when - then
        assertThatThrownBy(() -> loader.load(stream(json)))
                .isInstanceOf(InvalidNetworkException.class)
                .hasMessageContaining("2 errors");
    }

    @Test
    void givenBranchWithoutFee_whenLoading_thenThrowsNetworkLoadException() {
        // given
        NetworkLoader loader = NetworkLoader.strict();
        String json = """
            { "branches": [ { "id": "A" } ], "links": [] }""";

        // when - then
        assertThatThrownBy(() -> loader.load(stream(json)))
                .isInstanceOf(NetworkLoadException.class);
    }

    @Test
    void givenMissingLinksField_whenLoading_thenThrowsNetworkLoadException() {
        // given
        NetworkLoader loader = NetworkLoader.strict();
        String json = """
        { "branches": [ { "id": "A", "fee": 5 } ] }""";

        // when - then
        assertThatThrownBy(() -> loader.load(stream(json)))
                .isInstanceOf(NetworkLoadException.class);
    }

    @Test
    void givenUnknownField_whenLoading_thenThrowsNetworkLoadException() {
        // given
        NetworkLoader loader = NetworkLoader.strict();
        String json = """
            { "branches": [ { "id": "A", "fee": 5, "location": "GB" } ], "links": [] }""";

        // when - then
        assertThatThrownBy(() -> loader.load(stream(json)))
                .isInstanceOf(NetworkLoadException.class);
    }

    @Test
    void givenDecimalFee_whenLoading_thenThrowsNetworkLoadException() {
        // given
        NetworkLoader loader = NetworkLoader.strict();
        String json = """
            { "branches": [ { "id": "A", "fee": 5.5 } ], "links": [] }""";

        // when - then
        assertThatThrownBy(() -> loader.load(stream(json)))
                .isInstanceOf(NetworkLoadException.class);
    }

    @Test
    void givenNullFee_whenLoading_thenThrowsNetworkLoadException() {
        // given
        NetworkLoader loader = NetworkLoader.strict();
        String json = """
        { "branches": [ { "id": "A", "fee": null } ], "links": [] }""";

        // when - then
        assertThatThrownBy(() -> loader.load(stream(json)))
                .isInstanceOf(NetworkLoadException.class);
    }

    @Test
    void givenPackagedNetworkFile_whenLoading_thenRoutesMatchSpec() throws IOException {
        // given
        try (InputStream in = getClass().getResourceAsStream("/network.json")) {
            assertThat(in).as("network.json on the classpath").isNotNull();

            // when
            BranchNetwork network = NetworkLoader.strict().load(in);

            // then
            Route route = new DijkstraRouteFinder(network).find("A", "D");
            assertThat(route).isEqualTo(new Route(List.of("A", "C", "E", "D"), 35));
        }
    }

    private static ByteArrayInputStream stream(String json){
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
