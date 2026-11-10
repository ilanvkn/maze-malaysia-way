"""Historical 0.4.0 junction-routing diagnostic; not the current 0.4.2 segment router."""
import sqlite3,math,heapq,time,json,sys,urllib.request,urllib.parse,argparse,hashlib
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('database',type=Path)
parser.add_argument('--output',type=Path,required=True)
parser.add_argument('--legacy-junction',action='store_true',help='Explicitly run the historical junction algorithm on a segment-indexed graph')
args=parser.parse_args()
case=args.output.parent;case.mkdir(parents=True,exist_ok=True)
if not args.database.is_file():raise SystemExit("Select an existing reviewed public graph")
DB=args.database.resolve().as_uri()+"?mode=ro"
with sqlite3.connect(DB,uri=True) as check_db:
 if check_db.execute("SELECT 1 FROM sqlite_master WHERE name='segment_cells'").fetchone() and not args.legacy_junction:
  raise SystemExit("This is the historical junction router. Use the Android ApproachProof for current segment routing, or explicitly pass --legacy-junction for a historical comparison.")
if args.output.exists():raise SystemExit('Choose a new report path; preserve prior measurements')
queries=[('KL Sentral–KLCC',(3.1341,101.6869),(3.15785,101.71165)),('KLCC–KL Sentral',(3.15785,101.71165),(3.1341,101.6869)),('Merdeka–Sentral',(3.1478,101.6937),(3.1341,101.6869)),('George Town–Gurney',(5.4141,100.3288),(5.4398311,100.3091867)),('Kuching–Vivacity',(1.5574,110.3500),(1.52674275,110.3692989)),('Kota Kinabalu–Suria',(5.9804,116.0735),(5.98761945,116.07716655)),('KL Sentral–George Town',(3.1341,101.6869),(5.4141,100.3288))]
def distance(p,q):
 a,b=map(math.radians,(p[0],q[0]));return 12742000*math.asin(min(1,math.sqrt(math.sin((b-a)/2)**2+math.cos(a)*math.cos(b)*math.sin(math.radians(q[1]-p[1])/2)**2)))
def maze(a,b,weight):
 db=sqlite3.connect(DB,uri=True)
 def snaps(p,origin):
  cells=[lat*36000+lon for lat in range(int((p[0]-.004+90)*100),int((p[0]+.004+90)*100)+1) for lon in range(int((p[1]-.004+180)*100),int((p[1]+.004+180)*100)+1)];best={}
  for n,lat,lon,component in db.execute('SELECT id,lat,lon,component FROM spatial WHERE '+('can_start' if origin else 'can_end')+'=1 AND cell IN ('+','.join('?'*len(cells))+')',cells):
   d=distance(p,(lat,lon))
   if d<=350 and (component not in best or d<best[component][0]):best[component]=(d,n)
  return best
 starts,ends=snaps(a,True),snaps(b,False);_,start,end=min((aa[0]+ends[k][0],aa,ends[k]) for k,aa in starts.items() if k in ends)
 s,t=start[1],end[1];target=db.execute('SELECT lat,lon FROM nodes WHERE id=?',(t,)).fetchone()
 first=(s,0,0);q=[(0,0,first)];costs={first:0};prev={};count=0;t0=time.monotonic();found=None
 long_trip=distance(a,b)>50000;max_states=250000 if long_trip else 120000
 while q:
  if count>=max_states or time.monotonic()-t0>(45 if long_trip else 15):raise RuntimeError('host diagnostic budget')
  _,cost,key=heapq.heappop(q);n,w,previous=key
  if cost!=costs[key]:continue
  if n==t:found=key;break
  rules=db.execute('SELECT toway,onlyturn,kind FROM turns WHERE via=? AND fromway=?',(n,w)).fetchall()
  for dst,way,seconds,meters,gid,lat,lon in db.execute('SELECT e.dst,e.way,e.seconds,e.meters,e.geom,n.lat,n.lon FROM edges e JOIN nodes n ON n.id=e.dst WHERE e.src=?',(n,)):
   def matches(r):
    dest,only,kind=r
    if dest!=way:return False
    if dest!=w:return True
    if kind in ('no_u_turn','only_u_turn'):return dst==previous
    if kind in ('no_straight_on','only_straight_on'):return dst!=previous
    return True
   if any(r[0]==-1 for r in rules) or any(not r[1] and matches(r) for r in rules) or (any(r[1] for r in rules) and not any(r[1] and matches(r) for r in rules)):continue
   k=(dst,way,n);nc=cost+seconds
   if nc<costs.get(k,math.inf):costs[k]=nc;prev[k]=(key,meters,seconds,gid);heapq.heappush(q,(nc+weight*distance((lat,lon),target)/40,nc,k))
  count+=1
 if not found:raise RuntimeError('disconnected')
 meters=seconds=0;ways=[]
 while found!=first:
  ways.append(found[1]);found,m,s,gid=prev[found];meters+=m;seconds+=s
 db.close()
 return dict(meters=meters,estimated_seconds=seconds,expanded=count,host_seconds=time.monotonic()-t0,snap_meters=[start[0],end[0]],snapped_nodes=[start[1],end[1]],ways=list(reversed(ways)))
