package de.kalorien.app.feature.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kalorien.app.R
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.data.TemplateWithItems
import de.kalorien.app.core.data.ThemeMode
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.ui.AppCard
import de.kalorien.app.core.ui.AppChip
import de.kalorien.app.core.ui.AppNumberField
import de.kalorien.app.core.ui.theme.Tokens
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import de.kalorien.app.core.ui.theme.Palette
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Abweichendes Kalorienziel an einzelnen Wochentagen. Leer = Standardziel. */
@Composable
fun WeekGoalsCard(s: Settings, onSave: (Map<DayOfWeek, Int>) -> Unit) {
    val c = Tokens.colors
    val initial = remember(s.weekdayGoals) { DayOfWeek.entries.associateWith { s.weekdayGoals[it]?.toString().orEmpty() } }
    var values by remember(s.weekdayGoals) { mutableStateOf(initial) }
    val parsed = values.mapNotNull { (d, v) -> v.toIntOrNull()?.let { d to it } }.toMap()
    val invalid = parsed.values.any { it !in Settings.MIN_GOAL..Settings.MAX_GOAL }
    AppCard {
        Text(stringResource(R.string.week_goals_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Text(
            stringResource(R.string.week_goals_hint, Nutrition.format(s.dailyGoal)),
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
        Spacer(Modifier.height(8.dp))
        DayOfWeek.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                pair.forEach { d ->
                    AppNumberField(
                        values[d].orEmpty(), { v -> values = values + (d to v.take(4)) },
                        d.getDisplayName(TextStyle.FULL, Locale.GERMANY), Modifier.weight(1f), integer = true,
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (invalid) {
            Text(
                stringResource(R.string.goal_invalid, Settings.MIN_GOAL, Settings.MAX_GOAL),
                style = MaterialTheme.typography.bodySmall, color = c.over,
            )
        }
        Row {
            TextButton(onClick = { values = DayOfWeek.entries.associateWith { "" }; onSave(emptyMap()) }) {
                Text(stringResource(R.string.week_goals_clear), color = c.accent)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onSave(parsed) }, enabled = !invalid && values != initial,
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.surface),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

/** Hell/Dunkel und Start-Reiter beim Eintragen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceCard(s: Settings, onTheme: (ThemeMode) -> Unit, onStartTab: (Int) -> Unit, onPalette: (Palette) -> Unit) {
    val c = Tokens.colors
    val dark = when (s.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    AppCard {
        Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.palette_label), style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Palette.entries.forEach { p -> PaletteSwatch(p, dark, selected = s.palette == p) { onPalette(p) } }
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.bodySmall, color = c.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                .forEach { (m, res) -> AppChip(stringResource(res), s.themeMode == m) { onTheme(m) } }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.start_tab_label), style = MaterialTheme.typography.bodySmall, color = c.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.tab_search, R.string.tab_scan, R.string.tab_photo, R.string.tab_quick)
                .forEachIndexed { i, res -> AppChip(stringResource(res), s.startTab == i) { onStartTab(i) } }
        }
    }
}

/** Kleine Vorschau eines Farbschemas: Hintergrund, Hauptfarbe, Akzent, Makro-Farben. */
@Composable
private fun PaletteSwatch(p: Palette, dark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = Tokens.colors
    val t = p.colors(dark)
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.width(96.dp).clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.line, shape)
            .background(t.bg).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(t.accent, t.accentSoft, t.over, t.protein).forEach { col ->
                Box(Modifier.size(16.dp).background(col, CircleShape).border(1.dp, t.line, CircleShape))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(paletteName(p)), style = MaterialTheme.typography.labelSmall, color = t.fg, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun paletteName(p: Palette) = when (p) {
    Palette.DEFAULT -> R.string.palette_default
    Palette.PEACH -> R.string.palette_peach
    Palette.LAVENDER -> R.string.palette_lavender
    Palette.MINT -> R.string.palette_mint
    Palette.SKY -> R.string.palette_sky
    Palette.ROSE -> R.string.palette_rose
    Palette.MEMPHIS -> R.string.palette_memphis
    Palette.VAPORWAVE -> R.string.palette_vaporwave
}

/** Gespeicherte Mahlzeit-Vorlagen ansehen und löschen. */
@Composable
fun TemplatesCard(templates: List<TemplateWithItems>, onDelete: (TemplateWithItems) -> Unit) {
    val c = Tokens.colors
    AppCard {
        Text(stringResource(R.string.templates), style = MaterialTheme.typography.titleMedium, color = c.fg)
        if (templates.isEmpty()) {
            Text(stringResource(R.string.templates_empty), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        templates.forEach { t ->
            HorizontalDivider(color = c.line, modifier = Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(t.template.name, style = MaterialTheme.typography.bodyLarge, color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.template_summary, t.items.size, Nutrition.format(t.kcal)) + " · " +
                            t.sortedItems.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onDelete(t) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = c.muted)
                }
            }
        }
    }
}

/** Einstieg in „Eigene Lebensmittel“. */
@Composable
fun OwnFoodsCard(count: Int, onOpen: () -> Unit) {
    val c = Tokens.colors
    AppCard {
        Text(stringResource(R.string.own_foods_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Text(stringResource(R.string.own_foods_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = onOpen, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
        ) { Text(stringResource(R.string.own_foods_open, count)) }
    }
}

/** Fehlerprotokoll: nach einem Absturz teilen (z. B. per Mail an sich selbst) oder löschen. */
@Composable
fun CrashLogCard(lastCrash: String?, shareUri: () -> android.net.Uri, onClear: () -> Unit) {
    val c = Tokens.colors
    val context = LocalContext.current
    AppCard {
        Text(stringResource(R.string.crash_title), style = MaterialTheme.typography.titleMedium, color = c.fg)
        Text(
            if (lastCrash == null) stringResource(R.string.crash_none) else stringResource(R.string.crash_last, lastCrash),
            style = MaterialTheme.typography.bodySmall, color = if (lastCrash == null) c.muted else c.over,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, shareUri())
                        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_subject))
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, context.getString(R.string.crash_share)))
                },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.accentSoft, contentColor = c.accent),
            ) { Text(stringResource(R.string.crash_share)) }
            Spacer(Modifier.width(8.dp))
            if (lastCrash != null) TextButton(onClick = onClear) { Text(stringResource(R.string.crash_clear), color = c.muted) }
        }
    }
}
