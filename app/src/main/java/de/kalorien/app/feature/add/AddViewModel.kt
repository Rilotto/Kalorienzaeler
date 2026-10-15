package de.kalorien.app.feature.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.BlsImporter
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.FoodRepository
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
class AddViewModel(
    private val foods: FoodRepository,
    private val entries: EntryRepository,
    bls: BlsImporter,
) : ViewModel() {
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

    fun addFood(food: FoodEntity, grams: Double, meal: Meal, date: LocalDate, onDone: (EntryEntity) -> Unit) =
        viewModelScope.launch { onDone(entries.addFood(food, grams, meal, date)) }

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
        if (local != null && local.kcal100 > 0) return BarcodeResult.Found(local)
        val product = try {
            OpenFoodFacts.lookup(ean)
        } catch (e: Exception) {
            return if (local != null) BarcodeResult.Found(local)
            else BarcodeResult.NeedsData(CustomPrefill(ean = ean, note = CustomPrefill.Note.Offline))
        }
        return when {
            product == null -> BarcodeResult.NeedsData(CustomPrefill(ean = ean, note = CustomPrefill.Note.BarcodeUnknown))
            product.isComplete -> BarcodeResult.Found(foods.saveFromOff(product))
            else -> BarcodeResult.NeedsData(
                CustomPrefill(
                    name = product.name.orEmpty(), brand = product.brand.orEmpty(), ean = ean,
                    kcal = product.kcal100, protein = product.protein100, carbs = product.carbs100, fat = product.fat100,
                    basis = if (product.isLiquid) NutritionBasis.PER_100ML else NutritionBasis.PER_100G,
                    servingGrams = product.servingGrams, note = CustomPrefill.Note.BarcodeIncomplete,
                ),
            )
        }
    }

    /** Erkannten Text (Foto) in ein Formular übersetzen; vorhandene Eingaben (z. B. Barcode) bleiben. */
    fun prefillFromText(text: String, base: CustomPrefill?): CustomPrefill {
        val p = NutritionTextParser.parse(text)
        val b = base ?: CustomPrefill()
        return b.copy(
            name = b.name.ifBlank { p.name.orEmpty() },
            kcal = p.kcal ?: b.kcal,
            protein = p.protein ?: b.protein,
            carbs = p.carbs ?: b.carbs,
            fat = p.fat ?: b.fat,
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
