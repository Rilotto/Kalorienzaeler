package de.kalorien.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.ui.theme.Tokens

/** Weiße Karte mit feinem Rand – das Grundelement aller Bereiche. */
@Composable
fun AppCard(modifier: Modifier = Modifier, padding: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = Tokens.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .then(if (padding) Modifier.padding(16.dp) else Modifier),
        content = content,
    )
}

@Composable
fun AppChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Tokens.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = c.accentSoft,
            selectedLabelColor = c.accent,
            labelColor = c.fg,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealChips(selected: Meal, onSelect: (Meal) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Meal.entries.forEach { m -> AppChip(m.label(), m == selected) { onSelect(m) } }
    }
}
