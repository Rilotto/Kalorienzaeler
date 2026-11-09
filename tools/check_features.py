#!/usr/bin/env python3
"""Prüft FEATURES.md: Jede Funktion mit Status ✅ oder 🧪 muss ihren Prüfanker im Code haben.
So fällt sofort auf, wenn eine neue Version versehentlich eine fertige Funktion entfernt."""
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parent.parent
src = root / "app" / "src" / "main"
files = [p for p in src.rglob("*") if p.suffix in (".kt", ".xml")] + [root / "app" / "build.gradle.kts"]
text = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in files)

missing, checked = [], 0
for line in (root / "FEATURES.md").read_text(encoding="utf-8").splitlines():
    if not line.startswith("| ") or ("✅" not in line and "🧪" not in line):
        continue
    cells = [c.strip() for c in line.strip("|").split("|")]
    anchor = re.findall(r"`([^`]+)`", cells[-1])
    if not anchor:
        continue
    checked += 1
    if not re.search(anchor[0], text):
        missing.append(f"{cells[0]} {cells[1][:70]} → Anker fehlt: {anchor[0]}")

print(f"Funktionsliste: {checked} Funktionen geprüft.")
if missing:
    print("FEHLT im Code (Funktion versehentlich entfernt? Sonst Anker in FEATURES.md anpassen):")
    print("\n".join("  - " + m for m in missing))
    sys.exit(1)
print("Alles da.")
