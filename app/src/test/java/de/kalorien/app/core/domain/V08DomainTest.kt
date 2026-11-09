package de.kalorien.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V08DomainTest {
    /** Typische Mensa-Tabelle: zwei Spalten, Portionsgewicht in der Überschrift. */
    private val mensa = """
        Hähnchen-Curry mit Basmatireis
        Nährwerte pro 100 g pro Portion (350 g)
        Energie 520 kJ / 124 kcal 1820 kJ / 435 kcal
        Fett 4,2 g 14,7 g
        davon gesättigte Fettsäuren 1,1 g 3,9 g
        Kohlenhydrate 15 g 52,5 g
        davon Zucker 2,0 g 7,0 g
        Eiweiß 6,1 g 21,4 g
        Salz 0,4 g 1,4 g
    """.trimIndent()

    @Test
    fun readsBothColumns() {
        val p = NutritionTextParser.parse(mensa)
        assertEquals(2, p.columns.size)
        val per100 = p.column(NutritionBasis.PER_100G)!!
        val portion = p.column(NutritionBasis.PER_PORTION)!!
        assertEquals(124.0, per100.kcal!!, 0.01)
        assertEquals(435.0, portion.kcal!!, 0.01)
        assertEquals(14.7, portion.fat!!, 0.01) // nicht die gesättigten Fettsäuren
        assertEquals(52.5, portion.carbs!!, 0.01)
        assertEquals(7.0, portion.sugar!!, 0.01)
        assertEquals(21.4, portion.protein!!, 0.01)
        assertEquals(350.0, p.portionGrams!!, 0.01)
        assertEquals("Hähnchen-Curry mit Basmatireis", p.name)
        assertEquals(435.0, p.perPortion()!!.kcal!!, 0.01)
    }

    @Test
    fun portionColumnFirstAndWeightFromKcal() {
        val text = """
            Pro Portion | pro 100 g
            Brennwert 559 kcal 160 kcal
            Fett 20 g 5,7 g
            Kohlenhydrate 70 g 20 g
            Eiweiß 25 g 7,1 g
        """.trimIndent()
        val p = NutritionTextParser.parse(text)
        assertEquals(NutritionBasis.PER_PORTION, p.columns.first().basis)
        assertEquals(559.0, p.column(NutritionBasis.PER_PORTION)!!.kcal!!, 0.01)
        assertEquals(160.0, p.column(NutritionBasis.PER_100G)!!.kcal!!, 0.01)
        assertEquals(349.0, p.portionGrams!!, 0.5) // 559 / 160 × 100
    }

    @Test
    fun singleColumnMonitorCountsAsPortion() {
        val p = NutritionTextParser.parse("Spaghetti Bolognese\n745 kcal\nEiweiß 32 g\nKohlenhydrate 98 g\nFett 22 g")
        assertTrue(p.columns.isEmpty())
        val portion = p.perPortion()
        assertNotNull(portion)
        assertEquals(745.0, portion!!.kcal!!, 0.01)
        assertEquals(NutritionBasis.PER_PORTION, portion.basis)
    }

    @Test
    fun per100WithoutWeightHasNoPortion() {
        val p = NutritionTextParser.parse("pro 100 g\n250 kcal\nFett 10 g")
        assertNull(p.perPortion())
        assertEquals(250.0, p.per100()!!.kcal!!, 0.01)
    }

    @Test
    fun dishPerPortion() {
        // Sandwich: 2 Scheiben Brot (100 g), 1 Scheibe Gouda (25 g), 1 TL Butter (5 g)
        val parts = listOf(
            DishPart(Amount(222, 7.0, 41.0, 1.5, 2.0), 100.0),
            DishPart(Amount(89, 6.2, 0.0, 7.2, null), 25.0),
            DishPart(Amount(37, 0.0, 0.0, 4.1, 0.0), 5.0),
        )
        val one = DishCalc.perPortion(parts, 1.0)
        assertEquals(348.0, one.kcal, 0.01)
        assertEquals(12.8, one.fat, 0.01)
        assertEquals(2.0, one.sugar!!, 0.01) // nur bekannte Zuckerwerte
        assertEquals(130.0, DishCalc.gramsPerPortion(parts, 1.0)!!, 0.01)
        val half = DishCalc.perPortion(parts, 2.0)
        assertEquals(174.0, half.kcal, 0.01)
        assertEquals(65.0, DishCalc.gramsPerPortion(parts, 2.0)!!, 0.01)
        assertNull(DishCalc.total(listOf(DishPart(Amount(10, 0.0, 0.0, 0.0, null), null))).sugar)
        assertNull(DishCalc.gramsPerPortion(listOf(DishPart(Amount(10, 0.0, 0.0, 0.0), null)), 1.0))
    }

    @Test
    fun mensaPortion() {
        val p = MensaCalc.portionFrom(Per100(124.0, 6.1, 15.0, 4.2, 2.0), 350.0, null)!!
        assertEquals(434.0, p.kcal, 0.01)
        assertEquals(52.5, p.carbs, 0.01)
        val direct = Per100(435.0, 21.4, 52.5, 14.7, 7.0)
        assertEquals(direct, MensaCalc.portionFrom(Per100(124.0, 6.1, 15.0, 4.2), 350.0, direct))
        assertNull(MensaCalc.portionFrom(Per100(124.0, 6.1, 15.0, 4.2), null, null))
    }
}
