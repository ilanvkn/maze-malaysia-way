import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile


class PackagingTests(unittest.TestCase):
    def test_explicit_inputs_roundtrip_and_no_overwrite(self):
        script = Path(__file__).resolve().parents[1] / "tools/package-offline-pack.py"
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "tools").mkdir()
            worker = root / "tools/package-offline-pack.py"
            shutil.copyfile(script, worker)
            notice = root / "app/src/main/assets/offline/NOTICE.txt"
            notice.parent.mkdir(parents=True)
            notice.write_text("Synthetic test data only; no geographic claims")
            args = [sys.executable, str(worker)]
            for name in ("roads", "places", "tiles"):
                source = root / name
                source.write_bytes(("synthetic " + name).encode())
                args += ["--" + name, str(source)]
            output = root / "pack.zip"
            args += ["--osm-timestamp", "2000-01-01T00:00:00Z", "--output", str(output)]
            subprocess.run(args, check=True, capture_output=True, timeout=10)
            manifest = json.loads(output.with_suffix(".manifest.json").read_text())
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(set(archive.namelist()), {"roads.sqlite", "places.sqlite", "region.mbtiles", "manifest.json", "NOTICE.txt"})
                for name, entry in manifest["files"].items():
                    self.assertEqual(hashlib.sha256(archive.read(name)).hexdigest(), entry["sha256"])
            before = output.read_bytes()
            self.assertNotEqual(subprocess.run(args, capture_output=True, timeout=10).returncode, 0)
            self.assertEqual(output.read_bytes(), before)


if __name__ == "__main__":
    unittest.main()
