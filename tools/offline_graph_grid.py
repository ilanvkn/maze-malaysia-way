"""Portable grid with directed strongly connected components; no routing service dependency."""
from array import array
from collections import Counter
from itertools import accumulate

def build_grid(db):
 lookup={row[0]:i for i,row in enumerate(db.execute('SELECT id FROM nodes ORDER BY id'))};n=len(lookup)
 out_count=array('i',[0])*n;in_count=array('i',[0])*n
 for src,dst in db.execute('SELECT src,dst FROM edges'):out_count[lookup[src]]+=1;in_count[lookup[dst]]+=1
 out_offset=array('i',[0]);out_offset.extend(accumulate(out_count));in_offset=array('i',[0]);in_offset.extend(accumulate(in_count));edges=out_offset[-1]
 forward=array('i',[0])*edges;backward=array('i',[0])*edges;out_pos=array('i',out_offset[:-1]);in_pos=array('i',in_offset[:-1])
 for src,dst in db.execute('SELECT src,dst FROM edges'):
  a,b=lookup[src],lookup[dst];forward[out_pos[a]]=b;out_pos[a]+=1;backward[in_pos[b]]=a;in_pos[b]+=1
 del out_pos,in_pos
 seen=bytearray(n);order=array('i')
 for root in range(n):
  if seen[root]:continue
  seen[root]=1;stack=[root];positions=[out_offset[root]]
  while stack:
   v=stack[-1];pos=positions[-1]
   if pos<out_offset[v+1]:
    target=forward[pos];positions[-1]=pos+1
    if not seen[target]:seen[target]=1;stack.append(target);positions.append(out_offset[target])
   else:order.append(v);stack.pop();positions.pop()
 components=array('i',[-1])*n;sizes=Counter()
 for root in reversed(order):
  if components[root]!=-1:continue
  components[root]=root;stack=[root];count=0
  while stack:
   v=stack.pop();count+=1
   for pos in range(in_offset[v],in_offset[v+1]):
    target=backward[pos]
    if components[target]==-1:components[target]=root;stack.append(target)
  sizes[root]=count
 del forward,backward,out_offset,in_offset,seen,order
 db.executescript('DROP TABLE IF EXISTS spatial;CREATE TABLE spatial(id INTEGER PRIMARY KEY,cell INTEGER,lat REAL,lon REAL,component INTEGER,can_start INTEGER,can_end INTEGER);')
 def records():
  for node,lat,lon in db.execute('SELECT id,lat,lon FROM nodes ORDER BY id'):
   i=lookup[node];yield node,int((lat+90)*100)*36000+int((lon+180)*100),lat,lon,components[i],int(out_count[i]>0),int(in_count[i]>0)
 db.executemany('INSERT INTO spatial VALUES(?,?,?,?,?,?,?)',records());db.execute('CREATE INDEX spatial_cell ON spatial(cell)')
 return {'component_kind':'directed strong','components':len(sizes),'largest_component_node_counts':sizes.most_common(12),'note':'Strong directed connectivity ignores turn-state restrictions and is not a legal-access guarantee.'}
