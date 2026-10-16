package de.kalorien.app.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry WHERE date = :date ORDER BY createdAt")
    fun observeDay(date: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun get(id: Long): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("SELECT * FROM entry WHERE date = :date AND meal = :meal ORDER BY createdAt")
    suspend fun getMeal(date: String, meal: String): List<EntryEntity>

    @Query("SELECT * FROM entry WHERE foodId = :foodId AND date = :date")
    suspend fun getForFoodOnDay(foodId: Long, date: String): List<EntryEntity>

    @Insert
    suspend fun insertAll(entries: List<EntryEntity>): List<Long>

    @Query("DELETE FROM entry WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)

    // --- Sicherung
    @Query("SELECT * FROM entry ORDER BY date, createdAt")
    suspend fun all(): List<EntryEntity>

    @Query("DELETE FROM entry")
    suspend fun deleteAll()
}

@Dao
interface FoodDao {
    /** Grobe Vorauswahl über das erste Suchwort; Feinsortierung macht SearchRanker. */
    @Query("SELECT * FROM food WHERE searchKey LIKE '%' || :term || '%' AND source != 'QUICK' LIMIT :limit")
    suspend fun searchRaw(term: String, limit: Int = 500): List<FoodEntity>

    @Query("SELECT * FROM food WHERE lastUsedAt IS NOT NULL AND source != 'QUICK' ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 12): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE favorite = 1 ORDER BY name COLLATE NOCASE")
    fun observeFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun get(id: Long): FoodEntity?

    /** Bekanntes Produkt zu einem Barcode (egal ob von Open Food Facts oder selbst angelegt), neuestes zuerst. */
    @Query("SELECT * FROM food WHERE ean = :ean ORDER BY id DESC LIMIT 1")
    suspend fun findByEan(ean: String): FoodEntity?

    @Query("SELECT * FROM food WHERE source = :source AND sourceId = :sourceId LIMIT 1")
    suspend fun findBySource(source: String, sourceId: String): FoodEntity?

    @Update
    suspend fun update(food: FoodEntity)

    @Query("UPDATE food SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE food SET lastUsedAt = :time, useCount = useCount + 1 WHERE id = :id")
    suspend fun markUsed(id: Long, time: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(food: FoodEntity): Long

    @Query(
        """UPDATE food SET name = :name, kcal100 = :kcal, protein100 = :protein, carbs100 = :carbs,
           fat100 = :fat, sugar100 = :sugar, isLiquid = :isLiquid, searchKey = :searchKey
           WHERE source = 'BLS' AND sourceId = :code AND isAdjusted = 0""",
    )
    suspend fun updateBls(
        code: String, name: String, kcal: Double, protein: Double, carbs: Double, fat: Double, sugar: Double?,
        isLiquid: Boolean, searchKey: String,
    ): Int

    /** Korrigierte BLS-Einträge: nur die Originalwerte auf den neuen Stand bringen. */
    @Query(
        """UPDATE food SET origKcal100 = :kcal, origProtein100 = :protein, origCarbs100 = :carbs,
           origFat100 = :fat, origSugar100 = :sugar
           WHERE source = 'BLS' AND sourceId = :code AND isAdjusted = 1""",
    )
    suspend fun updateBlsOriginal(code: String, kcal: Double, protein: Double, carbs: Double, fat: Double, sugar: Double?): Int

    // --- Sicherung: alles, was nicht aus der eingebauten BLS-Tabelle neu erzeugt werden kann
    @Query("SELECT * FROM food WHERE source != 'BLS' OR favorite = 1 OR useCount > 0 OR isAdjusted = 1")
    suspend fun userRelevant(): List<FoodEntity>

    @Query("DELETE FROM food WHERE source != 'BLS'")
    suspend fun deleteNonBls()

    @Query(
        """UPDATE food SET favorite = 0, useCount = 0, lastUsedAt = NULL WHERE source = 'BLS'""",
    )
    suspend fun resetBlsUsage()

    @Query("SELECT * FROM food WHERE source = 'BLS' AND sourceId = :code LIMIT 1")
    suspend fun findBls(code: String): FoodEntity?

    @Query("UPDATE entry SET foodId = :foodId WHERE id = :entryId")
    suspend fun linkEntry(entryId: Long, foodId: Long)

    @Query("SELECT COUNT(*) FROM food WHERE source = 'BLS'")
    suspend fun countBls(): Int
}
