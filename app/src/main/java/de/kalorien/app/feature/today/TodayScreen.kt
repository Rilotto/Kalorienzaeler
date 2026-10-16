package de.kalorien.app.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.ui.input.pointer.pointerInput
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.UnitAmount
import de.kalorien.app.core.ui.germanLong
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import de.kalorien.app.core.ui.labelRes
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
    var templateFor by remember { mutableStateOf<Meal?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday(state.isToday) }

    fun deleteWithUndo(entry: EntryEntity) {
        vm.delete(entry)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
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
            // Kopf + Bilanz: nach links/rechts wischen = Tag wechseln (Einträge selbst wischen = löschen)
            Column(
                Modifier.daySwipe(onPrev = vm::previousDay, onNext = vm::nextDay),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DayHeader(state.date, state.isToday, vm::previousDay, vm::nextDay, vm::goToToday)
                Summary(state)
            }
            Meal.entries.forEach { meal ->
                MealCard(
                    meal = meal,
                    entries = state.byMeal[meal].orEmpty(),
                    canCopyYesterday = meal in state.yesterdayMeals,
                    onAdd = { addFor = meal },
                    onSaveTemplate = { templateFor = meal },
                    onEdit = { editing = it },
                    onDelete = { deleteWithUndo(it) },
                    onAgain = { e ->
                        vm.duplicateToToday(e) { copy ->
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val r = snackbar.showSnackbar(
                                    context.getString(R.string.entry_again, e.name), context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (r == SnackbarResult.ActionPerformed) vm.delete(copy)
                            }
                        }
                    },
                    onCopyYesterday = {
                        vm.copyFromYesterday(meal) { ids ->
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val r = snackbar.showSnackbar(
                                    context.getString(R.string.copied_yesterday, ids.size), context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (r == SnackbarResult.ActionPerformed) vm.undoCopy(ids)
                            }
                        }
                    },
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
            onAdded = { msg, entry ->
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = msg, actionLabel = context.getString(R.string.undo), duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.delete(entry)
                }
            },
            onTemplateAdded = { msg, ids ->
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = msg, actionLabel = context.getString(R.string.undo), duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.undoCopy(ids)
                }
            },
        )
    }
    templateFor?.let { meal ->
        SaveTemplateDialog(
            suggestion = context.getString(R.string.template_default_name, context.getString(meal.labelRes())),
            onDismiss = { templateFor = null },
            onSave = { name ->
                templateFor = null
                vm.saveTemplate(name, meal) { ok ->
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(context.getString(if (ok) R.string.template_saved else R.string.template_empty, name))
                    }
                }
            },
        )
    }
    editing?.let { entry ->
        EditEntrySheet(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { edit ->
                vm.save(entry, edit)
                editing = null
                if (edit.date.toString() != entry.date) {
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.entry_moved, edit.date.germanLong())) }
                }
            },
            onDelete = { editing = null; deleteWithUndo(entry) },
            loadFavorite = { vm.isFavorite(entry) },
            toggleFavorite = { current -> vm.toggleFavorite(entry, current) },
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
    val goal = state.goal
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
            val t = state.targets
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val compact = state.settings.showSugar
                MacroBar(stringResource(R.string.protein), state.total.protein, t.protein, c.protein, Modifier.weight(1f), compact = compact)
                MacroBar(stringResource(R.string.carbs_short), state.total.carbs, t.carbs, c.carbs, Modifier.weight(1f), compact = compact)
                MacroBar(stringResource(R.string.fat), state.total.fat, t.fat, c.fat, Modifier.weight(1f), compact = compact)
                if (state.settings.showSugar) {
                    MacroBar(stringResource(R.string.sugar), state.total.sugar ?: 0.0, t.sugar, c.sugar, Modifier.weight(1f), isLimit = true, compact = true)
                }
            }
            if (state.settings.showSugar && state.withoutSugar > 0 && state.count > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.sugar_missing, state.withoutSugar), style = MaterialTheme.typography.bodySmall, color = c.muted,
                )
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
private fun MacroBar(label: String, value: Double, target: Int, color: Color, modifier: Modifier, isLimit: Boolean = false, compact: Boolean = false) {
    val c = Tokens.colors
    // Zucker ist ein Höchstwert: über dem Limit wird der Balken rot
    @Suppress("NAME_SHADOWING") val color = if (isLimit && target > 0 && value > target) c.over else color
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Text(
            stringResource(R.string.macro_value, value.roundToInt(), target),
            // bei vier Balken etwas kleiner, damit „275 / 275 g“ in eine Zeile passt
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = c.fg, maxLines = 1,
        )
        LinearProgressIndicator(
            progress = { if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color, trackColor = c.line, drawStopIndicator = {}, gapSize = 0.dp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealCard(
    meal: Meal,
    entries: List<EntryEntity>,
    canCopyYesterday: Boolean,
    onAdd: () -> Unit,
    onSaveTemplate: () -> Unit,
    onEdit: (EntryEntity) -> Unit,
    onDelete: (EntryEntity) -> Unit,
    onAgain: (EntryEntity) -> Unit,
    onCopyYesterday: () -> Unit,
) {
    val c = Tokens.colors
    val sum = entries.sumOf { it.kcal }
    AppCard(padding = false) {
        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(meal.label(), style = MaterialTheme.typography.titleMedium, color = c.fg, modifier = Modifier.weight(1f))
            if (sum > 0) {
                Text(stringResource(R.string.kcal_value, Nutrition.format(sum)), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Spacer(Modifier.width(2.dp))
                var more by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { more = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = c.muted)
                    }
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.template_save)) }, onClick = { more = false; onSaveTemplate() })
                    }
                }
            }
            FilledTonalIconButton(
                onClick = onAdd, shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_meal, meal.label())) }
        }
        if (entries.isEmpty()) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.meal_empty), style = MaterialTheme.typography.bodySmall, color = c.muted,
                    modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                )
                if (canCopyYesterday) {
                    TextButton(onClick = onCopyYesterday) { Text(stringResource(R.string.like_yesterday), color = c.accent) }
                }
            }
        }
        entries.forEach { e ->
            HorizontalDivider(color = c.line)
            // key: damit nach dem Löschen nicht der nächste Eintrag den „weggewischt“-Zustand erbt
            key(e.id) {
                var menu by remember { mutableStateOf(false) }
                SwipeToDelete(onDelete = { onDelete(e) }) {
                    Box {
                        Row(
                            Modifier.fillMaxWidth().background(c.surface)
                                .combinedClickable(onClick = { onEdit(e) }, onLongClick = { menu = true })
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = listOfNotNull(entryAmount(e), e.brand).joinToString(" · ")
                                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                            Text(Nutrition.format(e.kcal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.fg)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.again_today)) }, onClick = { menu = false; onAgain(e) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.edit_or_move)) }, onClick = { menu = false; onEdit(e) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete(e) })
                        }
                    }
                }
            }
        }
    }
}

