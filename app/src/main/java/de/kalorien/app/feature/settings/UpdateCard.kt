package de.kalorien.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.kalorien.app.BuildConfig
import de.kalorien.app.R
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.theme.Tokens
import de.kalorien.app.core.update.ApkInstaller
import kotlin.math.roundToInt

/** Karte „Updates“: Knopf → prüfen → herunterladen → Android-Installer. */
@Composable
fun UpdateCard(
    state: UpdateState,
    includeTestVersions: Boolean,
    onIncludeTestVersions: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onDownload: (de.kalorien.app.core.update.ReleaseInfo) -> Unit,
) {
    val c = Tokens.colors
    val context = LocalContext.current
    // Nach Rückkehr aus den Android-Einstellungen neu prüfen, ob Installieren erlaubt ist
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val canInstall = remember(resumes) { ApkInstaller.canInstall(context) }

    // Fertig geladen + Erlaubnis da → Installer automatisch öffnen (einmal pro Download)
    LaunchedEffect(state, canInstall) {
        if (state is UpdateState.Ready && canInstall) ApkInstaller.install(context, state.file)
    }

    AppCard {
        Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Text(
            stringResource(R.string.update_installed, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (state) {
                UpdateState.Idle -> {}
                UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
                    Text(stringResource(R.string.update_checking), color = c.fg, style = MaterialTheme.typography.bodyMedium)
                }
                UpdateState.UpToDate -> Text(stringResource(R.string.update_up_to_date), color = c.accent, style = MaterialTheme.typography.bodyMedium)
                is UpdateState.Available -> {
                    Text(stringResource(R.string.update_available, state.release.tag), color = c.fg, style = MaterialTheme.typography.bodyLarge)
                    val notes = state.release.notes.lines().map { it.trim() }.filter { it.startsWith("- ") }.take(6)
                    if (notes.isNotEmpty()) {
                        Text(notes.joinToString("\n") { "• " + it.removePrefix("- ") }, color = c.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is UpdateState.Downloading -> {
                    Text(stringResource(R.string.update_downloading, (state.progress * 100).roundToInt()), color = c.fg, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = c.accent, trackColor = c.line, drawStopIndicator = {}, gapSize = 0.dp,
                    )
                }
                is UpdateState.Ready -> Text(
                    stringResource(if (canInstall) R.string.update_ready else R.string.update_need_permission),
                    color = c.fg, style = MaterialTheme.typography.bodyMedium,
                )
                is UpdateState.Failed -> Text(stringResource(R.string.update_failed, state.message), color = c.over, style = MaterialTheme.typography.bodyMedium)
            }

            val buttonColors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface)
            when (state) {
                is UpdateState.Available -> Button(onClick = { onDownload(state.release) }, colors = buttonColors, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.update_download))
                }
                is UpdateState.Ready -> if (canInstall) {
                    Button(onClick = { ApkInstaller.install(context, state.file) }, colors = buttonColors, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.update_install))
                    }
                } else {
                    Button(onClick = { ApkInstaller.openPermissionSettings(context) }, colors = buttonColors, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.update_allow))
                    }
                }
                UpdateState.Checking, is UpdateState.Downloading -> {}
                else -> Button(onClick = onCheck, colors = buttonColors, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.update_check))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.update_test_versions), style = MaterialTheme.typography.bodyLarge, color = c.fg)
                    Text(stringResource(R.string.update_test_versions_sub), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Switch(
                    checked = includeTestVersions, onCheckedChange = onIncludeTestVersions,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.surface),
                )
            }
        }
    }
}
