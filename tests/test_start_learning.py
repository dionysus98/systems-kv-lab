"""Verify that onboarding preserves work and exports clean, reusable inputs."""

import importlib.util
from pathlib import Path
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("start_learning", ROOT / "scripts/start-learning.py")
EXPORTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(EXPORTER)


class StarterExportTests(unittest.TestCase):
    def test_fresh_export_has_no_inherited_evidence_or_runtime_files(self):
        records = [ROOT / name for name in
                   ("session-doc.md", "learning-checkpoints.md", "learner-profile.md",
                    "lessons/01-build-and-run/c/main.c")]
        before = {path: path.read_bytes() for path in records}
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary) / "new-learner"
            EXPORTER.export_workspace(ROOT, destination)
            for path in (ROOT / "starter").rglob("*"):
                if path.is_file():
                    self.assertEqual(path.read_bytes(),
                                     (destination / path.relative_to(ROOT / "starter")).read_bytes())
            for name in (".git", "build", "target", ".zig-cache", ".joyride/.nrepl-port"):
                self.assertFalse((destination / name).exists())
            self.assertNotIn("hello, rebuilt systems",
                             (destination / "lessons/01-build-and-run/c/main.c").read_text())
            self.assertTrue((destination / ".agents/skills/systems-programming-tutor/SKILL.md").is_file())
        self.assertEqual(before, {path: path.read_bytes() for path in records})

    def test_existing_destination_is_untouched(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary)
            marker = destination / "my-work.c"
            marker.write_text("unfinished learner work")
            with self.assertRaisesRegex(ValueError, "already exists"):
                EXPORTER.export_workspace(ROOT, destination)
            self.assertEqual(list(destination.iterdir()), [marker])
            self.assertEqual(marker.read_text(), "unfinished learner work")

    def test_export_can_create_another_clean_workspace(self):
        with tempfile.TemporaryDirectory() as temporary:
            first, second = (Path(temporary) / name for name in ("first", "second"))
            EXPORTER.export_workspace(ROOT, first)
            (first / "session-doc.md").write_text("private first learner evidence")
            (first / "lessons/03-fixed-store/c/main.c").write_text("private implementation")
            EXPORTER.export_workspace(first, second)
            self.assertEqual((second / "session-doc.md").read_bytes(),
                             (ROOT / "starter/session-doc.md").read_bytes())
            self.assertEqual((second / "lessons/03-fixed-store/c/main.c").read_bytes(),
                             (ROOT / "starter/lessons/03-fixed-store/c/main.c").read_bytes())

    def test_destination_inside_source_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "outside"):
            EXPORTER.export_workspace(ROOT, ROOT / "new-learner")
        self.assertFalse((ROOT / "new-learner").exists())

    def test_missing_baseline_fails_before_creating_destination(self):
        with tempfile.TemporaryDirectory() as temporary:
            source, destination = (Path(temporary) / name for name in ("source", "destination"))
            EXPORTER.export_workspace(ROOT, source)
            (source / "starter/learning-checkpoints.md").unlink()
            with self.assertRaisesRegex(ValueError, "Missing starter baseline"):
                EXPORTER.export_workspace(source, destination)
            self.assertFalse(destination.exists())


if __name__ == "__main__":
    unittest.main()
