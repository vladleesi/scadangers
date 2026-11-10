"""Audit resolved runtime Maven dependencies against OSV and a license allowlist.

Fails closed on missing metadata, unknown licenses, vulnerabilities or service errors.
Only public dependency coordinates are sent; no application data is accessed.
"""
import concurrent.futures
import json
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

MAX_RESPONSE = 4 * 1024 * 1024
COORDINATE = re.compile(r"[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9_.+-]+")
REPOSITORIES = ("https://dl.google.com/dl/android/maven2/", "https://repo.maven.apache.org/maven2/")
ALLOWED_LICENSES = {
    "Apache License, Version 2.0", "Apache License 2.0", "Apache 2.0",
    "The Apache Software License, Version 2.0", "The Apache License, Version 2.0", "Apache-2.0",
    "MIT", "MIT License", "The MIT License", "BSD-2-Clause", "BSD-3-Clause",
    "BSD 2-Clause License", "BSD 3-Clause License", "Eclipse Public License 1.0",
    "Eclipse Public License - v 1.0", "EPL-1.0", "Eclipse Distribution License - v 1.0",
    "The BSD 3-Clause License", "The BSD License",
}
NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


def request(url, payload=None):
    body = None if payload is None else json.dumps(payload).encode()
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=20) as response:
        data = response.read(MAX_RESPONSE + 1)
    if len(data) > MAX_RESPONSE:
        raise ValueError("Metadata response exceeds size limit")
    return data


def coordinates(path):
    items = sorted(set(Path(path).read_text().splitlines()))
    if not items or any(not COORDINATE.fullmatch(item) for item in items):
        raise ValueError("Missing or malformed Maven dependency inventory")
    return items


def parse_pom(data):
    # Do not accept entity expansion or document type declarations from remote metadata.
    if b"<!DOCTYPE" in data.upper() or b"<!ENTITY" in data.upper():
        raise ValueError("Unsafe POM document")
    root = ET.fromstring(data)
    licenses = [node.text.strip() for node in root.findall("m:licenses/m:license/m:name", NAMESPACE)
                if node.text and node.text.strip()]
    parent = root.find("m:parent", NAMESPACE)
    parent_coordinate = None
    if parent is not None:
        parts = [parent.findtext("m:" + key, namespaces=NAMESPACE)
                 for key in ("groupId", "artifactId", "version")]
        if all(parts):
            parent_coordinate = ":".join(parts)
    return licenses, parent_coordinate


def licenses_for(coordinate, seen=None):
    seen = set() if seen is None else seen
    if coordinate in seen or len(seen) >= 8 or not COORDINATE.fullmatch(coordinate):
        raise ValueError("Invalid or cyclic POM ancestry")
    seen.add(coordinate)
    group, artifact, version = coordinate.split(":")
    suffix = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    for repository in REPOSITORIES:
        try:
            data = request(repository + suffix)
            break
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
    else:
        raise ValueError(f"No POM found for {coordinate}")
    licenses, parent = parse_pom(data)
    if licenses:
        return licenses
    if parent:
        return licenses_for(parent, seen)
    raise ValueError(f"Missing license metadata for {coordinate}")


def audit(items):
    findings = []
    for offset in range(0, len(items), 100):
        batch = items[offset:offset + 100]
        queries = [{"package": {"ecosystem": "Maven", "name": ":".join(item.split(":")[:2])},
                    "version": item.split(":")[2]} for item in batch]
        response = json.loads(request("https://api.osv.dev/v1/querybatch", {"queries": queries}))
        results = response["results"]
        if not isinstance(results, list) or len(results) != len(batch):
            raise ValueError("Incomplete vulnerability response")
        for item, result in zip(batch, results):
            if not isinstance(result, dict) or result.get("next_page_token"):
                raise ValueError("Malformed or paginated vulnerability response requires review")
            if result.get("vulns"):
                findings.append({"dependency": item, "vulnerabilities": result["vulns"]})
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        license_results = list(executor.map(licenses_for, items))
    for item, names in zip(items, license_results):
        if any(name not in ALLOWED_LICENSES for name in names):
            findings.append({"dependency": item, "unapproved_licenses": names})
    return {"dependencies": dict(zip(items, license_results)), "findings": findings}


def main():
    report = Path("app/build/reports/dependencies/audit.json")
    report.parent.mkdir(parents=True, exist_ok=True)
    try:
        result = audit(coordinates(sys.argv[1]))
    except (OSError, ValueError, KeyError, ET.ParseError) as error:
        report.write_text(json.dumps({"status": "incomplete", "error": str(error)}, indent=2))
        print(f"Dependency audit incomplete: {error}", file=sys.stderr)
        return 1
    report.write_text(json.dumps(result, indent=2))
    print(f"Audited {len(result['dependencies'])} runtime dependencies; {len(result['findings'])} findings")
    return bool(result["findings"])


if __name__ == "__main__":
    sys.exit(main())
