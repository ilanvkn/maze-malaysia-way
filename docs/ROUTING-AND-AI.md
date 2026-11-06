# Routing, traffic and useful AI

## Implemented now

Maze builds a filtered OSM driving graph and runs bounded A* on-device. A search key includes junction, incoming way and previous junction so turn rules can distinguish approach and reversal. Directed strong-component pairing finds supported endpoint snaps within 350 m; it is not a legal-access guarantee. Via-way, conditional, dimensional, barrier/access ambiguities and ferries remain conservatively unsupported.

The isolated Kotlin `boundedAStar` core has two modes. **Quick** uses heuristic weight 8 and can return a longer route. **Careful** uses weight 1; if the heuristic is admissible and search completes, it minimizes stored estimated travel time on the filtered graph. It does not guarantee the real-world fastest route. Distance / 40 m/s is a lower-bound heuristic for the current builder, which caps speed at 130 km/h. If costs, speed caps or graph semantics change, revalidate that assumption.

Short trips have a 15 s / 120,000 expansion budget; trips over 50 km straight-line have 45 s / 250,000. Discovery and queue size are bounded, cancellation is checked during expansion, and budget exhaustion is distinct from a disconnected graph. Careful mode can time out where Quick succeeds. The More sheet selects search effort; refresh a current route to apply the selection.

Per-route bounded LRU caches reuse outgoing edges, node positions and turn-rule reads across incoming states. This removes duplicate SQLite work but is not a measured battery or nationwide latency improvement. Synthetic tests demonstrate optimal-vs-weighted behavior, turn-state separation, disconnected graphs, invalid costs, cancellation, time/state/discovery limits and cache eviction. Exact-device tests and benchmarks have their own scope.

## First fix the known detours

A better A* cannot restore roads excluded during graph building or fix a bad endpoint snap. Keep the KL Sentral–KLCC detour as a failing quality example, not a success badge. Next compare Quick/Careful and a mature routing-engine baseline on identical source data, examine excluded connectors and turn rules, and measure endpoint snap distance alongside route cost. No straight-line replacement on failure.

Valhalla's [core](https://github.com/valhalla/valhalla) and [mobile integration](https://github.com/Rallista/valhalla-mobile) are candidates, not dependencies already integrated. Licensing, Android ABI/build size, extract preparation, restrictions, memory, cold-start and on-device results need a bounded comparison before migration. CH/MLD/hierarchical A* may improve long-distance queries but add preprocessing, update and turn-cost complexity; select them from evidence rather than a list of algorithm names.

## Traffic needs observations

There is **no live traffic in Maze today**. OSM speed estimates are not congestion. We have no national sensor feed, paid provider contract or volunteer probe collection. Offline-first traffic would require an explicitly downloaded, timestamped data pack with a stated geographic scope and expiry; stale/missing data must fall back visibly to static estimates.

A future feed must identify redistribution rights, segment matching, observation time/age, confidence, incident direction, validation and abuse controls. Anonymous-looking movement traces can still expose people; do not begin collecting trips through a generic feature flag. Any opt-in probe design requires a separate public privacy proposal and review. [Valhalla historical-traffic documentation](https://valhalla.github.io/valhalla/concepts/historical-traffic/) describes engine inputs, not a free Malaysian traffic feed.

## AI has specific jobs to earn

The local MCP tools already let an assistant retrieve mapped names and access points with source attribution. They do not give an LLM authority to edit maps or guess routes. Client privacy matters: a local stdio service does not make an external model local.

Candidate improvements, in order:

| Problem | Baseline first | Candidate | Evidence before shipping |
|---|---|---|---|
| Malay/English/Chinese/Tamil aliases and misspellings | Prefix/alias/token ranking | Small on-device multilingual reranker | Human-reviewed query set, Recall@20/MRR by language, no invented result IDs, APK/RAM/latency budget |
| GPS road ambiguity | Accuracy-aware nearest candidates | Probabilistic map matching | Consented labelled traces, wrong-road rate, uncertainty calibration, loop/tunnel cases |
| ETA bias | Static speed estimates by road class | Calibrated regression | Licensed observed travel times, geography/time held-out split, MAE/p90 and stale-data fallback |
| Incident reports | Human-reviewed structured reports | Duplicate clustering or language parsing | Abuse/error rate, multilingual review, no automatic closures or route penalties from unverified text |

No weights are bundled and no ML quality, traffic or fastest-route improvement has been measured. Proposed models must have compatible code/weight/data licenses, a small deployable footprint and a documented baseline they actually beat. Model-generated roads, entrances and traffic are never acceptable ground truth.
