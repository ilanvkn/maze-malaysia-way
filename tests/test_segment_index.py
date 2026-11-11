import unittest,sys,sqlite3,struct
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from offline_segment_grid import build_segment_grid,segment_cells
class SegmentIndexTest(unittest.TestCase):
 def test_long_edge_is_discoverable_between_distant_junctions(self):
  db=sqlite3.connect(':memory:');db.executescript('CREATE TABLE geometry(id INTEGER PRIMARY KEY,points BLOB);CREATE TABLE edges(geom INTEGER);')
  points=[(3.,101.),(3.,101.04)];db.execute('INSERT INTO geometry VALUES(?,?)',(1,b''.join(struct.pack('<ff',*p) for p in points)));db.execute('INSERT INTO edges VALUES(1)')
  build_segment_grid(db);middle=int((3+90)*100)*36000+int((101.02+180)*100)
  self.assertEqual([(1,)],db.execute('SELECT geom FROM segment_cells WHERE cell=?',(middle,)).fetchall())
  self.assertEqual('ok',db.execute('PRAGMA integrity_check').fetchone()[0])
 def test_bent_line_does_not_fill_its_entire_bounding_box(self):
  cells=set(segment_cells([(3.,101.),(3.,101.04),(3.04,101.04)]))
  absent=int((3.02+90)*100)*36000+int((101.01+180)*100)
  self.assertNotIn(absent,cells)
if __name__=='__main__':unittest.main()
