"""Build-time glyph fetch with reviewed sizes and Git blob identities."""
import concurrent.futures,json,pathlib,hashlib,subprocess,re
root=pathlib.Path(__file__).resolve().parents[1]
inventory=json.loads((root/'tools/glyph-inventory.json').read_text())
dest=root/'app/src/main/assets/offline/glyphs'
dest.mkdir(parents=True,exist_ok=True)
def fetch(item):
 name=item['name']
 assert re.fullmatch(r'\d+-\d+\.pbf',name),name
 url=item['url']
 assert url.startswith('https://raw.githubusercontent.com/maplibre/demotiles/')
 target=dest/name
 if target.exists():
  cached=target.read_bytes()
  if len(cached)==item['size'] and hashlib.sha1(b'blob '+str(len(cached)).encode()+b'\0'+cached).hexdigest()==item['sha']:
   return dict(name=name,bytes=len(cached),sha256=hashlib.sha256(cached).hexdigest(),url=url)
 data=subprocess.check_output(['curl','-fsSL','--max-time','30','--retry','2','--retry-delay','1',url])
 assert len(data)==item['size'] and hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()==item['sha'],name
 (dest/name).write_bytes(data)
 return dict(name=name,bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),url=url)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:results=list(pool.map(fetch,inventory))
(dest/'download-receipt.json').write_text(json.dumps(results,indent=2)+'\n')
print(len(results))
