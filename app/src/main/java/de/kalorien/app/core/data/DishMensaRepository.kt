package de.kalorien.app.core.data

import androidx.room.withTransaction
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.DishCalc
import de.kalorien.app.core.domain.DishPart
import de.kalorien.app.core.domain.Per100
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Zutat im Bearbeiten-Fenster eines Gerichts (noch nicht gespeichert). */
data class DishDraftItem(
    val foodId: Long?,
    val name: String,
    val grams: Double?,
    val isLiquid: Boolean,
    val amount: Amount,
    val unitLabel: String? = null,
    val unitCount: Double? = null,
) {
    val part: DishPart get() = DishPart(amount, grams)
}

/** Ein Gericht mit Zutaten, wie es im Bearbeiten-Fenster gebraucht wird. */
data class DishDraft(
    /** null = neues Gericht */
    val foodId: Long? = null,
    val name: String = "",
    /** z. B. „1 Sandwich“ – wird beim Eintragen als Einheit gezeigt */
    val portionLabel: String = "",
    val servings: Double = 1.0,
    val items: List<DishDraftItem> = emptyList(),
) {
    val perPortion: Per100? get() = if (items.isEmpty() || servings <= 0) null else DishCalc.perPortion(items.map { it.part }, servings)
    val gramsPerPortion: Double? get() = if (servings <= 0) null else DishCalc.gramsPerPortion(items.map { it.part }, servings)
}

/** Gerichte aus Zutaten (Sandwich & Co.). Das Gericht selbst ist ein Portions-Lebensmittel (Quelle DISH). */
class DishRepository(private val db: KalorienDatabase) {
    private val dishes = db.dishDao()
    private val foods = db.foodDao()

    fun observeDishes(): Flow<List<FoodEntity>> = db.mensaDao().observeDishes()

    /** Gericht zum Bearbeiten laden. */
    suspend fun load(food: FoodEntity): DishDraft {
        val dish = dishes.get(food.id)
        val items = dishes.ingredients(food.id).map {
            DishDraftItem(it.foodId, it.name, it.grams, it.isLiquid, Amount(it.kcal, it.protein, it.carbs, it.fat, it.sugar), it.unitLabel, it.unitCount)
        }
        return DishDraft(food.id, food.name, food.servingLabel.orEmpty(), dish?.servings ?: 1.0, items)
    }

    /**
     * Speichern: legt das Portions-Lebensmittel an oder ändert es, und ersetzt die Zutaten.
     * Alte Einträge behalten ihre Werte (Snapshot).
     */
    suspend fun save(d: DishDraft): FoodEntity = db.withTransaction {
        val per = d.perPortion ?: Per100(0.0, 0.0, 0.0, 0.0)
        val label = d.portionLabel.trim().ifEmpty { null }
        val existing = d.foodId?.let { foods.get(it) }
        val food = if (existing != null) {
            val f = existing.copy(
                name = d.name.trim().ifEmpty { existing.name }, searchKey = FoodRepository.searchKey(d.name, null),
                kcal100 = per.kcal, protein100 = per.protein, carbs100 = per.carbs, fat100 = per.fat, sugar100 = per.sugar,
                servingLabel = label, servingGrams = d.gramsPerPortion, isPortionFood = true,
            )
            foods.update(f); f
        } else {
            val f = FoodEntity(
                name = d.name.trim(), source = FoodSource.DISH, kcal100 = per.kcal, protein100 = per.protein, carbs100 = per.carbs,
                fat100 = per.fat, sugar100 = per.sugar, searchKey = FoodRepository.searchKey(d.name, null), isPortionFood = true,
                servingLabel = label, servingGrams = d.gramsPerPortion, favorite = true,
            )
            f.copy(id = foods.insert(f))
        }
        dishes.upsert(DishEntity(food.id, d.servings))
        dishes.clearIngredients(food.id)
        dishes.insertIngredients(
            d.items.mapIndexed { i, it ->
                DishIngredientEntity(
                    dishFoodId = food.id, position = i, foodId = it.foodId, name = it.name, grams = it.grams, isLiquid = it.isLiquid,
                    kcal = it.amount.kcal, protein = it.amount.protein, carbs = it.amount.carbs, fat = it.amount.fat,
                    sugar = it.amount.sugar, unitLabel = it.unitLabel, unitCount = it.unitCount,
                )
            },
        )
        food
    }

    suspend fun ingredients(foodId: Long) = dishes.ingredients(foodId)
}

/** Mensa: feste Plätze („Menü 1“), die jeden Tag mit einem Gericht (Quelle MENSA) belegt werden. */
class MensaRepository(private val db: KalorienDatabase) {
    private val mensa = db.mensaDao()
    private val foods = db.foodDao()

    fun observeSlots(): Flow<List<MensaSlotEntity>> = mensa.observeSlots()
    fun observeKnownDishes(): Flow<List<FoodEntity>> = mensa.observeMensaFoods()

    /** Beim ersten Öffnen drei übliche Plätze anlegen. */
    suspend fun ensureDefaultSlots(names: List<String>) {
        if (mensa.slots().isEmpty()) mensa.insertAll(names.mapIndexed { i, n -> MensaSlotEntity(name = n, position = i) })
    }

    suspend fun addSlot(name: String) {
        val pos = (mensa.slots().maxOfOrNull { it.position } ?: -1) + 1
        mensa.insert(MensaSlotEntity(name = name.trim(), position = pos))
    }

    suspend fun renameSlot(slot: MensaSlotEntity, name: String) = mensa.update(slot.copy(name = name.trim().ifEmpty { slot.name }))
    suspend fun deleteSlot(slot: MensaSlotEntity) = mensa.delete(slot.id)
    suspend fun restoreSlot(slot: MensaSlotEntity) { mensa.insert(slot.copy(id = 0)) }

    /** Gericht für [date] auf den Platz legen. */
    suspend fun assign(slot: MensaSlotEntity, food: FoodEntity, date: LocalDate) =
        mensa.update(slot.copy(foodId = food.id, date = date.toString()))

    /**
     * Gericht aus dem Foto (oder von Hand) speichern: Gibt es schon ein Mensa-Gericht mit demselben Namen,
     * werden dessen Werte aktualisiert (die Mensa ändert Rezepte manchmal), sonst wird es neu angelegt.
     */
    suspend fun saveDish(name: String, perPortion: Per100, portionGrams: Double?): FoodEntity = db.withTransaction {
        val key = FoodRepository.searchKey(name, null)
        val existing = mensa.findMensaFood(key)
        if (existing != null) {
            val f = existing.copy(
                name = name.trim(), kcal100 = perPortion.kcal, protein100 = perPortion.protein, carbs100 = perPortion.carbs,
                fat100 = perPortion.fat, sugar100 = perPortion.sugar, servingGrams = portionGrams ?: existing.servingGrams,
            )
            foods.update(f); f
        } else {
            val f = FoodEntity(
                name = name.trim(), source = FoodSource.MENSA, kcal100 = perPortion.kcal, protein100 = perPortion.protein,
                carbs100 = perPortion.carbs, fat100 = perPortion.fat, sugar100 = perPortion.sugar, searchKey = key,
                isPortionFood = true, servingGrams = portionGrams,
            )
            f.copy(id = foods.insert(f))
        }
    }
}
