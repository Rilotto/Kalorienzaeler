package de.kalorien.app.feature.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.R
import de.kalorien.app.core.data.BlsImporter
import de.kalorien.app.core.data.EntryEntity
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.FoodSource
import de.kalorien.app.core.domain.Amount
import de.kalorien.app.core.domain.Barcode
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.NumberInput
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.domain.NutritionBasis
import de.kalorien.app.core.domain.Per100
import de.kalorien.app.core.domain.Portion
import de.kalorien.app.core.domain.PortionGuesser
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.AppViewModels
import de.kalorien.app.core.ui.MealChips
import de.kalorien.app.core.ui.label
import de.kalorien.app.core.ui.labelRes
import de.kalorien.app.core.ui.theme.Tokens
import de.kalorien.app.feature.scan.BarcodeCamera
import de.kalorien.app.feature.scan.TextFromImage
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Was im Eintragen-Fenster gerade zu sehen ist. */
private sealed interface Screen {
    data class Main(val tab: Int) : Screen
    data class Detail(val food: FoodEntity) : Screen
    data class Lookup(val ean: String) : Screen
    data object Reading : Screen
    data class Custom(val prefill: CustomPrefill) : Screen
}

private const val TAB_SEARCH = 0
private const val TAB_SCAN = 1
private const val TAB_PHOTO = 2
private const val TAB_QUICK = 3

/**
 * Eintragen: Reiter „Suchen“, „Scannen“ (Barcode), „Foto“ (Nährwerte abfotografieren) und „Schnell“.
 * [onAdded] liefert den gespeicherten Eintrag für „Rückgängig“.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    initialMeal: Meal,
    date: LocalDate,
    onDismiss: () -> Unit,
    onAdded: (message: String, entry: EntryEntity) -> Unit,
    vm: AddViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val c = Tokens.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var meal by remember { mutableStateOf(initialMeal) }
    var screen by remember { mutableStateOf<Screen>(Screen.Main(TAB_SEARCH)) }
    var lastTab by remember { mutableStateOf(TAB_SEARCH) }
    // Formularstand, in den ein Foto eingelesen wird (z. B. nach unbekanntem Barcode)
    var photoBase by remember { mutableStateOf<CustomPrefill?>(null) }

    LaunchedEffect(Unit) { vm.reset() }

    // --- Foto → Texterkennung → Formular ---
    fun readImage(uri: Uri) {
        screen = Screen.Reading
        scope.launch {
            val text = runCatching { TextFromImage.read(context, uri) }.getOrDefault("")
            screen = Screen.Custom(vm.prefillFromText(text, photoBase))
        }
    }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) readImage(uri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) readImage(uri)
    }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) afterPermission?.invoke()
        afterPermission = null
    }
    fun withCamera(action: () -> Unit) {
        if (cameraGranted) action() else {
            afterPermission = action
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }
    fun takePhoto(base: CustomPrefill?) {
        photoBase = base
        withCamera {
            val uri = TextFromImage.newPhotoUri(context)
            photoUri = uri
            takePicture.launch(uri)
        }
    }
    fun pickPhoto(base: CustomPrefill?) {
        photoBase = base
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun added(entry: EntryEntity) {
        onAdded(context.getString(R.string.added_to, entry.name, context.getString(meal.labelRes())), entry)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val s = screen) {
                is Screen.Detail -> Scrollable {
                    FoodDetail(
                        food = s.food,
                        meal = meal,
                        onMeal = { meal = it },
                        onFavorite = { screen = Screen.Detail(vm.toggleFavorite(s.food)) },
                        onBack = { screen = Screen.Main(lastTab) },
                        onAdd = { amount ->
                            if (s.food.isPortionFood) vm.addPortions(s.food, amount, meal, date, ::added)
                            else vm.addFood(s.food, amount, meal, date, ::added)
                        },
                    )
                }
                is Screen.Lookup -> {
                    LaunchedEffect(s.ean) {
                        screen = when (val r = vm.lookupBarcode(s.ean)) {
                            is BarcodeResult.Found -> Screen.Detail(r.food)
                            is BarcodeResult.NeedsData -> Screen.Custom(r.prefill)
                        }
                    }
                    Busy(stringResource(R.string.scan_looking_up, s.ean))
                }
                Screen.Reading -> Busy(stringResource(R.string.photo_reading))
                is Screen.Custom -> Scrollable {
                    CustomFoodForm(
                        prefill = s.prefill,
                        onBack = { screen = Screen.Main(lastTab) },
                        onPhoto = { base -> takePhoto(base) },
                        onGallery = { base -> pickPhoto(base) },
                        onSave = { name, brand, basis, per, serving, favorite ->
                            scope.launch {
                                val food = vm.saveCustom(name, brand, basis, per, serving, s.prefill.ean, favorite)
                                screen = Screen.Detail(food)
                            }
                        },
                    )
                }
                is Screen.Main -> {
                    Text(stringResource(R.string.add_to_meal, meal.label()), style = MaterialTheme.typography.titleLarge, color = c.fg)
                    val tabs = listOf(R.string.tab_search, R.string.tab_scan, R.string.tab_photo, R.string.tab_quick)
                    TabRow(
                        selectedTabIndex = s.tab, containerColor = c.surface, contentColor = c.accent,
                        indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[s.tab]), color = c.accent) },
                    ) {
                        tabs.forEachIndexed { i, res ->
                            Tab(
                                selected = s.tab == i,
                                onClick = { lastTab = i; screen = Screen.Main(i) },
                                text = { Text(stringResource(res), maxLines = 1) },
                                unselectedContentColor = c.muted,
                            )
                        }
                    }
                    when (s.tab) {
                        TAB_SEARCH -> SearchTab(
                            vm,
                            onPick = { screen = Screen.Detail(it) },
                            onCreate = { name -> screen = Screen.Custom(CustomPrefill(name = name)) },
                        )
                        TAB_SCAN -> Scrollable {
                            ScanTab(
                                cameraGranted = cameraGranted,
                                onRequestCamera = { withCamera {} },
                                onCode = { code -> screen = Screen.Lookup(code) },
                            )
                        }
                        TAB_PHOTO -> Scrollable {
                            PhotoTab(
                                onPhoto = { takePhoto(null) },
                                onGallery = { pickPhoto(null) },
                                onManual = { screen = Screen.Custom(CustomPrefill(basis = NutritionBasis.PER_PORTION)) },
                            )
                        }
                        else -> Scrollable {
                            QuickTab(meal, onMeal = { meal = it }) { name, kcal ->
                                vm.addQuick(name, kcal, meal, date) { entry ->
                                    onAdded(context.getString(R.string.quick_added, kcal), entry)
                                    onDismiss()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Inhalt, der bei offener Tastatur scrollbar bleiben muss (Formulare). */
