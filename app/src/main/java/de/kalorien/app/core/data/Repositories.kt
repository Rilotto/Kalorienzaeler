package de.kalorien.app.core.data

import androidx.room.withTransaction
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.Per100
import de.kalorien.app.core.domain.SearchCandidate
import de.kalorien.app.core.domain.SearchRanker
import de.kalorien.app.core.domain.TextNormalizer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class EntryRepository(private val db: KalorienDatabase) {
    private val entries = db.entryDao()
    private val foods = db.foodDao()

    fun observeDay(date: LocalDate): Flow<List<EntryEntity>> = entries.observeDay(date.toString())

    suspend fun get(id: Long): EntryEntity? = entries.get(id)

    suspend fun addFood(food: FoodEntity, grams: Double, meal: Meal, date: LocalDate) {
        val now = System.currentTimeMillis()
        val a = Amount.of(food.per100, grams)
        db.withTransaction {
            entries.insert(
                EntryEntity(
                    date = date.toString(), meal = meal.name, foodId = food.id, name = food.name, brand = food.brand,
                    grams = grams, isLiquid = food.isLiquid, kcal = a.kcal, protein = a.protein, carbs = a.carbs,
                    fat = a.fat, createdAt = now,
                ),
            )
            foods.markUsed(food.id, now)
        }
    }

    /** Portions-Lebensmittel (gemerkter Schnelleintrag): Anzahl Portionen statt Gramm. */
    suspend fun addPortions(food: FoodEntity, portions: Double, meal: Meal, date: LocalDate) {
        val now = System.currentTimeMillis()
        val a = Amount.of(food.per100, portions * 100.0)
        val name = if (portions == 1.0) food.name else "${food.name} (${de.kalorien.app.core.domain.NumberInput.format(portions)}×)"
        db.withTransaction {
            entries.insert(
                EntryEntity(
                    date = date.toString(), meal = meal.name, foodId = food.id, name = name, brand = food.brand,
                    grams = null, kcal = a.kcal, protein = a.protein, carbs = a.carbs, fat = a.fat, createdAt = now,
                ),
            )
            foods.markUsed(food.id, now)
        }
    }

    suspend fun addQuick(name: String, kcal: Int, meal: Meal, date: LocalDate) {
        entries.insert(
            EntryEntity(
                date = date.toString(), meal = meal.name, foodId = null, name = name, grams = null,
                kcal = kcal, protein = 0.0, carbs = 0.0, fat = 0.0, createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Menge/Mahlzeit eines Lebensmittel-Eintrags ändern; Nährwerte werden neu berechnet. */
    suspend fun updateFoodEntry(entry: EntryEntity, grams: Double, meal: Meal) {
        val per100 = entry.foodId?.let { foods.get(it) }?.per100 ?: snapshotPer100(entry)
        val a = Amount.of(per100, grams)
        entries.update(entry.copy(grams = grams, meal = meal.name, kcal = a.kcal, protein = a.protein, carbs = a.carbs, fat = a.fat))
    }

    suspend fun updateQuickEntry(entry: EntryEntity, name: String, kcal: Int, meal: Meal) {
        entries.update(entry.copy(name = name, kcal = kcal, meal = meal.name))
    }

    suspend fun delete(entry: EntryEntity) = entries.delete(entry)

    /** Für „Rückgängig“: denselben Eintrag mit derselben ID wiederherstellen. */
    suspend fun restore(entry: EntryEntity) {
        entries.insert(entry)
    }

    private fun snapshotPer100(e: EntryEntity): Per100 {
        val g = e.grams?.takeIf { it > 0 } ?: 100.0
        val f = 100.0 / g
        return Per100(e.kcal * f, e.protein * f, e.carbs * f, e.fat * f)
    }
}

class FoodRepository(private val db: KalorienDatabase) {
    private val foods = db.foodDao()

    fun observeRecent(): Flow<List<FoodEntity>> = foods.observeRecent()
    fun observeFavorites(): Flow<List<FoodEntity>> = foods.observeFavorites()

    suspend fun get(id: Long): FoodEntity? = foods.get(id)

    suspend fun setFavorite(id: Long, favorite: Boolean) = foods.setFavorite(id, favorite)

    suspend fun search(query: String): List<FoodEntity> {
        val terms = SearchRanker.prefilterTerms(query)
        if (terms.isEmpty()) return emptyList()
        // Vorauswahl in der Datenbank über das längste Wort (+ Synonyme), Feinsortierung im SearchRanker
        val raw = terms.flatMap { foods.searchRaw(it) }.distinctBy { it.id }
        val byId = raw.associateBy { it.id }
        val ranked = SearchRanker.rank(
            query,
            raw.map {
                SearchCandidate(
                    it.id, TextNormalizer.normalize(listOfNotNull(it.name, it.brand).joinToString(" ")),
                    it.favorite, it.useCount, it.source == FoodSource.CUSTOM,
                )
            },
        )
        return ranked.mapNotNull { byId[it.id] }
    }

    /** Eigenes Lebensmittel anlegen (z. B. aus dem Schnelleintrag „als Lebensmittel merken“). */
    /**
     * Herz bei einem Schnelleintrag: legt ein Portions-Lebensmittel mit Name + kcal als Favorit an
     * und verknüpft den Eintrag damit (damit das Herz im Eintrag gefüllt bleibt).
     */
    suspend fun favoriteFromQuickEntry(entry: EntryEntity): FoodEntity {
        val entity = FoodEntity(
            name = entry.name.trim(), source = FoodSource.CUSTOM,
            kcal100 = entry.kcal.toDouble(), protein100 = entry.protein, carbs100 = entry.carbs, fat100 = entry.fat,
            favorite = true, lastUsedAt = System.currentTimeMillis(), useCount = 1,
            searchKey = searchKey(entry.name, null), isPortionFood = true,
        )
        return db.withTransaction {
            val id = foods.insert(entity)
            foods.linkEntry(entry.id, id)
            entity.copy(id = id)
        }
    }

    suspend fun addCustom(name: String, brand: String?, per100: Per100, isLiquid: Boolean): FoodEntity {
        val entity = FoodEntity(
            name = name.trim(), brand = brand?.trim()?.ifEmpty { null }, source = FoodSource.CUSTOM,
            kcal100 = per100.kcal, protein100 = per100.protein, carbs100 = per100.carbs, fat100 = per100.fat,
            isLiquid = isLiquid, searchKey = searchKey(name, brand),
        )
        val id = foods.insert(entity)
        return entity.copy(id = id)
    }

    companion object {
        /**
         * Normalisierter Suchtext. Zusätzlich die Variante ohne Leerzeichen, damit „haferflocken“
         * auch „Hafer Flocken“ (so heißt es im BLS) findet.
         */
        fun searchKey(name: String, brand: String?): String {
            val n = TextNormalizer.normalize(listOfNotNull(name, brand).joinToString(" "))
            val joined = n.replace(" ", "")
            return if (joined == n) n else "$n $joined"
        }
    }
}
