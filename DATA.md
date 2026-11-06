# Building an offline pack

Obtain a legally redistributable Geofabrik Malaysia/Singapore/Brunei OSM PBF and matching Shortbread1.0 MBTiles from [the regional download page](https://download.geofabrik.de/asia/malaysia-singapore-brunei.html). Record exact download URLs, dates, hashes and licenses. The extract also includes Singapore and Brunei; it is not an authoritative Malaysia-only boundary. Do not bulk-download an entire country through a public small-area tile endpoint.

Create an isolated Python environment and install `requirements-data.txt` (PyOsmium4.3.2). It is a build-time dependency; record the version used for your data release. Create an untracked data/ directory. Then, from repository root, using new output filenames:

```sh
python tools/build-offline-index.py data/region.osm.pbf data/places.sqlite data/places-receipt.json
python tools/build-offline-roads.py data/region.osm.pbf data/roads.sqlite data/roads-receipt.json
python tools/package-offline-pack.py --roads data/roads.sqlite --places data/places.sqlite --tiles data/region.mbtiles --osm-timestamp YOUR_VERIFIED_SOURCE_TIMESTAMP --output data/maze-pack.zip
```

The packager writes a ZIP and adjacent manifest, then verifies decompressed hashes. Inspect the results. Copy the generated manifest to app/src/main/assets/offline/manifest.json and rebuild the APK: imports accept the dataset hashes pinned in that build. Do not label newly generated data with the old lab timestamp. Keep original receipts and publish ODbL attribution/source/derived-database access as required when distributing a pack. This source repository does not include or distribute the maintainer's lab databases.

Run `python tools/fetch-offline-glyphs.py` before the app build to obtain the reviewed Noto Sans glyphs from MapLibre demotiles. This is a build-time download; retain the packaged font and repository license texts. The fetcher pins each glyph by upstream Git blob SHA and rejects changed content. It does not execute upstream code.

Validate regional POI counts, missing-coverage behavior, turn restrictions, route failures and complete airplane-mode cold starts on your explicit test target. Rebuilding an extract does not guarantee every shop, entrance, exit or route exists. The graph excludes unsupported conditional/access/barrier cases and ferries; route weights use estimates, not current traffic. These scripts and finite preview searches are research code, not a production navigation engine.

## Retained maintainer pack

See [docs/DATA-RELEASE.md](docs/DATA-RELEASE.md) for the exact release source timestamp, hashes and ODbL assets. `migrate-offline-grid.py INPUT OUTPUT RECEIPT` and `migrate-turn-kinds.py INPUT PLACES OUTPUT RECEIPT` preserve the historical grid/turn transformations for compatible older schemas; they are not steps to apply blindly to a current fresh build. The historical 0.3.1/0.3.2 migrated graph is not asserted byte reproducible from the current builder. The revised 0.4.0 graph is a fresh build; use its new receipt and hash rather than applying historical migrations.
