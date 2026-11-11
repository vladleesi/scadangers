"""Extract one stable release's Markdown notes from changelog input on stdin."""

import argparse
import re
import sys

STABLE_VERSION = re.compile(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)")


def release_notes(changelog_markdown: str, version: str) -> str:
    """Return the unique version section, excluding its heading and other releases."""
    if not STABLE_VERSION.fullmatch(version):
        raise ValueError("Expected a stable SemVer release version")
    heading = re.compile(r"## " + re.escape(version) + r"(?: — [^\r\n]+)?")
    lines = changelog_markdown.splitlines()
    starts = [index for index, line in enumerate(lines) if heading.fullmatch(line)]
    if len(starts) != 1:
        raise ValueError(f"Expected exactly one changelog section for {version}")
    start = starts[0] + 1
    end = next((index for index in range(start, len(lines)) if lines[index].startswith("## ")), len(lines))
    notes = "\n".join(lines[start:end]).strip()
    if not notes:
        raise ValueError(f"Changelog section for {version} is empty")
    return notes + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    arguments = parser.parse_args()
    try:
        notes = release_notes(sys.stdin.read(), arguments.version)
    except ValueError as error:
        parser.error(str(error))
    sys.stdout.write(notes)


if __name__ == "__main__":
    main()
