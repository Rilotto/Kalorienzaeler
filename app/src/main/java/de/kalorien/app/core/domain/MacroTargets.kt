package de.kalorien.app.core.domain

import kotlin.math.roundToInt

/**
 * Richtwerte für die Makro-Balken aus dem Kalorienziel:
 * 20 % Eiweiß, 50 % Kohlenhydrate (je 4 kcal/g), 30 % Fett (9 kcal/g). Nur Anzeige, keine Empfehlung.
 */
data class MacroTargets(val protein: Int, val carbs: Int, val fat: Int, val sugar: Int = 0) {
    companion object {
        /** Zucker-Richtwert: höchstens 10 % der Kalorien (4 kcal/g). */
        fun fromGoal(goalKcal: Int): MacroTargets = MacroTargets(
            protein = (goalKcal * 0.20 / 4).roundToInt(),
            carbs = (goalKcal * 0.50 / 4).roundToInt(),
            fat = (goalKcal * 0.30 / 9).roundToInt(),
            sugar = (goalKcal * 0.10 / 4).roundToInt(),
        )

        /** Eigene Ziele (null = automatisch aus dem Kalorienziel). */
        fun resolve(goalKcal: Int, protein: Int?, carbs: Int?, fat: Int?, sugar: Int?): MacroTargets {
            val auto = fromGoal(goalKcal)
            return MacroTargets(protein ?: auto.protein, carbs ?: auto.carbs, fat ?: auto.fat, sugar ?: auto.sugar)
        }
    }
}
