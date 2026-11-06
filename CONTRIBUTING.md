# Build Maze with us

Selamat datang. You do not need permission to start a small improvement. Read the current limits in README and choose a problem you can reproduce. For a large rewrite, paid service or new runtime dependency, open a discussion first so we can agree on scope and maintenance.

## Find your way in

- **Know a place?** Report a missing/wrong mapped detail, with a public OSM link if available. Correct source data in OpenStreetMap when appropriate and follow its community rules; a GitHub report does not automatically edit OSM.
- **Use Android?** Try search, no-results, offline import, Day/Night, large text, permission denial and saved trips. Device model/API and app/pack versions help us reproduce failures.
- **Write code?** Start with a focused issue, keep a readable diff, and test the behavior it changes. The Kotlin GPS and A* policy tests are a good starting point.
- **Speak our languages?** Help us write natural Malay, English, Chinese or Tamil interface text. These are contribution opportunities; the current UI is mainly English.
- **Work on routing/data?** Add small synthetic fixtures before changing restrictions, access rules, snapping or costs. Include a counterexample and a baseline.

## A useful pull request

Explain the concrete problem and the behavior after your change. Include the smallest reproduction, tests you ran, and anything not tested. Follow the build commands in README. New Python MCP helpers must remain stdlib-only unless a dependency is discussed. Run `python3 -B -m unittest discover -s tests -v` for helpers and the relevant Gradle checks for Android changes.

Use synthetic coordinates and public landmarks for tests. Never attach personal GPS histories, private trip files, credentials, signing keys or unfiltered device logs. Public issues and MCP client histories may be retained. The MCP server is local, but the client you connect can forward results to a model provider; review that client's policy.

No automatic device selection. Name your own emulator/test phone before install or input operations. Do not use production displays for app testing. Offline maps, GPS simulation and real GNSS field performance are different tests.

## AI-assisted contributions

You are responsible for what you submit. Read generated code, check dependency licenses, run the tests, and explain the important decisions. State relevant AI assistance and unverified parts in the PR. Do not post bulk generated issues, comments or benchmark claims. A real fix with a small test is more useful than a large speculative framework.

## Licensing and review

Contributions to original Maze code use the repository MIT license. Do not copy third-party source without preserving its license and attribution. OSM data is separately ODbL; do not relabel it MIT or import proprietary maps. No CLA is currently required. Submit work you have the right to share.

Maintainers review behavior, clarity and evidence. This is an early volunteer project; response times vary. We will not promise acceptance or shipping dates. Be kind, be specific, and help the next person understand the code.
