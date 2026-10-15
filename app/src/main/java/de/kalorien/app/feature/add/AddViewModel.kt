package de.kalorien.app.feature.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.BlsImporter
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.FoodRepository
import de.kalorien.app.core.domain.Meal
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

    fun addFood(food: FoodEntity, grams: Double, meal: Meal, date: LocalDate) =
        viewModelScope.launch { entries.addFood(food, grams, meal, date) }

    fun addQuick(name: String, kcal: Int, meal: Meal, date: LocalDate) =
        viewModelScope.launch { entries.addQuick(name, kcal, meal, date) }

    /** Gibt das aktualisierte Lebensmittel zurück, damit die Anzeige sofort stimmt. */
    fun toggleFavorite(food: FoodEntity): FoodEntity {
        val updated = food.copy(favorite = !food.favorite)
        viewModelScope.launch { foods.setFavorite(food.id, updated.favorite) }
        return updated
    }
}
