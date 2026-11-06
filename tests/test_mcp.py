import hashlib
import importlib.util
import io
import json
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("maze_server", ROOT/"mcp/server.py")
server = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)


class McpTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.places = self.root/"places.sqlite"
        db = sqlite3.connect(self.places)
        db.executescript("CREATE TABLE places(id TEXT,name TEXT,detail TEXT,lat REAL,lon REAL,kind TEXT,tags TEXT); CREATE VIRTUAL TABLE search USING fts4(name,detail,tokenize=unicode61);")
        rows = [("node/1", "7-Eleven", "shop", 3.15, 101.71, "shop", '{}'),
                ("node/2", "Main entrance", "entrance", 3.1501, 101.7101, "entrance", '{"entrance": "main"}'),
                ("node/3", "Far entrance", "entrance", 3.159, 101.719, "entrance", '{}'),
                ("node/4", "Not an entrance", "entrance", 3.1502, 101.7102, "entrance", '{"entrance": "no"}')]
        for row in rows:
            db.execute("INSERT INTO places VALUES(?,?,?,?,?,?,?)", row)
            db.execute("INSERT INTO search(docid,name,detail) VALUES(last_insert_rowid(),?,?)", row[1:3])
        db.commit(); db.close()
        entry = {"bytes": self.places.stat().st_size, "sha256": hashlib.sha256(self.places.read_bytes()).hexdigest()}
        self.manifest = self.root/"manifest.json"
        self.manifest.write_text(json.dumps({"schema": 1, "files": {key: entry for key in ("roads.sqlite", "places.sqlite", "region.mbtiles")}}))
        self.catalog = server.MazeCatalog(self.manifest, self.places)

    def tearDown(self):
        self.catalog.close(); self.temp.cleanup()

    def test_real_fts_and_nearby_with_missing_and_excluded_points(self):
        self.assertEqual([x["name"] for x in self.catalog.call("maze_search", {"query": "7-Eleven"})["results"]], ["7-Eleven"])
        self.assertEqual(self.catalog.call("maze_search", {"query": '" OR * -- nope'})["results"], [])
        self.assertEqual([x["osm_id"] for x in self.catalog.call("maze_nearby_access", {"lat": 3.15, "lon": 101.71})["results"]], ["node/2"])
        self.assertFalse(self.catalog.call("maze_status", {})["live_traffic"])

    def test_rejects_arbitrary_paths_sql_extra_fields_and_bad_coordinates(self):
        for args in [{"lat": True, "lon": 101}, {"lat": float('nan'), "lon": 101}, {"lat": 51, "lon": 101}]:
            with self.assertRaises(server.InvalidArguments): self.catalog.call("maze_nearby_access", args)
        for name, args in [("maze_search", {"query": "ok", "path": "/etc/passwd"}), ("sql", {"query": "DROP TABLE places"}), ("maze_search", {"query": "x"*161})]:
            with self.assertRaises(server.InvalidArguments): self.catalog.call(name, args)
        with self.assertRaises(sqlite3.OperationalError): self.catalog.db.execute("DELETE FROM places")

    def test_rejects_hash_mismatch_and_symlinks_at_startup(self):
        other = self.root/"different.sqlite"; other.write_bytes(b"not the reviewed pack")
        with self.assertRaises(ValueError): server.MazeCatalog(self.manifest, other)
        link = self.root/"link.sqlite"; link.symlink_to(self.places)
        with self.assertRaises(ValueError): server.MazeCatalog(self.manifest, link)

    def test_bounded_transport_recovers_after_malformed_and_oversized_requests(self):
        incoming = io.BytesIO(b"{bad\n" + b"x"*(server.MAX_LINE+10) + b"\n" + b'{"jsonrpc":"2.0","id":3,"method":"ping"}\n')
        outgoing = io.StringIO(); server.serve(self.catalog, incoming, outgoing)
        responses = [json.loads(line) for line in outgoing.getvalue().splitlines()]
        self.assertEqual([x.get('error', {}).get('code') for x in responses], [-32700, -32600, None])
        self.assertEqual(responses[-1]['result'], {})

    def test_actual_stdio_process_initialize_notification_list_and_call(self):
        messages = [dict(jsonrpc="2.0", id=1, method="initialize", params={"protocolVersion": server.PROTOCOL, "capabilities": {}, "clientInfo": {"name": "synthetic-test", "version": "1"}}),
                    dict(jsonrpc="2.0", method="notifications/initialized"),
                    dict(jsonrpc="2.0", id=2, method="tools/list"),
                    dict(jsonrpc="2.0", id=3, method="tools/call", params={"name": "maze_search", "arguments": {"query": "7-Eleven"}})]
        result = subprocess.run([sys.executable, str(ROOT/"mcp/server.py"), "--manifest", str(self.manifest), "--places", str(self.places)],
            input="\n".join(map(json.dumps, messages))+"\n", text=True, capture_output=True, timeout=10, check=True)
        responses = [json.loads(line) for line in result.stdout.splitlines()]
        self.assertEqual(len(responses), 3); self.assertEqual(result.stderr, "")
        self.assertEqual(responses[0]['result']['protocolVersion'], server.PROTOCOL)
        self.assertEqual(len(responses[1]['result']['tools']), 3)
        self.assertEqual(responses[-1]['result']['structuredContent']['results'][0]['name'], "7-Eleven")

    def test_no_catalog_status_and_protocol_errors(self):
        catalog = server.MazeCatalog(self.manifest)
        handler = server.Server(catalog)
        self.assertEqual(handler.handle({"jsonrpc": "2.0", "id": 1, "method": "tools/list"})['error']['code'], -32000)
        self.assertEqual(handler.handle([])['error']['code'], -32600)
        handler.handle({"jsonrpc": "2.0", "id": 1, "method": "initialize"})
        result = handler.handle({"jsonrpc": "2.0", "id": 2, "method": "tools/call", "params": {"name": "maze_search", "arguments": {"query": "ok"}}})
        self.assertTrue(result['result']['isError'])
        self.assertIsNone(handler.handle({"jsonrpc": "2.0", "method": "notifications/initialized"}))


if __name__ == '__main__':
    unittest.main()
