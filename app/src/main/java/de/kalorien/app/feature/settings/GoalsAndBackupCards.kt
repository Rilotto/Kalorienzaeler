package de.kalorien.app.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kalorien.app.R
import de.kalorien.app.core.data.BackupManager
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.domain.MacroTargets
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.AppNumberField
import de.kalorien.app.core.ui.theme.Tokens
import java.time.LocalDate

/** Eigene Tagesziele für Eiweiß, Kohlenhydrate, Fett und Zucker-Limit (leer = automatisch). */
@Composable
fun MacroGoalsCard(
    s: Settings,
    onSave: (Int?, Int?, Int?, Int?) -> Unit,
    onShowSugar: (Boolean) -> Unit,
    sugarRefresh: SugarRefresh,
    onRefreshSugar: () -> Unit,
) {
    val c = Tokens.colors
    val auto = MacroTargets.fromGoal(s.dailyGoal)
    var protein by remember(s.proteinGoal) { mutableStateOf(s.proteinGoal?.toString().orEmpty()) }
    var carbs by remember(s.carbsGoal) { mutableStateOf(s.carbsGoal?.toString().orEmpty()) }
    var fat by remember(s.fatGoal) { mutableStateOf(s.fatGoal?.toString().orEmpty()) }
    var sugar by remember(s.sugarLimit) { mutableStateOf(s.sugarLimit?.toString().orEmpty()) }
    val changed = protein != s.proteinGoal?.toString().orEmpty() || carbs != s.carbsGoal?.toString().orEmpty() ||
        fat != s.fatGoal?.toString().orEmpty() || sugar != s.sugarLimit?.toString().orEmpty()

    AppCard {
        Text(stringResource(R.string.goals_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Text(
            stringResource(R.string.goals_hint, auto.protein, auto.carbs, auto.fat, auto.sugar),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppNumberField(protein, { protein = it }, stringResource(R.string.goal_protein, auto.protein), Modifier.weight(1f), integer = true)
            AppNumberField(carbs, { carbs = it }, stringResource(R.string.goal_carbs, auto.carbs), Modifier.weight(1f), integer = true)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppNumberField(fat, { fat = it }, stringResource(R.string.goal_fat, auto.fat), Modifier.weight(1f), integer = true)
            AppNumberField(sugar, { sugar = it }, stringResource(R.string.goal_sugar, auto.sugar), Modifier.weight(1f), integer = true)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { protein = ""; carbs = ""; fat = ""; sugar = ""; onSave(null, null, null, null) }) {
                Text(stringResource(R.string.goals_auto), color = c.accent)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onSave(protein.toIntOrNull(), carbs.toIntOrNull(), fat.toIntOrNull(), sugar.toIntOrNull()) },
                enabled = changed, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
            ) { Text(stringResource(R.string.save)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_sugar), style = MaterialTheme.typography.bodyLarge, color = c.fg)
                Text(stringResource(R.string.settings_sugar_sub), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Switch(
                checked = s.showSugar, onCheckedChange = onShowSugar,
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.surface),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sugar_refresh_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
        FilledTonalButton(
            onClick = onRefreshSugar, enabled = sugarRefresh != SugarRefresh.Working, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.sugar_refresh)) }
        when (sugarRefresh) {
            SugarRefresh.Working -> Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodySmall, color = c.muted)
            SugarRefresh.Offline -> Text(stringResource(R.string.sugar_refresh_offline), style = MaterialTheme.typography.bodySmall, color = c.over)
            is SugarRefresh.Done -> Text(
                pluralStringResource(R.plurals.sugar_refresh_done, sugarRefresh.found, sugarRefresh.found),
                style = MaterialTheme.typography.bodySmall, color = c.accent,
            )
            SugarRefresh.Idle -> {}
        }
    }
}

/** Sicherung: automatisch täglich, jetzt sichern, wiederherstellen, CSV teilen. */
@Composable
fun BackupCard(
    s: Settings,
    state: BackupState,
    onAuto: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onRestore: (Uri) -> Unit,
    onCsv: ((Uri) -> Unit) -> Unit,
) {
    val c = Tokens.colors
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pending = uri }
    LaunchedEffect(state) { if (state is BackupState.Restored) pending = null }

    AppCard {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Text(
            stringResource(R.string.backup_hint, BackupManager.FOLDER),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
        val last = s.lastBackup?.let {
            if (it == LocalDate.now().toString()) stringResource(R.string.backup_last_today) else stringResource(R.string.backup_last, it)
        } ?: stringResource(R.string.backup_never)
        Spacer(Modifier.height(6.dp))
        Text(last, style = MaterialTheme.typography.bodyMedium, color = c.fg)
        when (state) {
            BackupState.Working -> Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodySmall, color = c.muted)
            is BackupState.Saved -> Text(stringResource(R.string.backup_saved, state.fileName), style = MaterialTheme.typography.bodySmall, color = c.accent)
            is BackupState.Restored -> Text(stringResource(R.string.backup_restored, state.entries), style = MaterialTheme.typography.bodySmall, color = c.accent)
            is BackupState.Failed -> Text(stringResource(R.string.backup_failed, state.message), style = MaterialTheme.typography.bodySmall, color = c.over)
            BackupState.Idle -> {}
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.backup_auto), style = MaterialTheme.typography.bodyLarge, color = c.fg, modifier = Modifier.weight(1f))
            Switch(
                checked = s.autoBackup, onCheckedChange = onAuto,
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.surface),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onBackupNow, enabled = state !is BackupState.Working, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
        ) { Text(stringResource(R.string.backup_now)) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { pick.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
            ) { Text(stringResource(R.string.backup_restore)) }
            FilledTonalButton(
                onClick = {
                    onCsv { uri ->
                        val send = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.csv_share)))
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
            ) { Text(stringResource(R.string.csv_export)) }
        }
        pending?.let { uri ->
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.backup_confirm), style = MaterialTheme.typography.bodyMedium, color = c.over)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel), color = c.muted) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onRestore(uri) },
                    colors = ButtonDefaults.buttonColors(containerColor = c.over, contentColor = c.surface),
                ) { Text(stringResource(R.string.backup_restore_confirm)) }
            }
        }
    }
}
