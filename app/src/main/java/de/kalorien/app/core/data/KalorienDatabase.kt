package de.kalorien.app.core.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1 (Block 1/2), Version 2 (v0.3.1: food.isPortionFood). Jede Änderung: version + 1, Migration in [MIGRATIONS] oder
 * `autoMigrations`, Schema-Datei in app/schemas wird von CI eingecheckt.
 * NIEMALS fallbackToDestructiveMigration – das würde Nutzerdaten löschen.
 */
@Database(
    entities = [FoodEntity::class, EntryEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class KalorienDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun foodDao(): FoodDao

    companion object {
        private val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()

        fun create(context: Context): KalorienDatabase =
            Room.databaseBuilder(context, KalorienDatabase::class.java, "kalorien.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
