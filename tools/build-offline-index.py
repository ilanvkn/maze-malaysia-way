"""Build reproducible regional OSM search data. Never infer entrances or routing legality."""
import collections, hashlib, json, pathlib, sqlite3, sys, time
import osmium
source, output, receipt = map(pathlib.Path, sys.argv[1:4])
assert not output.exists(), 'Use a new output path; do not overwrite a dataset'
t0=time.monotonic(); db=sqlite3.connect(output)
db.executescript('''PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF;
CREATE TABLE places(id TEXT PRIMARY KEY,name TEXT,detail TEXT,lat REAL,lon REAL,kind TEXT,tags TEXT);
CREATE VIRTUAL TABLE search USING fts4(name,detail,tokenize=unicode61);
CREATE TABLE restrictions(id INTEGER PRIMARY KEY,tags TEXT,members TEXT);
CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT);
''')
counts=collections.Counter()
class Index(osmium.SimpleHandler):
 def add(self, obj, lat, lon, geometry):
  t=dict(obj.tags); counts[geometry]+=1
  if t.get('shop'): kind='shop'
  elif t.get('entrance') and t['entrance']!='no': kind='entrance'
  elif t.get('highway')=='motorway_junction': kind='motorway exit'
  elif t.get('addr:housenumber'): kind='address'
  elif t.get('name') and any(k in t for k in ('amenity','tourism','public_transport','railway','place')): kind='place'
  else: return
  counts[kind]+=1
  if not (0.5<=lat<=8.0 and 99<=lon<=120): counts['outside_bounds']+=1; return
  name=t.get('name') or t.get('brand') or t.get('addr:housenumber') or t.get('ref') or (kind+' '+t.get('entrance',t.get('shop','')))
  detail=' · '.join(filter(None,[kind,t.get('shop'),t.get('addr:street'),t.get('addr:city'),t.get('addr:postcode'), 'OSM '+geometry+'/'+str(obj.id), 'representative point' if geometry=='way' else '', 'access='+t['access'] if 'access' in t else '']))
  tags={k:v for k,v in t.items() if k in ('shop','entrance','access','wheelchair','opening_hours','level','ref','highway','oneway','barrier','addr:housenumber','addr:street','addr:city','addr:postcode','name:en','name:ms','name:zh','name:ta')}
  db.execute('INSERT INTO places VALUES(?,?,?,?,?,?,?)',(geometry+'/'+str(obj.id),name[:300],detail[:900],lat,lon,kind,json.dumps(tags,ensure_ascii=False)))
  db.execute('INSERT INTO search(docid,name,detail) VALUES(last_insert_rowid(),?,?)',(name+' '+' '.join(t.get(k,'') for k in ('name:en','name:ms','name:zh','name:ta','brand','ref')),detail))
 def node(self,n):
  if len(n.tags)==0: counts['node']+=1; return
  if n.location.valid(): self.add(n,n.location.lat,n.location.lon,'node')
 def way(self,w):
  if w.tags.get('highway'):
   counts['highway_ways']+=1
   for k in ('oneway','access','motor_vehicle','maxspeed','toll'):
    if k in w.tags: counts['way_tag_'+k]+=1
  if not any(k in w.tags for k in ('shop','entrance','addr:housenumber','amenity','tourism','place','railway','public_transport')): return
  pts=[(n.lat,n.lon) for n in w.nodes if n.location.valid()]
  if pts:
   # Bounding-box centre is a display point, never a claimed doorway or routable access point.
   self.add(w,(min(p[0] for p in pts)+max(p[0] for p in pts))/2,(min(p[1] for p in pts)+max(p[1] for p in pts))/2,'way')
 def relation(self,r):
  if r.tags.get('type')=='restriction':
   counts['turn_restrictions']+=1
   db.execute('INSERT INTO restrictions VALUES(?,?,?)',(r.id,json.dumps(dict(r.tags)),json.dumps([(m.type,m.ref,m.role) for m in r.members])))
reader=osmium.io.Reader(str(source)); header=reader.header(); stamp=header.get('osmosis_replication_timestamp'); reader.close()
Index().apply_file(str(source),locations=True,idx='flex_mem')
meta={'source_url':'https://download.geofabrik.de/asia/malaysia-singapore-brunei-latest.osm.pbf','source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'osm_timestamp':stamp,'license':'ODbL 1.0','attribution':'© OpenStreetMap contributors','scope':'Malaysia, Singapore and Brunei extract; bounding filter is not a country boundary','geometry':'nodes and way representative points; relations except restrictions not indexed','counts':dict(counts)}
for k,v in meta.items(): db.execute('INSERT INTO metadata VALUES(?,?)',(k,json.dumps(v,ensure_ascii=False)))
db.commit(); db.execute("INSERT INTO search(search) VALUES('optimize')"); db.commit()
assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
meta.update(rows=db.execute('SELECT count(*) FROM places').fetchone()[0],seconds=time.monotonic()-t0,bytes=output.stat().st_size)
db.close(); meta['sha256']=hashlib.sha256(output.read_bytes()).hexdigest();receipt.write_text(json.dumps(meta,indent=2)+'\n');print(json.dumps(meta,indent=2))
