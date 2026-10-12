package com.example.paymentrouting.config;

import com.example.paymentrouting.graph.Branch;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.graph.InvalidNetworkException;
import com.example.paymentrouting.graph.Link;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/** Reads a JSON network definition and builds a validated
 * {@link BranchNetwork}. */
public final class NetworkLoader {

    private final JsonMapper mapper;

    /**
     * Public constructor for building the NetworkLoader object.
     *
     * @param mapper the mapper used to read the file
     */
    public NetworkLoader(JsonMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Responsible for taking in the JSON network definition to read the contents
     * to branches and links that are used to build the BranchNetwork.
     *
     * @param in the input file with contents to parse
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

    /** Shape of the network.json file which includes the graph branches and links.
     * Raw input only; BranchNetwork#of validates.*/
    private record NetworkFile(List<Branch> branches, List<Link> links) {}
}
