package de.kalorien.app.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.Per100
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

/** Gericht speichern/ändern, Mensa-Gericht wiederfinden, Platz belegen, Sicherung mit beidem. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DishMensaRepositoryTest {
    private fun db() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KalorienDatabase::class.java)
        .allowMainThreadQueries().build()

    @Test
    fun dishSaveEditAndBackup() = runBlocking {
        val db = db()
        val dishes = DishRepository(db)
        val bread = db.foodDao().insert(FoodEntity(name = "Brot", source = FoodSource.CUSTOM, kcal100 = 222.0, protein100 = 7.0, carbs100 = 41.0, fat100 = 1.5, searchKey = "brot"))
        val draft = DishDraft(
            name = "Mein Sandwich", portionLabel = "1 Sandwich", servings = 1.0,
            items = listOf(
                DishDraftItem(bread, "Brot", 100.0, false, Amount(222, 7.0, 41.0, 1.5, 2.0), "Scheibe", 2.0),
                DishDraftItem(null, "Butter", 5.0, false, Amount(37, 0.0, 0.0, 4.1, 0.0)),
            ),
        )
        val food = dishes.save(draft)
        assertEquals(FoodSource.DISH, food.source)
        assertTrue(food.isPortionFood)
        assertEquals(259.0, food.kcal100, 0.01)
        assertEquals(105.0, food.servingGrams!!, 0.01)
        assertEquals("1 Sandwich", food.servingLabel)

        // ändern: 2 Portionen → Werte halbieren, Zutaten bleiben
        val loaded = dishes.load(food)
        assertEquals(2, loaded.items.size)
        val edited = dishes.save(loaded.copy(servings = 2.0))
        assertEquals(food.id, edited.id)
        assertEquals(129.5, edited.kcal100, 0.01)
        assertEquals(2, dishes.ingredients(food.id).size)

        // Sicherung: Gericht und Zutaten kommen mit
        val data = BackupData(Settings(), db.foodDao().userRelevant(), emptyList(), emptyList(), db.dishDao().all(), db.dishDao().allIngredients(), emptyList())
        val back = BackupCodec.decode(BackupCodec.encode(data, 0))
        assertEquals(1, back.dishes.size)
        assertEquals(2.0, back.dishes.single().servings, 0.01)
        assertEquals(listOf("Brot", "Butter"), back.dishIngredients.sortedBy { it.position }.map { it.name })
        assertEquals("Scheibe", back.dishIngredients.first { it.name == "Brot" }.unitLabel)
        db.close()
    }

    @Test
    fun mensaDishIsReusedAndSlotAssigned() = runBlocking {
        val db = db()
        val mensa = MensaRepository(db)
        mensa.ensureDefaultSlots(listOf("Menü 1", "Menü 2"))
        mensa.ensureDefaultSlots(listOf("X")) // nur beim ersten Mal
        val slots = mensa.observeSlots().first()
        assertEquals(listOf("Menü 1", "Menü 2"), slots.map { it.name })

        val a = mensa.saveDish("Hähnchen-Curry mit Reis", Per100(435.0, 21.4, 52.5, 14.7, 7.0), 350.0)
        val b = mensa.saveDish("Hähnchen-Curry mit Reis", Per100(440.0, 22.0, 53.0, 15.0, 7.0), null)
        assertEquals(a.id, b.id) // gleiches Gericht wird aktualisiert, nicht doppelt angelegt
        assertEquals(440.0, b.kcal100, 0.01)
        assertEquals(350.0, b.servingGrams!!, 0.01) // Gewicht bleibt, wenn kein neues angegeben
        assertEquals(1, mensa.observeKnownDishes().first().size)

        val day = LocalDate.of(2026, 11, 9)
        mensa.assign(slots.first(), b, day)
        val s1 = mensa.observeSlots().first().first()
        assertEquals(b.id, s1.foodId)
        assertEquals("2026-11-09", s1.date)

        // Lebensmittel löschen gibt den Platz frei
        FoodRepository(db).deleteOwn(b)
        assertNull(mensa.observeSlots().first().first().foodId)
        db.close()
    }
}
