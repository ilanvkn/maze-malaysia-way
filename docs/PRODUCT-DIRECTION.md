# Maze: make the first useful journey easy

The product promise is a Malaysia map you can carry, improve and share. Growth should follow a useful experience, not a sign-up wall or fabricated activity. The owner explicitly chose account-free maps with optional GitHub contribution.

## Four review perspectives

**Engineering:** setup is a real state, not an empty map with disabled journey controls. Checking, importing, ready, cancelled and failed states must be distinguishable. Show actionable file/storage errors, retain hash validation and remove staging files on cancellation. The `qa` variant isolates first-use testing from an existing lab install; neither selects a device automatically.

**Design:** keep the ink/porcelain/indigo palette in DESIGN.md and Android native controls. Put the next useful action first, use one scroll owner per surface, wrap actions at large text sizes and keep permission requests contextual. The Apple Blueprint review contributes task hierarchy, readable states and deliberate ownership; Maze remains an Android app. Existing saved journeys remain reachable without a map pack.

**Community/marketing:** the invitation is specific: fix a place, test a journey, improve a word. A native community hub explains how to join. Share Maze opens the Android Sharesheet with a project invitation only; no contacts, GPS, search or trip data are attached. Reporting a place opens a reviewable GitHub draft with an allowlisted public OSM identity, not a private route. Reading is public; contributing uses the person's own GitHub account. No silent issue submission, fake community counts or promised virality.

**Operations:** source CI must prepare and verify the offline glyph payload, run JVM/Python/lint checks, and retain reports. A payload pass does not prove UI or navigation. Hash-pin Actions and preserve credentials, licenses, pack version compatibility and previous failed evidence. No production signer is added by this sprint.

## Measured adoption without tracking people

Start with volunteer-observed tasks: fresh install → correct ZIP found → successful import → named place found → useful preview → one contribution or deliberate invitation. Record completion/time/error categories only with participant consent, using public-landmark scenarios. GitHub release downloads and reviewed contributions are aggregate project signals, not unique users or share conversions. No analytics SDK, fingerprint, contact upload, referral reward or probe collection is introduced.

Improve the biggest observed failure before adding accounts or ML. Future download management needs storage/update recovery, bandwidth control and licensed versioned packages. Route quality still needs work; an excellent welcome cannot correct a bad graph. The current conservative preview, no-traffic and field-GPS limits remain.

## Reference scope

Reviewed 6 November 2026: [Organic Maps](https://organicmaps.app/) documents offline use without registration or mandatory tutorials; [OsmAnd's map context menu](https://www.osmand.net/docs/user/map/map-context-menu/) explains selected-place actions and public OSM objects; [Android Sharesheet guidance](https://developer.android.com/develop/ui/compose/sharing/send) describes native user-controlled sharing; [GitHub issue URL parameters](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/creating-an-issue#creating-an-issue-from-a-url-query) support reviewable form prefills. These are inspected documentation patterns, not reverse-engineered binaries or claims of feature parity. KC generic UI context returned no records; no Maze project identifiers or records were sent to company Memory.
