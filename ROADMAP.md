# Where Maze goes next

Build a useful Malaysia map before adding slogans or features nobody can verify. Work is open to contributors; these are priorities, not promised release dates.

## Current contributor release

Native offline map/search, mapped nearby access, bounded driving previews, foreground fix selection, Quick/Careful A*, local retrieval MCP, skill and reproducible checks. See [docs/QUALITY.md](docs/QUALITY.md) for exact proof and limits.

## Priority 1 — trust the location and the route

- Compare the known KL detours against Careful A* and a mature engine on identical data. Track graph exclusions, restrictions and snaps, not just "route found".
- Field-test cold/warm GNSS, approximate permission and uncertain fixes on volunteer-owned devices. Publish only consented/sanitized aggregates.
- Add restriction, barrier and disconnected-road fixtures from Peninsular Malaysia, Sabah and Sarawak. No invented land routes over sea gaps.
- Complete TalkBack, keyboard/switch and large-text interaction checks; screenshots alone are insufficient.

## Priority 2 — make local detail useful

- Distinguish same-name places with locality/type and stable OSM identity.
- Improve Malay/English/Chinese/Tamil aliases, addresses, postcode and typo handling, measured on a community-reviewed query set.
- Link entrances to buildings only where source topology supports it. Separate pedestrian/accessible/vehicle access.
- Produce versioned data releases with source dates, checksums, ODbL compliance, interrupted-update recovery and a supported update lifecycle.

## Priority 3 — navigation and traffic, with evidence

- Evaluate Valhalla/mobile or another mature engine against size/memory/cold-start/route-quality budgets before replacing the router.
- Design active guidance with uncertainty, reroute behavior and real field validation. It is not available now.
- Publish a traffic-source and privacy proposal before collecting probes or applying traffic costs.
- Benchmark small on-device search/ETA models only after clean baselines and licensed evaluation data exist.

## Growing the project

Use issues for concrete work and Discussions for proposals or local feedback. Keep PRs small enough to review. Invite regional testers, OSM mappers, accessibility users and language contributors—not just framework rewrites. Track repeatable improvements and regressions rather than stars, generated issue counts or AI branding.
