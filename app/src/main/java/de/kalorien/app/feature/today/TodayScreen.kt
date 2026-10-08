package de.kalorien.app.feature.today

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.kalorien.app.R
import de.kalorien.app.core.domain.Nutrition
import de.kalorien.app.core.ui.theme.Tokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Meal(@StringRes val label: Int) {
    Breakfast(R.string.meal_breakfast), Lunch(R.string.meal_lunch),
    Dinner(R.string.meal_dinner), Snacks(R.string.meal_snacks),
}

/** Platzhalter für Block 0: zeigt Aufbau und Design der Tagesansicht ohne echte Daten. */
@Composable
fun TodayScreen(snackbar: SnackbarHostState) {
    val c = Tokens.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val goal = 2200
    val eaten = 0
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.today_title), style = MaterialTheme.typography.titleLarge, color = c.fg)
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }

        // Tagesbilanz
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface, RoundedCornerShape(14.dp))
                .border(1.dp, c.line, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(118.dp), contentAlignment = Alignment.Center) {
                val progress = Nutrition.progress(goal, eaten)
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(c.line, -90f, 360f, false, style = stroke)
                    if (progress > 0f) drawArc(c.accent, -90f, 360f * progress, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Nutrition.format(Nutrition.remaining(goal, eaten)),
                        style = MaterialTheme.typography.displaySmall, color = c.fg,
                    )
                    Text(
                        stringResource(R.string.kcal_left).uppercase(Locale.GERMANY),
                        style = MaterialTheme.typography.labelSmall, color = c.muted,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow(stringResource(R.string.goal), stringResource(R.string.kcal_value, Nutrition.format(goal)))
                StatRow(stringResource(R.string.eaten), stringResource(R.string.kcal_value, Nutrition.format(eaten)))
                StatRow(stringResource(R.string.entries), "0")
            }
        }

        // Mahlzeiten
        Meal.entries.forEach { meal ->
            val label = stringResource(meal.label)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, c.line, RoundedCornerShape(14.dp)),
            ) {
                Row(
                    Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = c.fg, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(
                        onClick = {
                            scope.launch {
                                snackbar.showSnackbar(context.getString(R.string.coming_in_block, 1))
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = c.accentSoft, contentColor = c.accent,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_meal, label))
                    }
                }
                Text(
                    stringResource(R.string.meal_empty),
                    style = MaterialTheme.typography.bodySmall, color = c.muted,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val c = Tokens.colors
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.fg, textAlign = TextAlign.End)
    }
}
