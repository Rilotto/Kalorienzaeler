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
    /**
     * Bei Tabellen mit zwei Spalten (z. B. Mensa: „pro 100 g“ und „pro Portion“) jede Spalte einzeln,
     * in der Reihenfolge der Tabelle. Leer, wenn nur eine Spalte erkannt wurde (dann gelten die Felder oben).
     */
    val columns: List<NutritionColumn> = emptyList(),
    /** Portionsgewicht in g, wenn es auf der Tabelle steht oder aus beiden Spalten folgt */
    val portionGrams: Double? = null,
) {
    val hasAnyValue: Boolean get() = kcal != null || protein != null || carbs != null || fat != null

    fun column(basis: NutritionBasis): NutritionColumn? = columns.firstOrNull { it.basis == basis }

    private val single: NutritionColumn get() = NutritionColumn(basis, kcal, protein, carbs, fat, sugar)

    /**
     * Werte pro Portion: aus der Portions-Spalte, sonst 100-g-Werte × Portionsgewicht,
     * sonst die erkannten Werte, wenn die Tabelle nur eine Spalte ohne „100 g“ hat (Mensa-Monitor: meist pro Gericht).
     */
    fun perPortion(): NutritionColumn? {
        column(NutritionBasis.PER_PORTION)?.let { return it }
        val per100 = column(NutritionBasis.PER_100G) ?: column(NutritionBasis.PER_100ML)
            ?: single.takeIf { basis == NutritionBasis.PER_100G || basis == NutritionBasis.PER_100ML }
        if (per100 != null) return portionGrams?.let { per100.scaled(it / 100.0, NutritionBasis.PER_PORTION) }
        return single.copy(basis = NutritionBasis.PER_PORTION)
    }

    /** Werte pro 100 g/ml, falls vorhanden. */
    fun per100(): NutritionColumn? = column(NutritionBasis.PER_100G) ?: column(NutritionBasis.PER_100ML)
        ?: single.takeIf { basis == NutritionBasis.PER_100G || basis == NutritionBasis.PER_100ML }
}

/** Eine Spalte einer Nährwerttabelle. */
data class NutritionColumn(
    val basis: NutritionBasis,
    val kcal: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val sugar: Double?,
) {
    fun scaled(f: Double, newBasis: NutritionBasis) = NutritionColumn(
        newBasis, kcal?.times(f), protein?.times(f), carbs?.times(f), fat?.times(f), sugar?.times(f),
    )
}

/**
 * Liest Nährwerte aus erkanntem Text (Kantinen-Schild, Monitor-Foto, Nährwerttabelle).
 * Robust gegen typische Texterkennungsfehler („kcaI“, „1O“, Komma statt Punkt).
 * Steht die Zahl nicht in derselben Zeile wie das Wort, wird die nächste Zeile genommen.
 */
object NutritionTextParser {
    private val num = """(\d{1,4}(?:[.,]\d{1,2})?)"""
    /** Energie: auch mit Tausenderpunkt („2.287 kJ“, „1.050 kcal“) */
    private val energyNum = """(\d{1,2}\.\d{3}(?![.,]?\d)|\d{1,4}(?:[.,]\d{1,2})?)"""
    private val kcalRegex = Regex("""$energyNum\s*k\s?c\s?a\s?[l1iI|]""", RegexOption.IGNORE_CASE)
    private val kjRegex = Regex("""(\d{1,2}\.\d{3}(?![.,]?\d)|\d{2,5}(?:[.,]\d)?)\s*k\s?j""", RegexOption.IGNORE_CASE)
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

        val kcal = kcalRegex.find(text)?.let { energy(it.groupValues[1]) }
            ?: kjRegex.find(text)?.let { energy(it.groupValues[1])?.let(NutritionUnits::kjToKcal) }

