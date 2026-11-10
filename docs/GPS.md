# Foreground location and road choice

Location is optional. "Locate me" asks for foreground permission at the point of use. There is no background-location permission, continuous tracking, raw-fix upload or hosted map matching. Manual start selection remains available.

## Bounded sample policy · 0.4.4

A request lasts at most 20 seconds. Precise permission permits GPS and network providers; approximate permission permits network only. Maze never requests GPS behind approximate-only permission. Missing/off providers have an actionable error.

The pure `FixWindow` retains at most eight usable samples from the recent ten seconds. Samples must arrive with monotonic timestamps, be from the requested session, have a valid regional position and positive finite accuracy, and be no more than five seconds old on arrival. Duplicate/out-of-order or less than 500 ms spaced samples from the same provider do not count again. The provider radius limit remains 250 m for precise permission and 5 km for approximate permission.

One optimistic sample cannot end a request. Early completion requires at least three samples spanning two seconds, one provider, pairwise spread no greater than the smallest reported radius (with a provisional 12 m floor), and a displayed estimate no greater than 50 m. Otherwise, the deadline may return the latest usable sample with an uncertainty label. A latest sample older than five seconds is rejected; an old smallest-radius fix is not a fallback.

Maze keeps the **latest actual point**. It does not average fixes across a median or divide reported error by the square root of the sample count. The displayed radius encloses each retained sample's reported radius around that latest point: maximum of distance to the sample plus its reported radius, rounded upward. If that exceeds 5 km, the request fails instead of clamping a misleading circle. The UI separately labels:

- the displayed location estimate;
- the latest provider-reported accuracy;
- observed pairwise spread and sample count;
- consistent samples or road uncertainty, and Android mock/simulated status when present.

Movement can increase spread just as measurement noise can. This envelope is a conservative display heuristic, **not a calibrated confidence region**. A consistent bias can remain undetected. Android documents its reported horizontal accuracy as a provider-estimated 68th-percentile radius; neither that radius nor Maze's envelope guarantees the true position. [Android Location reference](https://developer.android.com/reference/android/location/Location#getAccuracy()).

Subscriptions and timeout are cancelled on result, retry or Activity stop. Generation tokens reject late callbacks. Starting a retry invalidates the previous live snapshot for new routing even if the new request later fails. A current-position snapshot also expires after a provisional two-minute monotonic review budget, checked before road review, confirmation and new route calculation. It is not reconstructed as live location after process recreation or opening a saved trip. Existing saved route previews remain local and visible as previews; explicitly saving a trip can persist its chosen coordinates locally.

## Wrong-carriageway protection

A GPS-derived start has **no default road selection**. The visual chooser counts mapped ways whose normalized road projection falls within the displayed estimate before limiting its list to the nearest 20. It states when other roads were omitted. Counts refer to supported offline road geometry; they are not proof of complete real-world coverage or entry permission.

Choose a road to inspect its actual highlighted segment, snap ring, original place marker and mapped direction arrows. Check "I checked this road and direction" before confirmation. Changing the road resets that check. A fresh GPS fix, its nearest road or a noisy bearing cannot silently authorize a carriageway. Acknowledgment is also enforced in the ViewModel; loading/cancellation guards remain intact. No connector crosses the place/road gap and no turn, access or one-way restriction is relaxed.

Missing uncertainty or an estimate wider than the 350 m road-search area requires a known manual start. Missing/expired live timestamps also block a new GPS-based route. Every current-location journey offers "Choose start manually". The list retains the nearest geometry per OSM way; parallel branches or loops within one way are not separate projection candidates. Use a known manual start when the intended section is absent. These are conservative safeguards, not automatic identification of the correct carriageway; a user can still choose an unsuitable road and OSM can be incomplete or outdated.

## Evidence and limits

The [0.4.4 qualification](benchmarks/2026-11-gps/README.md) separates JVM policy tests, synthetic divided-road fixtures, Android test-provider/UI handling and exact-APK offline journeys. Earlier [0.4.3 visual checks](benchmarks/2026-11-visual-approaches/README.md) remain predecessor evidence.

Emulator injection does not prove satellite acquisition, urban-canyon accuracy, time to first GNSS fix, physical low-end performance or energy use. Thresholds above are provisional engineering policy, not field-calibrated accuracy claims. Before automatic/continuous map matching, gather opt-in labelled field traces on representative phones, evaluate wrong-road rate and lag against ground truth, and test permissions/lifecycle/power. Do not add smoothing merely to make a marker look steady.

Primary references: [permissions](https://developer.android.com/develop/sensors-and-location/location/permissions), [LocationManager](https://developer.android.com/reference/android/location/LocationManager), [elapsed realtime timestamps](https://developer.android.com/reference/android/location/Location#getElapsedRealtimeNanos()).
