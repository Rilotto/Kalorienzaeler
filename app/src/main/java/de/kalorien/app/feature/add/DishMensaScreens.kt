package de.kalorien.app.feature.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kalorien.app.R
import de.kalorien.app.core.data.DishDraft
import de.kalorien.app.core.data.DishDraftItem
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.MensaSlotEntity
import de.kalorien.app.core.domain.MensaCalc
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.domain.NutritionColumn
import de.kalorien.app.core.domain.Per100
import de.kalorien.app.core.domain.TextNormalizer
import de.kalorien.app.core.domain.UnitAmount
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.AppNumberField
import de.kalorien.app.core.ui.theme.Tokens
import de.kalorien.app.feature.today.CountChips
import java.time.LocalDate
import kotlin.math.roundToInt

// ======================================================================== Gericht aus Zutaten

/**
 * Gericht anlegen/ändern (z. B. „Mein Sandwich“): Name, Bezeichnung einer Portion, Anzahl Portionen, Zutaten.
 * Unten stehen die Werte pro Portion – so wird das Gericht später eingetragen.
 */
@Composable
internal fun ColumnScope.DishEditor(
    draft: DishDraft,
    onChange: (DishDraft) -> Unit,
    onAddIngredient: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val c = Tokens.colors
    var servingsText by remember(draft.foodId) { mutableStateOf(NumberInput.format(draft.servings)) }
    Text(
        stringResource(if (draft.foodId == null) R.string.dish_new_title else R.string.dish_edit_title),
        style = MaterialTheme.typography.titleLarge, color = c.fg,
    )
    Text(stringResource(R.string.dish_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
    OutlinedTextField(
        value = draft.name, onValueChange = { onChange(draft.copy(name = it.take(80))) }, singleLine = true,
        label = { Text(stringResource(R.string.dish_name)) }, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = draft.portionLabel, onValueChange = { onChange(draft.copy(portionLabel = it.take(30))) }, singleLine = true,
        label = { Text(stringResource(R.string.dish_portion_label)) }, placeholder = { Text(stringResource(R.string.dish_portion_label_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.dish_servings), style = MaterialTheme.typography.bodySmall, color = c.muted)
    CountChips(NumberInput.parse(servingsText)) { n ->
        servingsText = NumberInput.format(n); onChange(draft.copy(servings = n))
    }
    AppNumberField(servingsText, { v ->
        servingsText = v; NumberInput.parse(v)?.takeIf { it > 0 }?.let { onChange(draft.copy(servings = it)) }
    }, stringResource(R.string.dish_servings_field))

    Text(stringResource(R.string.dish_ingredients).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
    if (draft.items.isEmpty()) {
        Text(stringResource(R.string.dish_no_ingredients), style = MaterialTheme.typography.bodyMedium, color = c.muted)
    }
    draft.items.forEachIndexed { i, it ->
        IngredientRow(it) { onChange(draft.copy(items = draft.items.filterIndexed { j, _ -> j != i })) }
    }
    FilledTonalButton(
        onClick = onAddIngredient, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(stringResource(R.string.dish_add_ingredient))
    }

    draft.perPortion?.let { p ->
        AppCard {
            Text(stringResource(R.string.dish_per_portion), style = MaterialTheme.typography.bodySmall, color = c.muted)
            Text(stringResource(R.string.kcal_value, Nutrition.format(p.kcal.roundToInt())), style = MaterialTheme.typography.headlineSmall, color = c.fg)
            Text(
                stringResource(R.string.macros_line, p.protein.roundToInt(), p.carbs.roundToInt(), p.fat.roundToInt()) +
                    (p.sugar?.let { " · " + stringResource(R.string.sugar_value, it.roundToInt()) } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = c.muted,
            )
            draft.gramsPerPortion?.let {
                Text(stringResource(R.string.dish_grams_per_portion, NumberInput.format(it)), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }
    }
    val ok = draft.name.isNotBlank() && draft.items.isNotEmpty() && draft.servings > 0
    if (!ok) Text(stringResource(R.string.dish_needs), style = MaterialTheme.typography.bodySmall, color = c.muted)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
            onClick = onCancel, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.cancel)) }
        Button(
            onClick = onSave, enabled = ok, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.save)) }
    }
}

@Composable
private fun IngredientRow(item: DishDraftItem, onRemove: () -> Unit) {
    val c = Tokens.colors
    val unit = stringResource(if (item.isLiquid) R.string.unit_ml else R.string.unit_g)
    val amount = when {
        item.unitLabel != null && item.unitCount != null && item.grams != null ->
            UnitAmount.describe(item.unitCount, item.unitLabel, item.grams, item.isLiquid)
        item.unitLabel != null && item.unitCount != null -> "${NumberInput.format(item.unitCount)} × ${item.unitLabel}"
        item.grams != null -> "${NumberInput.format(item.grams)} $unit"
        else -> ""
    }
    Column {
        HorizontalDivider(color = c.line)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (amount.isNotEmpty()) Text(amount, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Text(stringResource(R.string.kcal_value, Nutrition.format(item.amount.kcal)), style = MaterialTheme.typography.bodyMedium, color = c.fg)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dish_remove_ingredient), tint = c.muted)
            }
        }
    }
}

