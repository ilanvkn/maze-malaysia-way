"""Verify bundled offline glyph identities, notices and pack separation; not UI proof."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile


def verify(apk, inventory):
    expected = {"assets/offline/glyphs/"+item['name']: item for item in inventory}
    if len(expected) != 256 or any(not re.fullmatch(r'\d+-\d+\.pbf', item['name']) for item in inventory):
        raise ValueError('Expected the reviewed 256-block glyph inventory')
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if len(set(names)) != len(names):
            raise ValueError('Duplicate APK entries')
        actual = {name for name in names if name.startswith('assets/offline/glyphs/') and name.endswith('.pbf')}
        if actual != set(expected):
            raise ValueError('Missing or unexpected offline glyph blocks')
        for name, item in expected.items():
            if archive.getinfo(name).file_size != item['size'] or item['size'] > 1_048_576:
                raise ValueError('Invalid glyph size')
            data = archive.read(name)
            if hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest() != item['sha']:
                raise ValueError('Changed offline glyph identity')
        for name in ['FONT-LICENSE.txt', 'GLYPH-REPOSITORY-LICENSE.txt', 'NOTICE.txt']:
            if not 0 < archive.getinfo('assets/offline/'+name).file_size < 131072:
                raise ValueError('Missing or oversized offline notice')
        if any(name.endswith(('.sqlite', '.mbtiles')) for name in names):
            raise ValueError('Country databases must be delivered as a separate pack')
        if archive.getinfo('assets/offline/manifest.json').file_size > 65536:
            raise ValueError('Oversized pack manifest')
        manifest = json.loads(archive.read('assets/offline/manifest.json'))
        if set(manifest['files']) != {'roads.sqlite', 'places.sqlite', 'region.mbtiles'}:
            raise ValueError('Unexpected pack datasets')
        for entry in manifest['files'].values():
            if type(entry['bytes']) is not int or entry['bytes'] <= 0 or not re.fullmatch('[a-f0-9]{64}', entry['sha256']):
                raise ValueError('Invalid dataset identity')
    return {'glyph_blocks': 256, 'notices_present': True, 'country_databases_separate': True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    args = parser.parse_args()
    inventory = json.loads((Path(__file__).resolve().parent/'glyph-inventory.json').read_text())
    result = verify(args.apk, inventory)
    value = hashlib.sha256()
    with args.apk.open('rb') as stream:
        for block in iter(lambda: stream.read(1024*1024), b''): value.update(block)
    print(json.dumps(dict(result, apk_sha256=value.hexdigest(), bytes=args.apk.stat().st_size)))


if __name__ == '__main__':
    main()
