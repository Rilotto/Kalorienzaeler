package de.kalorien.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.SettingsRepository
import de.kalorien.app.core.domain.ChartType
import de.kalorien.app.core.domain.History
import de.kalorien.app.core.domain.HistoryBucket
import de.kalorien.app.core.domain.HistoryBuckets
import de.kalorien.app.core.domain.HistoryGrouping
import de.kalorien.app.core.domain.HistorySummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class HistoryUiState(
    val grouping: HistoryGrouping = HistoryGrouping.DAYS,
    val count: Int = HistoryGrouping.DAYS.defaultCount,
    val chart: ChartType = ChartType.BARS,
    val buckets: List<HistoryBucket> = emptyList(),
    /** Kennzahlen über den ganzen gezeigten Zeitraum (Tage) */
    val summary: HistorySummary? = null,
    val showMacros: Boolean = true,
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val entries: EntryRepository,
    private val settings: SettingsRepository,
    private val openDay: MutableStateFlow<LocalDate?>,
) : ViewModel() {
    private data class View(val grouping: HistoryGrouping, val count: Int, val chart: ChartType)

    private val view = settings.settings.map { View(it.historyGrouping, it.historyCount, it.chartType) }.distinctUntilChanged()

    val state: StateFlow<HistoryUiState> = view.flatMapLatest { v ->
        val today = LocalDate.now()
        val from = HistoryBuckets.firstDay(today, v.grouping, v.count)
        combine(entries.observeTotals(from, today), settings.settings) { totals, s ->
            val days = ChronoUnit.DAYS.between(from, today).toInt() + 1
            HistoryUiState(
                grouping = v.grouping, count = v.count, chart = v.chart,
                buckets = HistoryBuckets.build(today, v.grouping, v.count, totals, s::goalFor),
                summary = History.summarize(today, days, totals, s::goalFor),
                showMacros = s.showMacros, loaded = true,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setGrouping(g: HistoryGrouping) = save(g, g.defaultCount, state.value.chart)
    fun setCount(n: Int) = save(state.value.grouping, n, state.value.chart)
    fun setChart(c: ChartType) = save(state.value.grouping, state.value.count, c)

    private fun save(g: HistoryGrouping, n: Int, c: ChartType) = viewModelScope.launch { settings.setHistoryView(g, n, c) }

    /** Tag in der Heute-Ansicht öffnen (die Navigation macht der Bildschirm). */
    fun open(date: LocalDate) { openDay.value = date }
}
