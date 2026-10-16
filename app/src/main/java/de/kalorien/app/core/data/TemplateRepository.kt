package de.kalorien.app.core.data

import androidx.room.withTransaction
import de.kalorien.app.core.domain.Meal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Mahlzeit-Vorlagen: eine Mahlzeit speichern und später mit einem Tipp eintragen. */
class TemplateRepository(private val db: KalorienDatabase) {
    private val templates = db.templateDao()
    private val entries = db.entryDao()

    fun observeAll(): Flow<List<TemplateWithItems>> = templates.observeAll()

    /** Alle Einträge einer Mahlzeit an einem Tag als Vorlage speichern. Null, wenn die Mahlzeit leer ist. */
    suspend fun saveFromMeal(name: String, meal: Meal, date: LocalDate): Long? {
        val source = entries.getMeal(date.toString(), meal.name)
        if (source.isEmpty()) return null
        return db.withTransaction {
            val id = templates.insert(
                TemplateEntity(name = name.trim().ifEmpty { "Vorlage" }, meal = meal.name, createdAt = System.currentTimeMillis()),
            )
            templates.insertItems(
                source.mapIndexed { i, e ->
                    TemplateItemEntity(
                        templateId = id, position = i, foodId = e.foodId, name = e.name, brand = e.brand, grams = e.grams,
                        isLiquid = e.isLiquid, kcal = e.kcal, protein = e.protein, carbs = e.carbs, fat = e.fat,
                        sugar = e.sugar, unitLabel = e.unitLabel, unitCount = e.unitCount,
                    )
                },
            )
            id
        }
    }

    /** Vorlage in eine Mahlzeit eintragen. Gibt die neuen Eintrags-IDs zurück (für „Rückgängig“). */
    suspend fun apply(template: TemplateWithItems, meal: Meal, date: LocalDate): List<Long> {
        var now = System.currentTimeMillis()
        val list = template.sortedItems.map { t ->
            EntryEntity(
                date = date.toString(), meal = meal.name, foodId = t.foodId, name = t.name, brand = t.brand,
                grams = t.grams, isLiquid = t.isLiquid, kcal = t.kcal, protein = t.protein, carbs = t.carbs, fat = t.fat,
                sugar = t.sugar, createdAt = now++, unitLabel = t.unitLabel, unitCount = t.unitCount,
            )
        }
        return db.withTransaction {
            val ids = entries.insertAll(list)
            templates.markUsed(template.template.id)
            list.mapNotNull { it.foodId }.distinct().forEach { db.foodDao().markUsed(it, now) }
            ids
        }
    }

    suspend fun rename(id: Long, name: String) = templates.rename(id, name.trim())

    suspend fun delete(id: Long) = templates.delete(id)

    /** Gelöschte Vorlage wiederherstellen („Rückgängig“). */
    suspend fun restore(t: TemplateWithItems) = db.withTransaction {
        val id = templates.insert(t.template.copy(id = 0))
        templates.insertItems(t.items.map { it.copy(id = 0, templateId = id) })
    }
}
