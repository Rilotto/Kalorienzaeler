package de.kalorien.app.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ältere Einträge ohne Zucker bekommen ihn nachträglich – kcal bleiben unverändert. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FillSugarTest {
    @Test
    fun fillsSugarForOldEntries() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KalorienDatabase::class.java)
            .allowMainThreadQueries().build()
        val foods = db.foodDao()
        val entries = db.entryDao()
        val oats = foods.insert(
            FoodEntity(name = "Hafer Flocken", source = FoodSource.BLS, kcal100 = 360.0, protein100 = 13.0,
                carbs100 = 58.0, fat100 = 7.0, sugar100 = 1.2, searchKey = "hafer"),
        )
        val canteen = foods.insert(
            FoodEntity(name = "Kantine", source = FoodSource.CUSTOM, kcal100 = 600.0, protein100 = 30.0,
                carbs100 = 60.0, fat100 = 20.0, sugar100 = 10.0, searchKey = "kantine", isPortionFood = true),
        )
        val unknown = foods.insert(
            FoodEntity(name = "Alt", source = FoodSource.CUSTOM, kcal100 = 100.0, protein100 = 0.0,
                carbs100 = 0.0, fat100 = 0.0, searchKey = "alt"),
        )
        fun e(foodId: Long?, grams: Double?, kcal: Int, sugar: Double? = null) = EntryEntity(
            date = "2026-10-15", meal = "LUNCH", foodId = foodId, name = "x", grams = grams, kcal = kcal,
            protein = 0.0, carbs = 0.0, fat = 0.0, createdAt = 0, sugar = sugar,
        )
        val a = entries.insert(e(oats, 50.0, 180))
        val b = entries.insert(e(canteen, null, 900)) // 1,5 Portionen (alter Eintrag ohne Anzahl)
        val c = entries.insert(e(unknown, 100.0, 100))
        val d = entries.insert(e(null, null, 500)) // Schnelleintrag
        val f = entries.insert(e(oats, 50.0, 180, sugar = 3.0)) // schon bekannt → bleibt

        entries.fillMissingSugar()

        assertEquals(0.6, entries.get(a)!!.sugar!!, 0.001)
        assertEquals(15.0, entries.get(b)!!.sugar!!, 0.001)
        assertEquals(900, entries.get(b)!!.kcal)
        assertNull(entries.get(c)!!.sugar)
        assertNull(entries.get(d)!!.sugar)
        assertEquals(3.0, entries.get(f)!!.sugar!!, 0.001)
        db.close()
    }
}
