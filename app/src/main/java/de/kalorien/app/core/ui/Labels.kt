package de.kalorien.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.kalorien.app.R
import de.kalorien.app.core.domain.Meal
import de.kalorien.app.core.domain.NumberInput

@StringRes
fun Meal.labelRes(): Int = when (this) {
    Meal.BREAKFAST -> R.string.meal_breakfast
    Meal.LUNCH -> R.string.meal_lunch
    Meal.DINNER -> R.string.meal_dinner
    Meal.SNACK -> R.string.meal_snacks
}

@Composable
fun Meal.label(): String = stringResource(labelRes())

/** „250 g“ bzw. „200 ml“ */
@Composable
fun amountText(grams: Double, isLiquid: Boolean): String =
    stringResource(if (isLiquid) R.string.amount_ml else R.string.amount_g, NumberInput.format(grams))
