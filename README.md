# Payment Routing

Finds the cheapest route for a payment between two bank branches, and exposes
it through a small REST API.

## The problem in one paragraph

Branches are connected by directed links. A branch charges a fee when a payment
leaves it, receiving is free. The cost of a route is the sum of the fees of
every branch on it except the destination. A payment from a branch to itself
costs nothing. Given an origin and a destination, return the cheapest sequence
of branches, or nothing if the destination cannot be reached.

The example network from the brief:

| Branch | Fee |   | Links |
|---|---|---|---|
| A | 5 | | A->B, A->C, C->B, B->D |
| B | 50 | | C->E, D->E, E->D |
| C | 10 | | D->F, E->F |
| D | 10 | | |
| E | 20 | | |
| F | 5 | | |

For example, A to D is `A,C,E,D` at a cost of 35 (5 + 10 + 20).

## Running it

Requires Java 17. On Windows use `mvnw.cmd` instead of `./mvnw`.

```
./mvnw test                       # run the tests (135)
./mvnw spring-boot:run            # start the API on http://localhost:8080
./mvnw test -Dgroups=performance -DexcludedGroups=none   # also run the timing test
```

## The API

```
GET /api/v1/routes?origin=A&destination=D
```

| Case | Status | Body |
|---|---|---|
| Route found | 200 | `{"route":"A,C,E,D"}` |
| Destination not reachable | 200 | `{"route":null}` |
| Missing or malformed branch id | 400 | problem-details JSON |
| Well-formed id that is not in the network | 404 | problem-details JSON |

Branch ids are trimmed and upper-cased, so `origin=%20a%20` means `A`. A valid id
is 1 to 20 characters of `A-Z`, `0-9` or `-`.

Example error:

```json
{
  "detail": "Unknown branch: X",
  "instance": "/api/v1/routes",
  "status": 404,
  "title": "Unknown branch"
}
```

The Java entry point required by the brief is
`PaymentService.processPayment(origin, destination)`. It returns the route as a
comma separated string such as `"A,C,E,D"`, or `null` if there is no route.

## Changing the network

The network is data, not code. Edit `src/main/resources/network.json` (or point
`payment.network.location` at another file) and restart. No Java changes are
needed to add a branch or a link.

```json
{
  "branches": [ { "id": "A", "fee": 5 } ],
  "links": [ { "from": "A", "to": "B" } ]
}
```

The file is read strictly and the application refuses to start on a bad one:

- Unknown fields, a missing `fee`, a `null` fee, or a decimal fee such as `5.5`
  are rejected.
- Rule violations (negative fee, duplicate branch, a link to an unknown branch,
  a link from a branch to itself, and so on) are all reported together, not one
  at a time.

## How it works

- `graph`: `BranchNetwork` is an immutable, validated graph.
  It checks itself when it is built, so an invalid network cannot exist.
- `algorithm`: `DijkstraRouteFinder` finds the cheapest route. Fees cannot be
  negative, which is what makes Dijkstra correct here.
- `service`: `CheapestRoutePaymentService` validates the ids and calls the
  finder.
- `rest`: the controller and the error handler.
- `config`: loads `network.json` at startup and wires everything together.

Equal-cost routes are broken in a fixed order: fewest hops first. 
Any tie left after that is settled the same way every time, 
because the search settles branches in a fixed order (cost, then hops, then branch name).

## How it scales

Two different things can grow: the size of the network, and the number of
requests at once.

**Bigger network.** One request runs Dijkstra with a priority queue, which is
roughly O((V + E) log V) for V branches and E links. The timing test measures the
worst case: a destination that cannot be reached, so the whole network is
searched. It uses random networks with about 6 links per branch, a warm-up, and
the best of 5 runs:

| Branches | Network build | One worst-case lookup |
|---|---|---|
| 1,000 | 20 ms | 2.6 ms |
| 10,000 | 69 ms | 16.4 ms |
| 100,000 | 456 ms | 266.6 ms |

100 times more branches costs about 100 times longer, which is consistent with
O((V + E) log V). This is a rough timing on one laptop, not a rigorous benchmark.

**More requests at once.** Nothing is locked. The network never changes after
startup, and each request keeps its own working data (queue, costs, visited set)
in local variables, so requests cannot affect each other. Adding locks or
concurrent collections would only make requests wait for each other.

**If it had to go further:** cache answers for popular routes (safe because the
network is fixed), or, if the network had to change at runtime, build a new
immutable network and swap one reference atomically.

## Requirements and the tests that cover them

| Requirement from the brief | Where it is tested                                                                                                       |
|---|--------------------------------------------------------------------------------------------------------------------------|
| Cheapest route as a comma separated string, or null | `DijkstraRouteFinderTest`, `CheapestRoutePaymentServiceTest`, `RouteApiEndToEndTest`                                     |
| Directed links, fee on leaving, destination is free, A to A costs 0 | `DijkstraRouteFinderTest` (the example network cases), `BranchNetworkTest`                                               |
| REST API with input validation | `PaymentControllerTest`, `RouteApiEndToEndTest`, `BranchIdsTest`                                                         |
| Add branches and links without code changes | `NetworkLoaderTest` (including the packaged `network.json`), `BranchNetworkTest`                                         |
| Consider performance and show how it scales | `RoutingPerformanceTest` (tagged `performance`), table above                                                             |
| Thread safe | `PaymentServiceConcurrencyTest`, the repeated-call test in `DijkstraRouteFinderTest`, `BranchNetworkTest` (immutability) |
| Functional tests | All of the above, 135 tests in the default run                                                                           |
| Library assessment | Libraries section                                                                                                        |

## Libraries

All of these come through Spring Boot's dependency management, with no extra
libraries added.

| Library | Version | Used for | Licence |
|---|---|---|---|
| Spring Boot (web) | 4.1.1 | REST API, wiring, configuration | Apache 2.0 |
| Spring Framework | 7.0.9 | Underlies Spring Boot | Apache 2.0 |
| Jackson | 3.1.5 | Reading `network.json`, writing JSON responses | Apache 2.0 |
| Tomcat (embedded) | 11.0.24 | Web server | Apache 2.0 |
| JUnit Jupiter | 6.0.3 | Tests | EPL 2.0 |
| AssertJ | 3.27.7 | Test assertions | Apache 2.0 |
| Mockito | 5.23.0 | Test mocks (controller test only) | MIT |

- All versions come from Spring Boot's managed dependencies, so they're tested together.
- All are widely used, actively maintained, and under standard open-source licences.
- No libraries were added beyond Spring Boot's own.

Considered and left out:

- A graph library (for example JGraphT). Dijkstra is about 60 lines here, and I
  need my own tie-breaking rule. Writing it keeps the code small and fully
  explainable.
- Lombok. Java records already remove the boilerplate it would.
