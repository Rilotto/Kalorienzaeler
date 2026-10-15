package de.kalorien.app.feature.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.R
import de.kalorien.app.core.data.BlsImporter
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.FoodSource
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.domain.PortionGuesser
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.AppViewModels
import de.kalorien.app.core.ui.MealChips
import de.kalorien.app.core.ui.label
import de.kalorien.app.core.ui.labelRes
import de.kalorien.app.core.ui.theme.Tokens
import java.time.LocalDate
import kotlin.math.roundToInt

/** Eintragen: Reiter „Suchen“ (mit Mengenwahl) und „Schnell“ (nur Name + kcal). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    initialMeal: Meal,
    date: LocalDate,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit,
    vm: AddViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val c = Tokens.colors
    var meal by remember { mutableStateOf(initialMeal) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<FoodEntity?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.reset() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val food = selected
            if (food != null) {
                FoodDetail(
                    food = food,
                    meal = meal,
                    onMeal = { meal = it },
                    onFavorite = { selected = vm.toggleFavorite(food) },
                    onBack = { selected = null },
                    onAdd = { grams ->
                        vm.addFood(food, grams, meal, date)
                        onAdded(context.getString(R.string.added_to, food.name, context.getString(mealRes(meal))))
                        onDismiss()
                    },
                )
            } else {
                Text(stringResource(R.string.add_to_meal, meal.label()), style = MaterialTheme.typography.titleLarge, color = c.fg)
                TabRow(
                    selectedTabIndex = tab, containerColor = c.surface, contentColor = c.accent,
                    indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = c.accent) },
                ) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_search)) }, unselectedContentColor = c.muted)
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_quick)) }, unselectedContentColor = c.muted)
                }
                if (tab == 0) {
                    SearchTab(vm, onPick = { selected = it })
                } else {
                    QuickTab(meal, onMeal = { meal = it }) { name, kcal ->
                        vm.addQuick(name, kcal, meal, date)
                        onAdded(context.getString(R.string.quick_added, kcal))
                        onDismiss()
                    }
                }
            }
        }
    }
}

private fun mealRes(m: Meal) = m.labelRes()

@Composable
private fun SearchTab(vm: AddViewModel, onPick: (FoodEntity) -> Unit) {
    val c = Tokens.colors
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val bls by vm.blsState.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }

    OutlinedTextField(
        value = query, onValueChange = vm::setQuery,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    LazyColumn(Modifier.fillMaxWidth()) {
        if (bls == BlsImporter.State.Running) {
            item { Hint(stringResource(R.string.bls_preparing)) }
        }
        if (query.isBlank()) {
            if (favorites.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.favorites)) }
                items(favorites, key = { "f" + it.id }) { FoodRow(it) { onPick(it) } }
            }
            if (recent.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.recently_used)) }
                items(recent, key = { "r" + it.id }) { FoodRow(it) { onPick(it) } }
            }
            if (favorites.isEmpty() && recent.isEmpty()) {
                item { Hint(stringResource(R.string.search_empty_start)) }
            }
        } else {
            if (results.isEmpty()) {
                item { Hint(stringResource(R.string.search_no_results, query)) }
            }
            items(results, key = { it.id }) { FoodRow(it) { onPick(it) } }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Tokens.colors.muted,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.muted, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun FoodRow(food: FoodEntity, onClick: () -> Unit) {
    val c = Tokens.colors
    Column {
    HorizontalDivider(color = c.line)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val src = when (food.source) {
                FoodSource.BLS -> stringResource(R.string.source_bls)
                else -> stringResource(R.string.source_custom)
            }
            Text(listOfNotNull(food.brand, src).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        if (food.favorite) Icon(Icons.Filled.Favorite, contentDescription = null, tint = c.over, modifier = Modifier.padding(end = 6.dp))
        Text(
            stringResource(if (food.isLiquid) R.string.kcal_per_100ml else R.string.kcal_per_100g, food.kcal100.roundToInt()),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
    }
    }
}

private val plainAmount = Regex("""\d+ (g|ml)""")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodDetail(
    food: FoodEntity,
    meal: Meal,
    onMeal: (Meal) -> Unit,
    onFavorite: () -> Unit,
    onBack: () -> Unit,
    onAdd: (Double) -> Unit,
) {
    val c = Tokens.colors
    val portions = remember(food.id) { PortionGuesser.portionsFor(food.name, food.isLiquid) }
    var gramsText by remember(food.id) { mutableStateOf(NumberInput.format(portions.first().grams)) }
    val grams = NumberInput.parse(gramsText)
    val amount = grams?.let { Amount.of(food.per100, it) }
    val unit = stringResource(if (food.isLiquid) R.string.unit_ml else R.string.unit_g)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.titleLarge, color = c.fg)
            Text(
                stringResource(if (food.isLiquid) R.string.kcal_per_100ml else R.string.kcal_per_100g, food.kcal100.roundToInt()) +
                    " · " + stringResource(if (food.source == FoodSource.BLS) R.string.source_bls else R.string.source_custom),
                style = MaterialTheme.typography.bodySmall, color = c.muted,
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (food.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(if (food.favorite) R.string.unfavorite else R.string.favorite),
                tint = if (food.favorite) c.over else c.muted,
            )
        }
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(amount?.let { Nutrition.format(it.kcal) } ?: "–", style = MaterialTheme.typography.displaySmall, color = c.fg)
        Text(" kcal", style = MaterialTheme.typography.bodyLarge, color = c.muted)
    }
    if (amount != null) {
        Text(
            stringResource(R.string.macros_line, amount.protein.roundToInt(), amount.carbs.roundToInt(), amount.fat.roundToInt()),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        portions.forEach { p ->
            // „50 g“ steht für sich, „1 EL“ bekommt die Menge dazu: „1 EL (10 g)“
            val label = if (p.label.matches(plainAmount)) p.label else "${p.label} (${NumberInput.format(p.grams)} $unit)"
            AppChip(label, grams == p.grams) { gramsText = NumberInput.format(p.grams) }
        }
    }
    OutlinedTextField(
        value = gramsText, onValueChange = { gramsText = it },
        label = { Text(stringResource(R.string.amount_in, unit)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.meal), style = MaterialTheme.typography.bodySmall, color = c.muted)
    MealChips(meal, onMeal)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
            onClick = onBack, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.back)) }
        Button(
            onClick = { grams?.let(onAdd) }, enabled = (grams ?: 0.0) > 0, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.add)) }
    }
}

@Composable
private fun QuickTab(meal: Meal, onMeal: (Meal) -> Unit, onAdd: (String, Int) -> Unit) {
    val c = Tokens.colors
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    val defaultName = stringResource(R.string.quick_default_name)
    Text(stringResource(R.string.quick_hint), style = MaterialTheme.typography.bodyMedium, color = c.muted)
    OutlinedTextField(
        value = name, onValueChange = { name = it.take(60) }, label = { Text(stringResource(R.string.quick_name)) },
        placeholder = { Text(defaultName) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = kcal, onValueChange = { kcal = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.quick_kcal)) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.meal), style = MaterialTheme.typography.bodySmall, color = c.muted)
    MealChips(meal, onMeal)
    val k = kcal.toIntOrNull() ?: 0
    Button(
        onClick = { onAdd(name.trim().ifEmpty { defaultName }, k) }, enabled = k > 0, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
    ) { Text(stringResource(R.string.add)) }
}