@Composable
private fun Scrollable(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Busy(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Tokens.colors.accent)
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.fg)
    }
}

// ---------------------------------------------------------------- Suchen

@Composable
private fun SearchTab(vm: AddViewModel, onPick: (FoodEntity) -> Unit, onCreate: (String) -> Unit) {
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
        item { CreateOwnRow { onCreate(query.trim()) } }
    }
}

@Composable
private fun CreateOwnRow(onClick: () -> Unit) {
    val c = Tokens.colors
    Column {
        HorizontalDivider(color = c.line)
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = c.accent)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.custom_create), color = c.accent, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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
private fun sourceLabel(food: FoodEntity): String = stringResource(
    when (food.source) {
        FoodSource.BLS -> R.string.source_bls
        FoodSource.OFF -> R.string.source_off
        else -> R.string.source_custom
    },
)

@Composable
private fun FoodRow(food: FoodEntity, onClick: () -> Unit) {
    val c = Tokens.colors
    Column {
        HorizontalDivider(color = c.line)
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(food.brand, sourceLabel(food)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            if (food.favorite) Icon(Icons.Filled.Favorite, contentDescription = null, tint = c.over, modifier = Modifier.padding(end = 6.dp))
            Text(per100Label(food), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

@Composable
private fun per100Label(food: FoodEntity): String = when {
    food.isPortionFood -> stringResource(R.string.kcal_per_portion, food.kcal100.roundToInt())
    food.isLiquid -> stringResource(R.string.kcal_per_100ml, food.kcal100.roundToInt())
    else -> stringResource(R.string.kcal_per_100g, food.kcal100.roundToInt())
}

// ---------------------------------------------------------------- Scannen

@Composable
private fun ScanTab(cameraGranted: Boolean, onRequestCamera: () -> Unit, onCode: (String) -> Unit) {
    val c = Tokens.colors
    var torch by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { if (!cameraGranted) onRequestCamera() }

    if (cameraGranted) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(14.dp)).background(c.fg)) {
            BarcodeCamera(torchOn = torch, onCode = onCode, modifier = Modifier.fillMaxSize())
            // Zielrahmen
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth(0.8f).height(2.dp).background(c.over),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.scan_hint), style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.weight(1f))
            TextButton(onClick = { torch = !torch }) {
                Text(stringResource(if (torch) R.string.scan_torch_off else R.string.scan_torch_on), color = c.accent)
            }
        }
    } else {
        Text(stringResource(R.string.scan_permission), style = MaterialTheme.typography.bodyMedium, color = c.fg)
        Button(
            onClick = onRequestCamera, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.camera_allow)) }
    }

    Text(stringResource(R.string.scan_manual), style = MaterialTheme.typography.bodySmall, color = c.muted)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = manual, onValueChange = { manual = it.filter(Char::isDigit).take(14) },
            label = { Text(stringResource(R.string.scan_ean)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
            isError = manual.length >= 8 && !Barcode.isValid(manual),
        )
        Button(
            onClick = { onCode(manual) }, enabled = Barcode.isValid(manual),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.scan_lookup)) }
    }
}

