package de.kalorien.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTest {
    @Test fun kcalForPortion() {
        assertEquals(186, Nutrition.kcalFor(372.0, 50.0)) // 50 g Haferflocken
        assertEquals(0, Nutrition.kcalFor(372.0, 0.0))
    }

    @Test fun remainingCanBeNegative() {
        assertEquals(1735, Nutrition.remaining(2200, 465))
        assertEquals(-117, Nutrition.remaining(2200, 2317))
    }

    @Test fun progressIsClamped() {
        assertEquals(0.5f, Nutrition.progress(2000, 1000), 0.0001f)
        assertEquals(1f, Nutrition.progress(2000, 3000), 0.0001f)
        assertEquals(0f, Nutrition.progress(0, 500), 0.0001f)
    }

    @Test fun germanFormatting() {
        assertEquals("2.200", Nutrition.format(2200))
        assertEquals("-117", Nutrition.format(-117))
    }
}
