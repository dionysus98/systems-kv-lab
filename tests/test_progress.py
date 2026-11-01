"""Check faithful progress reporting, safe display, and fresh-workspace support."""

import ast
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("progress", ROOT / "scripts/progress.py")
PROGRESS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROGRESS)

RECORD = '''# Progress

## 01-c-example
- Language: C
- Stage: `verifying`
- Outcome: Build a small store.
- Next learner action: run the capacity checks,
  then inspect the result.

### Acceptance criteria and evidence
| Criterion | Evidence status | Source |
| --- | --- | --- |
| Insert works. | verified by tutor | Build and run exited 0. |
| Explain mutation. | explained by tutor | Learner explanation pending. |
| Apply independently. | pending | Fresh variation. |
| Search invariant. | demonstrated by learner | Learner explained loop boundary. |

## 01-rust-example
- Language: Rust
- Stage: `queued`
- Next action: implement after C.

## Plan changes
- A later checkpoint will be added.
'''


class ProgressTests(unittest.TestCase):
    def test_multiline_actions_and_distinct_evidence_are_preserved(self):
        checkpoints = PROGRESS.parse_checkpoints(RECORD)
        self.assertEqual(len(checkpoints), 2)
        self.assertEqual(checkpoints[0]["fields"]["next learner action"],
                         "run the capacity checks, then inspect the result.")
        self.assertEqual([row["status"] for row in checkpoints[0]["criteria"]],
                         ["verified by tutor", "explained by tutor", "pending", "demonstrated by learner"])
        self.assertEqual(checkpoints[0]["stage"], "verifying")
        self.assertEqual(PROGRESS.current_checkpoint(checkpoints)["id"], "01-c-example")

    def test_first_unfinished_is_next_even_when_later_item_is_active(self):
        checkpoints = PROGRESS.parse_checkpoints(RECORD)
        checkpoints[0]["stage"] = "queued"
        checkpoints[1]["stage"] = "implementing"
        self.assertIs(PROGRESS.current_checkpoint(checkpoints), checkpoints[0])
        checkpoints[0]["stage"] = "complete"
        self.assertIs(PROGRESS.current_checkpoint(checkpoints), checkpoints[1])
        checkpoints[1]["stage"] = "complete"
        self.assertIsNone(PROGRESS.current_checkpoint(checkpoints))

    def test_invalid_records_fail_instead_of_inventing_progress(self):
        for text in ("# Empty", RECORD.replace("`verifying`", "`almost done`"),
                     RECORD.replace("verified by tutor", "passed"),
                     RECORD.replace("- Stage: `verifying`\n", ""),
                     RECORD.replace("| Insert works. |", "| Unexpected | extra |")):
            with self.subTest(text=text), self.assertRaises(ValueError):
                PROGRESS.parse_checkpoints(text)

    def test_incomplete_evidence_is_flagged_without_changing_record(self):
        checkpoints = PROGRESS.parse_checkpoints(RECORD)
        checkpoints[0]["stage"] = "complete"
        page = PROGRESS.render_page(checkpoints)
        self.assertIn("Review this record", page)
        self.assertEqual(checkpoints[0]["criteria"][2]["status"], "pending")

    def test_html_in_evidence_is_text_and_escaped_pipes_are_supported(self):
        record = RECORD.replace("Insert works.", r'<script>alert(1)</script> \| value')
        checkpoints = PROGRESS.parse_checkpoints(record)
        self.assertIn("| value", checkpoints[0]["criteria"][0]["criterion"])
        page = PROGRESS.render_page(checkpoints)
        self.assertNotIn("<script>alert(1)</script>", page)
        self.assertIn("&lt;script&gt;alert(1)&lt;/script&gt;", page)

    def test_exported_workspace_generates_its_own_progress(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary) / "learner"
            subprocess.run([sys.executable, str(ROOT / "scripts/start-learning.py"), str(destination)],
                           capture_output=True, check=True)
            record = (destination / "learning-checkpoints.md").read_bytes()
            result = subprocess.run([sys.executable, str(destination / "scripts/progress.py")],
                                    capture_output=True, text=True, check=True)
            self.assertIn("01-c-build-and-run (queued)", result.stdout)
            page = (destination / "build/progress.html").read_text()
            self.assertIn("No comparisons", (destination / "notes/language-comparison.md").read_text())
            self.assertNotIn("03-c-fixed-store", page)
            self.assertEqual(record, (destination / "learning-checkpoints.md").read_bytes())

    def test_supported_python_syntax(self):
        ast.parse((ROOT / "scripts/progress.py").read_text(), feature_version=(3, 9))

    def test_chat_report_preserves_evidence_categories_and_next_action(self):
        checkpoints = PROGRESS.parse_checkpoints(RECORD)
        report = PROGRESS.render_chat(checkpoints)
        for status in PROGRESS.STATUSES:
            self.assertIn("1 " + status, report)
        self.assertIn("run the capacity checks, then inspect the result.", report)
        self.assertIn('step0["1. C: verifying"]', report)
        self.assertIn("step0 --> step1", report)
        self.assertIn("0 / 2 complete", report)

    def test_chat_mode_writes_no_files(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            record = root / "learning-checkpoints.md"
            record.write_text(RECORD)
            result = subprocess.run([sys.executable, str(ROOT / "scripts/progress.py"),
                                     "--root", str(root), "--chat"],
                                    capture_output=True, text=True, check=True)
            self.assertIn("```mermaid", result.stdout)
            self.assertEqual(list(root.iterdir()), [record])
            self.assertEqual(record.read_text(), RECORD)


if __name__ == "__main__":
    unittest.main()
