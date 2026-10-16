package de.kalorien.app.core.domain

import kotlin.math.roundToInt

/**
 * Menge in einer Einheit („1,5 × Scheibe“) statt in Gramm.
 * Die Einheit kommt aus den Portions-Vorschlägen („1 Scheibe“ = 25 g).
 */
object UnitAmount {
    /** „1 Scheibe“ → „Scheibe“, „1 Stück (M)“ → „Stück (M)“, „½ Pizza“ bleibt. */
    fun unitName(portionLabel: String): String = portionLabel.removePrefix("1 ").trim()

    /** Schnellauswahl für die Anzahl */
    val quickCounts = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

    /** Ist das eine reine Mengenangabe („100 g“, „50 g“)? Dann gibt es keine Einheit. */
    fun isPlainAmount(label: String): Boolean = Regex("""\d+ (g|ml)""").matches(label)

    /** „1,5 × Scheibe (38 g)“ */
    fun describe(count: Double, unit: String, grams: Double, isLiquid: Boolean): String {
        val u = if (isLiquid) "ml" else "g"
        return "${NumberInput.format(count)} × $unit (${grams.roundToInt()} $u)"
    }
}
