package de.kalorien.app.core.domain

import java.time.LocalTime

/** Die vier Mahlzeiten. Der Name (`name`) wird so in der Datenbank gespeichert – nicht umbenennen. */
enum class Meal {
    BREAKFAST, LUNCH, DINNER, SNACK;

    companion object {
        /** Vorschlag nach Uhrzeit: 4–10 Uhr Frühstück, 11–14 Mittag, 17–20 Abend, sonst Snack. */
        fun forTime(time: LocalTime): Meal = when (time.hour) {
            in 4..10 -> BREAKFAST
            in 11..14 -> LUNCH
            in 17..20 -> DINNER
            else -> SNACK
        }

        fun fromDb(value: String): Meal = entries.firstOrNull { it.name == value } ?: SNACK
    }
}
