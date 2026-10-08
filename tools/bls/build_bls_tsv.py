"""
Erzeugt app/src/main/assets/bls.tsv aus dem offiziellen BLS-4.0-ZIP (Max Rubner-Institut, CC BY 4.0).
Spalten: code, name, kcal, protein, carbs, fat, liquid  (Werte je 100 g essbarem Anteil).
Aufruf: python tools/bls/build_bls_tsv.py BLS.zip app/src/main/assets/bls.tsv
"""
import io, re, sys, zipfile
import openpyxl

# Flüssig = Getränkegruppen N/P oder das erste Wort (das Hauptwort) ist ein Getränk.
# Nur das erste Wort zählt, damit z. B. „Vollkornbrot mit Buttermilch“ nicht als Getränk gilt.
LIQUID_FIRST = re.compile(
    r"^(.*saft|.*nektar|.*drink|.*milch|kefir|smoothie|.*tee|kaffee|espresso|cappuccino|latte|.*wasser|"
    r"limonade|.*limo|cola|.*schorle|.*bier|wein|sekt|likör|.*getränk)$",
    re.IGNORECASE,
)


def is_liquid(code, name):
    if code[:1].upper() in ("N", "P"):
        return True
    first = re.split(r"[\s,(/]+", name.strip())[0]
    return bool(LIQUID_FIRST.match(first))


def num(v):
    if v is None or v == "" or v == "-":
        return 0.0
    try:
        return float(str(v).replace(",", "."))
    except ValueError:
        return 0.0


def fmt(x):
    s = f"{x:.1f}"
    return s[:-2] if s.endswith(".0") else s


def main(zip_path, out_path):
    z = zipfile.ZipFile(zip_path)
    name = next(n for n in z.namelist() if "Daten" in n and n.endswith(".xlsx"))
    wb = openpyxl.load_workbook(io.BytesIO(z.read(name)), read_only=True)
    ws = wb.worksheets[0]
    rows = ws.iter_rows(values_only=True)
    header = [str(h or "") for h in next(rows)]

    def col(prefix):
        for i, h in enumerate(header):
            if h.startswith(prefix + " ") and "[" in h:
                return i
        raise SystemExit(f"Spalte {prefix} nicht gefunden")

    i_code, i_name = header.index("BLS Code"), header.index("Lebensmittelbezeichnung")
    i_kcal, i_prot, i_fat, i_cho = col("ENERCC"), col("PROT625"), col("FAT"), col("CHO")

    out, liquids, skipped = [], 0, 0
    for r in rows:
        code = str(r[i_code] or "").strip()
        nm = re.sub(r"\s+", " ", str(r[i_name] or "")).strip().replace("\t", " ")
        if not code or not nm:
            skipped += 1
            continue
        liquid = is_liquid(code, nm)
        liquids += liquid
        out.append((code, nm, num(r[i_kcal]), num(r[i_prot]), num(r[i_cho]), num(r[i_fat]), 1 if liquid else 0))

    out.sort(key=lambda t: t[0])
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("code\tname\tkcal\tprotein\tcarbs\tfat\tliquid\n")
        for t in out:
            f.write(f"{t[0]}\t{t[1]}\t{fmt(t[2])}\t{fmt(t[3])}\t{fmt(t[4])}\t{fmt(t[5])}\t{t[6]}\n")

    print(f"Lebensmittel: {len(out)}, davon flüssig: {liquids}, übersprungen: {skipped}")
    groups = {}
    for t in out:
        groups.setdefault(t[0][0], []).append(t[1])
    for g in sorted(groups):
        print(f"Gruppe {g} ({len(groups[g])}): " + "; ".join(groups[g][:6]))
    liq = [t[1] for t in out if t[6] and t[0][0] not in "NP"]
    print(f"Flüssig außerhalb N/P ({len(liq)}): " + "; ".join(liq[:80]))
    for probe in ("Hafer Flocken", "Banane", "Vollmilch", "Apfel", "Quark", "Ei ", "Butter", "Nudel", "Reis", "Bier", "Kartoffel"):
        hits = [f"{t[1]} ({fmt(t[2])} kcal)" for t in out if probe.lower() in t[1].lower()][:4]
        print(f"Suche '{probe}': " + "; ".join(hits))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
