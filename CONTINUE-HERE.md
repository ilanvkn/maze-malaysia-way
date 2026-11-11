# Continue Maze in a fresh chat

Read this file, `AGENTS.md`, `skills/maze-contributor/SKILL.md` and the files you intend to change. This is the project-owned continuation record; the old chat is not required to build or inspect the work. Source and retained receipts outrank conversational claims.

## Checkout and delivery

Canonical checkout: `/Users/ik/Downloads/maze-community`. GitHub: https://github.com/ilanvkn/maze-malaysia-way. Branch `codex/uncertain-gps-start` contains the earlier routing and visual-chooser work; it is stacked on `codex/visual-road-chooser`. Review branches are not automatically merged into main. Do not substitute the legacy uncommitted project under `/Users/ik/Downloads/MazeMalaysiaWay` or the unrelated Varuna workspace for this checkout.

Current test release: `v0.4.4-gps.1`, package `my.maze.malaysia.qa`, code10, version `0.4.4-gps-review-qa`. APK SHA256 `8b49eac779fd51fb55a454ac57d3e38a4cab0f95c71a6f1f6c783bc7dc3472b8`, 63,894,883 bytes. It is minified and non-debuggable, locally Android Debug signed, not a Play production release. It can update the matching 0.4.3 QA install in place. `.lab` is a separate identity.

- [APK and installation instructions](https://github.com/ilanvkn/maze-malaysia-way/releases/tag/v0.4.4-gps.1)
- [Unchanged r3 map ZIP](https://github.com/ilanvkn/maze-malaysia-way/releases/download/v0.4.2-approaches.1/Maze-Malaysia-offline-2026-11-04-r3.zip), SHA256 `6b16d4e6ffc7d432877e5cc1135ca1465e0a483a8e2ef35ea90e04eb3da9f95c`, 698,632,735 bytes.
- Road DB SHA256 `f8e6aafbf278b05fc222546b5e1d6797a5b048d472d73b37238a8cff2fa17f0c`; OSM source4November2026. No new country pack is required.
- [0.4.4 qualification and actual screenshots](docs/benchmarks/2026-11-gps/README.md), [GPS policy](docs/GPS.md), [visual contract](DESIGN.md), [routing baseline/GraphHopper comparison](docs/benchmarks/2026-11-approaches/README.md).

## Current behavior and boundaries

Account-free offline map/search/routing after verified ZIP import. GitHub participation is optional. Preserve MIT code / ODbL map data / font and dependency license separation. No background tracking, raw-fix upload, hosted routing or traffic feed has been added. Read-only catalog MCP must stay read-only, without arbitrary SQL, files, device actions or uploads. Keep project continuity locally; do not submit project records to company Memory.

`MazeLocationPolicy` / `MazeForegroundLocation` implement a20s bounded request with at most8 samples, ten-second window, latest age<=5s and500ms minimum per-provider spacing. Early completion requires3 consistent samples spanning2s and envelope<=50m. Precise provider limits remain250m, approximate5000m. The displayed estimate encloses retained reported radii around the **latest actual point**; it is not averaging or calibrated confidence. The UI separates estimated radius, latest reported accuracy and observed spread. Consistent bias is still possible.

Current-location starts have no default road. `roadReview` counts overlapping normalized mapped approaches before retaining the nearest20 unique ways. The actual selected geometry/direction is previewed. Road/direction acknowledgment is required in the UI and ViewModel and resets on selection change. Missing/over350m uncertainty requires a known manual start. Live snapshots expire for new routing after a provisional two-minute monotonic review budget; retry invalidates the old live snapshot. Opening saved trips does not restore live GPS confidence. The snapshot is not continuous navigation.

Directed segment snapping, virtual endpoints, incoming-way/previous-junction turn state and A* budgets remain. Quick weight4 can take longer routes; Careful weight1 is not a real-world fastest-route promise. No off-road connector, guessed crossing, access permission, automatic bearing-based carriageway inference or traffic is invented. Preview caching is bounded4; selected display geometry<=2048 points and up to400m.

## Verification / development

Use pinned wrapper, JDK17 and the configured Android SDK. On the current Mac:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/Users/ik/Desktop/OSX/IKPremium/kanikaos/toolchain/android-sdk ./gradlew --max-workers=2 :app:testLabUnitTest :app:lintLab :app:assembleQa
python3 tools/verify-offline-apk.py app/build/outputs/apk/qa/app-qa.apk
```

The qualification report identifies actual JVM, native, test-provider and UI results separately. GitHub checks are available on the release commit. A passing build or synthetic trace is not physical GPS qualification. SDK/Gradle/APK/device checks ran locally; KHC does not support these workloads. No project data was offloaded.

Private case evidence: `/Users/ik/Downloads/MazeMalaysiaWay/research/revision-11-gps-uncertainty` (PLAN, runtime receipts, source/artifact hashes, screenshots, failed attempts and cleanup). Predecessor evidence remains in revision10; never relabel it as a0.4.4 pass. Public synthetic fixtures and summary receipts needed to inspect the feature are committed in the benchmark directory. Country databases, build caches, signing keys and private routes are deliberately excluded from Git; public test downloads belong in releases.

## Remaining work

No physical low-end phone, urban GNSS acquisition/error, battery, field carriageway arrival or full accessibility certification. The thresholds are provisional. Before automatic/continuous map matching, collect opt-in labelled traces on representative devices; evaluate wrong-road rate, lag, consistent bias, movement, lifecycle and power against ground truth. Do not add a filter merely to make a marker look stable. The20-road chooser retains the nearest geometry per OSM way; parallel branches or loops within the same way are not separate projection candidates. If the intended section is absent, use a known manual start. Multi-projection/heading-based matching requires separate ground-truth evaluation. The chooser is not a complete list of real roads; OSM access and conditions can be incomplete/outdated.

Only explicitly selected owned test targets may be operated. The recorded emulator is AV3X_Antartica_QA / emulator5582 / dedicated ADB5041, never the first attached device. Verify qemu and AVD identity before mutations; preserve `.lab` and unrelated downloads. Cold-boot phone-service failure and System UI ANR were retained separately from app tests. Restore settings/remove mock providers and owned QA data/stop only the owned runtime when done. Never touch a physical showroom display for this task.

Final owned-runtime cleanup was verified: test provider removed, mock mode restored, original settings/window/animation values read back, own QA installation removed, stable `.lab` APK hash unchanged and the owned emulator stopped. [Qualification receipt](docs/benchmarks/2026-11-gps/qualification.json). Source/test/report continuity is committed here; no company Memory write-back or whole-chat archive is required.
