package de.kalorien.app.core.domain

/** Prüft Barcodes von Lebensmitteln (EAN-13, EAN-8, UPC-A/-E) inkl. Prüfziffer, um Fehllesungen auszusortieren. */
object Barcode {
    fun clean(text: String): String = text.filter { it.isDigit() }

    fun isValid(code: String): Boolean {
        val c = clean(code)
        if (c.length != code.trim().length) return false
        return when (c.length) {
            8, 12, 13 -> checksumOk(c)
            6 -> true // UPC-E: Prüfung erst nach Expansion, selten bei Lebensmitteln in DE
            else -> false
        }
    }

    /** GTIN-Prüfziffer: von rechts (ohne Prüfziffer) abwechselnd ×3 und ×1. */
    private fun checksumOk(c: String): Boolean {
        val digits = c.map { it - '0' }
        val body = digits.dropLast(1).reversed()
        val sum = body.withIndex().sumOf { (i, d) -> if (i % 2 == 0) d * 3 else d }
        return (10 - sum % 10) % 10 == digits.last()
    }
}

/** Kleine Helfer rund um Nährwertangaben. */
object NutritionUnits {
    fun kjToKcal(kj: Double): Double = kj / 4.184

    /** Erkennt Getränke an der Mengenangabe („330 ml“, „1,5 l“, „0.5L“). */
    fun isLiquidQuantity(quantity: String?): Boolean {
        if (quantity.isNullOrBlank()) return false
        return Regex("""\d\s*(ml|cl|l)\b""", RegexOption.IGNORE_CASE).containsMatchIn(quantity)
    }
}
