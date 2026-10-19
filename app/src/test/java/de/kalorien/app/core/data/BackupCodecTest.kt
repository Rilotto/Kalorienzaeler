package de.kalorien.app.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sicherung → JSON → Sicherung muss dieselben Daten ergeben (org.json braucht Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCodecTest {
    @Test
    fun roundTrip() {
        val food = FoodEntity(
            id = 42, name = "Kantine", source = FoodSource.CUSTOM, kcal100 = 650.0, protein100 = 30.0,
            carbs100 = 70.0, fat100 = 20.0, sugar100 = 8.0, favorite = true, useCount = 3, searchKey = "kantine",
            isPortionFood = true,
        )
        val bls = FoodEntity(
            id = 7, name = "Hafer Flocken", source = FoodSource.BLS, sourceId = "C133000", kcal100 = 360.0,
            protein100 = 13.2, carbs100 = 53.3, fat100 = 6.7, favorite = true, searchKey = "hafer flocken haferflocken",
            isAdjusted = true, origKcal100 = 348.0,
        )
        val entry = EntryEntity(
            id = 1, date = "2026-10-16", meal = "LUNCH", foodId = 42, name = "Kantine", grams = null, kcal = 975,
            protein = 45.0, carbs = 105.0, fat = 30.0, sugar = 12.0, createdAt = 123, unitLabel = "Portion", unitCount = 1.5,
        )
        val settings = Settings(dailyGoal = 2000, proteinGoal = 140, sugarLimit = 40)

        val template = TemplateWithItems(
            TemplateEntity(id = 3, name = "Mein Frühstück", meal = "BREAKFAST", createdAt = 5, useCount = 2),
            listOf(
                TemplateItemEntity(id = 9, templateId = 3, position = 1, foodId = 7, name = "Hafer Flocken", grams = 50.0, kcal = 180,
                    protein = 6.6, carbs = 29.0, fat = 3.4, sugar = 0.6),
                TemplateItemEntity(id = 8, templateId = 3, position = 0, foodId = null, name = "Kaffee", grams = null, kcal = 5,
                    protein = 0.0, carbs = 0.0, fat = 0.0, unitLabel = "Tasse", unitCount = 1.0),
            ),
        )
        val back = BackupCodec.decode(
            BackupCodec.encode(BackupData(settings.copy(weekdayGoals = mapOf(java.time.DayOfWeek.SATURDAY to 2600), themeMode = ThemeMode.DARK, startTab = 1),
                listOf(food, bls), listOf(entry), listOf(template)), 0),
        )
        assertEquals(mapOf(java.time.DayOfWeek.SATURDAY to 2600), back.settings.weekdayGoals)
        assertEquals(ThemeMode.DARK, back.settings.themeMode)
        assertEquals(1, back.settings.startTab)
        assertEquals("Mein Frühstück", back.templates.single().template.name)
        assertEquals(listOf("Kaffee", "Hafer Flocken"), back.templates.single().sortedItems.map { it.name })
        assertEquals(7L, back.templates.single().sortedItems[1].foodId)
        assertEquals("Tasse", back.templates.single().sortedItems[0].unitLabel)

        assertEquals(2000, back.settings.dailyGoal)
        assertEquals(140, back.settings.proteinGoal)
        assertNull(back.settings.carbsGoal)
        assertEquals(40, back.settings.sugarLimit)
        assertEquals(food, back.foods[0])
        assertEquals(bls.copy(), back.foods[1])
        assertEquals(entry.copy(id = 0), back.entries[0].copy(id = 0))
        assertEquals(1.5, back.entries[0].unitCount!!, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignFiles() {
        BackupCodec.decode("""{"app":"Etwas anderes","format":1}""")
    }
}
