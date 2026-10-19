package de.kalorien.app.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.R
import de.kalorien.app.core.domain.History
import de.kalorien.app.core.domain.HistoryDay
import de.kalorien.app.core.domain.HistorySummary
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.AppViewModels
import de.kalorien.app.core.ui.theme.Tokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Verlauf: letzte 7/14/30 Tage als Balken mit Ziellinie, Durchschnitt und Tagesliste. Tippen öffnet den Tag. */
@Composable
fun HistoryScreen(onOpenDay: () -> Unit, vm: HistoryViewModel = viewModel(factory = AppViewModels.Factory)) {
    val c = Tokens.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember(state.range) { mutableStateOf<LocalDate?>(null) }
    fun open(d: LocalDate) { vm.open(d); onOpenDay() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge, color = c.fg,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            History.ranges.forEach { r -> AppChip(stringResource(R.string.history_days, r), state.range == r) { vm.setRange(r) } }
        }
        val s = state.summary ?: return@Column
        AppCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(stringResource(R.string.history_avg), s.average?.let { stringResource(R.string.kcal_value, Nutrition.format(it)) } ?: "–", Modifier.weight(1f))
                Stat(stringResource(R.string.history_avg_goal), stringResource(R.string.kcal_value, Nutrition.format(s.averageGoal)), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(stringResource(R.string.history_logged), stringResource(R.string.history_of, s.loggedDays, s.days.size), Modifier.weight(1f))
                Stat(stringResource(R.string.history_over), s.overDays.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            BarChart(s, selected) { selected = if (selected == it) null else it }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(c.accent); Text(stringResource(R.string.history_legend_ok), style = MaterialTheme.typography.labelSmall, color = c.muted)
                Spacer(Modifier.width(10.dp))
                LegendDot(c.over); Text(stringResource(R.string.history_legend_over), style = MaterialTheme.typography.labelSmall, color = c.muted)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.history_legend_goal), style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
            s.days.firstOrNull { it.total.date == selected }?.let { d ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = c.line)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(longDate(d.total.date), style = MaterialTheme.typography.bodyMedium, color = c.fg, fontWeight = FontWeight.SemiBold)
                        Text(dayLine(d), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    TextButton(onClick = { open(d.total.date) }) { Text(stringResource(R.string.history_open), color = c.accent) }
                }
            }
        }
        AppCard(padding = false) {
            s.days.asReversed().forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider(color = c.line)
                DayRow(d, state.showMacros) { open(d.total.date) }
            }
        }
        Text(stringResource(R.string.history_note), style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val c = Tokens.colors
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.fg)
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color) {
    Spacer(Modifier.size(8.dp).background(color, CircleShape))
    Spacer(Modifier.width(4.dp))
}

/** Balken je Tag; gestrichelte Linie = Ziel des Tages. Antippen wählt den Tag. */
@Composable
private fun BarChart(s: HistorySummary, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val c = Tokens.colors
    val max = (s.maxKcal * 1.1f).coerceAtLeast(1f)
    val n = s.days.size
    Canvas(
        Modifier.fillMaxWidth().height(160.dp).pointerInput(s.days) {
            detectTapGestures { pos ->
                val i = (pos.x / (size.width / n.toFloat())).toInt().coerceIn(0, n - 1)
                if (s.days[i].logged) onSelect(s.days[i].total.date)
            }
        },
    ) {
        val slot = size.width / n
        val bar = slot * if (n <= 7) 0.55f else 0.7f
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
        s.days.forEachIndexed { i, d ->
            val x = i * slot + (slot - bar) / 2
            val h = size.height * (d.total.kcal / max)
            val isSel = d.total.date == selected
            val color = when {
                !d.logged -> c.line
                d.over -> c.over
                else -> c.accent
            }
            if (d.logged) {
                drawRoundRect(
                    color = if (selected == null || isSel) color else color.copy(alpha = 0.45f),
                    topLeft = Offset(x, size.height - h), size = Size(bar, h), cornerRadius = CornerRadius(4f, 4f),
                )
            } else {
                drawRoundRect(color, Offset(x, size.height - 3f), Size(bar, 3f))
            }
            val gy = size.height * (1f - d.goal / max)
            drawLine(c.muted, Offset(i * slot + 1f, gy), Offset((i + 1) * slot - 1f, gy), strokeWidth = 2f, pathEffect = dash)
        }
    }
    // Beschriftung: Wochentag (7 Tage) bzw. Tag des Monats an jedem n-ten Balken
    val step = when { n <= 7 -> 1; n <= 14 -> 2; else -> 5 }
    Row(Modifier.fillMaxWidth()) {
        s.days.forEachIndexed { i, d ->
            val show = (n - 1 - i) % step == 0
            Text(
                if (!show) "" else if (n <= 7) d.total.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMANY).take(2)
                else d.total.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DayRow(d: HistoryDay, showMacros: Boolean, onClick: () -> Unit) {
    val c = Tokens.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(longDate(d.total.date), style = MaterialTheme.typography.bodyLarge, color = c.fg)
            if (d.logged && showMacros) {
                Text(
                    stringResource(R.string.macros_line, d.total.protein.roundToInt(), d.total.carbs.roundToInt(), d.total.fat.roundToInt()),
                    style = MaterialTheme.typography.bodySmall, color = c.muted,
                )
            } else if (!d.logged) {
                Text(stringResource(R.string.history_no_entries), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }
        if (d.logged) {
            Text(
                Nutrition.format(d.total.kcal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (d.over) c.over else c.fg,
            )
            Text(" / " + Nutrition.format(d.goal), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

@Composable
private fun dayLine(d: HistoryDay): String =
    stringResource(R.string.history_day_line, Nutrition.format(d.total.kcal), Nutrition.format(d.goal), d.total.entries)

private val LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY)

@Composable
private fun longDate(d: LocalDate): String = when (d) {
    LocalDate.now() -> stringResource(R.string.today_title)
    LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday)
    else -> d.format(LONG)
}
