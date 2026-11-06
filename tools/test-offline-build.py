import json,pathlib,sqlite3,subprocess,sys,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class GraphBuildProof(unittest.TestCase):
 def test_access_direction_and_ambiguous_turns(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d);nodes=''.join(f'<node id="{i}" lat="3.0" lon="{101+i*.001}"'+('><tag k="barrier" v="gate"/></node>' if i==9 else '/>') for i in range(1,11))
   def way(i,ids,tags):return f'<way id="{i}">'+''.join(f'<nd ref="{n}"/>' for n in ids)+''.join(f'<tag k="{k}" v="{v}"/>' for k,v in dict(highway='residential',**tags).items())+'</way>'
   ways=way(10,[1,2],{'oneway':'yes'})+way(20,[2,3],{'access':'private'})+way(30,[3,4],{'access:conditional':'yes @ (Mo-Fr)'})+way(40,[4,5],{'motor_vehicle:forward':'no'})+way(50,[5,6],{'oneway:motor_vehicle':'-1'})+way(60,[8,9,10],{})+way(70,[2,6],{})+way(80,[5,6,7],{})
   relation='<relation id="99"><member type="way" ref="70" role="from"/><member type="node" ref="6" role="via"/><member type="way" ref="80" role="to"/><tag k="type" v="restriction"/><tag k="restriction" v="only_right_turn"/></relation>'
   (p/'test.osm').write_text('<osm version="0.6">'+nodes+ways+relation+'</osm>')
   subprocess.run([sys.executable,str(ROOT/'tools/build-offline-roads.py'),str(p/'test.osm'),str(p/'roads.sqlite'),str(p/'receipt.json')],check=True,capture_output=True)
   db=sqlite3.connect(p/'roads.sqlite');edges=list(db.execute('SELECT src,dst,way FROM edges'))
   self.assertIn((1,2,10),edges);self.assertNotIn((2,1,10),edges)
   self.assertIn((6,5,50),edges);self.assertNotIn((5,6,50),edges)
   for excluded in (20,30,40,60):self.assertFalse(any(e[2]==excluded for e in edges))
   self.assertIn((6,70,-1,1),list(db.execute('SELECT via,fromway,toway,onlyturn FROM turns')))
   self.assertEqual(db.execute('PRAGMA integrity_check').fetchone()[0],'ok')
   self.assertEqual(db.execute('SELECT can_start,can_end FROM spatial WHERE id=1').fetchone(),(1,0))
   comp=dict(db.execute('SELECT id,component FROM spatial'));self.assertNotEqual(comp[1],comp[2]);self.assertEqual(comp[5],comp[6])
 def test_via_way_restrictions_keep_roads_but_block_the_entry(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d)
   nodes=''.join(f'<node id="{i}" lat="3" lon="{101+i*.001}"/>' for i in range(1,13))
   def way(i,ns):return f'<way id="{i}">'+''.join(f'<nd ref="{n}"/>' for n in ns)+'<tag k="highway" v="residential"/></way>'
   def rule(i,f,v,t,kind,tag='restriction'):return f'<relation id="{i}"><member type="way" ref="{f}" role="from"/><member type="way" ref="{v}" role="via"/><member type="way" ref="{t}" role="to"/><tag k="type" v="restriction"/><tag k="{tag}" v="{kind}"/></relation>'
   # Normal traffic entering via way 20 from way 40 must remain possible.
   xml=nodes+way(10,[1,2])+way(20,[2,3])+way(30,[3,4])+way(40,[5,2])+way(50,[6,7])+way(60,[7,8])
   xml+=way(70,[2,3])+rule(93,70,20,30,'no_u_turn')
   xml+=way(80,[4,8])+'<relation id="94"><member type="way" ref="80" role="from"/><member type="way" ref="20" role="via"/><member type="way" ref="30" role="via"/><member type="way" ref="10" role="to"/><tag k="type" v="restriction"/><tag k="restriction" v="no_right_turn"/></relation>'
   xml+=way(90,[9,10])+way(100,[10,11])+way(110,[11,12])+rule(95,90,100,110,'no_right_turn @ (Mo-Fr)','restriction:motorcar:conditional')
   xml+=rule(90,10,20,30,'no_right_turn')+rule(91,50,60,30,'only_straight_on')+rule(92,40,20,30,'no_right_turn','restriction:hgv')
   (p/'test.osm').write_text('<osm version="0.6">'+xml+'</osm>')
   subprocess.run([sys.executable,str(ROOT/'tools/build-offline-roads.py'),str(p/'test.osm'),str(p/'roads.sqlite'),str(p/'receipt.json')],check=True,capture_output=True)
   db=sqlite3.connect(p/'roads.sqlite')
   self.assertEqual({r[0] for r in db.execute('SELECT distinct way FROM edges')},{10,20,30,40,50,60,70,80})
   turns=list(db.execute('SELECT via,fromway,toway,onlyturn FROM turns'))
   self.assertIn((2,10,20,0),turns)
   self.assertIn((7,50,-1,1),turns)
   self.assertFalse(any(r[1]==40 for r in turns))
   # Multiple shared nodes must all fail closed, independent of member order.
   receipt=json.loads((p/'receipt.json').read_text())
   self.assertEqual(receipt['counts']['via_way_entry_approaches_blocked'],5)
   self.assertIn((4,80,30,0),turns)
   self.assertIn((2,70,20,0),turns)
   self.assertIn((3,70,20,0),turns)
if __name__=='__main__':unittest.main(verbosity=2)
