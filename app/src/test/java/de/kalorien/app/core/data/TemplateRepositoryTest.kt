package de.kalorien.app.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.kalorien.app.core.domain.Meal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Vorlage speichern → eintragen; Lebensmittel löschen lässt Einträge und Vorlagen bestehen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TemplateRepositoryTest {
    @Test
    fun saveApplyAndDeleteFood() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KalorienDatabase::class.java)
            .allowMainThreadQueries().build()
        val entries = EntryRepository(db)
        val foods = FoodRepository(db)
        val templates = TemplateRepository(db)
        val day = LocalDate.of(2026, 10, 15)
        val quark = db.foodDao().insert(
            FoodEntity(name = "Quark", source = FoodSource.CUSTOM, kcal100 = 67.0, protein100 = 12.0, carbs100 = 4.0,
                fat100 = 0.2, sugar100 = 4.0, searchKey = "quark"),
        )
        val food = db.foodDao().get(quark)!!
        entries.addFood(food, 250.0, Meal.BREAKFAST, day)
        entries.addQuick("Kaffee", 5, Meal.BREAKFAST, day)

        assertNull(templates.saveFromMeal("Leer", Meal.DINNER, day))
        val id = templates.saveFromMeal("Mein Frühstück", Meal.BREAKFAST, day)!!
        val t = templates.observeAll().first().single()
        assertEquals(id, t.template.id)
        assertEquals(2, t.items.size)
        assertEquals(168 + 5, t.kcal)

        val today = day.plusDays(1)
        val ids = templates.apply(t, Meal.LUNCH, today)
        assertEquals(2, ids.size)
        val copied = entries.observeDay(today).first()
        assertEquals(setOf("Quark", "Kaffee"), copied.map { it.name }.toSet())
        assertTrue(copied.all { it.meal == Meal.LUNCH.name })
        assertEquals(1, templates.observeAll().first().single().template.useCount)

        // Lebensmittel löschen: Werte bleiben, Verknüpfung weg
        assertTrue(foods.deleteOwn(food))
        assertNull(db.foodDao().get(quark))
        assertTrue(entries.observeDay(day).first().all { it.foodId == null })
        assertEquals(168, entries.observeDay(day).first().first { it.name == "Quark" }.kcal)
        assertTrue(templates.observeAll().first().single().items.all { it.foodId == null })

        // Totale für den Verlauf
        val totals = entries.observeTotals(day, today).first().associateBy { it.date }
        assertEquals(173, totals[day]!!.kcal)
        assertEquals(2, totals[today]!!.entries)
        db.close()
    }
}
