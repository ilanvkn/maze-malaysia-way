# Maze · Malaysia Way

An offline-first Android map prototype built with Kotlin, Compose and MapLibre Native. After importing a matching regional pack, maps, local place/address search, mapped entrance/exit lookup and bounded road-route previews run on-device. No hosted map/search/routing API is needed for that flow.

**Development snapshot, not a store release or active navigation product.** This public source was copied from an ongoing0.3offline-beta sprint. It does not inherit final acceptance from later changes in that workspace. Route search can fail within its budget; incomplete OSM data, conservative road exclusions and junction snapping limit coverage. No live traffic, lane guidance, guaranteed legal entrance access or fastest-route claim is made.

## Build

Use Java17, Android SDK platform36/build-tools35.0.0 and a compatible Android development environment. Set ANDROID_HOME or create an untracked local.properties with your SDK path. The checked-in Gradle wrapper pins8.14.3 with its distribution checksum. Building downloads dependencies; application offline operation is a separate claim.

```sh
./gradlew --no-daemon :app:assembleLab :app:testLabUnitTest :app:lintLab
```

The lab package uses standard local Android debug signing, not a publisher key. No APK, keystore, emulator image, local SDK path or app-store account is included. CI builds and runs JVM checks; it does not certify airplane-mode interaction or physical-device use. Choose your own non-production emulator explicitly. This repository does not auto-select or operate connected devices.

## Offline data

Country databases and glyph binaries are deliberately separate from Git source. **The source checkout alone has no country pack.** The included manifest identifies the maintainer's lab data, not a public download promise. Use [DATA.md](DATA.md) to build a reviewed pack, pin its manifest into the app and rebuild. Then install the APK, copy the ZIP to your test device, tap Import Malaysia pack and select it in Android's file picker.

Font glyphs must be prepared before building a fully labelled offline app. The supplied fetch script verifies the reviewed upstream blob identities; no font service is used at runtime. Build/data preparation needs network access and substantial storage. A matching imported pack then supports airplane-mode testing. The loopback tile listener stays on the device.

Shop points, representative building points, entrances and motorway exits are distinct mapped facts. Missing entrances remain missing; opening hours are unverified mapped text. Road previews may snap to a junction and explicitly report unsupported/disconnected routes. Do not rely on this prototype for safety-critical travel.

Original app code is MIT. OSM/derived databases are ODbL, Noto fonts OFL and other libraries retain their licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). No Google Maps/Waze assets, SDK internals or proprietary map data are included.

## Contribute

Submit focused pull requests with reproducible synthetic tests and Android/tool versions. Report data gaps separately from algorithm/UI bugs. Keep real location histories, saved trips, participant details, credentials, signing keys and raw device logs out of issues. Preserve notices and identify the exact dataset/source revision when reporting a route. Community feedback is welcome; there is no automatic update service or zero-failure guarantee.
