package de.kalorien.app.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.R
import de.kalorien.app.core.domain.ChartType
import de.kalorien.app.core.domain.HistoryBucket
import de.kalorien.app.core.domain.HistoryGrouping
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

/**
 * Verlauf: Tage, Wochen oder Monate als Balken, Linie oder Fläche mit Ziellinie.
 * Wochen/Monate zeigen den Durchschnitt pro Tag mit Einträgen. Tippen wählt einen Abschnitt; Tage lassen sich öffnen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(onOpenDay: () -> Unit, vm: HistoryViewModel = viewModel(factory = AppViewModels.Factory)) {
    val c = Tokens.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember(state.grouping, state.count) { mutableStateOf<LocalDate?>(null) }
    fun open(d: LocalDate) { vm.open(d); onOpenDay() }
    val isDays = state.grouping == HistoryGrouping.DAYS

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge, color = c.fg,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        // Ansicht: Tage / Wochen / Monate, darunter der Zeitraum
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryGrouping.entries.forEach { g -> AppChip(stringResource(groupingName(g)), state.grouping == g) { vm.setGrouping(g) } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.grouping.counts.forEach { n -> AppChip(stringResource(countLabel(state.grouping), n), state.count == n) { vm.setCount(n) } }
        }
        if (!state.loaded) return@Column
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
            Spacer(Modifier.height(12.dp))
            // Diagrammart
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartType.entries.forEach { t -> AppChip(stringResource(chartName(t)), state.chart == t) { vm.setChart(t) } }
            }
            Spacer(Modifier.height(10.dp))
            Chart(state.buckets, state.chart, selected) { selected = if (selected == it) null else it }
            AxisLabels(state.buckets, state.grouping)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(c.accent); Text(stringResource(R.string.history_legend_ok), style = MaterialTheme.typography.labelSmall, color = c.muted)
                Spacer(Modifier.width(10.dp))
                LegendDot(c.over); Text(stringResource(R.string.history_legend_over), style = MaterialTheme.typography.labelSmall, color = c.muted)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.history_legend_goal), style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
            state.buckets.firstOrNull { it.start == selected }?.let { b ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = c.line)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(bucketTitle(b, state.grouping), style = MaterialTheme.typography.bodyMedium, color = c.fg, fontWeight = FontWeight.SemiBold)
                        Text(bucketLine(b, state.grouping), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    if (isDays) TextButton(onClick = { open(b.start) }) { Text(stringResource(R.string.history_open), color = c.accent) }
                }
            }
        }
        AppCard(padding = false) {
            state.buckets.asReversed().forEachIndexed { i, b ->
                if (i > 0) HorizontalDivider(color = c.line)
                BucketRow(b, state.grouping, state.showMacros) {
                    if (isDays) open(b.start) else selected = b.start
                }
            }
        }
        Text(
            stringResource(if (isDays) R.string.history_note else R.string.history_note_grouped),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
    }
}

private fun groupingName(g: HistoryGrouping) = when (g) {
    HistoryGrouping.DAYS -> R.string.history_by_day
    HistoryGrouping.WEEKS -> R.string.history_by_week
    HistoryGrouping.MONTHS -> R.string.history_by_month
}

private fun countLabel(g: HistoryGrouping) = when (g) {
    HistoryGrouping.DAYS -> R.string.history_days
    HistoryGrouping.WEEKS -> R.string.history_weeks
    HistoryGrouping.MONTHS -> R.string.history_months
}

private fun chartName(t: ChartType) = when (t) {
    ChartType.BARS -> R.string.chart_bars
    ChartType.LINE -> R.string.chart_line
    ChartType.AREA -> R.string.chart_area
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
private fun LegendDot(color: Color) {
    Spacer(Modifier.size(8.dp).background(color, CircleShape))
    Spacer(Modifier.width(4.dp))
}

/** Diagramm: je Abschnitt ein Wert; gestrichelt = Ziel. Antippen wählt den Abschnitt. */
@Composable
private fun Chart(buckets: List<HistoryBucket>, type: ChartType, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val c = Tokens.colors
    val n = buckets.size
    if (n == 0) return
    val max = (buckets.maxOf { maxOf(it.kcal ?: 0, it.goal) } * 1.1f).coerceAtLeast(1f)
    Canvas(
        Modifier.fillMaxWidth().height(170.dp).pointerInput(buckets) {
            detectTapGestures { pos ->
                val i = (pos.x / (size.width / n.toFloat())).toInt().coerceIn(0, n - 1)
                if (buckets[i].logged) onSelect(buckets[i].start)
            }
        },
    ) {
        val slot = size.width / n
        fun y(v: Int) = size.height * (1f - v / max)
        fun cx(i: Int) = i * slot + slot / 2
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
        fun colorOf(b: HistoryBucket) = if (b.over) c.over else c.accent
        fun dim(b: HistoryBucket, col: Color) = if (selected == null || b.start == selected) col else col.copy(alpha = 0.45f)

        when (type) {
            ChartType.BARS -> {
                val bar = slot * if (n <= 8) 0.55f else 0.7f
                buckets.forEachIndexed { i, b ->
                    val x = i * slot + (slot - bar) / 2
                    val k = b.kcal
                    if (k != null) {
                        val h = size.height - y(k)
                        drawRoundRect(dim(b, colorOf(b)), Offset(x, size.height - h), Size(bar, h), CornerRadius(4f, 4f))
                    } else {
                        drawRoundRect(c.line, Offset(x, size.height - 3f), Size(bar, 3f))
                    }
                }
            }
            ChartType.LINE, ChartType.AREA -> {
                // Linie durch alle Abschnitte mit Einträgen; Lücken unterbrechen die Linie
                val segments = mutableListOf<MutableList<Pair<Int, Int>>>()
                buckets.forEachIndexed { i, b ->
                    val k = b.kcal
                    if (k == null) segments.add(mutableListOf()) else {
                        if (segments.isEmpty()) segments.add(mutableListOf())
                        segments.last().add(i to k)
                    }
                }
                segments.filter { it.isNotEmpty() }.forEach { seg ->
                    val line = Path().apply {
                        seg.forEachIndexed { j, (i, k) -> if (j == 0) moveTo(cx(i), y(k)) else lineTo(cx(i), y(k)) }
                    }
                    if (type == ChartType.AREA && seg.size > 1) {
                        val area = Path().apply {
                            addPath(line)
                            lineTo(cx(seg.last().first), size.height)
                            lineTo(cx(seg.first().first), size.height)
                            close()
                        }
                        drawPath(area, c.accent.copy(alpha = 0.22f))
                    }
                    drawPath(line, c.accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
                buckets.forEachIndexed { i, b ->
                    val k = b.kcal ?: return@forEachIndexed
                    val r = if (b.start == selected) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(c.surface, r + 2f, Offset(cx(i), y(k)))
                    drawCircle(dim(b, colorOf(b)), r, Offset(cx(i), y(k)))
                }
            }
        }
        // Ziel je Abschnitt (kann sich durch Wochentag-Ziele unterscheiden)
        buckets.forEachIndexed { i, b ->
            val gy = y(b.goal)
            drawLine(c.muted, Offset(i * slot + 1f, gy), Offset((i + 1) * slot - 1f, gy), strokeWidth = 2f, pathEffect = dash)
        }
    }
}

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.", Locale.GERMANY)

/** Beschriftung unter dem Diagramm; bei vielen Abschnitten nur jede zweite/fünfte. */
@Composable
private fun AxisLabels(buckets: List<HistoryBucket>, grouping: HistoryGrouping) {
    val c = Tokens.colors
    val n = buckets.size
    val step = when (grouping) {
        HistoryGrouping.DAYS -> when { n <= 7 -> 1; n <= 14 -> 2; else -> 5 }
        HistoryGrouping.WEEKS -> if (n <= 4) 1 else 2
        HistoryGrouping.MONTHS -> if (n <= 6) 1 else 2
    }
    Row(Modifier.fillMaxWidth()) {
        buckets.forEachIndexed { i, b ->
            val show = (n - 1 - i) % step == 0
            val text = when {
                !show -> ""
                grouping == HistoryGrouping.DAYS && n <= 7 -> b.start.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMANY).take(2)
                grouping == HistoryGrouping.DAYS -> b.start.dayOfMonth.toString()
                grouping == HistoryGrouping.WEEKS -> b.start.format(DAY_MONTH)
                else -> b.start.month.getDisplayName(TextStyle.SHORT, Locale.GERMANY).removeSuffix(".")
            }
            Text(
                text, style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BucketRow(b: HistoryBucket, grouping: HistoryGrouping, showMacros: Boolean, onClick: () -> Unit) {
    val c = Tokens.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(bucketTitle(b, grouping), style = MaterialTheme.typography.bodyLarge, color = c.fg)
            val sub = when {
                !b.logged -> stringResource(R.string.history_no_entries)
                grouping != HistoryGrouping.DAYS -> stringResource(R.string.history_logged_days, b.loggedDays, b.days)
                showMacros -> stringResource(R.string.macros_line, b.protein.roundToInt(), b.carbs.roundToInt(), b.fat.roundToInt())
                else -> null
            }
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        b.kcal?.let { k ->
            Text(
                (if (grouping == HistoryGrouping.DAYS) "" else "Ø ") + Nutrition.format(k),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (b.over) c.over else c.fg,
            )
            Text(" / " + Nutrition.format(b.goal), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

private val LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY)
private val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMANY)
private val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY)

@Composable
private fun bucketTitle(b: HistoryBucket, grouping: HistoryGrouping): String = when (grouping) {
    HistoryGrouping.DAYS -> when (b.start) {
        LocalDate.now() -> stringResource(R.string.today_title)
        LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday)
        else -> b.start.format(LONG)
    }
    HistoryGrouping.WEEKS -> stringResource(R.string.history_week_title, b.start.format(SHORT), b.start.plusDays(6).format(SHORT))
    HistoryGrouping.MONTHS -> b.start.format(MONTH)
}

@Composable
private fun bucketLine(b: HistoryBucket, grouping: HistoryGrouping): String {
    val k = b.kcal ?: 0
    return if (grouping == HistoryGrouping.DAYS) {
        stringResource(R.string.history_day_short, Nutrition.format(k), Nutrition.format(b.goal))
    } else {
        stringResource(R.string.history_bucket_line, Nutrition.format(k), Nutrition.format(b.goal), b.loggedDays, b.days)
    }
}
