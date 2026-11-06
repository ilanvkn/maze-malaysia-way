import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('apk_gate', Path(__file__).resolve().parents[1]/'tools/verify-offline-apk.py')
gate = importlib.util.module_from_spec(spec); spec.loader.exec_module(gate)


class PayloadTests(unittest.TestCase):
    def make_apk(self, path, missing=False, corrupt=False, database=False):
        inventory=[]
        with zipfile.ZipFile(path, 'w') as archive:
            for i in range(256):
                name=f'{i*256}-{i*256+255}.pbf'; data=f'synthetic glyph {i}'.encode()
                inventory.append({'name':name, 'size':len(data), 'sha':hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()})
                if not (missing and i==255): archive.writestr('assets/offline/glyphs/'+name, b'x'*len(data) if corrupt else data)
            for name in ['FONT-LICENSE.txt','GLYPH-REPOSITORY-LICENSE.txt','NOTICE.txt']:archive.writestr('assets/offline/'+name,'Synthetic notice')
            archive.writestr('assets/offline/manifest.json',json.dumps({'files':{k:{'bytes':1,'sha256':'a'*64} for k in ['roads.sqlite','places.sqlite','region.mbtiles']}}))
            if database:archive.writestr('assets/offline/roads.sqlite',b'synthetic')
        return inventory

    def test_complete_payload_passes_and_missing_glyph_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'test.apk'; inventory=self.make_apk(path)
            self.assertEqual(gate.verify(path,inventory)['glyph_blocks'],256)
            inventory=self.make_apk(path,missing=True)
            with self.assertRaisesRegex(ValueError,'Missing'):gate.verify(path,inventory)

    def test_same_size_corruption_and_embedded_country_data_fail(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'test.apk'
            for args in [{'corrupt':True},{'database':True}]:
                inventory=self.make_apk(path,**args)
                with self.assertRaises(ValueError):gate.verify(path,inventory)
