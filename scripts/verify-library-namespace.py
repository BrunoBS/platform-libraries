#!/usr/bin/env python3
"""Reject Java library sources outside the library namespace."""

from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
prefix = Path("br/com/portalmanager/platform/library")
package_prefix = "br.com.portalmanager.platform.library"
errors = []

for source in sorted((root / "modules").glob("*/src/*/java/**/*.java")):
    relative = source.relative_to(root)
    java_root = relative.parts.index("java")
    package_path = Path(*relative.parts[java_root + 1 : -1])
    match = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;", source.read_text())
    expected = ".".join(package_path.parts)
    if not package_path.is_relative_to(prefix) or not match or match.group(1) != expected:
        errors.append(f"{relative}: expected package {expected} under {package_prefix}")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Library Java source paths and packages verified.")
