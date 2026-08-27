#!/usr/bin/env python3
"""Verify WorldTaste's IE1/IE2 resolver matches the current IE2 migration table."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
WORLD_RESOLVER = ROOT / "src/main/java/com/haiman233/worldtaste/compat/SlimefunItemResolver.java"
IE2_MAPPER_REL = pathlib.Path(
    "src/main/kotlin/net/guizhanss/infinityexpansion2/core/migration/LegacyIdMapper.kt"
)

KOTLIN_ENTRY = re.compile(r'"([A-Z0-9_]+)"\s+to\s+"([A-Z0-9_]+)"')
JAVA_ENTRY = re.compile(r'Map\.entry\("([A-Z0-9_]+)",\s*"([A-Z0-9_]+)"\)')


def extract_ie2_explicit(text: str) -> dict[str, str]:
    start = text.find("private val explicit = linkedMapOf(")
    if start < 0:
        raise ValueError("Could not find IE2 explicit migration map")
    end = text.find("\n    )", start)
    if end < 0:
        raise ValueError("Could not find end of IE2 explicit migration map")
    return dict(KOTLIN_ENTRY.findall(text[start:end]))


def extract_worldtaste_explicit(text: str) -> dict[str, str]:
    start = text.find("private static final Map<String, String> IE1_TO_IE2")
    if start < 0:
        raise ValueError("Could not find WorldTaste IE1_TO_IE2 map")
    end = text.find("\n    );", start)
    if end < 0:
        raise ValueError("Could not find end of WorldTaste IE1_TO_IE2 map")
    return dict(JAVA_ENTRY.findall(text[start:end]))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ie2-root", required=True, type=pathlib.Path)
    args = parser.parse_args()

    mapper = args.ie2_root / IE2_MAPPER_REL
    if not mapper.is_file():
        print(f"IE2 LegacyIdMapper not found: {mapper}", file=sys.stderr)
        return 2

    ie2 = extract_ie2_explicit(mapper.read_text(encoding="utf-8"))
    world_text = WORLD_RESOLVER.read_text(encoding="utf-8")
    world = extract_worldtaste_explicit(world_text)

    missing = {key: value for key, value in ie2.items() if world.get(key) != value}
    stale = {key: value for key, value in world.items() if ie2.get(key) != value}

    if missing or stale:
        if missing:
            print("Missing or incorrect IE2 mappings in WorldTaste:")
            for key, value in sorted(missing.items()):
                print(f"  {key} -> {value}")
        if stale:
            print("Stale/extra WorldTaste IE2 mappings:")
            for key, value in sorted(stale.items()):
                print(f"  {key} -> {value}")
        return 1

    dynamic_markers = (
        'endsWith("_DATA_CARD")',
        '"IE_MOB_DATA_CARD_"',
        'startsWith("QUARRY_OSCILLATOR_")',
        '"IE_OSCILLATOR_"',
    )
    missing_dynamic = [marker for marker in dynamic_markers if marker not in world_text]
    if missing_dynamic:
        print("WorldTaste resolver is missing IE2 dynamic migration rules:")
        for marker in missing_dynamic:
            print(f"  {marker}")
        return 1

    print(
        f"IE2 mapping sync passed: {len(ie2)} explicit mappings plus dynamic "
        "mob-card and quarry-oscillator rules."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
