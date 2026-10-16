package de.kalorien.app.core.domain

/** Worauf sich die erkannten Werte beziehen. */
enum class NutritionBasis { PER_100G, PER_100ML, PER_PORTION, UNKNOWN }

/** Ergebnis der Texterkennung – alles optional, der Nutzer prüft und ergänzt. */
data class ParsedNutrition(
    val name: String?,
    val kcal: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val basis: NutritionBasis,
    val sugar: Double? = null,
) {
    val hasAnyValue: Boolean get() = kcal != null || protein != null || carbs != null || fat != null
}

/**
 * Liest Nährwerte aus erkanntem Text (Kantinen-Schild, Monitor-Foto, Nährwerttabelle).
 * Robust gegen typische Texterkennungsfehler („kcaI“, „1O“, Komma statt Punkt).
 * Steht die Zahl nicht in derselben Zeile wie das Wort, wird die nächste Zeile genommen.
 */
object NutritionTextParser {
    private val num = """(\d{1,4}(?:[.,]\d{1,2})?)"""
    private val kcalRegex = Regex("""$num\s*k\s?c\s?a\s?[l1iI|]""", RegexOption.IGNORE_CASE)
    private val kjRegex = Regex("""(\d{2,5}(?:[.,]\d)?)\s*k\s?j""", RegexOption.IGNORE_CASE)
    private val gramRegex = Regex("""$num\s*g\b""", RegexOption.IGNORE_CASE)
    private val anyNumber = Regex(num)

    private val proteinWords = Regex("""eiwei(ß|ss|b)|protein""", RegexOption.IGNORE_CASE)
    private val carbWords = Regex("""kohlen\s?hydrat|kohlenh\.|carbohydrate|\bkh\b""", RegexOption.IGNORE_CASE)
    private val sugarWords = Regex("""zucker|sugars?\b""", RegexOption.IGNORE_CASE)
    private val fatWords = Regex("""\bfett\b|\bfat\b""", RegexOption.IGNORE_CASE)
    private val subLine = Regex("""davon|fettsäure|fettsaure|ges[äa]ttigt|zucker|sugar|saturat""", RegexOption.IGNORE_CASE)
    private val nutritionWords = Regex(
        """kcal|kj|energie|brennwert|eiwei|protein|fett|kohlen|zucker|salz|ballast|portion|nährwert|naehrwert|\d""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(raw: String): ParsedNutrition {
        // häufige OCR-Verwechslungen in Zahlen: O → 0, l/I → 1 (nur zwischen Ziffern)
        val text = raw
            .replace(Regex("""(?<=\d)[oO](?=\d|\s|g|k)"""), "0")
            .replace(Regex("""(?<=\d)[lI](?=\d)"""), "1")
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val kcal = kcalRegex.find(text)?.let { toDouble(it.groupValues[1]) }
            ?: kjRegex.find(text)?.let { toDouble(it.groupValues[1])?.let(NutritionUnits::kjToKcal) }

        return ParsedNutrition(
            name = guessName(lines),
            kcal = kcal?.takeIf { it in 1.0..5000.0 },
            protein = valueAfter(lines, proteinWords),
            carbs = valueAfter(lines, carbWords),
            fat = valueAfter(lines, fatWords),
            basis = basis(text),
            sugar = valueAfter(lines, sugarWords),
        )
    }

    private fun valueAfter(lines: List<String>, label: Regex): Double? {
        for ((i, line) in lines.withIndex()) {
            val m = label.find(line) ?: continue
            // „davon gesättigte Fettsäuren“ o. Ä. ist nicht der Fettwert
            if (label === fatWords && subLine.containsMatchIn(line)) continue
            val rest = line.substring(m.range.last + 1)
            val v = (gramRegex.find(rest) ?: anyNumber.find(rest))?.groupValues?.get(1)
                ?: lines.getOrNull(i + 1)?.let { next -> (gramRegex.find(next) ?: anyNumber.find(next))?.groupValues?.get(1) }
            val d = v?.let(::toDouble)
            if (d != null && d in 0.0..1000.0) return d
        }
        return null
    }

    private fun basis(text: String): NutritionBasis {
        val t = text.lowercase()
        return when {
            Regex("""100\s*ml""").containsMatchIn(t) -> NutritionBasis.PER_100ML
            Regex("""100\s*g\b""").containsMatchIn(t) -> NutritionBasis.PER_100G
            Regex("""portion|teller|je\s+gericht|pro\s+gericht""").containsMatchIn(t) -> NutritionBasis.PER_PORTION
            else -> NutritionBasis.UNKNOWN
        }
    }

    /** Erste Zeile, die nach einem Gerichtsnamen aussieht (Buchstaben, keine Nährwert-Wörter/Zahlen). */
    private fun guessName(lines: List<String>): String? = lines
        .firstOrNull { l -> l.count { it.isLetter() } >= 4 && !nutritionWords.containsMatchIn(l) }
        ?.take(60)

    private fun toDouble(s: String): Double? = s.replace(',', '.').toDoubleOrNull()
}
