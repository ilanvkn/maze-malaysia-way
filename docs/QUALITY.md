# What has actually been checked

This document separates source checks, previous lab evidence and unmeasured field behavior. It is not a production certification.

## Current 0.3.1 source

GPS policy rejects stale/future/invalid fixes and respects approximate permission. A* separates incoming turn states, budgets and cancellation; Quick trades optimality for effort, Careful uses an admissible bound for the current builder. Local JVM/build/lint and Python transport checks are recorded in release notes with exact counts. GitHub Actions independently checks source builds, JVM/lint and stdlib tests. CI does not operate an emulator or prove physical GPS.

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
