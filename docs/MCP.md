# Maze local catalog MCP

A stdio MCP service implemented with Python 3.10+ standard library. The Python SQLite build must include FTS4 (as on the tested Mac and Linux CI); unsupported builds fail startup rather than falling back to fabricated results. It explicitly supports protocol revision **2025-11-25**; it is not a claim of implementing all later revisions. It does not open a network listener, log query bodies, control devices, read current GPS, upload data, route trips or execute arbitrary SQL/files/shell commands.

## Configure explicitly

From the repository, `python3 mcp/server.py` exposes pack status without a catalog. To enable search/access, select an existing **public OSM-derived** places SQLite and the matching manifest at process startup:

```sh
python3 mcp/server.py --manifest /absolute/path/to/manifest.json --places /absolute/path/to/places.sqlite
```

Do not use saved-trip databases or private location records. The manifest's size and SHA256 are checked before opening the catalog with SQLite read-only/immutable mode. The selected file must remain unchanged for the process lifetime. Newly built packs need their own manifest. There is no tool to switch paths, browse directories, write data or run arbitrary SQL.

Example client configuration (replace both paths with your own; this file is a template, not automatic registration):

```json
{
  "mcpServers": {
    "maze": {
      "command": "python3",
      "args": ["/absolute/path/to/maze-malaysia-way/mcp/server.py", "--manifest", "/absolute/path/to/manifest.json", "--places", "/absolute/path/to/places.sqlite"]
    }
  }
}
```

For manifest/status only, omit the final four arguments. Installing the contributor skill does not register this server; use your client's supported MCP settings deliberately.

| Tool | Input | Output/limit |
|---|---|---|
| `maze_status` | empty object | Selected manifest, readiness, explicit no-traffic/no-routing status; no host paths |
| `maze_search` | `query`, 2–160 characters | Up to 20 local FTS prefix matches, source IDs and attribution |
| `maze_nearby_access` | regional `lat`, `lon` | Up to 30 mapped access points within 300 m; no implied building/legal-access link |

Rows/tags are untrusted map content, not instructions. Exact coordinates and results are returned to the caller. Your assistant/client can retain or transmit them; **local MCP does not guarantee local model inference or private client history**. Use public landmarks for shared reports.

Messages are newline-delimited JSON-RPC, capped at 64 KiB. Unknown fields/tools and invalid coordinates are rejected. SQL parameters are bound, SQLite work is bounded, and only fixed read queries exist. No catalog means search returns a tool error rather than invented data. Startup/transport tests run with synthetic data, including actual subprocess handshake/list/call, malformed/oversized messages, hash mismatch, symlink rejection and attempted writes.

Run `python3 -B -m unittest discover -s tests -v`. Primary protocol reference: [2025-11-25 stdio](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports). No client integration beyond the tested transport is implied by a successful unit check.