// ---------------------------------------------------------------- Foto

@Composable
private fun PhotoTab(onPhoto: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    val c = Tokens.colors
    Text(stringResource(R.string.photo_hint), style = MaterialTheme.typography.bodyMedium, color = c.fg)
    Text(stringResource(R.string.photo_tip), style = MaterialTheme.typography.bodySmall, color = c.muted)
    Button(
        onClick = onPhoto, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
    ) { Text(stringResource(R.string.photo_take)) }
    FilledTonalButton(
        onClick = onGallery, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
    ) { Text(stringResource(R.string.photo_gallery)) }
    TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.photo_manual), color = c.accent)
    }
}

// ---------------------------------------------------------------- Eigenes Lebensmittel

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomFoodForm(
    prefill: CustomPrefill,
    onBack: () -> Unit,
    onPhoto: (CustomPrefill) -> Unit,
    onGallery: (CustomPrefill) -> Unit,
    onSave: (name: String, brand: String, basis: NutritionBasis, per: Per100, serving: Double?, favorite: Boolean) -> Unit,
) {
    val c = Tokens.colors
    fun fmt(d: Double?) = d?.let(NumberInput::format).orEmpty()
    var name by remember(prefill) { mutableStateOf(prefill.name) }
    var brand by remember(prefill) { mutableStateOf(prefill.brand) }
    var basis by remember(prefill) { mutableStateOf(if (prefill.basis == NutritionBasis.UNKNOWN) NutritionBasis.PER_100G else prefill.basis) }
    var kcal by remember(prefill) { mutableStateOf(prefill.kcal?.roundToInt()?.toString().orEmpty()) }
    var protein by remember(prefill) { mutableStateOf(fmt(prefill.protein)) }
    var carbs by remember(prefill) { mutableStateOf(fmt(prefill.carbs)) }
    var fat by remember(prefill) { mutableStateOf(fmt(prefill.fat)) }
    var serving by remember(prefill) { mutableStateOf(fmt(prefill.servingGrams)) }
    var favorite by remember(prefill) { mutableStateOf(prefill.basis == NutritionBasis.PER_PORTION) }

    fun current() = prefill.copy(
        name = name, brand = brand, basis = basis, kcal = NumberInput.parse(kcal), protein = NumberInput.parse(protein),
        carbs = NumberInput.parse(carbs), fat = NumberInput.parse(fat), servingGrams = NumberInput.parse(serving),
    )

    Text(stringResource(R.string.custom_title), style = MaterialTheme.typography.titleLarge, color = c.fg)
    prefill.note?.let { note ->
        val res = when (note) {
            CustomPrefill.Note.BarcodeUnknown -> R.string.note_barcode_unknown
            CustomPrefill.Note.BarcodeIncomplete -> R.string.note_barcode_incomplete
            CustomPrefill.Note.PhotoRead -> R.string.note_photo_read
            CustomPrefill.Note.PhotoNothingFound -> R.string.note_photo_nothing
            CustomPrefill.Note.Offline -> R.string.note_offline
        }
        Text(
            stringResource(res), style = MaterialTheme.typography.bodySmall, color = c.fg,
            modifier = Modifier.fillMaxWidth().background(c.accentSoft, RoundedCornerShape(10.dp)).padding(12.dp),
        )
    }
    OutlinedTextField(
        value = name, onValueChange = { name = it.take(80) }, label = { Text(stringResource(R.string.custom_name)) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = brand, onValueChange = { brand = it.take(40) }, label = { Text(stringResource(R.string.custom_brand)) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.custom_basis), style = MaterialTheme.typography.bodySmall, color = c.muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppChip(stringResource(R.string.basis_100g), basis == NutritionBasis.PER_100G) { basis = NutritionBasis.PER_100G }
        AppChip(stringResource(R.string.basis_100ml), basis == NutritionBasis.PER_100ML) { basis = NutritionBasis.PER_100ML }
        AppChip(stringResource(R.string.basis_portion), basis == NutritionBasis.PER_PORTION) { basis = NutritionBasis.PER_PORTION }
    }
    NumberField(kcal, { kcal = it }, stringResource(R.string.custom_kcal), integer = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(protein, { protein = it }, stringResource(R.string.protein), Modifier.weight(1f))
        NumberField(carbs, { carbs = it }, stringResource(R.string.carbs_short), Modifier.weight(1f))
        NumberField(fat, { fat = it }, stringResource(R.string.fat), Modifier.weight(1f))
    }
    if (basis != NutritionBasis.PER_PORTION) {
        NumberField(
            serving, { serving = it },
            stringResource(if (basis == NutritionBasis.PER_100ML) R.string.custom_serving_ml else R.string.custom_serving_g),
        )
    }
    prefill.ean?.let { Text(stringResource(R.string.custom_ean, it), style = MaterialTheme.typography.bodySmall, color = c.muted) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = { onPhoto(current()) }, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.custom_photo)) }
        FilledTonalButton(
            onClick = { onGallery(current()) }, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.photo_gallery_short)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { favorite = !favorite }) {
        Checkbox(checked = favorite, onCheckedChange = { favorite = it }, colors = CheckboxDefaults.colors(checkedColor = c.accent))
        Text(stringResource(R.string.custom_favorite), style = MaterialTheme.typography.bodyMedium, color = c.fg)
    }
    val k = NumberInput.parse(kcal)
    val canSave = name.isNotBlank() && k != null && k > 0
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
            onClick = onBack, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.back)) }
        Button(
            onClick = {
                onSave(
                    name.trim(), brand.trim(), basis,
                    Per100(k ?: 0.0, NumberInput.parse(protein) ?: 0.0, NumberInput.parse(carbs) ?: 0.0, NumberInput.parse(fat) ?: 0.0),
                    NumberInput.parse(serving)?.takeIf { it > 0 }, favorite,
                )
            },
            enabled = canSave, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.custom_save)) }
    }
    if (!canSave) Text(stringResource(R.string.custom_need), style = MaterialTheme.typography.bodySmall, color = c.muted)
}

