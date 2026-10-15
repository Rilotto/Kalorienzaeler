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

    @Query("UPDATE food SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE food SET lastUsedAt = :time, useCount = useCount + 1 WHERE id = :id")
    suspend fun markUsed(id: Long, time: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(food: FoodEntity): Long

    @Query(
        """UPDATE food SET name = :name, kcal100 = :kcal, protein100 = :protein, carbs100 = :carbs,
           fat100 = :fat, isLiquid = :isLiquid, searchKey = :searchKey
           WHERE source = 'BLS' AND sourceId = :code""",
    )
    suspend fun updateBls(
        code: String, name: String, kcal: Double, protein: Double, carbs: Double, fat: Double,
        isLiquid: Boolean, searchKey: String,
    ): Int

    @Query("SELECT COUNT(*) FROM food WHERE source = 'BLS'")
    suspend fun countBls(): Int
}
