"""Package three explicitly selected public datasets; never sweep a workspace.

The manifest is output alongside the ZIP. Review/copy it into the app assets and
rebuild the APK to pin a newly generated dataset release.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
for name in ("roads", "places", "tiles"):
    parser.add_argument("--" + name, type=Path, required=True)
parser.add_argument("--osm-timestamp", required=True)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
sources = {"roads.sqlite": args.roads, "places.sqlite": args.places, "region.mbtiles": args.tiles}
notice = Path(__file__).resolve().parents[1] / "app/src/main/assets/offline/NOTICE.txt"
manifest_path = args.output.with_suffix(".manifest.json")
if args.output.exists() or manifest_path.exists():
    raise SystemExit("Choose unused output paths; existing packs/manifests are preserved")


def digest(stream):
    value = hashlib.sha256()
    for block in iter(lambda: stream.read(1024 * 1024), b""):
        value.update(block)
    return value.hexdigest()


entries = {}
for name, source in sources.items():
    if not source.is_file() or source.is_symlink():
        raise SystemExit("Use regular reviewed dataset files")
    with source.open("rb") as stream:
        entries[name] = {"bytes": source.stat().st_size, "sha256": digest(stream)}
manifest = {"schema": 1, "summary": "Developer-built regional OSM pack; inspect coverage and date", "osm_timestamp": args.osm_timestamp, "files": entries}
with zipfile.ZipFile(args.output, "x", compression=zipfile.ZIP_DEFLATED, compresslevel=3, allowZip64=True) as archive:
    for name, source in sources.items():
        archive.write(source, name)
    archive.writestr("manifest.json", json.dumps(manifest, indent=2) + "\n")
    archive.write(notice, "NOTICE.txt")
with zipfile.ZipFile(args.output) as archive:
    for name, entry in entries.items():
        with archive.open(name) as stream:
            if digest(stream) != entry["sha256"]:
                raise SystemExit("Pack verification failed; do not distribute")
manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
print(json.dumps({"pack": str(args.output), "manifest": str(manifest_path), "verified": True}))
