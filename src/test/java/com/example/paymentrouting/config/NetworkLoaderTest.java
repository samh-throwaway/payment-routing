package com.example.paymentrouting.config;

import com.example.paymentrouting.graph.BranchNetwork;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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
}
