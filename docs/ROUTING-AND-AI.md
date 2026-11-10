# Routing, traffic and useful AI

## Implemented now

Maze builds a filtered OSM driving graph and runs bounded A* on-device. A search key includes junction, incoming way and previous junction so turn rules can distinguish approach and reversal. A line-segment grid finds the nearest supported road approach within 350 m, including long roads far from junctions. Virtual endpoints retain allowed direction, partial-edge costs and incoming turn state. A near tie (within 1 m) between distinct approaches is rejected unless both project to the same graph junction. Disconnected nearest approaches fail rather than moving endpoints to farther roads. The app asks for explicit starting and destination road choices before calculation; disconnected landmark points are not silently moved onto another road. This is not a legal-access guarantee. Via-way sequences remain unsupported: their entry approaches are conservatively blocked while member roads remain usable from other approaches. Conditional, dimensional, barrier/access ambiguities and ferries remain conservatively unsupported.

The isolated Kotlin `boundedAStar` core has two modes. **Quick** uses heuristic weight 4 and can return a longer route. **Careful** uses weight 1; if the heuristic is admissible and search completes, it minimizes stored estimated travel time on the filtered graph. It does not guarantee the real-world fastest route. Distance / 40 m/s is a lower-bound heuristic for the current builder, which caps speed at 130 km/h. If costs, speed caps or graph semantics change, revalidate that assumption.

Short trips have a 15 s / 120,000 expansion budget; trips over 50 km straight-line have 45 s / 250,000. Discovery and queue size are bounded, cancellation is checked during expansion, and budget exhaustion is distinct from a disconnected graph. Careful mode can time out where Quick succeeds. The More sheet selects search effort; refresh a current route to apply the selection.

Per-route bounded LRU caches reuse outgoing edges, node positions and turn-rule reads across incoming states. This removes duplicate SQLite work but is not a measured battery or nationwide latency improvement. Synthetic tests demonstrate optimal-vs-weighted behavior, turn-state separation, disconnected graphs, invalid costs, cancellation, time/state/discovery limits and cache eviction. Exact-device tests and benchmarks have their own scope.

## Measured graph correction · 0.4.0

The KL Sentral–KLCC fixture fell from 14.86 km to 6.34 km in Quick and Careful host runs on the same OSM snapshot. The old builder erased every member road of an unsupported via-way restriction. The replacement retains those roads and blocks the unsupported entry approaches instead. It checks all shared nodes and does not assume relation-member order. It remains conservative rather than executing complete multi-way turn sequences.

A pinned local GraphHopper 10.2 baseline uses the identical full PBF. It returns 7.63 km for the original KL points, but its stock car model snaps the tower into customer car-park access that Maze excludes. A separate request at Maze's junctions returns 5.70 km. These are different cost/access/snapping policies, not evidence that either distance is automatically fastest or legally accessible. [Reproduce the comparison and inspect all seven pairs](benchmarks/2026-11-routing/README.md).

Directed segment snapping is implemented in 0.4.2. Full via-way state, verified road-to-gate access, heading/uncertainty-aware matching and long-distance preprocessing remain next work. A better heuristic cannot recover excluded roads. Keep failing counterexamples and separate budget exhaustion from disconnection. No straight-line replacement on failure.

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

## Malaysian traffic source review · 6 November 2026

The official [data.gov.my API quickstart](https://developer.data.gov.my/quickstart) documents token-free, rate-limited public access and links realtime GTFS/GTFS-R/weather APIs; that alone is not evidence of a nationwide road-speed feed. The [LLM API landing page](https://api.llm.gov.my/) returned an under-maintenance page during review. The Ministry of Works [RTVM portal](https://rtvm.kkr.gov.my/) was identified as a road-traffic-volume source, but the direct fetch timed out; usable datasets, freshness and redistribution rights were not verified. These are research leads, not integrated feeds or a claim that no other source exists. Maze does not scrape proprietary Google/Waze/TomTom traffic or infer congestion from public-transport vehicle positions.
