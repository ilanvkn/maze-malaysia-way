# Foreground location: what Maze does

"Locate me" requests permission at the point of use. Denial leaves manual start selection available. Only coarse/fine foreground permissions are declared; there is no background-location permission, location service, continuous trip tracking or upload endpoint.

The old code selected GPS when it was enabled regardless of precise permission and accepted fixes without checking age or accuracy. The current code:

1. Uses GPS + network providers for precise permission and network only for approximate permission. Provider availability is checked; absent/off providers produce an actionable message. It does not require Google Play Services.
2. Runs one session for at most 20 seconds, with both providers eligible when permitted. It accepts the first fresh fix reporting ≤50 m accuracy, or the best usable precise fix (≤250 m) at the deadline. Approximate results may be up to 5 km and are explicitly labelled. These are policy thresholds, not accuracy guarantees.
3. Rejects nonfinite/missing/zero accuracy, fixes older than 30 seconds, future/invalid monotonic timestamps and points outside the regional bounds. No stale last-known fallback is silently accepted.
4. Uses `elapsedRealtimeNanos` rather than wall-clock timestamps to judge age. Displays the provider-reported uncertainty and labels fixes that Android marks as mock. Emulator geo injection need not set that mock flag. Reported accuracy is an estimate, not proof of a real entrance or correct road.
5. Cancels subscriptions and timeout on success, retry or Activity stop, with a cancelled-request message on return when a request was active. A generation token rejects delayed callbacks from a previous request. Current-location coordinates are not restored through saved instance state; explicitly saving a trip remains a separate user action that can persist its start point locally.

GPS/GNSS hardware can work in airplane mode when Android location services permit it. A cold start, blocked sky view, urban reflections and device restrictions can still delay or distort a fix. Android's network provider may need connectivity or be unavailable; approximate-only permission is never a reason to request GPS behind the user's choice. Try outdoors, check Android location settings, or choose a start point. Maze cannot manufacture a fix or repair receiver hardware.

## Evidence and next work

JVM tests cover permission/provider selection, age/future/invalid timestamps, precision thresholds, nonfinite accuracy, regional bounds and mock labels. Owned-emulator permission/provider/simulated-fix checks are recorded separately in QUALITY.md when complete. Emulator injection proves application handling, not satellite acquisition, urban-canyon accuracy, time to first GNSS fix or physical energy use.

Before continuous navigation: gather opt-in field trials, define uncertainty-aware snapping, handle permissions/process/lifecycle carefully, and compare matched provider profiles for power and quality. Do not add aggressive smoothing or a Kalman filter merely to make a marker look stable: filtering can introduce lag and put users on a plausible wrong road. Evaluate any filter against labelled traces first.

Primary Android references: [location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions), [LocationManager](https://developer.android.com/reference/android/location/LocationManager), [monotonic location timestamps and accuracy](https://developer.android.com/reference/android/location/Location).

## Visual road review

0.4.3 carries the current fix’s reported uncertainty into the map and road-review UI. The dashed circle is a provider estimate. Starts reporting more than 350 m uncertainty require a known manual starting point instead of presenting a misleading nearby-road list. [Exact emulator/synthetic evidence and limits](benchmarks/2026-11-visual-approaches/README.md).
