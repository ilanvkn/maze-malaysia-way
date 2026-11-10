"""Copy an explicit reviewed road graph and add a segment index; preserve original."""
import sys,sqlite3,pathlib,json,hashlib
from offline_segment_grid import build_segment_grid
source,out,receipt=map(pathlib.Path,sys.argv[1:]);assert not out.exists() and not receipt.exists()
a=sqlite3.connect(f'file:{source}?mode=ro',uri=True);b=sqlite3.connect(out);a.backup(b);a.close()
r=build_segment_grid(b)
b.execute('INSERT OR REPLACE INTO metadata VALUES(?,?)',('segment_grid',json.dumps(r)))
b.commit();assert b.execute('PRAGMA integrity_check').fetchone()[0]=='ok';b.close()
r.update(source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),sha256=hashlib.sha256(out.read_bytes()).hexdigest(),bytes=out.stat().st_size)
receipt.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps(r))
