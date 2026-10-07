package de.kalorien.app.core.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Reine Rechenlogik ohne Android-Abhängigkeiten – wird per Unit-Test geprüft. */
object Nutrition {
    /** Kalorien für eine Menge, ausgehend von kcal je 100 g/ml. */
    fun kcalFor(kcalPer100: Double, grams: Double): Int {
        require(grams >= 0) { "Menge darf nicht negativ sein" }
        return (kcalPer100 * grams / 100.0).roundToInt()
    }

    /** Verbleibende Kalorien; negativ = über dem Ziel. */
    fun remaining(goal: Int, eaten: Int): Int = goal - eaten

    /** Fortschritt 0..1 für Ring und Balken (über dem Ziel bleibt es bei 1). */
    fun progress(goal: Int, eaten: Int): Float =
        if (goal <= 0) 0f else (eaten.toFloat() / goal).coerceIn(0f, 1f)

    private val german: NumberFormat = NumberFormat.getIntegerInstance(Locale.GERMANY)

    /** Deutsche Tausendertrennung: 2200 → "2.200". */
    fun format(value: Int): String = german.format(value)
}
