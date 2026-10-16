package de.kalorien.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.SettingsRepository
import de.kalorien.app.core.domain.History
import de.kalorien.app.core.domain.HistorySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryUiState(val range: Int = 7, val summary: HistorySummary? = null, val showMacros: Boolean = true)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val entries: EntryRepository,
    settings: SettingsRepository,
    private val openDay: MutableStateFlow<LocalDate?>,
) : ViewModel() {
    private val range = MutableStateFlow(7)

    val state: StateFlow<HistoryUiState> = range.flatMapLatest { days ->
        val today = LocalDate.now()
        combine(entries.observeTotals(today.minusDays(days - 1L), today), settings.settings) { totals, s ->
            HistoryUiState(days, History.summarize(today, days, totals, s::goalFor), s.showMacros)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(days: Int) { range.value = days }

    /** Tag in der Heute-Ansicht öffnen (die Navigation macht der Bildschirm). */
    fun open(date: LocalDate) { openDay.value = date }
}