// ======================================================================== Mensa

/**
 * Reiter „Mensa“: feste Plätze („Menü 1“ …), die für den angezeigten Tag mit einem Gericht belegt werden –
 * per Foto der Nährwerttabelle, von Hand oder aus den bekannten Gerichten.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.MensaTab(
    vm: AddViewModel,
    date: LocalDate,
    onPhoto: (MensaSlotEntity) -> Unit,
    onManual: (MensaSlotEntity?) -> Unit,
    onOpen: (FoodEntity) -> Unit,
    onSlotDeleted: (MensaSlotEntity) -> Unit,
) {
    val c = Tokens.colors
    val slots by vm.mensaSlots.collectAsStateWithLifecycle()
    val known by vm.mensaDishes.collectAsStateWithLifecycle()
    val defaults = stringArrayResource(R.array.mensa_default_slots).toList()
    LaunchedEffect(Unit) { vm.ensureMensaSlots(defaults) }
    var nameDialog by remember { mutableStateOf<Pair<MensaSlotEntity?, String>?>(null) }
    var pickFor by remember { mutableStateOf<MensaSlotEntity?>(null) }

    Text(stringResource(R.string.mensa_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
    slots.forEach { slot ->
        val today = known.firstOrNull { it.id == slot.foodId }?.takeIf { slot.date == date.toString() }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(slot.name, style = MaterialTheme.typography.titleMedium, color = c.fg, modifier = Modifier.weight(1f))
                var menu by remember { mutableStateOf(false) }
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = c.muted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.mensa_slot_rename)) }, onClick = { menu = false; nameDialog = slot to slot.name })
                    DropdownMenuItem(text = { Text(stringResource(R.string.mensa_slot_delete)) }, onClick = { menu = false; onSlotDeleted(slot) })
                }
            }
            if (today != null) {
                Text(today.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.mensa_per_portion, Nutrition.format(today.kcal100.roundToInt())) +
                        (today.servingGrams?.let { " · " + stringResource(R.string.mensa_portion_grams, NumberInput.format(it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = c.muted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onOpen(today) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
                    ) { Text(stringResource(R.string.mensa_add)) }
                    FilledTonalButton(
                        onClick = { onPhoto(slot) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
                    ) { Text(stringResource(R.string.mensa_new_photo)) }
                }
            } else {
                Text(stringResource(R.string.mensa_empty_today), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Button(
                    onClick = { onPhoto(slot) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
                ) { Text(stringResource(R.string.mensa_photo)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { pickFor = slot }, enabled = known.isNotEmpty(), modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
                    ) { Text(stringResource(R.string.mensa_known)) }
                    FilledTonalButton(
                        onClick = { onManual(slot) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
                    ) { Text(stringResource(R.string.mensa_manual)) }
                }
            }
        }
    }
    TextButton(onClick = { nameDialog = null to "" }) { Text(stringResource(R.string.mensa_slot_add), color = c.accent) }

    if (known.isNotEmpty()) {
        Text(stringResource(R.string.mensa_known_title).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
        known.take(15).forEach { f -> MensaDishRow(f) { onOpen(f) } }
    }

    nameDialog?.let { (slot, initial) ->
        var name by remember { mutableStateOf(initial) }
        AlertDialog(
            onDismissRequest = { nameDialog = null },
            title = { Text(stringResource(if (slot == null) R.string.mensa_slot_add else R.string.mensa_slot_rename)) },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(30) }, singleLine = true,
                    label = { Text(stringResource(R.string.mensa_slot_name)) },
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (slot == null) vm.addMensaSlot(name) else vm.renameMensaSlot(slot, name)
                    nameDialog = null
                }) { Text(stringResource(R.string.save), color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { nameDialog = null }) { Text(stringResource(R.string.cancel), color = c.muted) } },
        )
    }
    pickFor?.let { slot ->
        var filter by remember { mutableStateOf("") }
        val key = TextNormalizer.normalize(filter).replace(" ", "")
        AlertDialog(
            onDismissRequest = { pickFor = null },
            title = { Text(stringResource(R.string.mensa_known_for, slot.name)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = filter, onValueChange = { filter = it }, singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    known.filter { key.isEmpty() || it.searchKey.contains(key) }.take(12).forEach { f ->
                        MensaDishRow(f) { vm.assignMensa(slot, f, date); pickFor = null; onOpen(f) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickFor = null }) { Text(stringResource(R.string.cancel), color = c.muted) } },
        )
    }
}

@Composable
private fun MensaDishRow(f: FoodEntity, onClick: () -> Unit) {
    val c = Tokens.colors
    Column {
        HorizontalDivider(color = c.line)
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.kcal_value, Nutrition.format(f.kcal100.roundToInt())), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

/**
 * Formular nach dem Foto (oder von Hand): Gerichtname, Werte pro Portion oder pro 100 g + Portionsgewicht.
 * Gespeichert wird immer pro Portion; bei zwei Spalten ist „pro Portion“ vorgewählt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.MensaForm(
    prefill: MensaPrefill,
    knownNames: Set<String>,
    onPhoto: () -> Unit,
    onBack: () -> Unit,
    onSave: (name: String, perPortion: Per100, portionGrams: Double?) -> Unit,
) {
    val c = Tokens.colors
    fun fmt(d: Double?) = d?.let(NumberInput::format).orEmpty()
    // Welche Spalte wird bearbeitet? Portion, wenn vorhanden.
    var byPortion by remember(prefill) { mutableStateOf(prefill.portion != null || prefill.per100 == null) }
    fun col(): NutritionColumn? = if (byPortion) prefill.portion else prefill.per100
    var name by remember(prefill) { mutableStateOf(prefill.name) }
    var kcal by remember(prefill, byPortion) { mutableStateOf(fmt(col()?.kcal)) }
    var protein by remember(prefill, byPortion) { mutableStateOf(fmt(col()?.protein)) }
    var carbs by remember(prefill, byPortion) { mutableStateOf(fmt(col()?.carbs)) }
    var fat by remember(prefill, byPortion) { mutableStateOf(fmt(col()?.fat)) }
    var sugar by remember(prefill, byPortion) { mutableStateOf(fmt(col()?.sugar)) }
    var grams by remember(prefill) { mutableStateOf(fmt(prefill.portionGrams)) }

    Text(prefill.slotName ?: stringResource(R.string.mensa_form_title), style = MaterialTheme.typography.titleLarge, color = c.fg)
    when (prefill.found) {
        true -> Text(stringResource(R.string.mensa_read_ok), style = MaterialTheme.typography.bodySmall, color = c.accent)
        false -> Text(stringResource(R.string.note_photo_nothing), style = MaterialTheme.typography.bodySmall, color = c.over)
        null -> {}
    }
    OutlinedTextField(
        value = name, onValueChange = { name = it.take(80) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.mensa_dish_name)) },
    )
    if (TextNormalizer.normalize(name).replace(" ", "") in knownNames) {
        Text(stringResource(R.string.mensa_known_update), style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
    Text(stringResource(R.string.mensa_values_for), style = MaterialTheme.typography.bodySmall, color = c.muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppChip(stringResource(R.string.mensa_basis_portion), byPortion) { byPortion = true }
        AppChip(stringResource(R.string.mensa_basis_100), !byPortion) { byPortion = false }
    }
    if (prefill.portion != null && prefill.per100 != null) {
        Text(
            stringResource(R.string.mensa_both_columns, Nutrition.format((prefill.per100.kcal ?: 0.0).roundToInt()), Nutrition.format((prefill.portion.kcal ?: 0.0).roundToInt())),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
    }
    AppNumberField(kcal, { kcal = it }, stringResource(R.string.kcal))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppNumberField(protein, { protein = it }, stringResource(R.string.protein), Modifier.weight(1f))
        AppNumberField(carbs, { carbs = it }, stringResource(R.string.carbs_short), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppNumberField(fat, { fat = it }, stringResource(R.string.fat), Modifier.weight(1f))
        AppNumberField(sugar, { sugar = it }, stringResource(R.string.sugar_optional), Modifier.weight(1f))
    }
    AppNumberField(grams, { grams = it }, stringResource(if (byPortion) R.string.mensa_grams_optional else R.string.mensa_grams_needed))

    val entered = NumberInput.parse(kcal)?.let {
        Per100(it, NumberInput.parse(protein) ?: 0.0, NumberInput.parse(carbs) ?: 0.0, NumberInput.parse(fat) ?: 0.0, NumberInput.parse(sugar))
    }
    val g = NumberInput.parse(grams)?.takeIf { it > 0 }
    val perPortion = if (byPortion) MensaCalc.portionFrom(null, g, entered) else MensaCalc.portionFrom(entered, g, null)
    perPortion?.let {
        Text(
            stringResource(R.string.mensa_result, Nutrition.format(it.kcal.roundToInt())),
            style = MaterialTheme.typography.titleMedium, color = c.fg,
        )
    }
    if (!byPortion && g == null) Text(stringResource(R.string.mensa_need_grams), style = MaterialTheme.typography.bodySmall, color = c.over)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
            onClick = onPhoto, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.mensa_photo_again)) }
        Button(
            onClick = { perPortion?.let { onSave(name, it, g) } }, enabled = perPortion != null && name.isNotBlank(),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.mensa_save)) }
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back), color = c.muted) }
}
