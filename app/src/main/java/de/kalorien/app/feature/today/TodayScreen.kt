package de.kalorien.app.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.R
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.domain.MacroTargets
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.AppViewModels
import de.kalorien.app.core.ui.amountText
import de.kalorien.app.core.ui.label
import de.kalorien.app.core.ui.theme.Tokens
import de.kalorien.app.feature.add.AddSheet
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TodayScreen(snackbar: SnackbarHostState, vm: TodayViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Tokens.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var addFor by remember { mutableStateOf<Meal?>(null) }
    var editing by remember { mutableStateOf<EntryEntity?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday(state.isToday) }

    fun deleteWithUndo(entry: EntryEntity) {
        vm.delete(entry)
        scope.launch {
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.entry_deleted, entry.name),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restore(entry)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DayHeader(state.date, state.isToday, vm::previousDay, vm::nextDay, vm::goToToday)
            Summary(state)
            Meal.entries.forEach { meal ->
                MealCard(
                    meal = meal,
                    entries = state.byMeal[meal].orEmpty(),
                    onAdd = { addFor = meal },
                    onEdit = { editing = it },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { addFor = Meal.forTime(LocalTime.now()) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add_entry)) },
            containerColor = c.accent,
            contentColor = c.surface,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    addFor?.let { meal ->
        AddSheet(
            initialMeal = meal,
            date = state.date,
            onDismiss = { addFor = null },
            onAdded = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
        )
    }
    editing?.let { entry ->
        EditEntrySheet(
            entry = entry,
            onDismiss = { editing = null },
            onSaveFood = { grams, meal -> vm.updateFoodEntry(entry, grams, meal); editing = null },
            onSaveQuick = { name, kcal, meal -> vm.updateQuickEntry(entry, name, kcal, meal); editing = null },
            onDelete = { editing = null; deleteWithUndo(entry) },
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate, isToday: Boolean,
    onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit,
) {
    val c = Tokens.colors
    val title = when (date) {
        LocalDate.now() -> stringResource(R.string.today_title)
        LocalDate.now().minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMANY))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_day), tint = c.fg)
        }
        Column(
            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(enabled = !isToday, onClick = onToday),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.fg)
            Text(
                date.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY)) +
                    if (!isToday) " · " + stringResource(R.string.tap_for_today) else "",
                style = MaterialTheme.typography.bodySmall, color = c.muted,
            )
        }
        IconButton(onClick = onNext, enabled = !isToday) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_day),
                tint = if (isToday) c.line else c.fg,
            )
        }
    }
}

@Composable
private fun Summary(state: TodayUiState) {
    val c = Tokens.colors
    val goal = state.settings.dailyGoal
    val eaten = state.total.kcal
    val rest = Nutrition.remaining(goal, eaten)
    val over = rest < 0
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(118.dp), contentAlignment = Alignment.Center) {
                val progress = Nutrition.progress(goal, eaten)
                val ringColor = if (over) c.over else c.accent
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(c.line, -90f, 360f, false, style = stroke)
                    if (progress > 0f) drawArc(ringColor, -90f, 360f * progress, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Nutrition.format(abs(rest)), style = MaterialTheme.typography.displaySmall, color = if (over) c.over else c.fg)
                    Text(
                        stringResource(if (over) R.string.kcal_over else R.string.kcal_left).uppercase(Locale.GERMANY),
                        style = MaterialTheme.typography.labelSmall, color = c.muted,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow(stringResource(R.string.goal), stringResource(R.string.kcal_value, Nutrition.format(goal)))
                StatRow(stringResource(R.string.eaten), stringResource(R.string.kcal_value, Nutrition.format(eaten)))
                StatRow(stringResource(R.string.entries), state.count.toString())
            }
        }
        if (state.settings.showMacros) {
            Spacer(Modifier.height(14.dp))
            val t = MacroTargets.fromGoal(goal)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroBar(stringResource(R.string.protein), state.total.protein, t.protein, c.protein, Modifier.weight(1f))
                MacroBar(stringResource(R.string.carbs_short), state.total.carbs, t.carbs, c.carbs, Modifier.weight(1f))
                MacroBar(stringResource(R.string.fat), state.total.fat, t.fat, c.fat, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val c = Tokens.colors
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.fg)
    }
}

@Composable
private fun MacroBar(label: String, value: Double, target: Int, color: Color, modifier: Modifier) {
    val c = Tokens.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Text(
            stringResource(R.string.macro_value, value.roundToInt(), target),
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.fg,
        )
        LinearProgressIndicator(
            progress = { if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color, trackColor = c.line, drawStopIndicator = {}, gapSize = 0.dp,
        )
    }
}

@Composable
private fun MealCard(meal: Meal, entries: List<EntryEntity>, onAdd: () -> Unit, onEdit: (EntryEntity) -> Unit) {
    val c = Tokens.colors
    val sum = entries.sumOf { it.kcal }
    AppCard(padding = false) {
        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(meal.label(), style = MaterialTheme.typography.titleMedium, color = c.fg, modifier = Modifier.weight(1f))
            if (sum > 0) {
                Text(stringResource(R.string.kcal_value, Nutrition.format(sum)), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Spacer(Modifier.width(8.dp))
            }
            FilledTonalIconButton(
                onClick = onAdd, shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_meal, meal.label())) }
        }
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.meal_empty), style = MaterialTheme.typography.bodySmall, color = c.muted,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        entries.forEach { e ->
            HorizontalDivider(color = c.line)
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(e) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOfNotNull(e.grams?.let { amountText(it, e.isLiquid) }, e.brand).joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Text(Nutrition.format(e.kcal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.fg)
            }
        }
    }
}
