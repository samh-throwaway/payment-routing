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

/**
 * An immutable, validated directed graph of branches and the links between them.
 *
 * <p>Instances can only be created through {@link #of}, which checks the whole input,
 * so any BranchNetwork that exists is valid. Each branch holds its fee and its outgoing
 * links, sorted by id so {@link #outgoing} never depends on the order of the input file.
 *
 * <p>Nothing changes after construction, so one instance can be shared between threads
 * without locking.
 */
public final class BranchNetwork {

    /**
     * A valid branch id: 1-20 characters of upper-case letters, digits or hyphen.
     * Upper case only, because request ids are upper-cased before lookup, so a
     * lower-case id in the data could never be reached.
     */
    private static final Pattern BRANCH_FORMAT = Pattern.compile("[A-Z0-9-]{1,20}");

    /** What the network stores for one branch: its fee and the ids it links to. */
    private record Node(long fee, List<String> outgoing) {
    }

    private final Map<String, Node> nodes;

    private BranchNetwork(Map<String, Node> nodes) {
        this.nodes = Map.copyOf(nodes);
    }

    /**
     * Validates the branches and the links and builds a network from them.
     * The lists are copied, so later changes to them don't affect the network.
     *
     * @param branches the branches, each with an id and a fee
     * @param links the directed links between branches, by id
     * @return a valid network
     * @throws InvalidNetworkException listing every problem found, if the input is invalid
     */
    public static BranchNetwork of(List<Branch> branches, List<Link> links) {
        List<String> localErrors = new ArrayList<>();
        Map<String, Long> fees = validateBranches(branches, localErrors);
        validateFeeTotal(fees, localErrors);
        Map<String, SortedSet<String>> validLinks = validateLinks(links, fees.keySet(), localErrors);
        if (!localErrors.isEmpty()) {
            throw new InvalidNetworkException(localErrors);
        }
        return build(fees, validLinks);
    }

    /**
     * Checks every branch and returns each branch's fee by id.
     * Problems are added to {@code errors} rather than thrown, so all of them can be
     * reported together. Ids are recorded even when invalid, so a link to such a branch
     * isn't also reported as unknown.
     */
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
                errors.add(where + ": fee " + branch.fee() + " of '" + branchId + "' must not be negative");
            }
        }

        return fees;
    }

    /**
     * Checks that the fees of all branches add up to no more than a long can hold.
     * Dijkstra only adds fees along a path that visits each branch at most once, so
     * no route cost can be larger than this total, and one check here means the
     * cost sum can never overflow while serving requests.
     */
    private static void validateFeeTotal(Map<String, Long> fees, List<String> errors) {
        long total = 0;
        for (long fee : fees.values()) {
            if (fee < 0) {
                continue; // already reported as negative
            }
            if (total > Long.MAX_VALUE - fee) {
                errors.add("the sum of all fees must not exceed " + Long.MAX_VALUE);
                return;
            }
            total += fee;
        }
    }

    /**
     * Checks every link and returns the outgoing branch ids for each branch.
     * Every problem with a link is reported; the self-link and duplicate checks
     * only run once both ends are valid.
     *
     * <p>Problems are added to {@code errors} rather than thrown. The TreeSet sorts the
     * targets by id and its {@code add} result detects duplicate links.
     */
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

            boolean fromOk = checkEndpoint(from, "from", where, knownIds, errors);
            boolean toOk = checkEndpoint(to, "to", where, knownIds, errors);

            if (!fromOk || !toOk) {
                continue;
            }

            if (from.equals(to)) {
                errors.add(where + ": branch '" + from + "' cannot link to itself");
            } else {
                SortedSet<String> outgoings = validLinks.computeIfAbsent(from, k -> new TreeSet<>());
                if (!outgoings.add(to)) {
                    errors.add(where + ": duplicate link '" + from + "' -> '" + to + "'");
                }
            }
        }
        return validLinks;
    }

    /** Combines the validated fees and links into the final immutable network.*/
    private static BranchNetwork build(Map<String, Long> fees, Map<String, SortedSet<String>> outgoing) {
        Map<String, Node> nodes = new HashMap<>();
        for (Map.Entry<String, Long> entry : fees.entrySet()) {
            String id = entry.getKey();
            SortedSet<String> targets = outgoing.getOrDefault(id, new TreeSet<>());
            nodes.put(id, new Node(entry.getValue(), List.copyOf(targets)));
        }
        return new BranchNetwork(nodes);
    }

    /**
     * Returns the fee charged when a payment leaves the given branch,
     * in whole currency units. Never negative; zero means leaving is free.
     *
     * @param id the id of the branch being left
     * @return the fee for sending a payment onward from this branch
     * @throws NullPointerException if {@code id} is null
     * @throws IllegalArgumentException if there is no branch with this id
     */
    public long fee(String id) {
        return requireNode(id).fee();
    }

    /**
     * Returns whether the network has a branch with this id. Null-safe.
     * The match is exact: no trimming or case change, because the service
     * normalises ids before looking them up.
     *
     * @param id the id to look for, may be null
     * @return true if the branch exists, false if it doesn't or {@code id} is null
     */
    public boolean contains(String id) {
        if (id == null) {
            return false;
        }
        return nodes.containsKey(id);
    }

    /**
     * Returns the ids of the branches this branch links to, sorted by id.
     * The list is unmodifiable, and empty (never null) if the branch has no
     * outgoing links.
     *
     * @param id the id of the branch the links start from
     * @return the ids of the branches that can be reached directly
     * @throws NullPointerException if {@code id} is null
     * @throws IllegalArgumentException if there is no branch with this id
     */
    public List<String> outgoing(String id) {
        return requireNode(id).outgoing();
    }

    /**
     * Returns the stored data for a branch.
     *
     * @throws NullPointerException if {@code id} is null
     * @throws IllegalArgumentException if there is no branch with this id
     */
    private Node requireNode(String id) {
        Objects.requireNonNull(id, "id must not be null");
        Node node = nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("branch with id " + id + " not found");
        }
        return node;
    }

    /** Whether {@code id} has the format every branch id must have. */
    public static boolean isValidId(String id) {
        return id != null && BRANCH_FORMAT.matcher(id).matches();
    }

    /** Checks whether the endpoint branch exists in knownIds. Returns true if it does, false if
     * id is null or knownIds doesn't contain the Branch ID.*/
    private static boolean checkEndpoint(String id, String field, String where, Set<String> knownIds,
                                         List<String> errors) {
        if (id == null) {
            errors.add(where + ": " + field + " is missing");
            return false;
        } else if (!knownIds.contains(id)) {
            errors.add(where + ": " + field + " '" + id + "' is not known");
            return false;
        }
        return true;
    }
}
