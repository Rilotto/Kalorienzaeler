package de.kalorien.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kalorien.app.R
import de.kalorien.app.core.ui.theme.Tokens

/** Platzhalter bis Block 4. */
@Composable
fun HistoryScreen() {
    val c = Tokens.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge, color = c.fg)
        Column(
            Modifier.fillMaxWidth()
                .background(c.surface, RoundedCornerShape(14.dp))
                .border(1.dp, c.line, RoundedCornerShape(14.dp))
                .padding(18.dp),
        ) {
            Text(stringResource(R.string.history_placeholder), style = MaterialTheme.typography.bodyMedium, color = c.fg)
            Text(stringResource(R.string.coming_in_block, 4), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}
