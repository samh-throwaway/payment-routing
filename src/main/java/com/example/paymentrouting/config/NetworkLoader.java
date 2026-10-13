package com.example.paymentrouting.config;

import com.example.paymentrouting.graph.Branch;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.graph.InvalidNetworkException;
import com.example.paymentrouting.graph.Link;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/** Reads a JSON network definition and builds a validated
 * {@link BranchNetwork}. */
public final class NetworkLoader {

    private final JsonMapper mapper;

    /**
     * Creates a loader that parses network files with the given mapper.
     *
     * @param mapper the mapper used to read the file
     */
    public NetworkLoader(JsonMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Reads a network definition from {@code in} and builds a validated network.
     * Does not close the stream.
     *
     * @param in the JSON network definition to read
     * @return a validated BranchNetwork
     * @throws InvalidNetworkException if the content describes an invalid network
     * @throws NetworkLoadException if the input is not valid JSON or does not
     * match the expected structure
     */
    public BranchNetwork load(InputStream in) {
        NetworkFile file;
        try {
            file = mapper.readValue(in, NetworkFile.class);
        } catch (JacksonException e) {
            throw new NetworkLoadException("Network file could not be read as JSON: "
                    + e.getOriginalMessage(), e);
        }
        return BranchNetwork.of(file.branches(), file.links());
    }

    /** A loader that rejects unknown fields, missing fields, a null or missing fee,
     * and a fee written as a decimal. */
    public static NetworkLoader strict() {
        return new NetworkLoader(JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .build());
    }

    /** Shape of the network.json file which includes the graph branches and links.
     * Raw input only; {@link BranchNetwork#of} validates. */
    private record NetworkFile(List<Branch> branches, List<Link> links) {}
}
