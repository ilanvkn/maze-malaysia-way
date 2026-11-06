# Maintainer regional packs · OSM 4 November 2026

Each preview's matching ZIP distributes derived OpenStreetMap databases under ODbL 1.0, independently of the MIT app code. It is a Malaysia/Singapore/Brunei regional extract, not a complete Malaysia-only gazetteer.

- Attribution: **© OpenStreetMap contributors**. [OSM copyright and attribution](https://www.openstreetmap.org/copyright); [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/).
- Regional source: [Geofabrik Malaysia/Singapore/Brunei](https://download.geofabrik.de/asia/malaysia-singapore-brunei.html), fetched from `https://download.geofabrik.de/asia/malaysia-singapore-brunei-latest.osm.pbf`. Replication timestamp: **2026-11-04T21:21:03Z**. The upstream `latest` URL changes; use the exact release asset/hash for this retained source.
- Retained PBF SHA256: `d3036a09bdad1bcb2b90054dce5b84b9a9d957134bfdd7806cd5bf98e06dfb48`. The [retained source PBF](https://github.com/ilanvkn/maze-malaysia-way/releases/download/v0.3.1-community.1/malaysia-singapore-brunei-2026-11-04.osm.pbf) is a separate release asset; it is not relicensed MIT.
## Revised r2 pack · 0.4.0

`Maze-Malaysia-offline-2026-11-04-r2.zip`: 661,639,476 bytes, SHA256 `7d2a7db4a79fdcfcfc228f5553b946b625596a0ad2da0307c125d11c5fa5af9d`.

The revised roads database is 731,869,184 bytes, SHA256 `032c24ae6e8c01b47d81181183e645efcfebe699f0c48f3b094bd27caf6518da`: 2,363,706 nodes, 5,300,250 directed edges and 172,950 turn rows. It was freshly built from the retained source PBF with the corrected conservative via-way entry policy; places and map tiles retain their previous hashes below. Full graph counts, policy and matched-engine measurements are in [the routing report](benchmarks/2026-11-routing/README.md). The app pins this revised graph; old ZIPs are rejected. Pack staging can require more than 3 GB while updating. Saved trips are preserved and can contain older geometry until refreshed.

The source PBF remains the separate ODbL asset from the original data release; the rolling upstream URL is not a frozen snapshot. No more recent OSM date is implied. The app manifest has a human-readable display summary; import qualification checks the three identical dataset hashes/sizes in the ZIP, not that summary.

## Original pack · 0.3.1 / 0.3.2 (preserved history)

- ZIP SHA256: `48f165809b116152e3dba2ad501c4d512f1d38490e9f60f5e6805475d0177bc1`, 646,330,117 bytes. It includes the derived databases, manifest and NOTICE. The databases themselves are available by extracting that ZIP.

| Dataset | Bytes | SHA256 |
|---|---:|---|
| `roads.sqlite` | 716730368 | `260ee312be9476e7d97b1b02b3fc7bb801242f5630b2969a9920e271d6e83b7a` |
| `places.sqlite` | 236511232 | `de7c21fa2b17ecf981182ddda9d1c00d7c3a6ad26b639485cad13f23f0c29ca2` |
| `region.mbtiles` | 294506496 | `fca6976425e3b1bab3719375dd1743573f825935a5a6550208386874b723a01b` |

Map tiles use Geofabrik Shortbread schema 1.0, z0–14 with overzoom, bounds 99.25611,0.830813,119.6683,7.736849. Tile packaging date is not the modification date of each feature. The 470,369 indexed records include shops, entrances, motorway exits, addresses and places; ways may use representative points and POI relations are not indexed. Missing or obsolete data remains possible.

Source transformation recipes are in DATA.md and tools/. This retained graph includes grid/component and turn-kind migrations. The current fresh builder may produce different bytes or conservative exclusions; **bit reproducibility of the retained ZIP is not claimed**. Build a new pack with a new reviewed manifest/version instead of reusing these hashes for different output. See the migration helpers for the retained transformations. A matching source checkout and pack can be inspected without contacting a hosted map/search/routing API.

Noto glyphs bundled in the APK are OFL 1.1, with the MapLibre demotiles packaging notice retained. The ZIP contains no personal trips, GPS history or model weights. Download/build network access and storage are separate from offline app operation.