        val columns = twoColumns(text, lines)
        // Kacheln („Brennwert (kCal)  Fett (g) …“ und darunter die Zahlen) als Rückfall, wenn die Tabelle fehlt
        val tiles = tiles(lines)
        return ParsedNutrition(
            name = guessName(lines),
            kcal = (kcal ?: tiles[energyWords])?.takeIf { it in 1.0..5000.0 },
            protein = valueAfter(lines, proteinWords) ?: tiles[proteinWords],
            carbs = valueAfter(lines, carbWords) ?: tiles[carbWords],
            fat = valueAfter(lines, fatWords) ?: tiles[fatWords],
            basis = columns.firstOrNull()?.basis ?: basis(text),
            sugar = valueAfter(lines, sugarWords) ?: tiles[sugarWords],
            columns = columns,
            portionGrams = portionGrams(text, columns),
        )
    }

    private val per100Header = Regex("""100\s*(g|ml)\b""", RegexOption.IGNORE_CASE)
    private val portionHeader = Regex("""portion|teller|je\s+gericht|pro\s+gericht|pro\s+stück|je\s+stück""", RegexOption.IGNORE_CASE)

    /**
     * Zwei Spalten, wenn beide Überschriften vorkommen und in den Zeilen je zwei Zahlen stehen.
     * Die Reihenfolge der Spalten folgt der Reihenfolge der Überschriften im Text.
     */
    private fun twoColumns(text: String, lines: List<String>): List<NutritionColumn> {
        val h100 = per100Header.find(text) ?: return emptyList()
        val hPortion = portionHeader.find(text) ?: return emptyList()
        val b100 = if (h100.groupValues[1].lowercase() == "ml") NutritionBasis.PER_100ML else NutritionBasis.PER_100G
        // Reihenfolge aus der Zeile, in der beide Überschriften stehen („Je 100 g   Je Portion“);
        // sonst aus dem ersten Vorkommen (eine Überschrift wie „Nährwerte je Portion (650 g)“ darüber zählt dann nicht)
        val header = lines.firstOrNull { per100Header.containsMatchIn(it) && portionHeader.containsMatchIn(it) && !portionWeightOnly(it) }
        val per100First = if (header != null) per100Header.find(header)!!.range.first < portionHeader.find(header)!!.range.first
        else h100.range.first < hPortion.range.first
        val bases = if (per100First) listOf(b100, NutritionBasis.PER_PORTION) else listOf(NutritionBasis.PER_PORTION, b100)

        val kcals = kcalRegex.findAll(text).mapNotNull { energy(it.groupValues[1]) }.filter { it in 1.0..5000.0 }.toList()
            .ifEmpty { kjRegex.findAll(text).mapNotNull { energy(it.groupValues[1])?.let(NutritionUnits::kjToKcal) }.toList() }
        val protein = valuesAfter(lines, proteinWords)
        val carbs = valuesAfter(lines, carbWords)
        val fat = valuesAfter(lines, fatWords)
        val sugar = valuesAfter(lines, sugarWords)
        // wirklich zweispaltig: kcal doppelt oder mindestens zwei Nährwerte doppelt
        val doubled = listOf(protein, carbs, fat).count { it.size >= 2 }
        if (kcals.size < 2 && doubled < 2) return emptyList()
        return bases.mapIndexed { i, b ->
            NutritionColumn(b, kcals.getOrNull(i), protein.getOrNull(i), carbs.getOrNull(i), fat.getOrNull(i), sugar.getOrNull(i))
        }
    }

    /** Alle Zahlen hinter dem Wort (zwei Spalten = zwei Zahlen), sonst die der nächsten Zeile. */
    private fun valuesAfter(lines: List<String>, label: Regex): List<Double> {
        fun numbers(t: String): List<Double> {
            val grams = gramRegex.findAll(t).mapNotNull { toDouble(it.groupValues[1]) }.toList()
            return grams.ifEmpty { anyNumber.findAll(t).mapNotNull { toDouble(it.groupValues[1]) }.toList() }
        }
        for ((i, line) in lines.withIndex()) {
            val m = label.find(line) ?: continue
            if (label === fatWords && subLine.containsMatchIn(line)) continue
            if (isLabelRow(line)) continue
            val rest = line.substring(m.range.last + 1)
            val found = numbers(rest).ifEmpty { lines.getOrNull(i + 1)?.let(::numbers).orEmpty() }.filter { it in 0.0..1000.0 }
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    /**
     * Kacheln: eine Zeile mit mehreren Nährwert-Namen, darunter gleich viele Zahlen in derselben Reihenfolge
     * (die Zeile mit „%“ darunter wird ignoriert). Ergebnis: Wort → Wert.
     */
    private fun tiles(lines: List<String>): Map<Regex, Double> {
        val words = listOf(energyWords, fatWords, carbWords, proteinWords, sugarWords, saltWords)
        for ((i, line) in lines.withIndex()) {
            if (!isLabelRow(line)) continue
            val order = words.mapNotNull { w -> w.find(line)?.let { it.range.first to w } }.sortedBy { it.first }.map { it.second }
            val next = lines.getOrNull(i + 1) ?: continue
            if (next.contains('%')) continue
            val numbers = anyNumber.findAll(next).mapNotNull { toDouble(it.groupValues[1]) }.toList()
            if (numbers.size < order.size) continue
            return order.zip(numbers).toMap()
        }
        return emptyMap()
    }

    /** „Portion (650 g)“ allein ist keine Spaltenüberschrift für 100 g. */
    private fun portionWeightOnly(line: String): Boolean =
        !Regex("""100\s*(g|ml)""", RegexOption.IGNORE_CASE).containsMatchIn(line.replace(Regex("""\(\s*\d+\s*g\s*\)"""), ""))

    private val portionWeight = listOf(
        Regex("""(?:portion|teller|gericht)\s*\(?\s*(?:à|a|=|zu|ca\.?)?\s*(\d{2,4})\s*g\b""", RegexOption.IGNORE_CASE),
        Regex("""(\d{2,4})\s*g\s*\)?\s*(?:portion|teller)""", RegexOption.IGNORE_CASE),
    )

    /** Portionsgewicht: steht es da („Portion (350 g)“), sonst kcal pro Portion ÷ kcal pro 100 g. */
    private fun portionGrams(text: String, columns: List<NutritionColumn>): Double? {
        portionWeight.firstNotNullOfOrNull { r -> r.find(text)?.groupValues?.get(1)?.let(::toDouble) }
            ?.takeIf { it in 20.0..2000.0 && it != 100.0 }?.let { return it }
        val p = columns.firstOrNull { it.basis == NutritionBasis.PER_PORTION }?.kcal
        val h = columns.firstOrNull { it.basis != NutritionBasis.PER_PORTION }?.kcal
        if (p != null && h != null && h > 0) return kotlin.math.round(p / h * 100).takeIf { it in 20.0..2000.0 }
        return null
    }

    private fun valueAfter(lines: List<String>, label: Regex): Double? {
        for ((i, line) in lines.withIndex()) {
            val m = label.find(line) ?: continue
            // „davon gesättigte Fettsäuren“ o. Ä. ist nicht der Fettwert
            if (label === fatWords && subLine.containsMatchIn(line)) continue
            if (isLabelRow(line)) continue
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
    /** Bildschirm-Reste, die kein Gerichtname sind (Mensa-Webseite, Reiter) */
    private val notAName = Regex("""datenschutz|impressum|^details\b|cookie|anmelden|speiseplan|allergene|zusatzstoffe|^davon\b|alkohol|stärke""", RegexOption.IGNORE_CASE)

    private fun guessName(lines: List<String>): String? = lines
        .firstOrNull { l -> l.count { it.isLetter() } >= 4 && !nutritionWords.containsMatchIn(l) && !notAName.containsMatchIn(l) }
        ?.take(60)

    private fun toDouble(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

    /** Energiewerte: „2.287“ ist zweitausend…, kein Komma */
    private fun energy(s: String): Double? =
        if (Regex("""\d{1,2}\.\d{3}""").matches(s)) s.replace(".", "").toDoubleOrNull() else toDouble(s)

    /**
     * Kopfzeile mit mehreren Nährwert-Namen nebeneinander (z. B. Kacheln „Brennwert (kCal)  Fett (g)  Kohlenhydrate (g)“)
     * – deren Zahlen (und die %-Zeile darunter) gehören nicht zu einer einzelnen Zeile der Tabelle.
     */
    private fun isLabelRow(line: String): Boolean =
        listOf(proteinWords, carbWords, fatWords, energyWords, saltWords).count { it.containsMatchIn(line) } >= 3

    private val energyWords = Regex("""brennwert|energie|energy""", RegexOption.IGNORE_CASE)
    private val saltWords = Regex("""\bsalz\b|\bsalt\b""", RegexOption.IGNORE_CASE)

    /**
     * Mehrere Lesarten desselben Fotos (z. B. zeilenweise neu zusammengesetzt und wie erkannt) –
     * die Lesart mit den meisten gefundenen Werten gewinnt; bei Gleichstand die erste.
     */
    fun parseBest(variants: List<String>): ParsedNutrition {
        val parsed = variants.filter { it.isNotBlank() }.map(::parse).ifEmpty { listOf(parse("")) }
        fun score(p: ParsedNutrition) = p.columns.size * 10 + p.columns.sumOf { c -> listOfNotNull(c.kcal, c.protein, c.carbs, c.fat, c.sugar).size } +
            listOfNotNull(p.kcal, p.protein, p.carbs, p.fat, p.sugar, p.portionGrams).size
        return parsed.maxWith(compareBy<ParsedNutrition> { score(it) }.thenByDescending { -parsed.indexOf(it) })
    }
}
