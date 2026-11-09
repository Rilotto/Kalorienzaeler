package de.kalorien.app.core.domain

import kotlin.math.roundToInt

/** Eine Zutat mit ihren Nährwerten (für die Menge, die ins Gericht kommt). */
data class DishPart(val amount: Amount, val grams: Double?)

/**
 * Rechnet ein Gericht aus Zutaten aus: Summe aller Zutaten geteilt durch die Anzahl Portionen.
 * Zucker: Summe der bekannten Werte; unbekannt nur, wenn keine Zutat einen Zuckerwert hat.
 */
object DishCalc {
    fun total(parts: List<DishPart>): Amount {
        val sugarKnown = parts.mapNotNull { it.amount.sugar }
        return Amount(
            kcal = parts.sumOf { it.amount.kcal },
            protein = parts.sumOf { it.amount.protein },
            carbs = parts.sumOf { it.amount.carbs },
            fat = parts.sumOf { it.amount.fat },
            sugar = if (sugarKnown.isEmpty()) null else sugarKnown.sum(),
        )
    }

    /** Werte pro Portion als [Per100] – so werden Portions-Lebensmittel gespeichert (Werte gelten für 1 Portion). */
    fun perPortion(parts: List<DishPart>, servings: Double): Per100 {
        require(servings > 0) { "Mindestens eine Portion" }
        val t = total(parts)
        return Per100(t.kcal / servings, t.protein / servings, t.carbs / servings, t.fat / servings, t.sugar?.div(servings))
    }

    /** Gewicht einer Portion, wenn alle Zutaten ein Gewicht haben. */
    fun gramsPerPortion(parts: List<DishPart>, servings: Double): Double? {
        if (parts.isEmpty() || parts.any { it.grams == null }) return null
        return (parts.sumOf { it.grams!! } / servings * 10).roundToInt() / 10.0
    }
}

/** Mensa: aus einer erkannten Tabelle die Werte pro Portion für ein Gericht machen. */
object MensaCalc {
    /**
     * Werte pro Portion aus „pro 100 g“ und Portionsgewicht – oder direkt aus der Portions-Spalte.
     * Gibt null zurück, wenn keine kcal bekannt sind.
     */
    fun portionFrom(per100: Per100?, portionGrams: Double?, perPortion: Per100?): Per100? {
        if (perPortion != null && perPortion.kcal > 0) return perPortion
        if (per100 != null && portionGrams != null && portionGrams > 0) {
            val f = portionGrams / 100.0
            return Per100(per100.kcal * f, per100.protein * f, per100.carbs * f, per100.fat * f, per100.sugar?.times(f))
        }
        return null
    }
}
