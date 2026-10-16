package de.kalorien.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeTest {
    @Test fun validEans() {
        assertTrue(Barcode.isValid("3017624010701")) // Nutella
        assertTrue(Barcode.isValid("5449000000996")) // Coca-Cola
        assertTrue(Barcode.isValid("96385074")) // EAN-8
    }

    @Test fun wrongChecksumOrLengthIsRejected() {
        assertFalse(Barcode.isValid("3017624010702"))
        assertFalse(Barcode.isValid("12345"))
        assertFalse(Barcode.isValid("30176240107a1"))
    }
}

class NutritionUnitsTest {
    @Test fun kj() = assertEquals(100.0, NutritionUnits.kjToKcal(418.4), 0.001)

    @Test fun liquidQuantity() {
        assertTrue(NutritionUnits.isLiquidQuantity("330 ml"))
        assertTrue(NutritionUnits.isLiquidQuantity("1,5 L"))
        assertFalse(NutritionUnits.isLiquidQuantity("400 g"))
        assertFalse(NutritionUnits.isLiquidQuantity(null))
    }
}

class NutritionTextParserTest {
    @Test fun canteenSign() {
        val p = NutritionTextParser.parse(
            "Hähnchengeschnetzeltes in Rahmsauce\nmit Reis\nEnergie: 2650 kJ / 634 kcal\nEiweiß: 38,5 g\nKohlenhydrate: 61 g\nFett: 24,2 g\n",
        )
        assertEquals("Hähnchengeschnetzeltes in Rahmsauce", p.name)
        assertEquals(634.0, p.kcal!!, 0.0)
        assertEquals(38.5, p.protein!!, 0.0)
        assertEquals(61.0, p.carbs!!, 0.0)
        assertEquals(24.2, p.fat!!, 0.0)
    }

    @Test fun packageTableSkipsSubLines() {
        val p = NutritionTextParser.parse(
            "Nährwerte pro 100 g\nBrennwert 1465 kJ\n348 kcal\nFett 6,7 g\ndavon gesättigte Fettsäuren 1,2 g\n" +
                "Kohlenhydrate 53 g\ndavon Zucker 0,7 g\nEiweiß 13 g\nSalz 0,02 g",
        )
        assertEquals(348.0, p.kcal!!, 0.0)
        assertEquals(6.7, p.fat!!, 0.0)
        assertEquals(53.0, p.carbs!!, 0.0)
        assertEquals(0.7, p.sugar!!, 0.0)
        assertEquals(NutritionBasis.PER_100G, p.basis)
    }

    @Test fun valuesInNextLine() {
        val p = NutritionTextParser.parse("Fett\n6,7g\nKohlenhydrate\n53g\nEiweiss\n13g\n348 kcaI")
        assertEquals(348.0, p.kcal!!, 0.0)
        assertEquals(13.0, p.protein!!, 0.0)
        assertEquals(6.7, p.fat!!, 0.0)
    }

    @Test fun typicalOcrMistakes() {
        val p = NutritionTextParser.parse("Gemüselasagne\n7O1 kcal\nProtein 2O g\nKH 8O g\nFett 3O g")
        assertEquals(701.0, p.kcal!!, 0.0)
        assertEquals(20.0, p.protein!!, 0.0)
        assertEquals(80.0, p.carbs!!, 0.0)
    }

    @Test fun kjOnlyIsConverted() {
        val p = NutritionTextParser.parse("Pasta\n2100 kJ")
        assertEquals(502.0, p.kcal!!, 1.0)
    }

    @Test fun nothingFound() {
        val p = NutritionTextParser.parse("Guten Appetit")
        assertNull(p.kcal)
        assertFalse(p.hasAnyValue)
        assertEquals(NutritionBasis.UNKNOWN, p.basis)
    }
}