@Composable
private fun NumberField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), integer: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (!integer && (it == ',' || it == '.')) }.take(7)) },
        label = { Text(label, maxLines = 1) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        modifier = modifier,
    )
}

// ---------------------------------------------------------------- Menge wählen

/** Für Portions-Lebensmittel: Anzahl Portionen statt Gramm. */
private val portionCounts = listOf(0.5, 1.0, 1.5, 2.0)

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
    val byPortion = food.isPortionFood
    val serving = food.servingGrams?.takeIf { it > 0 }?.let { Portion(food.servingLabel ?: stringResource(R.string.one_portion), it) }
    val portions = remember(food.id, serving) { PortionGuesser.portionsFor(food.name, food.isLiquid, serving) }
    var gramsText by remember(food.id) { mutableStateOf(if (byPortion) "1" else NumberInput.format(portions.first().grams)) }
    // bei Portions-Lebensmitteln ist das die Anzahl Portionen, sonst Gramm/ml
    val grams = NumberInput.parse(gramsText)
    val amount = grams?.let { Amount.of(food.per100, if (byPortion) it * 100.0 else it) }
    val unit = stringResource(if (food.isLiquid) R.string.unit_ml else R.string.unit_g)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.titleLarge, color = c.fg)
            Text(
                listOfNotNull(food.brand, per100Label(food), sourceLabel(food)).joinToString(" · "),
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
    if (amount != null && (amount.protein + amount.carbs + amount.fat) > 0) {
        Text(
            stringResource(R.string.macros_line, amount.protein.roundToInt(), amount.carbs.roundToInt(), amount.fat.roundToInt()),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (byPortion) {
            portionCounts.forEach { n ->
                AppChip(stringResource(R.string.portion_count, NumberInput.format(n)), grams == n) { gramsText = NumberInput.format(n) }
            }
        } else {
            portions.forEach { p ->
                // „50 g“ steht für sich, „1 EL“ bekommt die Menge dazu: „1 EL (10 g)“
                val label = if (p.label.matches(plainAmount)) p.label else "${p.label} (${NumberInput.format(p.grams)} $unit)"
                AppChip(label, grams == p.grams) { gramsText = NumberInput.format(p.grams) }
            }
        }
    }
    OutlinedTextField(
        value = gramsText, onValueChange = { gramsText = it },
        label = { Text(if (byPortion) stringResource(R.string.portions_amount) else stringResource(R.string.amount_in, unit)) }, singleLine = true,
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

// ---------------------------------------------------------------- Schnell

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
