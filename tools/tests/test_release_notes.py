import importlib.util
from pathlib import Path
import subprocess
import sys
import unittest

SCRIPT = Path(__file__).parents[1] / "release_notes.py"
SPEC = importlib.util.spec_from_file_location("release_notes", SCRIPT)
notes_module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(notes_module)


class ReleaseNotesTest(unittest.TestCase):
    def test_extracts_only_requested_version_and_preserves_markdown(self):
        changelog = """# Changelog

## Unreleased
- Future work.

## 0.2.0 — 2026-11-11

### Added

- A feature with **Markdown**.

### Fixed

- A fix.

## 0.1.0 — 2026-11-10

- Initial release.
"""
        self.assertEqual(
            "### Added\n\n- A feature with **Markdown**.\n\n### Fixed\n\n- A fix.\n",
            notes_module.release_notes(changelog, "0.2.0"),
        )
        self.assertEqual("- Initial release.\n", notes_module.release_notes(changelog, "0.1.0"))

    def test_supports_undated_heading_and_crlf(self):
        self.assertEqual("- Fix.\n", notes_module.release_notes("## 1.0.0\r\n\r\n- Fix.\r\n", "1.0.0"))

    def test_missing_duplicate_empty_and_prefix_sections_fail(self):
        for changelog in [
            "## Unreleased\n- Future.\n",
            "## 1.0.0\n- First.\n## 1.0.0 — 2026-11-11\n- Duplicate.\n",
            "## 1.0.0\n\n## 0.9.0\n- Earlier.\n",
            "## 1.0.01\n- Different version.\n",
        ]:
            with self.subTest(changelog=changelog), self.assertRaises(ValueError):
                notes_module.release_notes(changelog, "1.0.0")

    def test_rejects_invalid_or_nonstable_version(self):
        for version in ["v1.0.0", "01.0.0", "1.0", "1.0.0-rc.1", "1.0.0\n## Other"]:
            with self.subTest(version=version), self.assertRaises(ValueError):
                notes_module.release_notes("## 1.0.0\n- Fix.\n", version)

    def test_cli_writes_notes_and_fails_without_a_matching_section(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "1.0.0"],
            input="## 1.0.0\n- Fix.\n",
            text=True,
            capture_output=True,
            check=True,
        )
        self.assertEqual("- Fix.\n", result.stdout)
        missing = subprocess.run(
            [sys.executable, str(SCRIPT), "1.0.0"],
            input="## 0.9.0\n- Earlier.\n",
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertNotEqual(0, missing.returncode)
        self.assertEqual("", missing.stdout)


if __name__ == "__main__":
    unittest.main()
