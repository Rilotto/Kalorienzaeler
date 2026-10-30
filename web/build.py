#!/usr/bin/env python3
"""Baut die Browser-Version (eine HTML-Datei mit eingebauter BLS-Liste) nach web/dist/kalorienzaehler.html.
Veröffentlicht als Claude-Artefakt: https://claude.ai/artifact/Wfxe1BABcSSGv9fLogYevc"""
import json, pathlib
w = pathlib.Path(__file__).resolve().parent
tsv = (w.parent / "app/src/main/assets/bls.tsv").read_text(encoding="utf-8")
js = "\n".join((w / f).read_text(encoding="utf-8") for f in ["core.js", "state.js", "ui1.js", "ui2.js", "ui3.js", "ui4.js", "ui5.js"])
js = js.replace("if (typeof module !== 'undefined') module.exports", "if (false) module.exports")
html = ((w / "head.html").read_text(encoding="utf-8") + "<style>\n" + (w / "style.css").read_text(encoding="utf-8") + "</style>\n"
        + (w / "body.html").read_text(encoding="utf-8") + "<script>\nconst BLS_TSV = " + json.dumps(tsv, ensure_ascii=False).replace("</", "<\\/")
        + ";\n" + js + "\n</script>\n")
out = w / "dist" / "kalorienzaehler.html"; out.parent.mkdir(exist_ok=True); out.write_text(html, encoding="utf-8")
print(out, len(html.encode()))
