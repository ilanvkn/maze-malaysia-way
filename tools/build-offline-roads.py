"""Conservative motorcar graph, not a general-purpose navigation engine."""
from offline_graph_grid import build_grid
import osmium,sqlite3,sys,pathlib,json,collections,math,struct,time,hashlib
source,out,receipt=map(pathlib.Path,sys.argv[1:4]);assert not out.exists();t0=time.monotonic()
allowed={'motorway':100,'motorway_link':45,'trunk':80,'trunk_link':40,'primary':60,'primary_link':35,'secondary':50,'secondary_link':30,'tertiary':40,'tertiary_link':25,'unclassified':30,'residential':30,'living_street':10,'service':15}
refs=collections.Counter();via=set();blocked=set();barriers=set();restrictions=[];via_way=[];restriction_nodes={};counts=collections.Counter()
def usable(t):
 t=dict(t)
 if t.get('highway') not in allowed or t.get('area')=='yes':return False
 access=next((t[k] for k in ('motorcar','motor_vehicle','vehicle','access') if k in t),'yes')
 if access not in ('yes','permissive','designated'):return False
 if any('conditional' in k or (k.startswith(('access:', 'vehicle:', 'motor_vehicle:', 'motorcar:')) and k.endswith((':forward', ':backward'))) for k in t):return False
 if any(k in t for k in ('maxheight','maxwidth','maxlength','maxweight')):return False
 if t.get('impassable')=='yes' or t.get('construction') or t.get('proposed'):return False
 return True
class Pass1(osmium.SimpleHandler):
 def node(self,n):
  if n.tags.get('barrier') not in (None,'no','cattle_grid','toll_booth') or n.tags.get('ford') in ('yes','stepping_stones'):barriers.add(n.id)
 def way(self,w):
  if usable(w.tags):
   for n in w.nodes:refs[n.ref]+=1
 def relation(self,r):
  if r.tags.get('type')!='restriction':return
  if 'motorcar' in r.tags.get('except','').split(';'):return
  members=[(m.type,m.ref,m.role) for m in r.members];v=[m[1] for m in members if m[2]=='via' and m[0]=='n'];f=[m[1] for m in members if m[2]=='from' and m[0]=='w'];to=[m[1] for m in members if m[2]=='to' and m[0]=='w'];kind=r.tags.get('restriction:motorcar',r.tags.get('restriction:motor_vehicle',r.tags.get('restriction','')))
  if len(v)==len(f)==len(to)==1 and kind.startswith(('no_','only_')) and not any('conditional' in k for k in dict(r.tags)):
   via.add(v[0]);restrictions.append((v[0],f[0],to[0],int(kind.startswith('only_')),kind))
  elif len(f)==len(to)==1 and not v and any(m[0]=='w' and m[2]=='via' for m in members) and kind.startswith(('no_','only_')) and not any('conditional' in k for k in dict(r.tags)):
   # We do not yet execute via-way sequences. Forbid their entry approach instead
   # of deleting all member roads (which erased ordinary KL through routes).
   vias=tuple(m[1] for m in members if m[0]=='w' and m[2]=='via')
   via_way.append((f[0],vias,int(kind.startswith('only_'))))
  elif not kind and any(k.startswith('restriction:') and k!='restriction:conditional' for k in dict(r.tags)) and not any('conditional' in k for k in dict(r.tags)):
   counts['other_vehicle_restrictions_skipped']+=1
  else:
   blocked.update(m[1] for m in members if m[0]=='w');counts['unsupported_restriction_ways_excluded']+=1
Pass1().apply_file(str(source));needed_ways={w for f,vs,_ in via_way for w in (f,*vs)};print('pass1',len(refs),len(restrictions),flush=True)
db=sqlite3.connect(out);db.executescript('''PRAGMA journal_mode=OFF;PRAGMA synchronous=OFF;
CREATE TABLE nodes(id INTEGER PRIMARY KEY,lat REAL,lon REAL);
CREATE TABLE spatial(id INTEGER PRIMARY KEY,cell INTEGER,lat REAL,lon REAL);
CREATE TABLE geometry(id INTEGER PRIMARY KEY,points BLOB,name TEXT);
CREATE TABLE edges(src INTEGER,dst INTEGER,way INTEGER,meters REAL,seconds REAL,geom INTEGER,reverse INTEGER);
CREATE TABLE turns(via INTEGER,fromway INTEGER,toway INTEGER,onlyturn INTEGER,kind TEXT);
CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT);
''')
def distance(a,b):
 p,q=map(math.radians,(a[0],b[0]));d=math.radians(b[1]-a[1]);return 6371000*2*math.asin(min(1,math.sqrt(math.sin((q-p)/2)**2+math.cos(p)*math.cos(q)*math.sin(d/2)**2)))
