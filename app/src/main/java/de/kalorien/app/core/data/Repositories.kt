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

    /**
     * Lebensmittel in Gramm/ml eintragen. Optional mit Einheit („Scheibe“) und Anzahl (1,5),
     * damit der Eintrag „1,5 × Scheibe (38 g)“ zeigen kann. Gibt den gespeicherten Eintrag zurück (für „Rückgängig“).
     */
    suspend fun addFood(
        food: FoodEntity, grams: Double, meal: Meal, date: LocalDate,
        unitLabel: String? = null, unitCount: Double? = null,
    ): EntryEntity {
        val now = System.currentTimeMillis()
        val a = Amount.of(food.per100, grams)
        val entry = EntryEntity(
            date = date.toString(), meal = meal.name, foodId = food.id, name = food.name, brand = food.brand,
            grams = grams, isLiquid = food.isLiquid, kcal = a.kcal, protein = a.protein, carbs = a.carbs,
            fat = a.fat, sugar = a.sugar, createdAt = now, unitLabel = unitLabel, unitCount = unitCount,
        )
        return insertAndMarkUsed(entry, food.id, now)
    }

    /** Portions-Lebensmittel (z. B. gemerkter Kantinen-Eintrag): Anzahl Portionen statt Gramm. */
    suspend fun addPortions(food: FoodEntity, portions: Double, meal: Meal, date: LocalDate): EntryEntity {
        val now = System.currentTimeMillis()
        val a = Amount.of(food.per100, portions * 100.0)
        val entry = EntryEntity(
            date = date.toString(), meal = meal.name, foodId = food.id, name = food.name, brand = food.brand,
            grams = null, kcal = a.kcal, protein = a.protein, carbs = a.carbs, fat = a.fat, sugar = a.sugar,
            createdAt = now, unitLabel = PORTION_UNIT, unitCount = portions,
        )
        return insertAndMarkUsed(entry, food.id, now)
    }

    private suspend fun insertAndMarkUsed(entry: EntryEntity, foodId: Long, now: Long): EntryEntity =
        db.withTransaction {
            val id = entries.insert(entry)
            foods.markUsed(foodId, now)
            entry.copy(id = id)
        }

    suspend fun addQuick(name: String, kcal: Int, meal: Meal, date: LocalDate): EntryEntity {
        val entry = EntryEntity(
            date = date.toString(), meal = meal.name, foodId = null, name = name, grams = null,
            kcal = kcal, protein = 0.0, carbs = 0.0, fat = 0.0, createdAt = System.currentTimeMillis(),
        )
        return entry.copy(id = entries.insert(entry))
    }

    /** Menge/Mahlzeit/Tag eines Lebensmittel-Eintrags ändern; Nährwerte werden neu berechnet. */
    suspend fun updateFoodEntry(
        entry: EntryEntity, grams: Double, meal: Meal, date: LocalDate,
        unitLabel: String? = entry.unitLabel, unitCount: Double? = entry.unitCount,
    ) {
        val per100 = entry.foodId?.let { foods.get(it) }?.takeIf { !it.isPortionFood }?.per100 ?: snapshotPer100(entry)
        val a = Amount.of(per100, grams)
        entries.update(
            entry.copy(
                grams = grams, meal = meal.name, date = date.toString(), kcal = a.kcal, protein = a.protein,
                carbs = a.carbs, fat = a.fat, sugar = a.sugar, unitLabel = unitLabel, unitCount = unitCount,
            ),
        )
    }

    /** Portions-Eintrag: Anzahl Portionen ändern. */
    suspend fun updatePortionEntry(entry: EntryEntity, portions: Double, meal: Meal, date: LocalDate) {
        val food = entry.foodId?.let { foods.get(it) }
        val per = food?.per100 ?: run {
            val old = entry.unitCount?.takeIf { it > 0 } ?: 1.0
            Per100(entry.kcal / old, entry.protein / old, entry.carbs / old, entry.fat / old, entry.sugar?.let { it / old })
        }
        val a = Amount.of(per, portions * 100.0)
        entries.update(
            entry.copy(
                meal = meal.name, date = date.toString(), kcal = a.kcal, protein = a.protein, carbs = a.carbs,
                fat = a.fat, sugar = a.sugar, unitLabel = PORTION_UNIT, unitCount = portions,
            ),
        )
    }

    /** Schnelleintrag (Name + kcal) ändern, auch Tag und Mahlzeit. */
    suspend fun updateQuickEntry(entry: EntryEntity, name: String, kcal: Int, meal: Meal, date: LocalDate) {
        entries.update(entry.copy(name = name, kcal = kcal, meal = meal.name, date = date.toString()))
    }

    /** Werte nur dieses Eintrags von Hand überschreiben. */
    suspend fun overrideValues(entry: EntryEntity, kcal: Int, protein: Double, carbs: Double, fat: Double, sugar: Double?) {
        entries.update(entry.copy(kcal = kcal, protein = protein, carbs = carbs, fat = fat, sugar = sugar))
    }

    suspend fun delete(entry: EntryEntity) = entries.delete(entry)

    /** Für „Rückgängig“: denselben Eintrag mit derselben ID wiederherstellen. */
    suspend fun restore(entry: EntryEntity) {
        entries.insert(entry)
    }

    /** „Nochmal“: Eintrag mit gleicher Menge an einem Tag erneut eintragen. */
    suspend fun duplicate(entry: EntryEntity, date: LocalDate): EntryEntity {
        val now = System.currentTimeMillis()
        val copy = entry.copy(id = 0, date = date.toString(), createdAt = now)
        return db.withTransaction {
            val id = entries.insert(copy)
            entry.foodId?.let { foods.markUsed(it, now) }
            copy.copy(id = id)
        }
    }

    /** „Wie gestern“: alle Einträge einer Mahlzeit von [from] nach [to] kopieren. Gibt die neuen IDs zurück. */
    suspend fun copyMeal(meal: Meal, from: LocalDate, to: LocalDate): List<Long> {
        val source = entries.getMeal(from.toString(), meal.name)
        if (source.isEmpty()) return emptyList()
        var now = System.currentTimeMillis()
        val copies = source.map { it.copy(id = 0, date = to.toString(), createdAt = now++) }
        return entries.insertAll(copies)
    }

    suspend fun mealEntries(meal: Meal, date: LocalDate): List<EntryEntity> = entries.getMeal(date.toString(), meal.name)

    suspend fun deleteIds(ids: List<Long>) = entries.deleteIds(ids)

    /** Nach einer Korrektur am Lebensmittel: die Einträge dieses Tages neu berechnen. */
    suspend fun recalcForFood(food: FoodEntity, date: LocalDate) {
        entries.getForFoodOnDay(food.id, date.toString()).forEach { e ->
            if (food.isPortionFood || e.grams == null) {
                val count = e.unitCount ?: 1.0
                val a = Amount.of(food.per100, count * 100.0)
                entries.update(e.copy(kcal = a.kcal, protein = a.protein, carbs = a.carbs, fat = a.fat, sugar = a.sugar))
            } else {
                val a = Amount.of(food.per100, e.grams)
                entries.update(e.copy(kcal = a.kcal, protein = a.protein, carbs = a.carbs, fat = a.fat, sugar = a.sugar))
            }
        }
    }

    private fun snapshotPer100(e: EntryEntity): Per100 {
        val g = e.grams?.takeIf { it > 0 } ?: 100.0
        val f = 100.0 / g
        return Per100(e.kcal * f, e.protein * f, e.carbs * f, e.fat * f, e.sugar?.let { it * f })
    }

    companion object {
        /** Einheit für Portions-Lebensmittel */
        const val PORTION_UNIT = "Portion"
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

    /** Bekanntes Produkt zum Barcode in der eigenen Datenbank (Cache oder selbst angelegt). */
    suspend fun findByEan(ean: String): FoodEntity? = foods.findByEan(ean)

    /** Produkt von Open Food Facts speichern (bzw. aktualisieren) – danach ohne Internet verfügbar. */
    suspend fun saveFromOff(p: de.kalorien.app.core.food.OffProduct): FoodEntity {
        val existing = foods.findBySource(FoodSource.OFF, p.ean)
        if (existing != null && existing.isAdjusted) return existing // eigene Korrektur nicht überschreiben
        val name = p.name ?: p.ean
        val entity = FoodEntity(
            id = existing?.id ?: 0, name = name, brand = p.brand, source = FoodSource.OFF, sourceId = p.ean, ean = p.ean,
            kcal100 = p.kcal100 ?: 0.0, protein100 = p.protein100 ?: 0.0, carbs100 = p.carbs100 ?: 0.0, fat100 = p.fat100 ?: 0.0,
            sugar100 = p.sugar100, isLiquid = p.isLiquid, favorite = existing?.favorite ?: false, lastUsedAt = existing?.lastUsedAt,
            useCount = existing?.useCount ?: 0, searchKey = searchKey(name, p.brand),
            servingGrams = p.servingGrams, servingLabel = p.servingLabel,
        )
        return if (existing != null) {
            foods.update(entity); entity
        } else {
            entity.copy(id = foods.insert(entity))
        }
    }

    /**
     * Eigenes Lebensmittel anlegen (Formular „Eigenes Lebensmittel“, auch nach Scan oder Foto).
     * [perPortion] = Werte gelten für eine Portion (z. B. Kantinen-Gericht), sonst je 100 g/ml.
     */
    suspend fun createCustom(
        name: String, brand: String?, per: Per100, isLiquid: Boolean, perPortion: Boolean,
        servingGrams: Double?, ean: String?, favorite: Boolean,
    ): FoodEntity {
        val entity = FoodEntity(
            name = name.trim(), brand = brand?.trim()?.ifEmpty { null }, source = FoodSource.CUSTOM, ean = ean,
            kcal100 = per.kcal, protein100 = per.protein, carbs100 = per.carbs, fat100 = per.fat, sugar100 = per.sugar,
            isLiquid = isLiquid, favorite = favorite, searchKey = searchKey(name, brand),
            isPortionFood = perPortion, servingGrams = if (perPortion) null else servingGrams,
        )
        return entity.copy(id = foods.insert(entity))
    }

    /**
     * Nährwerte korrigieren. Bei BLS/Open Food Facts werden beim ersten Mal die Originalwerte gemerkt
     * (für „Auf Original zurücksetzen“) und das Lebensmittel als „angepasst“ markiert.
     * Eigene Lebensmittel werden einfach geändert (inkl. Name).
     */
    suspend fun correct(food: FoodEntity, per: Per100, name: String = food.name): FoodEntity {
        val updated = if (food.source == FoodSource.CUSTOM) {
            food.copy(
                name = name.trim().ifEmpty { food.name }, searchKey = searchKey(name.ifBlank { food.name }, food.brand),
                kcal100 = per.kcal, protein100 = per.protein, carbs100 = per.carbs, fat100 = per.fat, sugar100 = per.sugar,
            )
        } else {
            val withOrig = if (food.isAdjusted) food else food.copy(
                origKcal100 = food.kcal100, origProtein100 = food.protein100, origCarbs100 = food.carbs100,
                origFat100 = food.fat100, origSugar100 = food.sugar100,
            )
            withOrig.copy(
                isAdjusted = true, kcal100 = per.kcal, protein100 = per.protein, carbs100 = per.carbs, fat100 = per.fat,
                sugar100 = per.sugar,
            )
        }
        foods.update(updated)
        return updated
    }

    /** Korrektur rückgängig machen (nur BLS/Open Food Facts). */
    suspend fun resetToOriginal(food: FoodEntity): FoodEntity {
        if (!food.isAdjusted) return food
        val reset = food.copy(
            kcal100 = food.origKcal100 ?: food.kcal100, protein100 = food.origProtein100 ?: food.protein100,
            carbs100 = food.origCarbs100 ?: food.carbs100, fat100 = food.origFat100 ?: food.fat100,
            sugar100 = food.origSugar100, isAdjusted = false,
            origKcal100 = null, origProtein100 = null, origCarbs100 = null, origFat100 = null, origSugar100 = null,
        )
        foods.update(reset)
        return reset
    }

    /**
     * Herz bei einem Schnelleintrag: legt ein Portions-Lebensmittel mit Name + kcal als Favorit an
     * und verknüpft den Eintrag damit (damit das Herz im Eintrag gefüllt bleibt).
     */
    suspend fun favoriteFromQuickEntry(entry: EntryEntity): FoodEntity {
        val entity = FoodEntity(
            name = entry.name.trim(), source = FoodSource.CUSTOM,
            kcal100 = entry.kcal.toDouble(), protein100 = entry.protein, carbs100 = entry.carbs, fat100 = entry.fat,
            sugar100 = entry.sugar, favorite = true, lastUsedAt = System.currentTimeMillis(), useCount = 1,
            searchKey = searchKey(entry.name, null), isPortionFood = true,
        )
        return db.withTransaction {
            val id = foods.insert(entity)
            foods.linkEntry(entry.id, id)
            entity.copy(id = id)
        }
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
