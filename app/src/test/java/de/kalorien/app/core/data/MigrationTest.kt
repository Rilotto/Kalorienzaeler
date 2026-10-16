package de.kalorien.app.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prüft, dass Updates die Nutzerdaten behalten: alte Datenbank anlegen, Daten einfügen,
 * auf die neue Version migrieren, Daten prüfen. Für jede neue DB-Version hier einen Test ergänzen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KalorienDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsFavoritesAndEntries() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO food (id, name, source, kcal100, protein100, carbs100, fat100, isLiquid, favorite, useCount, searchKey) " +
                    "VALUES (1, 'Hafer Flocken', 'BLS', 348, 13.2, 53.3, 6.7, 0, 1, 3, 'hafer flocken haferflocken')",
            )
            execSQL(
                "INSERT INTO entry (id, date, meal, foodId, name, grams, isLiquid, kcal, protein, carbs, fat, createdAt) " +
                    "VALUES (1, '2026-10-15', 'BREAKFAST', 1, 'Hafer Flocken', 50, 0, 174, 6.6, 26.7, 3.4, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        db.query("SELECT favorite, useCount, isPortionFood FROM food WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals(3, c.getInt(1))
            assertEquals(0, c.getInt(2)) // neue Spalte mit Standardwert
        }
        db.query("SELECT kcal, grams FROM entry WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(174, c.getInt(0))
            assertEquals(50.0, c.getDouble(1), 0.0)
        }
        db.close()
    }

    @Test
    fun migrate2To3KeepsPortionFoods() {
        val dbName = "migration-2-3.db"
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO food (id, name, source, kcal100, protein100, carbs100, fat100, isLiquid, favorite, useCount, searchKey, isPortionFood) " +
                    "VALUES (7, 'Kantine', 'CUSTOM', 650, 0, 0, 0, 0, 1, 1, 'kantine', 1)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true)

        db.query("SELECT name, isPortionFood, favorite, servingGrams, servingLabel FROM food WHERE id = 7").use { c ->
            c.moveToFirst()
            assertEquals("Kantine", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals(1, c.getInt(2))
            assertEquals(true, c.isNull(3)) // neue Spalten sind leer
            assertEquals(true, c.isNull(4))
        }
        db.close()
    }

    @Test
    fun migrate3To4AddsSugarAndKeepsData() {
        val dbName = "migration-3-4.db"
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO food (id, name, source, kcal100, protein100, carbs100, fat100, isLiquid, favorite, useCount, searchKey, isPortionFood) " +
                    "VALUES (2, 'Gouda', 'BLS', 356, 24.9, 0.1, 28.6, 0, 1, 4, 'gouda', 0)",
            )
            execSQL(
                "INSERT INTO entry (id, date, meal, foodId, name, grams, isLiquid, kcal, protein, carbs, fat, createdAt) " +
                    "VALUES (3, '2026-10-15', 'LUNCH', 2, 'Gouda', 30, 0, 107, 7.5, 0.0, 8.6, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true)

        db.query("SELECT favorite, sugar100, isAdjusted, origKcal100 FROM food WHERE id = 2").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals(true, c.isNull(1))
            assertEquals(0, c.getInt(2))
            assertEquals(true, c.isNull(3))
        }
        db.query("SELECT kcal, sugar, unitLabel, unitCount FROM entry WHERE id = 3").use { c ->
            c.moveToFirst()
            assertEquals(107, c.getInt(0))
            assertEquals(true, c.isNull(1))
            assertEquals(true, c.isNull(2))
            assertEquals(true, c.isNull(3))
        }
        db.close()
    }
}
