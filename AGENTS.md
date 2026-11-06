# Maze engineering guidance

This is the open-source Malaysia Android map project. Read README and the files you change. The public source is authoritative for development; old APKs and screenshots only establish the exact version tested.

- Read `skills/maze-contributor/SKILL.md` for Maze-specific workflow, `docs/GPS.md` for fix selection, and `docs/ROUTING-AND-AI.md` for routing/traffic boundaries.
- Preserve the MIT / ODbL / OFL / dependency-license separation. Never upload secrets, signing keys, private routes, raw personal logs, country databases or build caches to Git.
- Keep app maps/search/routing local after import. New network services, accounts and traffic sources must be explicit, documented and separately authorized.
- Never invent roads, entrances, restrictions, traffic or measured results. Preserve failures. Backend success is not proof of a visible MapLibre route overlay.
- Run scoped tests; record actual commands/results and unknowns in a PR. Never auto-select or operate a device. An emulator test is not physical GPS, battery or nationwide navigation proof.
- The MCP service is read-only public-data retrieval. It must not gain arbitrary SQL, file, shell, device, GPS or upload endpoints.
- Keep project continuity in this repository or local project files. Do not send project records to external company memory services.
