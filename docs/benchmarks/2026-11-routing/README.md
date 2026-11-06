# Routing comparison · 6 November 2026

Public landmarks only. These are host diagnostics and separately recorded Android measurements, not field-navigation qualification.

## Reproduce the mature-engine baseline

GraphHopper 10.2, Apache 2.0, Java 17. The official Maven artifact is pinned by SHA256 in `graphhopper-identity.json`; its upstream Maven SHA1 was checked independently. No GraphHopper binary or derived graph is bundled with Maze.

1. Obtain the **same full** `malaysia-singapore-brunei.osm.pbf`, timestamp `2026-11-04T21:21:03Z`, SHA256 `d3036a09bdad1bcb2b90054dce5b84b9a9d957134bfdd7806cd5bf98e06dfb48`. A rolling “latest” extract will not reproduce this comparison. See DATA.md for source and redistribution terms.
2. Download [the official 10.2 JAR](https://repo.maven.apache.org/maven2/com/graphhopper/graphhopper-web/10.2/graphhopper-web-10.2.jar), retain its [Apache license](https://github.com/graphhopper/graphhopper/blob/10.2/LICENSE.txt), and place it beside the PBF and `graphhopper-config.yml` in a separate working directory.
3. Run `java -Xmx1600m -jar graphhopper-web-10.2.jar import graphhopper-config.yml`, then `java -Xmx1200m -jar graphhopper-web-10.2.jar server graphhopper-config.yml`. Both listeners bind to loopback. Stop only this owned server afterwards.
4. In the Maze checkout run `python3 tools/benchmark-routing.py /explicit/path/to/roads.sqlite --output /new/path/after.json`. Reports must use unused paths. Full GraphHopper geometry/responses are written beside the report. Build the SQLite graph with the pinned requirements and `tools/build-offline-roads.py`.

The stock car custom model has motorcar/motor_vehicle turn costs and a 60-second U-turn penalty. CH/LM are disabled; routing is flexible bidirectional A*. An initial CH preparation was stopped before completion; a separate successful flexible import supplied every baseline result. No request went to a paid or public demo routing endpoint.

## Interpret the comparison

Both engines receive the identical original points and source PBF, **not identical policies**. GraphHopper's stock model admits roads Maze declines, has different speed/priority/turn costs, and projects to road segments. Maze still selects a supported junction within 350 m and pairs strong components. The report includes a second GraphHopper request at Maze's chosen junctions to help separate endpoint choice from graph effects; GraphHopper's returned snap may still differ. Compare actual snapped coordinates before drawing a conclusion.

For the original KLCC point, GraphHopper's default snap reaches customer car-park access (OSM way 507063858, `access=customers`, tunnel, maxheight 2.1). Maze does not treat a tower point as a verified driving entrance. A distance win alone is not a legal-access or fastest-route verdict. Neither source establishes present-day signs or traffic.

## Root cause and conservative correction

The old builder deleted **all member ways** for unsupported via-way restrictions. This erased normal through traffic on streets such as Jalan Raja Chulan, not merely the prohibited turn. For example, OSM restriction 17161066 involves ways 715625587,775578844,504344460 and135623287; restriction 16228941 is an only-straight sequence through 676486576,1200981897 and1200981900.

The builder now retains those streets and blocks entry from the restricted `from` way into every touching `via` way. For `only_*` it blocks the entire unsupported approach. It does not rely on relation-member order or pick a single arbitrary shared node. Missing entry topology still excludes the `from` way. Other vehicle-only restrictions without a motorcar/general rule no longer delete car roads; conditional rules remain fail-closed. Existing node-via turn rules, barriers, one-way and private/dimensional/conditional exclusions remain enforced.

This is intentionally more restrictive than executing a complete via-way sequence. It can still reject legal turns and cause detours. It does not claim full multi-way restriction support, entrance access, guidance, traffic or nationwide correctness. The fixture checks preserved roads, blocked entries, multiple shared nodes, vehicle-specific scope and existing exclusions. The Android controller rejects the KL regression above 8.5 km in either tested mode.

## Final host distance results

Quick now uses weight 4; historical Quick used 8. Careful remains 1. This is a measured preset change, not an optimality or universal latency guarantee. The graph-only weight 8 candidate already reduced KL to 6.34 km, but its intercity query exceeded 250,000 expansions on Android. Lowering the weight resolved that sampled regression without increasing any budget. Penang illustrates the tradeoff: final Quick takes 5.28 km, versus 4.98 km for the graph-only weight 8 candidate; Careful takes 4.86 km. Stored travel-time cost, not metres alone, drives the search.

| Public journey | Old Quick km | Final Quick km | Final Careful km | GraphHopper original points km | GraphHopper Maze junctions km |
|---|---:|---:|---:|---:|---:|
| KL Sentral–KLCC | 14.86 | 6.34 | 6.34 | 7.63 | 5.70 |
| KLCC–KL Sentral | 10.15 | 7.07 | 7.07 | 6.56 | 5.71 |
| Merdeka–Sentral | 2.69 | 2.69 | 2.69 | 3.34 | 2.69 |
| George Town–Gurney | 5.78 | 5.28 | 4.86 | 4.29 | 4.55 |
| Kuching–Vivacity | 17.27 | 6.11 | 6.11 | 5.91 | 5.92 |
| Kota Kinabalu–Suria | 1.70 | 1.67 | 1.67 | 1.81 | 1.65 |
| KL Sentral–George Town | 431.87¹ | 398.40 | budget | 353.13 | 353.15 |

¹ Historical 0.3.1 Android sample, not a same-session host timing. The six older host pairs are retained in `before.json`. `after.json`, `after.identity.json` and `graph-build.json` retain final exact distances, costs, endpoints and dataset identity. Different tie ordering may change a weighted path; Android results are recorded separately.

## Android qualification

The publishable minified APK is SHA256 `fe991405f4c84d9c235fe25059b696a1a7146452ad3ea1a1f835b1646912bb7d`, version 0.4.0-offline-beta-lab / code 6. Installed APK hash matched. The explicitly owned API 35 ARM64 emulator had airplane mode on and no validated external network. Its normal app heap growth limit was 192 MiB; no large-heap flag or larger routing cap was introduced.

All seven Quick journeys and five Careful city journeys passed. Separate KL first/second requests after process restart passed in both modes: Quick 346/267 ms; Careful 1111/889 ms. The route database was newly opened in each process, but OS storage was warmed by import and the controller performs search/tile checks before routing. These are finite samples, not cold-storage/startup, p95, battery or competitor speed measurements. The intercity Quick result was 398.40 km/14,346 ms. Careful intercity remains a budget failure in the host diagnostic and is not claimed as qualified on Android.

The four public JSON controller reports and `qualification-final.json` preserve 16 successful route computations. They also check offline search, available/missing access points, local styles, glyphs and regional tiles. Actual route pixels and native layout checks are documented in QUALITY.md. No field-driving, full restriction, traffic or comprehensive accessibility claim follows.
