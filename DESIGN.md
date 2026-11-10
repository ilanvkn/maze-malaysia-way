# Maze · the map gets the space

Maze is an account-free Malaysian map, not a dashboard. Cartography is the main surface. The default view has one destination search and one compact journey card. Details, provenance, access points and community actions remain one deliberate step away. No invented activity counters, decorative graphs or motion backgrounds.

## Visual contract

Porcelain `#F7F8FC`, ink `#182239`, indigo `#354FD6`. Night uses ink surfaces, pale indigo actions and ice secondary accents. The route uses indigo with a white casing; a rust destination and indigo origin also differ by position and supporting text. Native system typography keeps Malaysian scripts and enlarged text dependable. Existing Maze M mark is retained in the new palette. No font download or new UI dependency.

Surfaces are opaque, rounded 20 dp, with restrained elevation. Use 4/8 dp spacing and at least 48 dp interactive targets. Keep visible attribution. Default trip details are collapsed; loading, cancellation and errors remain visible. Wide layouts place the journey beside the map. Enlarged text can scroll the card and sheets. Do not shrink text to make it fit.

## Cartography

Original Shortbread styling: cool neutral city fabric, muted park green, ice water, subtle buildings from zoom 14. Local roads appear after major roads; five independent zoom-scaled road tiers have casings. Major-road labels outrank local places. Transit stations appear at 14, selected landmarks at 15, other mapped businesses at 17. Footpaths are dashed and do not imply driving access. No invented labels, roads, greenery, terrain or traffic.

The regional tiles stop at zoom 14; higher zoom magnifies the existing geometry. Map and interface night palettes are separate and must both be checked on the emulator. A passing JSON check does not prove MapLibre rendered a route.

## Review gates

Inspect actual day/night map pixels, a visible corrected KL route, first use and the search/details flow. Test portrait and real landscape with enlarged text. Preserve all offline/privacy/error states. Record exact APK/data versions and limits in the qualification report. Emulator screenshots are not a claim of field navigation quality.

## Visual road review · 0.4.3

One map, one selected road. A teal highlight with a contrasting casing follows the actual offline segment for up to 400 m; the separate ROAD ring uses the routing projection, including its endpoint normalization. One-way arrows follow mapped direction; two-way roads have no arrows. Original A/B place markers remain visible and no line bridges their gap to the road. Indigo remains the confirmed route colour.

Previous/next and the numbered All roads list change only the preview. A separate confirmation commits the loaded selection. Loading clears the previous geometry, blocks confirmation and rejects stale asynchronous results. Cancel preserves an existing route. Camera changes are immediate; preview caching holds at most four entries and display geometry at most 2,048 points.

Day/Night, 48 dp controls, source details and attribution are part of this contract. On constrained screens the descriptive body scrolls while confirmation stays accessible. Landscape panels share the width and reserve map space. Provider-reported uncertainty is shown as an estimate, never a promise of the correct carriageway. When uncertainty exceeds the 350 m road-search area, request a known manual start instead.

## Location uncertainty · 0.4.4

Keep reported accuracy separate from observed spread and the displayed estimate. The dashed circle uses the sample envelope; the ROAD ring always means a selected mapped snap. A GPS start begins with no road highlighted. Name the overlapping-road ambiguity, retain a manual-start exit, and require a road/direction check before using it. Changing the selection clears that check. Use native checked semantics and a 48 dp row; do not rely on teal or arrows alone. Snapshot expiry returns to an actionable journey message, never a silent new road selection.
