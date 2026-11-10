"""Index every geometry segment, including roads with distant junctions."""
import math, struct

def segment_cells(points):
 cells=set()
 for a,b in zip(points,points[1:]):
  for lat in range(math.floor((min(a[0],b[0])+90)*100),math.floor((max(a[0],b[0])+90)*100)+1):
   for lon in range(math.floor((min(a[1],b[1])+180)*100),math.floor((max(a[1],b[1])+180)*100)+1):
    cells.add(lat*36000+lon)
    if len(cells)>10000:raise ValueError('Geometry exceeds segment-grid budget')
 return sorted(cells)

def build_segment_grid(db):
 db.execute('CREATE TABLE segment_cells(cell INTEGER NOT NULL,geom INTEGER NOT NULL,PRIMARY KEY(cell,geom)) WITHOUT ROWID')
 count=0
 for gid,blob in db.execute('SELECT id,points FROM geometry ORDER BY id'):
  points=list(struct.iter_unpack('<ff',blob))
  cells=segment_cells(points);db.executemany('INSERT INTO segment_cells VALUES(?,?)',((cell,gid) for cell in cells));count+=len(cells)
 db.execute('CREATE INDEX edges_geom ON edges(geom)')
 return {'schema':1,'cell_degrees':0.01,'rows':count,'scope':'Every line segment, not just graph junctions; no inferred access connectors'}
