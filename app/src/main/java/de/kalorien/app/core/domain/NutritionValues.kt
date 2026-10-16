package de.kalorien.app.core.domain

import kotlin.math.roundToInt

/** Nährwerte je 100 g bzw. 100 ml. Zucker ist optional (ältere Daten haben keinen Wert). */
data class Per100(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double, val sugar: Double? = null)

/** Nährwerte für eine konkrete Menge (Snapshot, der im Eintrag gespeichert wird). */
data class Amount(val kcal: Int, val protein: Double, val carbs: Double, val fat: Double, val sugar: Double? = null) {
    operator fun plus(o: Amount) = Amount(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat,
        if (sugar == null && o.sugar == null) null else (sugar ?: 0.0) + (o.sugar ?: 0.0),
    )

    companion object {
        val ZERO = Amount(0, 0.0, 0.0, 0.0)

        fun of(per100: Per100, grams: Double): Amount {
            require(grams >= 0) { "Menge darf nicht negativ sein" }
            val f = grams / 100.0
            return Amount(
                kcal = (per100.kcal * f).roundToInt(),
                protein = per100.protein * f,
                carbs = per100.carbs * f,
                fat = per100.fat * f,
                sugar = per100.sugar?.let { it * f },
            )
        }
    }
}

/** Liest Zahlen so, wie man sie auf einer deutschen Tastatur tippt („12,5“ oder „12.5“). */
object NumberInput {
    fun parse(text: String): Double? {
        val t = text.trim().replace(" ", "").replace(',', '.')
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    }

    /** 50.0 → "50", 12.5 → "12,5" */
    fun format(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString().replace('.', ',')
    }
}