def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
with sqlite3.connect(DB,uri=True) as c: metadata={k:json.loads(v) for k,v in c.execute('SELECT key,value FROM metadata')}
args.output.with_suffix('.identity.json').write_text(json.dumps(dict(database_sha256=sha(args.database),metadata=metadata,tool_sha256=sha(Path(__file__)),measurement='Host diagnostic; Android performance is measured separately'),indent=2)+'\n')
report=[]
for name,a,b in queries:
 row=dict(name=name,origin=a,destination=b,maze={})
 for mode,weight in [('quick',4),('careful',1)]:
  try:row['maze'][mode]=maze(a,b,weight)
  except Exception as e:row['maze'][mode]={'error':str(e)}
 params=urllib.parse.urlencode([('point',','.join(map(str,a))),('point',','.join(map(str,b))),('profile','car'),('points_encoded','false'),('details','osm_way_id')])
 try:
  raw=json.load(urllib.request.urlopen('http://127.0.0.1:8989/route?'+params,timeout=60));(case/(args.output.stem+'-graphhopper-'+str(len(report))+'.json')).write_text(json.dumps(raw,indent=2))
  p=raw['paths'][0];row['graphhopper']=dict(meters=p['distance'],estimated_seconds=p['time']/1000,snapped_waypoints=p['snapped_waypoints'],request=params)
  # Separate original-point comparison from a controlled junction-endpoint one.
  reference=row['maze']['careful'] if 'snapped_nodes' in row['maze']['careful'] else row['maze']['quick']
  if 'snapped_nodes' in reference:
   with sqlite3.connect(DB,uri=True) as c: snapped=[c.execute('SELECT lat,lon FROM nodes WHERE id=?',(n,)).fetchone() for n in reference['snapped_nodes']]
   controlled=urllib.parse.urlencode([('point',','.join(map(str,p))) for p in snapped]+[('profile','car'),('points_encoded','false')])
   matched=json.load(urllib.request.urlopen('http://127.0.0.1:8989/route?'+controlled,timeout=60))
   (case/(args.output.stem+'-matched-'+str(len(report))+'.json')).write_text(json.dumps(matched,indent=2))
   p=matched['paths'][0];row['graphhopper_maze_junctions']=dict(meters=p['distance'],estimated_seconds=p['time']/1000,snapped_waypoints=p['snapped_waypoints'],requested_junctions=snapped)
 except Exception as e:row['graphhopper']={'error':str(e)}
 report.append(row);print(name,{k:v.get('meters',v.get('error')) for k,v in row['maze'].items()},row['graphhopper'].get('meters',row['graphhopper'].get('error')),flush=True)
 args.output.write_text(json.dumps(report,indent=2)+'\n')
