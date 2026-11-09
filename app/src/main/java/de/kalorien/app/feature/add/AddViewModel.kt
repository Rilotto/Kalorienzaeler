package de.kalorien.app.feature.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.BlsImporter
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.FoodRepository
import de.kalorien.app.core.data.SettingsRepository
import de.kalorien.app.core.data.TemplateRepository
import de.kalorien.app.core.data.TemplateWithItems
import de.kalorien.app.core.data.DishDraft
import de.kalorien.app.core.data.DishDraftItem
import de.kalorien.app.core.data.DishRepository
import de.kalorien.app.core.data.MensaRepository
import de.kalorien.app.core.data.MensaSlotEntity
import de.kalorien.app.core.data.EntryRepository.Companion.PORTION_UNIT
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.NutritionColumn
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.NutritionBasis
import de.kalorien.app.core.domain.NutritionTextParser
import de.kalorien.app.core.domain.Per100
import de.kalorien.app.core.food.OpenFoodFacts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Vorbelegung für das Formular „Eigenes Lebensmittel“ (aus Suche, Barcode oder Foto). */
data class CustomPrefill(
    val name: String = "",
    val brand: String = "",
    val ean: String? = null,
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val sugar: Double? = null,
    val basis: NutritionBasis = NutritionBasis.PER_100G,
    val servingGrams: Double? = null,
    /** Hinweis oben im Formular, z. B. „Barcode unbekannt“ */
    val note: Note? = null,
) {
    enum class Note { BarcodeUnknown, BarcodeIncomplete, PhotoRead, PhotoNothingFound, Offline }
}

