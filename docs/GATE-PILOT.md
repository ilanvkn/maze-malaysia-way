# Gate pilot · prototype evaluation

7 November 2026. No participating industrial park, permission-cleared site data, human test responses or paying customer has been supplied. This extension tests software behaviour and makes a user review possible. It does not establish a business opportunity or successful arrivals.

## Try it

Open **Try gate pilot** from setup without downloading the country map, or **More → Gate pilot** with an installed map. Choose **Try test scenarios**, then switch Visitor / Delivery / Staff. Fictional gates have no coordinates and cannot become destinations. Check the closed, staff-only, stale and missing-evidence scenarios. The simulated dates follow the device date; they are not observations of real places.

**Import pilot JSON** accepts a user-selected UTF-8 file up to 64 KiB, 1–50 gates. Review site, source and replacement before saving. Cancel leaves stored records unchanged. Files stay in app-private no-backup storage; no account, telemetry, automatic sharing or MCP access is added. Selecting a gate copies it into the current map destination. Removing the pilot file keeps that selection and any explicitly saved trips; those are managed separately.

Start with `examples/gates/pilot-template.json` (fictional, no coordinates). For actual records, set demo=false only after obtaining permission to store/use the information; set permission=approved_for_pilot. The flag is a contributor assertion, not technical proof of ownership or redistribution rights. Do not put private pilot records in Git or send them through a public report.

All fields are required. `access`: public/delivery/staff/restricted/unknown; `availability`: open/closed/unknown; `motorcar`: yes/no/unknown. A public or delivery entrance does not imply motorcar permission. `checked_on` is empty or an ISO date; a supplied date requires checker and evidence. Future dates, duplicate IDs, unknown fields/types, oversized or deeply nested files and coordinates outside the supported regional bounds are rejected. Real records require numeric latitude/longitude. Fictional records must use null coordinates.

## What the rules mean

Closed, restricted, staff-permission, unconfirmed/denied motorcar, unknown and incomplete records do not offer road-preview selection. Selecting Staff does not prove a credential. Delivery gates are candidates only for Delivery. A check becomes stale after 30 days (a provisional pilot policy, not an operational standard). Freshness uses the device date and is re-evaluated on resume and destination confirmation. Offline clocks and imported provenance are not authenticated.

An eligible real record with an installed matching map pack offers **Review road preview**. A confirmation explains that Maze still snaps to a supported road junction within 350 m. It has not established a gate-to-road connection or permission to enter. Choosing a destination clears the old route; it does not start navigation or automatically calculate a new route. Free-form operating instructions are displayed, not interpreted as a live opening-hours schedule. Confirm hours with the operator.

This deliberately exposes the unresolved last-100-metres problem rather than treating a gate coordinate as a verified driveway. Building topology, approach direction, vehicle dimensions, live closures and full routing connectivity need a later evidence-backed implementation.

## External reviewer tasks

Ask a tester to complete these without coaching:

1. Choose a gate for a visitor, then for a delivery driver. Explain the evidence used.
2. Explain why a fresh staff record still needs permission.
3. Identify the stale/closed record and say what should happen next.
4. Import the fictional template, cancel a replacement, restart and find the stored records.
5. Explain whether "reported check" means Maze independently verified it.

Record task completion, confusing wording and wrong choices. No background analytics are collected. Screenshots should show fictional scenarios only unless the operator explicitly permits sharing real records.

A real field pilot additionally needs a participating operator, documented data permission, checked entrances and road connections, a correction owner and a recheck schedule. With volunteer consent, compare existing directions and the prototype using counterbalanced tasks: correct gate reached, wrong-gate attempts, delay minutes and record age. Do not encourage phone interaction while driving. Payment interest needs an actual operator conversation; prototype rule tests and simulated locations are not evidence of willingness to pay.

## Boundaries

The free map remains account-free. No business dashboard, commercial account, payment, paid verification badge, traffic service or automatic email submission is introduced. No partner role, equity arrangement or verified business relationship with the proposer is implied.

[Implementation findings and scoped test results](GATE-PILOT-RESULTS.md).
