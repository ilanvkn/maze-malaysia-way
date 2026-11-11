"""Original synthetic OSM: not a real Malaysian road or gate dataset."""
import pathlib,sqlite3,subprocess,sys,json,struct
root=pathlib.Path(__file__).resolve().parents[1];out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
nodes={};ways=[];rules=[];queries=[]
def node(i,x,y):nodes[i]=(3+y,101+x)
def way(i,ns,**tags):ways.append((i,ns,dict(highway='residential',name=f'Synthetic {i}',**tags)))
def query(name,a,b,status='route'):queries.append(dict(name=name,origin=a,destination=b,status=status))
# Long isolated one-way: endpoints are >350m from the query points. No SCC pairing.
node(1,0,0);node(2,.02,0);way(10,[1,2],oneway='yes')
query('oneway-forward',[3,101.005],[3,101.015]);query('oneway-reverse',[3,101.015],[3,101.005],'no_route')
# Two directed carriageways joined ONLY at the ends. No illegal median connector.
for i,x,y in [(11,0,.01),(12,.02,.01),(13,0,.0102),(14,.02,.0102)]:node(i,x,y)
way(20,[11,12],oneway='yes');way(21,[14,13],oneway='yes');way(22,[12,14],oneway='yes');way(23,[13,11],oneway='yes')
query('divided-turnaround',[3.01,101.005],[3.0102,101.005])
query('divided-forward',[3.01,101.005],[3.01,101.015])
query('median-ambiguous',[3.0101,101.005],[3.01,101.015],'ambiguous')
# Road cut by a locked gate. Adjacent supported stubs must not be re-snapped away.
for i,x,y in [(31,0,.02),(32,.009,.02),(33,.01,.02),(34,.011,.02),(35,.02,.02)]:node(i,x,y)
way(30,[31,32]);way(31,[32,33,34]);way(32,[34,35])
query('barrier-crossing',[3.02,101.005],[3.02,101.015],'no_route')
# Disconnected closer roads with a farther connected bypass; no endpoint teleport.
for i,x,y in [(41,0,.03),(42,.009,.03),(43,.011,.03),(44,.02,.03),(45,0,.0306),(46,.02,.0306)]:node(i,x,y)
way(40,[41,42]);way(41,[43,44]);way(42,[45,46])
query('no-farther-road-fallback',[3.03,101.005],[3.03,101.015],'no_route')
# Restriction at entry into a partial destination edge.
for i,x,y in [(51,0,.04),(52,.01,.04),(53,.01,.05),(54,.02,.04),(55,.02,.05)]:node(i,x,y)
way(50,[51,52]);way(51,[52,53]);way(52,[52,54,55,53])
rules.append((500,50,52,51,'no_left_turn'))
query('restricted-partial-destination',[3.04,101.005],[3.045,101.01])
# Reverse-tagged one-way and two-way same-segment clipping.
node(61,0,.06);node(62,.02,.06);way(60,[61,62],oneway='-1')
query('reverse-oneway-forward',[3.06,101.015],[3.06,101.005]);query('reverse-oneway-illegal',[3.06,101.005],[3.06,101.015],'no_route')
node(71,0,.07);node(72,.01,.075);node(73,.02,.07);way(70,[71,72,73])
query('curved-same-edge',[3.0725,101.005],[3.0725,101.015]);query('curved-same-edge-reverse',[3.0725,101.015],[3.0725,101.005])
node(81,.04,.09);node(82,.05,.09);way(80,[81,82])
xml='<osm version="0.6" generator="Maze synthetic approach fixture">'
for i,(lat,lon) in nodes.items():xml+=f'<node id="{i}" lat="{lat}" lon="{lon}">'+('<tag k="barrier" v="gate"/><tag k="access" v="no"/><tag k="locked" v="yes"/>' if i==33 else '')+'</node>'
for i,ns,tags in ways:xml+=f'<way id="{i}">'+''.join(f'<nd ref="{n}"/>' for n in ns)+''.join(f'<tag k="{k}" v="{v}"/>' for k,v in tags.items())+'</way>'
for i,f,v,t,k in rules:xml+=f'<relation id="{i}"><member type="way" ref="{f}" role="from"/><member type="node" ref="{v}" role="via"/><member type="way" ref="{t}" role="to"/><tag k="type" v="restriction"/><tag k="restriction" v="{k}"/></relation>'
xml+='</osm>';(out/'fixture.osm').write_text(xml)
subprocess.run([sys.executable,str(root/'tools/build-offline-roads.py'),str(out/'fixture.osm'),str(out/'fixture.sqlite'),str(out/'build.json')],check=True)
db=sqlite3.connect(out/'fixture.sqlite')
report={'synthetic':True,'nodes':[list(r) for r in db.execute('SELECT id,lat,lon FROM nodes')],'geometry':[{'id':gid,'name':name,'points':list(struct.iter_unpack('<ff',blob))} for gid,blob,name in db.execute('SELECT id,points,name FROM geometry')],'edges':[list(r) for r in db.execute('SELECT src,dst,way,meters,seconds,geom,reverse FROM edges')],'turns':[list(r) for r in db.execute('SELECT via,fromway,toway,onlyturn,kind FROM turns')],'queries':queries}
(out/'graph.json').write_text(json.dumps(report,indent=2)+'\n')
