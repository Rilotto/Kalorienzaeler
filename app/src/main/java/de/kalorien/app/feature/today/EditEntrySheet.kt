package de.kalorien.app.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.kalorien.app.R
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.domain.Portion
import de.kalorien.app.core.domain.PortionGuesser
import de.kalorien.app.core.domain.UnitAmount
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.AppNumberField
import de.kalorien.app.core.ui.DateRow
import de.kalorien.app.core.ui.MealChips
import de.kalorien.app.core.ui.theme.Tokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Eintrag ändern, verschieben (Tag/Mahlzeit), Werte von Hand anpassen oder löschen.
 * Drei Arten: in Gramm (optional mit Einheit wie „Scheibe“), Portions-Lebensmittel (Anzahl), Schnelleintrag (Name + kcal).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEntrySheet(
    entry: EntryEntity,
    onDismiss: () -> Unit,
    onSave: (EntryEdit) -> Unit,
    onDelete: () -> Unit,
    loadFavorite: suspend () -> Boolean,
    toggleFavorite: suspend (current: Boolean) -> Boolean,
) {
    val c = Tokens.colors
    val scope = rememberCoroutineScope()
    val kind = when {
        entry.unitLabel == EntryRepository.PORTION_UNIT -> EntryEdit.Kind.Portions
        entry.grams != null -> EntryEdit.Kind.Grams
        else -> EntryEdit.Kind.Quick
    }
    var favorite by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(entry.id) { favorite = loadFavorite() }

    var meal by remember { mutableStateOf(Meal.fromDb(entry.meal)) }
    var date by remember { mutableStateOf(LocalDate.parse(entry.date)) }

    // --- Menge in Gramm oder in einer Einheit
    val unit = if (entry.isLiquid) "ml" else "g"
    val units: List<Portion> = remember(entry.id) {
        val guessed = PortionGuesser.portionsFor(entry.name, entry.isLiquid).filterNot { UnitAmount.isPlainAmount(it.label) }
        val own = if (entry.unitLabel != null && entry.unitCount != null && entry.grams != null && entry.unitCount > 0) {
            Portion(entry.unitLabel, entry.grams / entry.unitCount)
        } else null
        (listOfNotNull(own) + guessed.map { Portion(UnitAmount.unitName(it.label), it.grams) }).distinctBy { it.label }
    }
    var selectedUnit by remember { mutableStateOf(units.firstOrNull { it.label == entry.unitLabel }) }
    var gramsText by remember { mutableStateOf(entry.grams?.let(NumberInput::format).orEmpty()) }
    var countText by remember { mutableStateOf(entry.unitCount?.let(NumberInput::format) ?: "1") }

    // --- Schnelleintrag
    var nameText by remember { mutableStateOf(entry.name) }
    var kcalText by remember { mutableStateOf(entry.kcal.toString()) }

    // --- Werte von Hand
    var showOverride by remember { mutableStateOf(false) }
    var oKcal by remember { mutableStateOf(entry.kcal.toString()) }
    var oProtein by remember { mutableStateOf(NumberInput.format(entry.protein)) }
    var oCarbs by remember { mutableStateOf(NumberInput.format(entry.carbs)) }
    var oFat by remember { mutableStateOf(NumberInput.format(entry.fat)) }
    var oSugar by remember { mutableStateOf(entry.sugar?.let(NumberInput::format).orEmpty()) }

    val count = NumberInput.parse(countText)
    val grams: Double? = when {
        kind != EntryEdit.Kind.Grams -> null
        selectedUnit != null -> count?.let { it * selectedUnit!!.grams }
        else -> NumberInput.parse(gramsText)
    }
    // Vorschau (proportional zum gespeicherten Wert)
    val preview: Int? = when (kind) {
        EntryEdit.Kind.Grams -> {
            val old = entry.grams ?: 0.0
            if (grams != null && old > 0) (entry.kcal * grams / old).roundToInt() else null
        }
        EntryEdit.Kind.Portions -> {
            val old = entry.unitCount?.takeIf { it > 0 } ?: 1.0
            count?.let { (entry.kcal * it / old).roundToInt() }
        }
        EntryEdit.Kind.Quick -> kcalText.toIntOrNull()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_entry), style = MaterialTheme.typography.titleLarge, color = c.fg, modifier = Modifier.weight(1f))
                val fav = favorite
                IconButton(onClick = { if (fav != null) scope.launch { favorite = toggleFavorite(fav) } }, enabled = fav != null) {
                    Icon(
                        if (fav == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(if (fav == true) R.string.unfavorite else R.string.favorite),
                        tint = if (fav == true) c.over else c.muted,
                    )
                }
            }
            if (kind == EntryEdit.Kind.Quick && favorite == false) {
                Text(stringResource(R.string.favorite_quick_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }

            when (kind) {
                EntryEdit.Kind.Grams -> {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium, color = c.fg)
                    KcalPreview(if (showOverride) oKcal.toIntOrNull() else preview, grams, entry.isLiquid)
                    Text(stringResource(R.string.unit_choose), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppChip(unit, selectedUnit == null) {
                            selectedUnit = null
                            grams?.let { gramsText = NumberInput.format(it) }
                        }
                        units.forEach { u ->
                            AppChip("${u.label} (${NumberInput.format(u.grams)} $unit)", selectedUnit == u) {
                                selectedUnit = u
                                countText = "1"
                            }
                        }
                    }
                    if (selectedUnit != null) {
                        CountChips(count) { countText = NumberInput.format(it) }
                        AppNumberField(countText, { countText = it }, stringResource(R.string.unit_count, selectedUnit!!.label))
                    } else {
                        AppNumberField(gramsText, { gramsText = it }, stringResource(R.string.amount_in, unit))
                    }
                }
                EntryEdit.Kind.Portions -> {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium, color = c.fg)
                    KcalPreview(if (showOverride) oKcal.toIntOrNull() else preview, null, entry.isLiquid)
                    CountChips(count) { countText = NumberInput.format(it) }
                    AppNumberField(countText, { countText = it }, stringResource(R.string.portions_amount))
                }
                EntryEdit.Kind.Quick -> {
                    OutlinedTextField(
                        value = nameText, onValueChange = { nameText = it },
                        label = { Text(stringResource(R.string.quick_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = kcalText, onValueChange = { kcalText = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(R.string.quick_kcal)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Text(stringResource(R.string.meal), style = MaterialTheme.typography.bodySmall, color = c.muted)
            MealChips(meal) { meal = it }
            DateRow(date) { date = it }

            if (kind != EntryEdit.Kind.Quick) {
                TextButton(onClick = { showOverride = !showOverride }) {
                    Text(stringResource(if (showOverride) R.string.override_hide else R.string.override_show), color = c.accent)
                }
                if (showOverride) {
                    Text(stringResource(R.string.override_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    AppNumberField(oKcal, { oKcal = it }, stringResource(R.string.custom_kcal), integer = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppNumberField(oProtein, { oProtein = it }, stringResource(R.string.protein), Modifier.weight(1f))
                        AppNumberField(oCarbs, { oCarbs = it }, stringResource(R.string.carbs_short), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppNumberField(oFat, { oFat = it }, stringResource(R.string.fat), Modifier.weight(1f))
                        AppNumberField(oSugar, { oSugar = it }, stringResource(R.string.sugar), Modifier.weight(1f))
                    }
                }
            }

            val canSave = when (kind) {
                EntryEdit.Kind.Grams -> (grams ?: 0.0) > 0
                EntryEdit.Kind.Portions -> (count ?: 0.0) > 0
                EntryEdit.Kind.Quick -> (kcalText.toIntOrNull() ?: 0) > 0
            } && (!showOverride || (oKcal.toIntOrNull() ?: -1) >= 0)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onDelete, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.overSoft, contentColor = c.over),
                ) { Text(stringResource(R.string.delete)) }
                Button(
                    onClick = {
                        val override = if (showOverride) Amount(
                            oKcal.toIntOrNull() ?: entry.kcal, NumberInput.parse(oProtein) ?: 0.0, NumberInput.parse(oCarbs) ?: 0.0,
                            NumberInput.parse(oFat) ?: 0.0, NumberInput.parse(oSugar),
                        ) else null
                        onSave(
                            when (kind) {
                                EntryEdit.Kind.Grams -> EntryEdit(
                                    kind, meal, date, grams = grams, unitLabel = selectedUnit?.label,
                                    unitCount = if (selectedUnit != null) count else null, override = override,
                                )
                                EntryEdit.Kind.Portions -> EntryEdit(kind, meal, date, unitCount = count, override = override)
                                EntryEdit.Kind.Quick -> EntryEdit(kind, meal, date, name = nameText.trim().ifEmpty { entry.name }, kcal = kcalText.toInt())
                            },
                        )
                    },
                    enabled = canSave, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable
private fun KcalPreview(kcal: Int?, grams: Double?, isLiquid: Boolean) {
    val c = Tokens.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(kcal?.let { Nutrition.format(it) } ?: "–", style = MaterialTheme.typography.displaySmall, color = c.fg)
        Text(" kcal", style = MaterialTheme.typography.bodyLarge, color = c.muted)
        if (grams != null) {
            Text(
                "  · ${NumberInput.format(grams)} ${if (isLiquid) "ml" else "g"}",
                style = MaterialTheme.typography.bodyLarge, color = c.muted,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountChips(selected: Double?, onPick: (Double) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitAmount.quickCounts.forEach { n ->
            AppChip(stringResource(R.string.portion_count, NumberInput.format(n)), selected == n) { onPick(n) }
        }
    }
}
