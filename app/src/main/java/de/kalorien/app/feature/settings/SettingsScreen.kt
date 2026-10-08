package de.kalorien.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kalorien.app.BuildConfig
import de.kalorien.app.R
import de.kalorien.app.core.ui.theme.Tokens

/** Block 0: nur „Über“. Ziel, Erinnerungen und Daten folgen in späteren Blöcken. */
@Composable
fun SettingsScreen() {
    val c = Tokens.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, color = c.fg)
        Column(
            Modifier.fillMaxWidth()
                .background(c.surface, RoundedCornerShape(14.dp))
                .border(1.dp, c.line, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, color = c.fg)
            InfoItem(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
            InfoItem(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_text))
            InfoItem(stringResource(R.string.settings_sources), stringResource(R.string.settings_sources_text))
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    val c = Tokens.colors
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = c.fg)
    }
}
