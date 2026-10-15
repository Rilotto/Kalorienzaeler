package de.kalorien.app.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.kalorien.app.R
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.domain.PortionGuesser
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.MealChips
import de.kalorien.app.core.ui.theme.Tokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Eintrag ändern oder löschen. Lebensmittel-Einträge: Menge + Mahlzeit. Schnelleinträge: Name + kcal + Mahlzeit. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEntrySheet(
    entry: EntryEntity,
    onDismiss: () -> Unit,
    onSaveFood: (grams: Double, meal: Meal) -> Unit,
    onSaveQuick: (name: String, kcal: Int, meal: Meal) -> Unit,
    onDelete: () -> Unit,
    loadFavorite: suspend () -> Boolean,
    toggleFavorite: suspend (current: Boolean) -> Boolean,
) {
    val c = Tokens.colors
    val scope = rememberCoroutineScope()
    var favorite by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(entry.id) { favorite = loadFavorite() }
    val isFood = entry.grams != null
    var meal by remember { mutableStateOf(Meal.fromDb(entry.meal)) }
    var gramsText by remember { mutableStateOf(entry.grams?.let(NumberInput::format) ?: "") }
    var nameText by remember { mutableStateOf(entry.name) }
    var kcalText by remember { mutableStateOf(entry.kcal.toString()) }
    val unit = stringResource(if (entry.isLiquid) R.string.unit_ml else R.string.unit_g)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_entry), style = MaterialTheme.typography.titleLarge, color = c.fg, modifier = Modifier.weight(1f))
                val fav = favorite
                IconButton(
                    onClick = { if (fav != null) scope.launch { favorite = toggleFavorite(fav) } },
                    enabled = fav != null,
                ) {
                    Icon(
                        if (fav == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(if (fav == true) R.string.unfavorite else R.string.favorite),
                        tint = if (fav == true) c.over else c.muted,
                    )
                }
            }
            if (!isFood && favorite == false) {
                Text(stringResource(R.string.favorite_quick_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            if (isFood) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, color = c.fg)
                val grams = NumberInput.parse(gramsText)
                val oldGrams = entry.grams ?: 0.0
                val preview = if (grams != null && oldGrams > 0) (entry.kcal * grams / oldGrams).roundToInt() else null
                Text(
                    preview?.let { stringResource(R.string.kcal_value, Nutrition.format(it)) } ?: "–",
                    style = MaterialTheme.typography.displaySmall, color = c.fg,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortionGuesser.portionsFor(entry.name, entry.isLiquid).forEach { p ->
                        AppChip(p.label, grams == p.grams) { gramsText = NumberInput.format(p.grams) }
                    }
                }
                OutlinedTextField(
                    value = gramsText, onValueChange = { gramsText = it },
                    label = { Text(stringResource(R.string.amount_in, unit)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
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
            Text(stringResource(R.string.meal), style = MaterialTheme.typography.bodySmall, color = c.muted)
            MealChips(meal) { meal = it }

            val canSave = if (isFood) (NumberInput.parse(gramsText) ?: 0.0) > 0 else (kcalText.toIntOrNull() ?: 0) > 0
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onDelete, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.overSoft, contentColor = c.over),
                ) { Text(stringResource(R.string.delete)) }
                Button(
                    onClick = {
                        if (isFood) onSaveFood(NumberInput.parse(gramsText)!!, meal)
                        else onSaveQuick(nameText.trim().ifEmpty { entry.name }, kcalText.toInt(), meal)
                    },
                    enabled = canSave, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}
