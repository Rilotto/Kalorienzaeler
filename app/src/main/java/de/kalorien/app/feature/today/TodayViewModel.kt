package de.kalorien.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.FoodRepository
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.data.SettingsRepository
import de.kalorien.app.core.data.TemplateRepository
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.MacroTargets
import de.kalorien.app.core.domain.Meal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
    /** Einträge ohne Zuckerangabe (z. B. vor v0.5 oder Schnelleinträge) */
    val withoutSugar: Int = 0,
    /** Mahlzeiten, die am Vortag Einträge hatten (für „Wie gestern“) */
    val yesterdayMeals: Set<Meal> = emptySet(),
    val loaded: Boolean = false,
) {
    val isToday: Boolean get() = date == LocalDate.now()
    /** Ziel für den angezeigten Tag (Wochentag-Ziel oder Standard) */
    val goal: Int get() = settings.goalFor(date)
    val targets: MacroTargets
        get() = MacroTargets.resolve(goal, settings.proteinGoal, settings.carbsGoal, settings.fatGoal, settings.sugarLimit)
}

/** Ergebnis des Bearbeiten-Fensters. */
data class EntryEdit(
    val kind: Kind,
    val meal: Meal,
    val date: LocalDate,
    val grams: Double? = null,
    val unitLabel: String? = null,
    val unitCount: Double? = null,
    val name: String = "",
    val kcal: Int = 0,
    /** Von Hand überschriebene Werte (nur dieser Eintrag), null = berechnen */
    val override: Amount? = null,
) {
    enum class Kind { Grams, Portions, Quick }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val entries: EntryRepository,
    private val foods: FoodRepository,
    settings: SettingsRepository,
    private val templates: TemplateRepository,
    private val openDay: MutableStateFlow<LocalDate?>,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    init {
        // „Tag öffnen“ aus dem Verlauf
        viewModelScope.launch {
            openDay.filterNotNull().collect { d ->
                date.value = if (d > LocalDate.now()) LocalDate.now() else d
                openDay.value = null
            }
        }
    }

    /** Mahlzeit des angezeigten Tages als Vorlage speichern. */
    fun saveTemplate(name: String, meal: Meal, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        onDone(templates.saveFromMeal(name, meal, date.value) != null)
    }

    private val day = date.flatMapLatest { d ->
        combine(entries.observeDay(d), entries.observeDay(d.minusDays(1))) { list, yesterday -> Triple(d, list, yesterday) }
    }

    val state: StateFlow<TodayUiState> = combine(day, settings.settings) { (d, list, yesterday), s ->
        TodayUiState(
            date = d,
            settings = s,
            byMeal = list.groupBy { Meal.fromDb(it.meal) },
            total = list.fold(Amount.ZERO) { acc, e -> acc + Amount(e.kcal, e.protein, e.carbs, e.fat, e.sugar) },
            count = list.size,
            withoutSugar = list.count { it.sugar == null },
            yesterdayMeals = yesterday.map { Meal.fromDb(it.meal) }.toSet(),
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

    fun updateFoodEntry(entry: EntryEntity, grams: Double, meal: Meal, date: LocalDate, unitLabel: String?, unitCount: Double?) =
        viewModelScope.launch { entries.updateFoodEntry(entry, grams, meal, date, unitLabel, unitCount) }

    fun updatePortionEntry(entry: EntryEntity, portions: Double, meal: Meal, date: LocalDate) =
        viewModelScope.launch { entries.updatePortionEntry(entry, portions, meal, date) }

    fun updateQuickEntry(entry: EntryEntity, name: String, kcal: Int, meal: Meal, date: LocalDate) =
        viewModelScope.launch { entries.updateQuickEntry(entry, name, kcal, meal, date) }

    fun overrideValues(entry: EntryEntity, kcal: Int, protein: Double, carbs: Double, fat: Double, sugar: Double?) =
        viewModelScope.launch { entries.overrideValues(entry, kcal, protein, carbs, fat, sugar) }

    /** Alles aus dem Bearbeiten-Fenster in einem Rutsch speichern (Reihenfolge wichtig: erst Menge, dann Handwerte). */
    fun save(entry: EntryEntity, edit: EntryEdit) = viewModelScope.launch {
        when (edit.kind) {
            EntryEdit.Kind.Grams -> entries.updateFoodEntry(entry, edit.grams ?: entry.grams ?: 0.0, edit.meal, edit.date, edit.unitLabel, edit.unitCount)
            EntryEdit.Kind.Portions -> entries.updatePortionEntry(entry, edit.unitCount ?: 1.0, edit.meal, edit.date)
            EntryEdit.Kind.Quick -> entries.updateQuickEntry(entry, edit.name, edit.kcal, edit.meal, edit.date)
        }
        edit.override?.let { o ->
            entries.get(entry.id)?.let { fresh -> entries.overrideValues(fresh, o.kcal, o.protein, o.carbs, o.fat, o.sugar) }
        }
    }

    /** „Nochmal“: gleiche Menge heute erneut eintragen. Rückgabe für „Rückgängig“. */
    fun duplicateToToday(entry: EntryEntity, onDone: (EntryEntity) -> Unit) =
        viewModelScope.launch { onDone(entries.duplicate(entry, LocalDate.now())) }

    /** „Wie gestern“: Mahlzeit vom Vortag des angezeigten Tages übernehmen. */
    fun copyFromYesterday(meal: Meal, onDone: (List<Long>) -> Unit) = viewModelScope.launch {
        val d = date.value
        onDone(entries.copyMeal(meal, d.minusDays(1), d))
    }

    fun undoCopy(ids: List<Long>) = viewModelScope.launch { entries.deleteIds(ids) }

    /** Ist das Lebensmittel hinter dem Eintrag ein Favorit? (Schnelleintrag ohne Lebensmittel: false) */
    suspend fun isFavorite(entry: EntryEntity): Boolean =
        (entries.get(entry.id) ?: entry).foodId?.let { foods.get(it) }?.favorite ?: false

    /** Gramm einer Einheit (z. B. „Scheibe“) für das Bearbeiten-Fenster: aus dem Eintrag selbst abgeleitet. */
    suspend fun foodOf(entry: EntryEntity) = entry.foodId?.let { foods.get(it) }

    /**
     * Herz im Eintrag umschalten. Bei Schnelleinträgen wird beim ersten Mal ein eigenes
     * Portions-Lebensmittel (Name + kcal) angelegt. Gibt den neuen Zustand zurück.
     */
    suspend fun toggleFavorite(entry: EntryEntity, current: Boolean): Boolean {
        // frisch aus der Datenbank: nach dem ersten Herz ist der Schnelleintrag schon verknüpft
        val fresh = entries.get(entry.id) ?: entry
        val food = fresh.foodId?.let { foods.get(it) }
        return if (food == null) {
            foods.favoriteFromQuickEntry(fresh)
            true
        } else {
            foods.setFavorite(food.id, !current)
            !current
        }
    }
}
