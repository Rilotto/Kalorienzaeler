package de.kalorien.app.core.domain

/**
 * Eine Zeile der aufbereiteten BLS-Datei `assets/bls.tsv`
 * (Spalten: code, name, kcal, protein, carbs, fat, liquid; Werte je 100 g; Kopfzeile beginnt mit „code“).
 */
data class BlsRow(val code: String, val name: String, val per100: Per100, val isLiquid: Boolean) {
    companion object {
        fun parse(line: String): BlsRow? {
            if (line.isBlank() || line.startsWith("code\t")) return null
            val p = line.split('\t')
            if (p.size < 7) return null
            fun num(i: Int) = p[i].replace(',', '.').toDoubleOrNull() ?: 0.0
            val name = p[1].trim()
            if (p[0].isBlank() || name.isEmpty()) return null
            return BlsRow(
                code = p[0].trim(),
                name = name,
                per100 = Per100(num(2), num(3), num(4), num(5)),
                isLiquid = p[6].trim() == "1",
            )
        }
    }
}
