package de.kalorien.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.data.SettingsRepository
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.Meal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val settings: Settings = Settings(),
    val byMeal: Map<Meal, List<EntryEntity>> = emptyMap(),
    val total: Amount = Amount.ZERO,
    val count: Int = 0,
    val loaded: Boolean = false,
) {
    val isToday: Boolean get() = date == LocalDate.now()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val entries: EntryRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    private val day = date.flatMapLatest { d -> entries.observeDay(d).map { d to it } }

    val state: StateFlow<TodayUiState> = combine(day, settings.settings) { (d, list), s ->
        TodayUiState(
            date = d,
            settings = s,
            byMeal = list.groupBy { Meal.fromDb(it.meal) },
            total = list.fold(Amount.ZERO) { acc, e -> acc + Amount(e.kcal, e.protein, e.carbs, e.fat) },
            count = list.size,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun previousDay() { date.value = date.value.minusDays(1) }

    fun nextDay() {
        if (date.value < LocalDate.now()) date.value = date.value.plusDays(1)
    }

    fun goToToday() { date.value = LocalDate.now() }

    /** Beim Zurückkehren in die App nach Mitternacht wieder auf „heute“ springen. */
    fun refreshToday(wasToday: Boolean) {
        if (wasToday && date.value != LocalDate.now()) date.value = LocalDate.now()
    }

    fun delete(entry: EntryEntity) = viewModelScope.launch { entries.delete(entry) }
    fun restore(entry: EntryEntity) = viewModelScope.launch { entries.restore(entry) }

    fun updateFoodEntry(entry: EntryEntity, grams: Double, meal: Meal) =
        viewModelScope.launch { entries.updateFoodEntry(entry, grams, meal) }

    fun updateQuickEntry(entry: EntryEntity, name: String, kcal: Int, meal: Meal) =
        viewModelScope.launch { entries.updateQuickEntry(entry, name, kcal, meal) }
}
