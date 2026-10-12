package com.example.paymentrouting;

import com.example.paymentrouting.graph.Branch;
import com.example.paymentrouting.graph.BranchNetwork;
import com.example.paymentrouting.graph.Link;

import java.util.List;

/** Small test helper class to use a consistent network throughout tests
 * that need it. */
public final class TestNetworks {

    private static final List<Branch> SPEC_BRANCHES = List.of(
            new Branch("A", 5), new Branch("B", 50), new Branch("C", 10),
            new Branch("D", 10), new Branch("E", 20), new Branch("F", 5));

    private static final List<Link> SPEC_LINKS = List.of(
            new Link("A", "B"), new Link("A", "C"), new Link("C", "B"),
            new Link("B", "D"), new Link("C", "E"), new Link("D", "E"),
            new Link("E", "D"), new Link("D", "F"), new Link("E", "F"));

    private TestNetworks() {}

    /** Builds and returns a valid BranchNetwork as defined by the specification. */
    public static BranchNetwork spec() {
        return BranchNetwork.of(SPEC_BRANCHES, SPEC_LINKS);
    }
}
