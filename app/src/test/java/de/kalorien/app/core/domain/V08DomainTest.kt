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

    // ------------------------------------------------------------------ echtes Mensa-Foto vom 09.11.
    // „Eieromlett auf Zucchini-Tomatengemüse mit Rosmarinkartoffeln“ – Monitor-Foto, Positionen aus dem Bild.
    // Reihenfolge wie ML Kit sie liefert: Blöcke spaltenweise (erst Beschriftungen, dann Zahlen je Spalte).
    private fun l(t: String, x: Int, y: Int, w: Int) = OcrLine(t, x, y, x + w, y + 23)
    private val mensaPhoto = listOf(
        l("DETAILS", 160, 155, 87), l("NÄHRWERTE", 382, 157, 133),
        l("Nährwerte je Portion (650 g)", 105, 260, 290),
        l("Brennwert (kCal)", 592, 363, 178), l("546", 655, 393, 52), l("27 %", 655, 429, 50),
        l("Fett (g)", 832, 366, 75), l("26", 852, 396, 34), l("37 %", 845, 431, 48),
        l("Kohlenhydrate (g)", 969, 369, 184), l("50", 1043, 399, 35), l("19 %", 1036, 433, 49),
        l("Eiweiß (g)", 1215, 371, 102), l("25", 1248, 401, 35), l("50 %", 1238, 435, 53),
        l("Salz (g)", 1379, 372, 77), l("2", 1407, 402, 18), l("29 %", 1391, 436, 50),
        // Beschriftungen der Tabelle (ein Block)
        l("Brennwert", 110, 647, 111), l("Fett", 110, 708, 41), l("davon gesättigte Fettsäuren", 125, 756, 294),
        l("davon einfach ungesättigte Fettsäuren", 125, 803, 402), l("davon mehrfach ungesättigte Fettsäuren", 125, 850, 423),
        l("Kohlenhydrate", 108, 894, 153), l("davon Zucker", 124, 942, 137), l("davon mehrwertige Alkohole", 124, 988, 293),
        l("davon Stärke", 123, 1034, 134), l("Eiweiß", 107, 1080, 70), l("Ballaststoffe", 107, 1127, 137), l("Salz", 107, 1173, 43),
        // Spalte „Je 100 g“
        l("Je 100 g", 1230, 555, 88), l("351 kJ", 1251, 646, 65), l("84 kcal", 1243, 673, 73), l("3,9 g", 1268, 720, 47),
        l("1,2 g", 1268, 766, 47), l("2,5 g", 1268, 812, 47), l("0,6 g", 1266, 858, 49), l("7,7 g", 1270, 904, 45),
        l("0,3 g", 1266, 950, 47), l("3,8 g", 1263, 1088, 49), l("1,3 g", 1266, 1134, 45), l("0,3 g", 1264, 1180, 47),
        // Spalte „Je Portion“
        l("Je Portion", 1818, 561, 104), l("650 g", 1864, 607, 58), l("2.287 kJ", 1838, 652, 83), l("546 kcal", 1838, 679, 83),
        l("25,6 g", 1861, 726, 60), l("7,8 g", 1874, 772, 47), l("16,3 g", 1866, 818, 55), l("4,0 g", 1872, 864, 49),
        l("50,0 g", 1858, 910, 61), l("2,1 g", 1873, 956, 46), l("24,8 g", 1857, 1094, 60), l("8,1 g", 1870, 1140, 47),
        l("1,8 g", 1870, 1186, 46),
        l("Datenschutzerklärung | Impressum", 826, 1322, 405),
    )

    @Test
    fun layoutRebuildsTableRows() {
        val rows = OcrLayout.rows(mensaPhoto).lines()
        assertTrue(rows.contains("Fett   3,9 g   25,6 g"))
        assertTrue(rows.contains("Kohlenhydrate   7,7 g   50,0 g"))
        assertTrue(rows.contains("Je 100 g   Je Portion"))
        assertTrue(rows.contains("546   26   50   25   2"))
    }

    @Test
    fun realMensaPhoto() {
        val asRead = mensaPhoto.joinToString("\n") { it.text }
        val p = NutritionTextParser.parseBest(listOf(OcrLayout.rows(mensaPhoto), asRead))
        val portion = p.perPortion()!!
        assertEquals(546.0, portion.kcal!!, 0.01)
        assertEquals(25.6, portion.fat!!, 0.01)
        assertEquals(50.0, portion.carbs!!, 0.01)
        assertEquals(2.1, portion.sugar!!, 0.01)
        assertEquals(24.8, portion.protein!!, 0.01)
        assertEquals(650.0, p.portionGrams!!, 0.01)
        val per100 = p.per100()!!
        assertEquals(84.0, per100.kcal!!, 0.01)
        assertEquals(3.9, per100.fat!!, 0.01)
        assertNull(p.name) // „Datenschutzerklärung | Impressum“ ist kein Gerichtname
    }

    @Test
    fun tilesAloneAndThousandsSeparator() {
        // nur die Kacheln oben im Bild (Tabelle abgeschnitten)
        val p = NutritionTextParser.parse("Nährwerte je Portion (650 g)\nBrennwert (kCal)   Fett (g)   Kohlenhydrate (g)   Eiweiß (g)   Salz (g)\n546   26   50   25   2\n27 %   37 %   19 %   50 %   29 %")
        val portion = p.perPortion()!!
        assertEquals(546.0, portion.kcal!!, 0.01)
        assertEquals(26.0, portion.fat!!, 0.01)
        assertEquals(50.0, portion.carbs!!, 0.01)
        assertEquals(25.0, portion.protein!!, 0.01)
        // „2.287 kJ“ = 2287 kJ ≈ 547 kcal, nicht 2,3 kJ
        assertEquals(547.0, NutritionTextParser.parse("Energie 2.287 kJ").kcal!!, 1.0)
    }
}
