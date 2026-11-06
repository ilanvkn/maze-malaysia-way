# What has actually been checked

This document separates source checks, previous lab evidence and unmeasured field behavior. It is not a production certification.

## Current 0.3.1 source

GPS policy rejects stale/future/invalid fixes and respects approximate permission. A* separates incoming turn states, budgets and cancellation; Quick trades optimality for effort, Careful uses an admissible bound for the current builder. Local JVM/build/lint and Python transport checks are recorded in release notes with exact counts. GitHub Actions independently checks source builds, JVM/lint and stdlib tests. CI does not operate an emulator or prove physical GPS.

## Current owned-emulator checks

The first 0.3.1 candidate APK (SHA256 `8df7795859aede2549533bbbc3deb26ba7b627940b6418660c864d821dd2ab11`) was installed on the owned API 35 ARM64 emulator with the existing hash-pinned regional pack. The first cold controller run passed local search, access, Day/Night styles, glyph and regional tile reads, then hit the first route's time/state budget. That run is retained as failed; no cold-route success is claimed. A separate warm run passed all seven route samples. The first cold route budget failure remains failed and is not covered by that warm pass. These are single functional samples, not comparative performance measurements.

Native UI checks observed the system permission prompt and denial fallback, an approximate-only session timing out without inventing a point, and a fresh emulator-injected KL Sentral fix becoming Current location with reported ±5 m accuracy. Coarse/fine test permissions were explicitly set on the owned lab package for the latter cases; the denied/retry state had Android USER_FIXED flags, so a repeated dialog was not assumed. The injected coordinate is a public landmark and is not satellite/physical GPS evidence. Android did not mark that injected provider fix as mock, so the UI's mock label is not a reliable emulator detector.

Local checks: **25 JVM tests**, **seven Python tests**, build success, lint zero errors (12 warnings / three hints). Main-source GitHub CI passed at `2d0d397e23d35ae8c1f96d14edcef7e15dd27ef3`: [run 42171601052](https://github.com/ilanvkn/maze-malaysia-way/actions/runs/42171601052). MCP was also exercised against the real selected public regional catalog: 20 Kuching search results and seven access points within 300 m of KLCC. These are functional samples, not retrieval-quality or safety benchmarks.

## Previous 0.3 offline lab

On the explicitly owned API 35 ARM64 emulator, the previous final APK passed seven sample routes with airplane mode enabled and no validated external network, eight search cases, local Day/Night styles/glyphs, four regional tile reads and three nearby-access cases. Sixteen JVM checks passed. Actual screenshots caught an overlay/camera failure after theme switches; map/style identity effects corrected it. Large-text landscape header wrapping was also corrected. Those fixes are reconciled here.

The screenshot in README is that previous actual lab render, using public landmarks. This evidence does not automatically qualify later source changes. A route found can still be a poor route: KL Sentral–KLCC was about 14.9 km, a known detour. Conservative access exclusions and quick weighted search need further route-quality work.

An interrupted device ZIP copy was rejected by the importer, then a hash-verified recopy/import succeeded. Country-in-APK staging exhausted the emulator storage, so APK and country ZIP are separate. A prior failed harness run remains failed; corrected qualification was recorded separately. No claim that all historical tests passed.

## Release gates still open

- Satellite acquisition, urban-canyon correctness, real driving and uncertainty-aware snapping.
- Whole-country graph/restriction/access correctness and useful detour bounds.
- Full TalkBack, keyboard/switch and adaptive interaction qualification.
- Physical FPS, power, thermal, low-storage update and long-session behavior.
- Active guidance, live traffic, ferries, production signing and store release.

No zero-failure, fastest-route, comprehensive POI or current-traffic promise. Source licenses, dataset dates/hashes, region and missing coverage must stay visible. Contributions should add reproducible counterexamples and measured results, not blanket quality badges.

## 0.3.1 warm route samples

| Public-landmark route | km | Sample elapsed ms |
|---|---:|---:|
| KL Sentral → Petronas Twin Towers | 14.86 | 502 |
| Dataran Merdeka → KL Sentral | 2.69 | 54 |
| George Town → Gurney Paragon | 5.78 | 132 |
| Kuching Waterfront → Vivacity Megamall | 17.27 | 301 |
| Kota Kinabalu → Suria Sabah | 1.70 | 46 |
| KL Sentral → George Town | 431.87 | 2492 |
| KL Sentral → Shah Alam | 43.70 | 550 |

Quick results preserve the known detours; successful computation does not establish useful or legal real-world routing. Field evaluation and the route-quality issue remain open.

## Careful-mode boundary

A separate Careful A* run completed the five local samples, then exhausted its time/state budget on KL Sentral–George Town; it did not reach Shah Alam. The suite is **passed=false**, not a seven-route pass. The finished KL Sentral–KLCC result stayed 14.86 km, matching Quick. Lowering the heuristic weight alone did not remove that detour on the retained graph; endpoint choice and conservative graph exclusions need investigation. Careful optimizes stored estimated seconds, not distance, so its Penang result can be longer in metres.

A native More-screen check subsequently caught a settings crash caused by an added inner scroll container. The candidate was withdrawn before release. The source correction removes the redundant child scroll and keeps the bounded parent. Build/unit/lint success did not catch this UI failure. Final replacement APK and positive native recheck are recorded in the release notes.

## Final replacement APK

The replacement APK is SHA256 `9b165c0470ab9417df63a1d026d9b96feb0a03305090888054f5d5144deeca07`, version 0.3.1-offline-beta-lab, code 4, non-debuggable and Android Debug lab-signed. Native checks positively opened More, displayed both search choices, selected Careful, returned to Quick and returned to the map. Actual screenshots were inspected; this closes the reproduced nested-scroll crash for this build, not general accessibility qualification. Its separate offline controller passed all seven Quick routes after an owned-AVD restart with the existing imported pack. Cold-storage/field performance is not established.

The final replacement repeated the separate Careful run: five local routes completed and the intercity sample exhausted its budget (passed=false). The implementation-fix commit `06ccdd0eff90d42993160790e3f9583712b19ed0` passed [GitHub CI run42173186112](https://github.com/ilanvkn/maze-malaysia-way/actions/runs/42173186112).

The final replacement also positively verified an in-flight approximate-only request before backgrounding, cancellation feedback on return with no late fix applied, and a new precise request accepting an injected public KL Sentral fix with reported ±5 m. An earlier controller attempt pressed Home before confirming the request had begun; that unsynchronized attempt is retained separately and not counted as a pass. No field GNSS claim.
