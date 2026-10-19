package de.kalorien.app.core.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1 (Block 1/2), Version 2 (v0.3.1: food.isPortionFood), Version 3 (v0.4.0: food.servingGrams/servingLabel), Version 4 (v0.5.0: Zucker, Einheiten, Korrekturen), Version 5 (v0.6.0: Mahlzeit-Vorlagen). Jede Änderung: version + 1, Migration in [MIGRATIONS] oder
 * `autoMigrations`, Schema-Datei in app/schemas wird von CI eingecheckt.
 * NIEMALS fallbackToDestructiveMigration – das würde Nutzerdaten löschen.
 */
@Database(
    entities = [FoodEntity::class, EntryEntity::class, TemplateEntity::class, TemplateItemEntity::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class KalorienDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun foodDao(): FoodDao
    abstract fun templateDao(): TemplateDao

    companion object {
        private val MIGRATIONS = emptyArray<androidx.room.migration.Migration>()

        fun create(context: Context): KalorienDatabase =
            Room.databaseBuilder(context, KalorienDatabase::class.java, "kalorien.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
