package de.kalorien.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kalorien.app.BuildConfig
import de.kalorien.app.R
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.AppViewModels
import de.kalorien.app.core.ui.theme.Tokens
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    snackbar: SnackbarHostState,
    onOpenFoods: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val c = Tokens.colors
    val settings by vm.settings.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val backupState by vm.backupState.collectAsStateWithLifecycle()
    val sugarRefresh by vm.sugarRefresh.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    val ownFoods by vm.ownFoods.collectAsStateWithLifecycle()
    val lastCrash by vm.lastCrash.collectAsStateWithLifecycle()
    val s = settings ?: return
    var goalText by remember { mutableStateOf("") }
    LaunchedEffect(s.dailyGoal) { goalText = s.dailyGoal.toString() }
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun saveGoal() {
        val g = goalText.toIntOrNull()
        if (g == null || g < Settings.MIN_GOAL || g > Settings.MAX_GOAL) {
            scope.launch { snackbar.showSnackbar(context.getString(R.string.goal_invalid, Settings.MIN_GOAL, Settings.MAX_GOAL)) }
            return
        }
        vm.setGoal(g)
        focus.clearFocus()
        scope.launch { snackbar.showSnackbar(context.getString(R.string.goal_saved, Nutrition.format(g))) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, color = c.fg,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        AppCard {
            Text(stringResource(R.string.settings_goal_section), style = MaterialTheme.typography.titleMedium, color = c.fg)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = goalText, onValueChange = { goalText = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.settings_goal_label)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { saveGoal() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { saveGoal() }, enabled = goalText != s.dailyGoal.toString(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_macros), style = MaterialTheme.typography.bodyLarge, color = c.fg)
                    Text(stringResource(R.string.settings_macros_sub), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Switch(
                    checked = s.showMacros, onCheckedChange = vm::setShowMacros,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.surface),
                )
            }
        }
        WeekGoalsCard(s) { goals ->
            vm.setWeekdayGoals(goals)
            focus.clearFocus()
            scope.launch { snackbar.showSnackbar(context.getString(R.string.week_goals_saved)) }
        }
        MacroGoalsCard(
            s = s,
            onSave = { p, c2, f, su ->
                vm.setMacroGoals(p, c2, f, su)
                scope.launch { snackbar.showSnackbar(context.getString(R.string.goals_saved)) }
            },
            onShowSugar = vm::setShowSugar,
            sugarRefresh = sugarRefresh,
            onRefreshSugar = vm::refreshSugar,
        )
        AppearanceCard(s, onTheme = vm::setThemeMode, onStartTab = vm::setStartTab)
        TemplatesCard(templates) { t ->
            vm.deleteTemplate(t)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val r = snackbar.showSnackbar(
                    context.getString(R.string.template_deleted, t.template.name), context.getString(R.string.undo),
                    duration = SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) vm.restoreTemplate(t)
            }
        }
        OwnFoodsCard(ownFoods.size, onOpenFoods)
        BackupCard(
            s = s, state = backupState, onAuto = vm::setAutoBackup, onBackupNow = vm::backupNow,
            onRestore = vm::restore, onCsv = { ready -> vm.exportCsv(ready) },
        )
        UpdateCard(
            state = update,
            includeTestVersions = s.includeTestVersions,
            onIncludeTestVersions = vm::setIncludeTestVersions,
            onCheck = vm::checkForUpdate,
            onDownload = vm::download,
        )
        CrashLogCard(lastCrash, shareUri = vm::crashShareUri, onClear = vm::clearCrashLog)
        AppCard {
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, color = c.fg)
            Spacer(Modifier.height(8.dp))
            InfoItem(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
            Spacer(Modifier.height(10.dp))
            InfoItem(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_text))
            Spacer(Modifier.height(10.dp))
            InfoItem(stringResource(R.string.settings_sources), stringResource(R.string.settings_sources_text))
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    val c = Tokens.colors
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = c.fg)
    }
}