/** „1,5 × Scheibe (38 g)“, „2 × Portion“ oder „250 g“ */
@Composable
private fun entryAmount(e: EntryEntity): String? {
    val count = e.unitCount
    val label = e.unitLabel
    return when {
        label != null && count != null && e.grams != null -> UnitAmount.describe(count, label, e.grams, e.isLiquid)
        label != null && count != null -> "${NumberInput.format(count)} × $label"
        e.grams != null -> amountText(e.grams, e.isLiquid)
        else -> null
    }
}

/** Waagerechtes Wischen über Kopf und Bilanz wechselt den Tag. */
private fun Modifier.daySwipe(onPrev: () -> Unit, onNext: () -> Unit): Modifier = pointerInput(Unit) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            val threshold = 80.dp.toPx()
            if (total > threshold) onPrev() else if (total < -threshold) onNext()
        },
    ) { _, dragAmount -> total += dragAmount }
}

/** Nach links wischen = löschen (mit „Rückgängig“ in der Snackbar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val c = Tokens.colors
    // Schutz: Material ruft confirmValueChange u. U. mehrfach auf → nur einmal löschen
    var done by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                if (!done) {
                    done = true
                    onDelete()
                }
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                Modifier.fillMaxSize().background(c.overSoft).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.delete), color = c.over, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Delete, contentDescription = null, tint = c.over)
            }
        },
    ) { content() }
}

/** Name für eine neue Vorlage abfragen. */
@Composable
private fun SaveTemplateDialog(suggestion: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = Tokens.colors
    var name by remember { mutableStateOf(suggestion) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.template_save)) },
        text = {
            Column {
                Text(stringResource(R.string.template_save_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text(stringResource(R.string.template_name)) })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save), color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = c.muted) } },
    )
}
