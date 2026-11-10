import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("audit", Path(__file__).parents[1] / "audit_dependencies.py")
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)


class AuditTest(unittest.TestCase):
    def test_inventory_rejects_path_injection_and_empty_input(self):
        for content in ("", "group:../../artifact:1", "group:artifact:1\ninvalid"):
            with self.subTest(content=content), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "inventory"
                path.write_text(content)
                with self.assertRaises(ValueError):
                    audit.coordinates(path)

    def test_pom_rejects_entity_declarations(self):
        with self.assertRaises(ValueError):
            audit.parse_pom(b'<!DOCTYPE project [<!ENTITY x "payload">]><project/>')

    def test_pom_reads_license_and_parent(self):
        pom = b'''<project xmlns="http://maven.apache.org/POM/4.0.0">
        <parent><groupId>g</groupId><artifactId>a</artifactId><version>1</version></parent>
        <licenses><license><name>MIT</name></license></licenses></project>'''
        self.assertEqual((["MIT"], "g:a:1"), audit.parse_pom(pom))

    def test_findings_include_vulnerabilities_and_unknown_licenses(self):
        with patch.object(audit, "request", return_value=b'{"results":[{"vulns":[{"id":"TEST"}]}]}'), \
                patch.object(audit, "licenses_for", return_value=["Unknown"]):
            result = audit.audit(["g:a:1"])
        self.assertEqual(2, len(result["findings"]))

    def test_incomplete_osv_response_fails(self):
        with patch.object(audit, "request", return_value=b'{"results":[]}'):
            with self.assertRaises(ValueError):
                audit.audit(["g:a:1"])

    def test_paginated_osv_response_fails_closed(self):
        with patch.object(audit, "request", return_value=b'{"results":[{"next_page_token":"more"}]}'):
            with self.assertRaises(ValueError):
                audit.audit(["g:a:1"])

    def test_cyclic_parent_rejected(self):
        with self.assertRaises(ValueError):
            audit.licenses_for("g:a:1", {"g:a:1"})


if __name__ == "__main__":
    unittest.main()