/** Ergebnis eines Barcode-Scans. */
sealed interface BarcodeResult {
    data class Found(val food: FoodEntity) : BarcodeResult
    data class NeedsData(val prefill: CustomPrefill) : BarcodeResult
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
/** Ergebnis des Mensa-Fotos für das Formular (alles änderbar). */
data class MensaPrefill(
    val name: String = "",
    val portion: NutritionColumn? = null,
    val per100: NutritionColumn? = null,
    val portionGrams: Double? = null,
    /** Hat die Texterkennung etwas gefunden? null = von Hand angelegt */
    val found: Boolean? = null,
    val slotName: String? = null,
)

class AddViewModel(
    private val foods: FoodRepository,
    private val entries: EntryRepository,
    bls: BlsImporter,
    private val templateRepo: TemplateRepository,
    private val settings: SettingsRepository,
    private val dishes: DishRepository,
    private val mensa: MensaRepository,
) : ViewModel() {
    // ---------------------------------------------------------------- Mensa (v0.8)
    val mensaSlots = mensa.observeSlots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val mensaDishes = mensa.observeKnownDishes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ensureMensaSlots(names: List<String>) = viewModelScope.launch { mensa.ensureDefaultSlots(names) }
    fun addMensaSlot(name: String) = viewModelScope.launch { mensa.addSlot(name) }
    fun renameMensaSlot(slot: MensaSlotEntity, name: String) = viewModelScope.launch { mensa.renameSlot(slot, name) }
    fun deleteMensaSlot(slot: MensaSlotEntity) = viewModelScope.launch { mensa.deleteSlot(slot) }
    fun restoreMensaSlot(slot: MensaSlotEntity) = viewModelScope.launch { mensa.restoreSlot(slot) }

    /** Bekanntes Gericht für den Tag auf den Platz legen. */
    fun assignMensa(slot: MensaSlotEntity, food: FoodEntity, date: LocalDate) = viewModelScope.launch { mensa.assign(slot, food, date) }

    /** Gericht aus Foto/Formular speichern und (falls ein Platz gewählt war) für den Tag zuordnen. */
    suspend fun saveMensaDish(name: String, perPortion: Per100, portionGrams: Double?, slot: MensaSlotEntity?, date: LocalDate): FoodEntity {
        val food = mensa.saveDish(name, perPortion, portionGrams)
        if (slot != null) mensa.assign(slot, food, date)
        return food
    }

    /** Foto der Mensa-Tabelle auswerten. */
    fun mensaPrefillFromText(text: List<String>, slotName: String?): MensaPrefill {
        val p = NutritionTextParser.parseBest(text)
        return MensaPrefill(
            name = p.name.orEmpty(), portion = p.perPortion(), per100 = p.per100(), portionGrams = p.portionGrams,
            found = p.hasAnyValue || p.columns.isNotEmpty(), slotName = slotName,
        )
    }

    // ---------------------------------------------------------------- Gerichte aus Zutaten (v0.8)
    suspend fun loadDish(food: FoodEntity): DishDraft = dishes.load(food)
    suspend fun saveDish(draft: DishDraft): FoodEntity = dishes.save(draft)
    suspend fun dishIngredients(foodId: Long) = dishes.ingredients(foodId)

    /** Zutat aus Lebensmittel + Menge (Gramm, oder Anzahl Portionen bei Portions-Lebensmitteln). */
    fun ingredientOf(food: FoodEntity, amount: Double, unitLabel: String?, unitCount: Double?): DishDraftItem =
        if (food.isPortionFood) {
            DishDraftItem(food.id, food.name, null, food.isLiquid, Amount.of(food.per100, amount * 100.0), PORTION_UNIT, amount)
        } else {
            DishDraftItem(food.id, food.name, amount, food.isLiquid, Amount.of(food.per100, amount), unitLabel, unitCount)
        }

    val templates = templateRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Reiter, der beim Öffnen zuerst gezeigt wird (Einstellung). */
    suspend fun startTab(): Int = settings.current().startTab

    /** Vorlage eintragen; [onDone] bekommt die neuen IDs für „Rückgängig“. */
    fun applyTemplate(t: TemplateWithItems, meal: Meal, date: LocalDate, onDone: (List<Long>) -> Unit) =
        viewModelScope.launch { onDone(templateRepo.apply(t, meal, date)) }

    val query = MutableStateFlow("")

    val results: StateFlow<List<FoodEntity>> = query
        .debounce(150)
        .mapLatest { foods.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent = foods.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites = foods.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val blsState: StateFlow<BlsImporter.State> = bls.state

    fun setQuery(q: String) { query.value = q }

    fun reset() { query.value = "" }

    fun addFood(
        food: FoodEntity, grams: Double, meal: Meal, date: LocalDate, unitLabel: String?, unitCount: Double?,
        onDone: (EntryEntity) -> Unit,
    ) = viewModelScope.launch { onDone(entries.addFood(food, grams, meal, date, unitLabel, unitCount)) }

    fun undo(entry: EntryEntity) = viewModelScope.launch { entries.delete(entry) }

    /** Nährwerte korrigieren; optional die Einträge von [recalcDate] neu berechnen. */
    suspend fun correctFood(food: FoodEntity, per: Per100, name: String, recalcDate: LocalDate?): FoodEntity {
        val updated = foods.correct(food, per, name)
        recalcDate?.let { entries.recalcForFood(updated, it) }
        return updated
    }

    suspend fun setServing(food: FoodEntity, label: String?, grams: Double?): FoodEntity = foods.setServing(food, label, grams)

    suspend fun resetFood(food: FoodEntity, recalcDate: LocalDate?): FoodEntity {
        val updated = foods.resetToOriginal(food)
        recalcDate?.let { entries.recalcForFood(updated, it) }
        return updated
    }

    fun addPortions(food: FoodEntity, portions: Double, meal: Meal, date: LocalDate, onDone: (EntryEntity) -> Unit) =
        viewModelScope.launch { onDone(entries.addPortions(food, portions, meal, date)) }

    fun addQuick(name: String, kcal: Int, meal: Meal, date: LocalDate, onDone: (EntryEntity) -> Unit) =
        viewModelScope.launch { onDone(entries.addQuick(name, kcal, meal, date)) }

    /** Gibt das aktualisierte Lebensmittel zurück, damit die Anzeige sofort stimmt. */
    fun toggleFavorite(food: FoodEntity): FoodEntity {
        val updated = food.copy(favorite = !food.favorite)
        viewModelScope.launch { foods.setFavorite(food.id, updated.favorite) }
        return updated
    }

    /**
     * Barcode auflösen: erst die eigene Datenbank (offline), dann Open Food Facts.
     * Unbekannt oder unvollständig → Formular zum Ergänzen.
     */
    suspend fun lookupBarcode(ean: String): BarcodeResult {
        val local = foods.findByEan(ean)
        // vor v0.5 gespeicherte Produkte haben keinen Zucker → einmal neu abfragen (ohne Internet: gespeicherte Version)
        if (local != null && local.kcal100 > 0 && !foods.needsSugarRefresh(local)) return BarcodeResult.Found(local)
        val product = try {
            OpenFoodFacts.lookup(ean)
        } catch (e: Exception) {
            return if (local != null) BarcodeResult.Found(local)
            else BarcodeResult.NeedsData(CustomPrefill(ean = ean, note = CustomPrefill.Note.Offline))
        }
        return when {
            // schon gespeichert, Neuabfrage brachte nichts Besseres → gespeicherte Version nehmen
            local != null && local.kcal100 > 0 && (product == null || !product.isComplete) -> BarcodeResult.Found(local)
            product == null -> BarcodeResult.NeedsData(CustomPrefill(ean = ean, note = CustomPrefill.Note.BarcodeUnknown))
            product.isComplete -> BarcodeResult.Found(foods.saveFromOff(product).also { foods.fillMissingSugar() })
            else -> BarcodeResult.NeedsData(
                CustomPrefill(
                    name = product.name.orEmpty(), brand = product.brand.orEmpty(), ean = ean,
                    kcal = product.kcal100, protein = product.protein100, carbs = product.carbs100, fat = product.fat100,
                    sugar = product.sugar100,
                    basis = if (product.isLiquid) NutritionBasis.PER_100ML else NutritionBasis.PER_100G,
                    servingGrams = product.servingGrams, note = CustomPrefill.Note.BarcodeIncomplete,
                ),
            )
        }
    }

    /** Erkannten Text (Foto) in ein Formular übersetzen; vorhandene Eingaben (z. B. Barcode) bleiben. */
    fun prefillFromText(text: List<String>, base: CustomPrefill?): CustomPrefill {
        val p = NutritionTextParser.parseBest(text)
        val b = base ?: CustomPrefill()
        // Tabelle mit zwei Spalten: für eigene Lebensmittel die 100-g-Spalte nehmen (dann geht auch Gramm), Portion als Portionsgröße
        p.per100()?.takeIf { p.columns.size == 2 }?.let { col ->
            return b.copy(
                name = b.name.ifBlank { p.name.orEmpty() },
                kcal = col.kcal ?: b.kcal, protein = col.protein ?: b.protein, carbs = col.carbs ?: b.carbs, fat = col.fat ?: b.fat,
                sugar = col.sugar ?: b.sugar, basis = col.basis, servingGrams = p.portionGrams ?: b.servingGrams,
                note = CustomPrefill.Note.PhotoRead,
            )
        }
        return b.copy(
            name = b.name.ifBlank { p.name.orEmpty() },
            kcal = p.kcal ?: b.kcal,
            protein = p.protein ?: b.protein,
            carbs = p.carbs ?: b.carbs,
            fat = p.fat ?: b.fat,
            sugar = p.sugar ?: b.sugar,
            basis = when (p.basis) {
                NutritionBasis.UNKNOWN -> if (b.ean != null) b.basis else NutritionBasis.PER_PORTION // Kantine: meist pro Portion
                else -> p.basis
            },
            note = if (p.hasAnyValue) CustomPrefill.Note.PhotoRead else CustomPrefill.Note.PhotoNothingFound,
        )
    }

    suspend fun saveCustom(
        name: String, brand: String, basis: NutritionBasis, per: Per100, servingGrams: Double?, ean: String?, favorite: Boolean,
    ): FoodEntity = foods.createCustom(
        name = name, brand = brand, per = per, isLiquid = basis == NutritionBasis.PER_100ML,
        perPortion = basis == NutritionBasis.PER_PORTION, servingGrams = servingGrams, ean = ean, favorite = favorite,
    )
}
