package de.kalorien.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class MealTest {
    @Test fun mealByTime() {
        assertEquals(Meal.BREAKFAST, Meal.forTime(LocalTime.of(7, 30)))
        assertEquals(Meal.LUNCH, Meal.forTime(LocalTime.of(12, 15)))
        assertEquals(Meal.DINNER, Meal.forTime(LocalTime.of(19, 0)))
        assertEquals(Meal.SNACK, Meal.forTime(LocalTime.of(16, 0)))
        assertEquals(Meal.SNACK, Meal.forTime(LocalTime.of(23, 30)))
    }

    @Test fun unknownDbValueFallsBackToSnack() {
        assertEquals(Meal.SNACK, Meal.fromDb("KAFFEE"))
        assertEquals(Meal.DINNER, Meal.fromDb("DINNER"))
    }
}

class AmountTest {
    @Test fun amountForPortion() {
        val oats = Per100(348.0, 13.2, 53.3, 6.7)
        val a = Amount.of(oats, 50.0)
        assertEquals(174, a.kcal)
        assertEquals(6.6, a.protein, 0.01)
    }

    @Test fun plusAddsUp() {
        val s = Amount(100, 1.0, 2.0, 3.0) + Amount(50, 1.0, 1.0, 1.0)
        assertEquals(Amount(150, 2.0, 3.0, 4.0), s)
    }

    @Test fun macroTargetsFromGoal() {
        assertEquals(MacroTargets(110, 275, 73), MacroTargets.fromGoal(2200))
    }
}

class NumberInputTest {
    @Test fun parsesGermanAndEnglishDecimals() {
        assertEquals(12.5, NumberInput.parse("12,5")!!, 0.0)
        assertEquals(12.5, NumberInput.parse(" 12.5 ")!!, 0.0)
        assertNull(NumberInput.parse(""))
        assertNull(NumberInput.parse("abc"))
        assertNull(NumberInput.parse("-3"))
    }

    @Test fun formatsWithoutTrailingZero() {
        assertEquals("50", NumberInput.format(50.0))
        assertEquals("12,5", NumberInput.format(12.5))
    }
}

class TextNormalizerTest {
    @Test fun removesUmlautsAndPunctuation() {
        assertEquals("kaese gouda mittelalt", TextNormalizer.normalize("Käse, Gouda (mittelalt)"))
        assertEquals(listOf("muesli", "ss"), TextNormalizer.tokens("Müsli ß"))
    }
}

class SearchRankerTest {
    private fun c(id: Long, key: String, fav: Boolean = false, uses: Int = 0, own: Boolean = false) =
        SearchCandidate(id, key, fav, uses, own)

    @Test fun allWordsMustMatch() {
        val list = listOf(c(1, "hafer flocken"), c(2, "hafer ganzes korn roh"))
        assertEquals(listOf(1L), SearchRanker.rank("hafer flocken", list).map { it.id })
    }

    @Test fun joinedWordFindsSplitNameFirst() {
        val list = listOf(c(1, "haferflocken nussplaetzchen"), c(2, "hafer flocken"), c(3, "apfel roh"))
        assertEquals(listOf(2L, 1L), SearchRanker.rank("Haferflocken", list).map { it.id })
    }

    @Test fun favoritesAndFrequentFirst() {
        val list = listOf(c(1, "banane roh"), c(2, "banane getrocknet", fav = true), c(3, "bananenchips", uses = 1))
        assertEquals(2L, SearchRanker.rank("banane", list).first().id)
    }

    @Test fun baseProductBeforeDishes() {
        val list = listOf(c(1, "bananenkuchen ruehrmasse"), c(2, "banane roh"), c(3, "joghurt mit banane"))
        assertEquals(2L, SearchRanker.rank("banane", list).first().id)
    }

    @Test fun synonymsFindBlsNames() {
        val list = listOf(c(1, "teigwaren eifrei gekocht"), c(2, "reisnudeln gekocht"), c(3, "kartoffel schupfnudeln roh"))
        assertEquals(1L, SearchRanker.rank("Nudeln", list).first().id)
        val eggs = listOf(c(4, "ei einlauf suppeneinlage"), c(5, "huehnerei gekocht"))
        assertEquals(5L, SearchRanker.rank("Ei", eggs).first().id)
    }

    @Test fun prefilterIncludesSynonyms() {
        assertEquals(listOf("nudeln", "teigwaren"), SearchRanker.prefilterTerms("Nudeln"))
    }

    @Test fun startsWithBeatsContains() {
        val list = listOf(c(1, "schokolade banane"), c(2, "banane roh"))
        assertEquals(2L, SearchRanker.rank("banane", list).first().id)
    }

    @Test fun emptyQueryGivesNothing() {
        assertTrue(SearchRanker.rank("  ", listOf(c(1, "x"))).isEmpty())
    }
}

class PortionGuesserTest {
    @Test fun knownFoodsGetTypicalPortions() {
        assertEquals(58.0, PortionGuesser.portionsFor("Hühnerei, gekocht", false).first().grams, 0.0)
        assertEquals("1 Scheibe", PortionGuesser.portionsFor("Roggenmischbrot", false).first().label)
    }

    @Test fun eiDoesNotMatchInsideOtherWords() {
        assertTrue(PortionGuesser.portionsFor("Reis, gekocht", false).none { it.grams == 58.0 })
    }

    @Test fun alwaysOffers100() {
        assertTrue(PortionGuesser.portionsFor("Unbekanntes", false).any { it.grams == 100.0 && it.label == "100 g" })
        assertTrue(PortionGuesser.portionsFor("Apfelsaft", true).any { it.label == "100 ml" })
    }
}

class BlsRowTest {
    @Test fun parsesLine() {
        val r = BlsRow.parse("C133000\tHafer Flocken\t348\t13.2\t53.3\t6.7\t0")!!
        assertEquals("C133000", r.code)
        assertEquals(348.0, r.per100.kcal, 0.0)
        assertEquals(53.3, r.per100.carbs, 0.0)
        assertEquals(false, r.isLiquid)
    }

    @Test fun skipsHeaderAndBrokenLines() {
        assertNull(BlsRow.parse("code\tname\tkcal\tprotein\tcarbs\tfat\tliquid"))
        assertNull(BlsRow.parse("C1\tnur zwei"))
        assertNull(BlsRow.parse(""))
    }
}
