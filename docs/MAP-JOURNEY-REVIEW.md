# Map journeys · first comparative review

10 November 2026. This is a bounded review, not a ranking of every map app. Maze source baseline: `cc05f4fd0896ba06256f838311e8fb2b5d6efbd0`. Competitor web observations used Chromium 157.0.8103.40, empty temporary profiles, 1280×720 and English. No accounts, real journeys, location permission, report submissions, proprietary traffic extraction or native competitor installations were used. Each owned browser was terminated. Native mobile behaviour and Malaysian offline coverage require separate device checks.

## What we learned

| Reference | Evidence and scope | Lesson for Maze |
|---|---|---|
| Waze Live Map | Observed separate start/destination fields, Leave now, arrival-time explanation and community links. A typed KLCC query remained in the field; no suggestion/result appeared in the captured four-second window. No completed route comparison. | Keep journey controls immediate. Never imitate departure-time predictions without historical/live observations. A silent search needs a visible empty/loading/error state. |
| Google Maps web | Observed search, Directions and Saved on entry. Submitting `KLCC Kuala Lumpur` selected the **Kuala Lumpur City Centre district**, with a limited-view notice. This is one observed resolution, not a general accuracy verdict. | Test ambiguous local aliases. District, towers, mall, entrance and car approach need distinct identities and clear result context. |
| Apple Maps web | Observed search/directions links, location, zoom and map-type controls. Directions exposed Drive/Walk/Cycle, two fields, Now and Avoid; Add Stop was disabled before endpoints. No route submitted. | Mode and avoidance controls are promises. Only expose modes backed by routing data and tests; Maze currently supports a generic car preview. |
| Organic Maps | [Project documentation](https://github.com/organicmaps/organicmaps) describes offline search, car/walking/cycling guidance and community source. Documentation only, not a tested native install. | Open source and offline availability already have mature precedents. Maze needs demonstrably better Malaysian journeys, not those labels alone. |
| OsmAnd | [Routing documentation](https://www.osmand.net/docs/user/navigation/routing/osmand-routing/) describes offline map-based calculation and configurable profiles. Documentation only. | Separate vehicle parameters from visitor/delivery purpose. Do not imply truck routing from a delivery gate. |
| HERE WeGo | [Official offline help](https://help.here.com/faq/offline_maps) explains downloading map areas before offline navigation. Documentation only. | Map preparation, storage and recovery are part of the journey, not a setup afterthought. |

[Google's offline help](https://support.google.com/maps/answer/6291838?hl=en) distinguishes offline driving from unavailable traffic/alternative routes. [Waze's FAQ](https://www.waze.com/faq) states an internet dependency for initial routing and live updates. [Apple's offline documentation](https://support.apple.com/en-gb/105084) describes downloaded maps and regional availability limits. These documents are not evidence that every Malaysian area has been downloaded or tested. Google's [destination API documentation](https://developers.google.com/maps/documentation/geocoding/search-for-destinations) already models entrances and navigation points; simply storing entrance pins is not a unique feature.

## Improvement made from this review

The development journey card labels time **Est.** and shows the straight-line gap between the displayed road geometry's final point and the selected place. It keeps **final access unverified** visible in both compact and expanded cards, including saved previews. A zero gap still does not assert permission or a usable entrance. No connector, walking directions or invented road is drawn. This improves disclosure; it does not repair endpoint snapping.

The published [Gate Pilot reviewer APK](https://github.com/ilanvkn/maze-malaysia-way/releases/tag/v0.4.1-gate-pilot.1) is the earlier fixed build. The later [0.4.2 road-approach preview](benchmarks/2026-11-approaches/README.md) includes this card change and separately qualified snapping/road-choice work. The original Gate Pilot APK remains fixed.

## Next engineering work, in order

| Priority | Concrete work | Acceptance before shipping |
|---|---|---|
| 1 · correct approach | Replace nearest-junction endpoint selection with directed nearest-segment candidates and partial-edge costs. Keep access and turn restrictions; do not connect across a barrier because it is nearby. | Synthetic one-way/parallel carriageway/divided-road/barrier/no-entry/same-edge cases; legal approach and visible geometry checked against the same pinned OSM snapshot and declared GraphHopper car profile. Retain inaccessible counterexamples. No route if a supported approach cannot be established. |
| 2 · destination clarity | Preserve place type/source and disambiguate KLCC district, towers, mall and access point; review Malay/English/Chinese/Tamil aliases with humans. | A consent-free public query set, labelled expected IDs, Recall@20/MRR by language, cold/warm on-device latency. Do not fabricate a relevance score or auto-promote a gate to a building entrance. |
| 3 · offline preparation | Smaller independently verified regional downloads with explicit coverage, source date, required space and recovery after interruption. | Wrong/stale pack, insufficient space, cancellation, process death and failed replacement leave a usable previous pack. Measure actual installed/staging sizes. Current national ZIP and import remain supported. |
| 4 · arrival evidence | Connect permission-cleared gate records to a supported approach, building/site and vehicle constraints. Give an operator a correction/recheck workflow. | One consenting site, 10–20 checked gates, documented provenance and update owner. Compare correct-gate arrivals and delay with a counterbalanced consented field trial. No automatic verification through payment or an imported flag. |
| 5 · navigation readiness | Define guidance state, rerouting, maneuvers, uncertainty, lifecycle and driver-safe interaction before a Start navigation button. | Labelled real traces and physical-device trials, wrong-road rate, missed-turn recovery, foreground/background policy, battery and thermal measures. A road-name list is not turn guidance. |
| 6 · traffic / ML | Evaluate a licensed timestamped traffic source or multilingual reranker only against a stated baseline. | Rights, expiry and missing-feed behaviour; held-out measurements. Never label static road speeds as current congestion or use LLM-generated restrictions. |

Regional packages and an improved maintenance flow are proposals, not implemented capabilities. No new network service, account, telemetry, AI model or paid infrastructure was added.

## What-if scenarios and actual proof

The WhatIF source-review tool was run against the Kotlin app directory. It selected **zero files**, so its empty finding list establishes nothing about Kotlin correctness or security. Its built-in HTTP load runner is inappropriate for this local native app; no dummy server or external map-provider load test was substituted. The matrix below uses actual JVM cases and explicitly identifies unexecuted native/field work.

| What if… | Evidence / status |
|---|---|
| A heuristic favours a detour or hits a trap? | `MazeAStarTest`: weighted vs admissible search, independent all-pairs baseline and bounded heuristic trap. JVM policy checks, not nationwide route qualification. |
| Two approaches reach the same junction with different turn rights? | `MazeAStarTest` incoming-state case and `MazeLocalRoutingTest` forbidden/only/U-turn cases. Full via-way sequences remain unsupported. |
| A route times out, is cancelled or has no connected path? | `MazeAStarTest` distinguishes budgets/cancellation/disconnection. End-to-end process-death recovery remains a separate device task. |
| GPS is stale, future-dated, missing accuracy or approximate-only? | `MazeLocationPolicyTest` rejects invalid fixes and keeps GPS out of approximate-only provider selection. No new physical GNSS trial. |
| A public entrance is pedestrian-only; a visitor selects Delivery or Staff? | `MazeGateTest` checks motorcar separately and never grants staff permission from a selector. |
| A gate check is 30/31 days old or in the future? | `MazeGateTest` boundary checks. Device date is still unauthenticated; operator recheck policy unvalidated. |
| A local import is oversized, deeply nested, duplicate or concatenated? | `MazeGateTest` parser checks. Prior fixed-APK native import/cancel/restart evidence is in `GATE-PILOT-RESULTS.md`; not repeated here. |
| Two sites use gate ID A? | `MazeGateTest` site-specific saved identity check. |
| A displayed road ends short of the selected place? | New `MazeDataTest` checks the final geometry point, a 111 m gap, zero gap retaining the access caveat and absent geometry. No UI pixel claim from this test. |
| A backend responds 429/503 or sends an invalid route? | `MazeRecoveryTest` bounded loopback fixture checks. This repository path is legacy/optional and is not the current on-device router or a capacity benchmark. |
| A saved preview is corrupted? | `MazeRecoveryTest` checksum/codec test. Native restoration and concurrent file operations need separate scoped checks. |
| Users need an inaccessible gate, an island/ferry crossing or a wrong-side road? | Acceptance fixtures for the next routing iteration; this review does not mark these complete from unit tests. |

Before the card change, all 38 JVM tests were freshly rerun successfully. After the change, **39 JVM tests, Lab lint and the minified QA build passed** using `./gradlew :app:testLabUnitTest :app:lintLab :app:assembleQa`, JDK 17 and the existing pinned SDK/dependencies. The additional endpoint-gap regression passed. These checks do not qualify the new UI on a device; it has not been installed or pixel-tested in this review.

## Evidence boundaries

Local retained case: `research/revision-08-map-review` in the original Maze project. It contains full REA Evidence, source/test identities, JVM results, public-release receipt and checksum verification. Web captures establish these particular controls/text, not internal algorithms, complete source recovery or native-app parity. The Reverse Lab coverage helper rejects browser observations without a static artifact identity; no application/semantic/runtime coverage-gate pass is claimed. Source and package versions cannot be recovered from a mutable website URL alone.

No synthetic persona has been counted as a real tester. The proposal author's availability does not block rule-based testing; actual driver and operator feedback still requires people. No correct-arrival, time-saved, willingness-to-pay or million-user claim is justified by this review.
