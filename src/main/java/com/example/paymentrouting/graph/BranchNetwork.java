package com.example.paymentrouting.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class BranchNetwork {

    private record Node(long fee, List<String> outgoing) {
    }

    private final Map<String, Node> nodes;
    private static final Pattern BRANCH_FORMAT = Pattern.compile("[A-Z0-9-]{1,20}");

    private BranchNetwork(Map<String, Node> nodes) {
        this.nodes = Map.copyOf(nodes);
    }

    public static BranchNetwork of(List<Branch> branches, List<Link> links) {
        List<String> localErrors = new ArrayList<>();
        Map<String, Long> fees = validateBranches(branches, localErrors);
        Map<String, SortedSet<String>> validLinks = validateLinks(links, fees.keySet(), localErrors);
        if (!localErrors.isEmpty()) {
            throw new InvalidNetworkException(localErrors);
        }
        return build(fees, validLinks);
    }

    private static Map<String, Long> validateBranches(List<Branch> branches, List<String> errors) {
        Map<String, Long> fees = new HashMap<>();
        if (branches == null || branches.isEmpty()) {
            errors.add("branches must contain at least one branch");
            return Collections.emptyMap();
        }
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            String where = "branches[" + i + "]";
            if (branch == null) {
                errors.add(where + ": branch is missing");
                continue;
            }
            String branchId = branch.id();
            if (branchId == null) {
                errors.add(where + ": id is missing");
                continue;
            }

            if (!BRANCH_FORMAT.matcher(branchId).matches()) {
                errors.add(where + ": id '" + branchId + "' is invalid; expected 1-20 characters of A-Z," +
                        " 0-9 or '-'");
            }
            if (fees.containsKey(branchId)) {
                errors.add(where + ": duplicate branch id '" + branchId + "'");
            }

            fees.putIfAbsent(branchId, branch.fee());

            if (branch.fee() < 0) {
                errors.add(where + ": fee " + branch.fee() + " of '" + branchId + "' must not be negative.");
            }
        }

        return fees;
    }

    private static Map<String, SortedSet<String>> validateLinks(
            List<Link> links, Set<String> knownIds, List<String> errors) {
        Map<String, SortedSet<String>> validLinks = new HashMap<>();
        if (links == null) {
            errors.add("links are missing; use an empty list for a network without links");
            return Collections.emptyMap();
        }

        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);
            String where = "links[" + i + "]";
            if (link == null) {
                errors.add(where + ": link is missing");
                continue;
            }

            String from = link.from();
            String to = link.to();

            if (from == null) {
                errors.add(where + ": from is missing");
                continue;
            } else if (!knownIds.contains(from)) {
                errors.add(where + ": from '" + from + "' is not known");
                continue;
            }

            if (to == null) {
                errors.add(where + ": to is missing");
                continue;
            } else if (!knownIds.contains(to)) {
                errors.add(where + ": to '" + to + "' is not known");
                continue;
            }

            if (from.equals(to)) {
                errors.add(where + ": branch '" + from + "' cannot link to itself");
            } else {
                SortedSet<String> outgoings = validLinks.computeIfAbsent(from, k -> new TreeSet<>());
                if  (!outgoings.add(to)) {
                    errors.add(where + ": duplicate link '" + from + "' -> '" + to + "'");
                }
            }
        }
        return validLinks;
    }

    private static BranchNetwork build(Map<String, Long> fees, Map<String, SortedSet<String>> outgoing) {
        Map<String, Node> nodes = new HashMap<>();
        for (Map.Entry<String, Long> entry : fees.entrySet()) {
            String id = entry.getKey();
            SortedSet<String> targets = outgoing.getOrDefault(id, new TreeSet<>());
            nodes.put(id, new Node(entry.getValue(), List.copyOf(targets)));
        }
        return new BranchNetwork(nodes);
    }

    public long fee(String id) {
        return requireNode(id).fee();
    }

    public boolean contains(String id) {
        if (id == null) {
            return false;
        }
        return nodes.containsKey(id);
    }

    public List<String> outgoing(String id) {
        return requireNode(id).outgoing();
    }

    private Node requireNode(String id) {
        Objects.requireNonNull(id, "id must not be null");
        Node node = nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("node with id " + id + " not found");
        }
        return node;
    }
}
