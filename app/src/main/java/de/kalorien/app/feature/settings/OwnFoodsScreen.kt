package de.kalorien.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.R
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.FoodRepository
import de.kalorien.app.core.data.FoodSource
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.Per100
import de.kalorien.app.core.ui.AppNumberField
import de.kalorien.app.core.ui.AppViewModels
import de.kalorien.app.core.ui.theme.Tokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Eigene, gescannte und korrigierte Lebensmittel: Werte ändern oder löschen. */
@Composable
fun OwnFoodsScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val c = Tokens.colors
    val foods by vm.ownFoods.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<FoodEntity?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val key = FoodRepository.searchKey(filter, null)
    val shown = if (filter.isBlank()) foods else foods.filter { it.searchKey.contains(key) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.fg)
            }
            Text(stringResource(R.string.own_foods_title), style = MaterialTheme.typography.titleLarge, color = c.fg)
        }
        Text(stringResource(R.string.own_foods_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = filter, onValueChange = { filter = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
        )
        if (shown.isEmpty()) {
            Text(stringResource(R.string.own_foods_empty), style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(shown, key = { it.id }) { f ->
                HorizontalDivider(color = c.line)
                Row(
                    Modifier.fillMaxWidth().clickable { editing = f }.padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(f.brand, stringResource(sourceRes(f)), if (f.isAdjusted) stringResource(R.string.adjusted) else null)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = c.muted,
                        )
                    }
                    Text(
                        stringResource(R.string.kcal_value, f.kcal100.roundToInt().toString()) + " " + stringResource(basisRes(f)),
                        style = MaterialTheme.typography.bodySmall, color = c.muted,
                    )
                }
            }
        }
    }

    editing?.let { f ->
        FoodEditDialog(
            food = f,
            onDismiss = { editing = null },
            onSave = { per, name -> vm.correctFood(f, per, name); editing = null },
            onReset = { vm.resetFood(f); editing = null },
            onDelete = if (f.source == FoodSource.BLS) null else {
                {
                    editing = null
                    vm.deleteFood(f)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        val r = snackbar.showSnackbar(
                            context.getString(R.string.food_deleted, f.name), context.getString(R.string.undo),
                            duration = SnackbarDuration.Long,
                        )
                        if (r == SnackbarResult.ActionPerformed) vm.restoreFood(f)
                    }
                }
            },
        )
    }
}

private fun sourceRes(f: FoodEntity) = when (f.source) {
    FoodSource.BLS -> R.string.source_bls
    FoodSource.OFF -> R.string.source_off
    else -> R.string.source_custom
}

private fun basisRes(f: FoodEntity) = when {
    f.isPortionFood -> R.string.basis_portion
    f.isLiquid -> R.string.basis_100ml
    else -> R.string.basis_100g
}

@Composable
private fun FoodEditDialog(
    food: FoodEntity,
    onDismiss: () -> Unit,
    onSave: (Per100, String) -> Unit,
    onReset: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val c = Tokens.colors
    fun fmt(d: Double?) = d?.let { NumberInput.format(it) }.orEmpty()
    var name by remember { mutableStateOf(food.name) }
    var kcal by remember { mutableStateOf(fmt(food.kcal100)) }
    var protein by remember { mutableStateOf(fmt(food.protein100)) }
    var carbs by remember { mutableStateOf(fmt(food.carbs100)) }
    var fat by remember { mutableStateOf(fmt(food.fat100)) }
    var sugar by remember { mutableStateOf(fmt(food.sugar100)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val k = NumberInput.parse(kcal)
    val custom = food.source == FoodSource.CUSTOM

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.food_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(if (custom) R.string.food_edit_hint_custom else R.string.food_edit_hint),
                    style = MaterialTheme.typography.bodySmall, color = c.muted,
                )
                if (custom) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(80) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.custom_name)) },
                    )
                }
                Text(stringResource(R.string.values_per, stringResource(basisRes(food))), style = MaterialTheme.typography.bodySmall, color = c.muted)
                AppNumberField(kcal, { kcal = it }, stringResource(R.string.kcal))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppNumberField(protein, { protein = it }, stringResource(R.string.protein), Modifier.weight(1f))
                    AppNumberField(carbs, { carbs = it }, stringResource(R.string.carbs_short), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppNumberField(fat, { fat = it }, stringResource(R.string.fat), Modifier.weight(1f))
                    AppNumberField(sugar, { sugar = it }, stringResource(R.string.sugar), Modifier.weight(1f))
                }
                if (food.isAdjusted) TextButton(onClick = onReset) { Text(stringResource(R.string.food_reset), color = c.accent) }
                if (onDelete != null) {
                    if (!confirmDelete) {
                        TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.food_delete), color = c.over) }
                    } else {
                        Text(stringResource(R.string.food_delete_hint), style = MaterialTheme.typography.bodySmall, color = c.over)
                        TextButton(onClick = onDelete) { Text(stringResource(R.string.food_delete_confirm), color = c.over) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = k != null,
                onClick = {
                    onSave(
                        Per100(
                            k ?: 0.0, NumberInput.parse(protein) ?: 0.0, NumberInput.parse(carbs) ?: 0.0,
                            NumberInput.parse(fat) ?: 0.0, NumberInput.parse(sugar),
                        ),
                        name,
                    )
                },
            ) { Text(stringResource(R.string.save), color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = c.muted) } },
    )
}
