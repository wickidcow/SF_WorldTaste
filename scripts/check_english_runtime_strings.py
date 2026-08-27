#!/usr/bin/env python3
"""Fail when Java runtime string literals still contain CJK characters.

Comments and character literals are ignored. The EnglishText translation dictionary
is intentionally excluded because its Chinese literals are source lookup keys used to
translate the original YAML content, not text shown directly to players or admins.
The large content YAML dataset is likewise preserved for stable IDs/data compatibility.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src" / "main" / "java"
CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
EXCLUDED_FILES = {
    pathlib.Path("src/main/java/com/haiman233/worldtaste/util/EnglishText.java"),
}


def scan_java(path: pathlib.Path) -> list[tuple[int, str]]:
    text = path.read_text(encoding="utf-8")
    failures: list[tuple[int, str]] = []
    state = "code"
    line = 1
    string_line = 1
    buf: list[str] = []
    i = 0

    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""

        if ch == "\n":
            line += 1

        if state == "code":
            if ch == "/" and nxt == "/":
                state = "line_comment"
                i += 2
                continue
            if ch == "/" and nxt == "*":
                state = "block_comment"
                i += 2
                continue
            if ch == '"':
                state = "string"
                string_line = line
                buf = []
                i += 1
                continue
            if ch == "'":
                state = "char"
                i += 1
                continue
            i += 1
            continue

        if state == "line_comment":
            if ch == "\n":
                state = "code"
            i += 1
            continue

        if state == "block_comment":
            if ch == "*" and nxt == "/":
                state = "code"
                i += 2
            else:
                i += 1
            continue

        if state == "char":
            if ch == "\\":
                i += 2
            elif ch == "'":
                state = "code"
                i += 1
            else:
                i += 1
            continue

        if state == "string":
            if ch == "\\":
                if i + 1 < len(text):
                    buf.extend((ch, text[i + 1]))
                i += 2
                continue
            if ch == '"':
                value = "".join(buf)
                if CJK.search(value):
                    failures.append((string_line, value))
                state = "code"
                i += 1
                continue
            buf.append(ch)
            i += 1
            continue

    return failures


def main() -> int:
    failures: list[tuple[pathlib.Path, int, str]] = []
    for path in sorted(SOURCE_ROOT.rglob("*.java")):
        relative = path.relative_to(ROOT)
        if relative in EXCLUDED_FILES:
            continue
        for line, value in scan_java(path):
            failures.append((relative, line, value))

    if not failures:
        print("English runtime string check passed: no CJK Java string literals found outside translation lookup data.")
        return 0

    print("Found non-English CJK runtime string literals:")
    for path, line, value in failures:
        preview = value.replace("\n", "\\n")
        if len(preview) > 180:
            preview = preview[:177] + "..."
        print(f"  {path}:{line}: {preview}")
    print(f"Total: {len(failures)}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