class Pass2(osmium.SimpleHandler):
 def way(self,w):
  t=dict(w.tags)
  if w.id in needed_ways:restriction_nodes[w.id]={n.ref for n in w.nodes}
  if w.id in blocked or not usable(t):return
  ns=list(w.nodes)
  if any(not n.location.valid() for n in ns):counts['missing_location_ways']+=1;return
  if any(n.ref in barriers for n in ns):counts['barrier_ways_excluded']+=1;return
  speed=allowed[t['highway']]
  try:
   raw=t.get('maxspeed','');s=float(raw.split()[0]);speed=min(speed,s*1.609344 if 'mph' in raw else s)
  except (ValueError,IndexError):pass
  speed=max(5,min(130,speed));one=t.get('oneway:motorcar',t.get('oneway:motor_vehicle',t.get('oneway', 'yes' if t.get('junction')=='roundabout' or t['highway']=='motorway' else 'no')))
  if one not in ('yes','1','true','-1','no','0','false'):counts['unknown_oneway_excluded']+=1;return
  cuts=[i for i,n in enumerate(ns) if i in (0,len(ns)-1) or refs[n.ref]>1 or n.ref in via]
  for a,b in zip(cuts,cuts[1:]):
   if b<=a:continue
   segment=ns[a:b+1];points=[(n.lat,n.lon) for n in segment];meters=sum(distance(p,q) for p,q in zip(points,points[1:]))
   if meters<0.1:continue
   for n in (segment[0],segment[-1]):
    db.execute('INSERT OR IGNORE INTO nodes VALUES(?,?,?)',(n.ref,n.lat,n.lon))
   blob=b''.join(struct.pack('<ff',lat,lon) for lat,lon in points);gid=db.execute('INSERT INTO geometry(points,name) VALUES(?,?)',(blob,t.get('name',t.get('ref',t['highway']))[:200])).lastrowid
   if one!='-1':db.execute('INSERT INTO edges VALUES(?,?,?,?,?,?,0)',(segment[0].ref,segment[-1].ref,w.id,meters,meters/(speed/3.6),gid))
   if one not in ('yes','1','true'):db.execute('INSERT INTO edges VALUES(?,?,?,?,?,?,1)',(segment[-1].ref,segment[0].ref,w.id,meters,meters/(speed/3.6),gid))
  counts['included_ways']+=1
Pass2().apply_file(str(source),locations=True,idx='flex_mem')
db.execute('CREATE INDEX edges_src ON edges(src)')
for from_way,vias,only in via_way:
 # Relation member order is not an entry-junction guarantee. Block every shared
 # entry into a via way; none can begin the unsupported restricted sequence.
 entries={(n,v) for v in vias for n in restriction_nodes.get(from_way,set()) & restriction_nodes.get(v,set())}
 if entries:
  for junction,first_via in sorted(entries):
   restrictions.append((junction,from_way,-1 if only else first_via,1 if only else 0,'blocked_via_way_entry'))
   counts['via_way_entry_approaches_blocked']+=1
 else:
  # Ambiguous/missing topology: no guessed junction or silently ignored rule.
  db.execute('DELETE FROM edges WHERE way=?',(from_way,))
  counts['missing_via_way_from_ways_excluded']+=1
# A way identifier alone cannot distinguish two outgoing segments of that same way.
# Block every outgoing edge at ambiguous restriction approaches, retaining a safe failure.
ambiguous=[]
for v,f,t,only,kind in restrictions:
 n=db.execute('SELECT count(*) FROM edges WHERE src=? AND way=?',(v,t)).fetchone()[0]
 if n>1 and only and not (f==t and kind in ('only_straight_on','only_u_turn')):
  ambiguous.append((v,f))
for v,f in ambiguous:
 db.execute('DELETE FROM edges WHERE src=? AND way=?',(v,f))
 # Also forbid all turns after approaching on f: onlyturn with unreachable sentinel.
 restrictions.append((v,f,-1,1,"blocked_ambiguous"))
counts['ambiguous_only_turn_approaches_blocked']=len(ambiguous)
db.executemany('INSERT INTO turns VALUES(?,?,?,?,?)',restrictions)
db.execute('CREATE INDEX turns_via ON turns(via,fromway)');grid=build_grid(db)
meta={'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'policy':'conservative motorcar; no conditional/dimensional/private/destination/barrier ways; via-way restrictions conservatively block entry approaches; missing entry topology excludes from way; other unsupported restrictions exclude member ways; node-via no/only restrictions retained','limitations':'not active navigation; no lane guidance or traffic; nearest junction snap; defaults estimate time; conservative exclusions can disconnect legitimate roads','counts':dict(counts),'grid':grid,'nodes':db.execute('SELECT count(*) FROM nodes').fetchone()[0],'edges':db.execute('SELECT count(*) FROM edges').fetchone()[0],'turns':len(restrictions)}
for k,v in meta.items():db.execute('INSERT INTO metadata VALUES(?,?)',(k,json.dumps(v)))
db.commit();assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok';db.close();meta.update(seconds=time.monotonic()-t0,bytes=out.stat().st_size,sha256=hashlib.sha256(out.read_bytes()).hexdigest());receipt.write_text(json.dumps(meta,indent=2)+'\n');print(json.dumps(meta,indent=2))
