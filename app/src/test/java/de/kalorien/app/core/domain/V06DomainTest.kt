package de.kalorien.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class V06DomainTest {
    private val today = LocalDate.of(2026, 10, 16) // Freitag

    @Test
    fun historyFillsGapsAndAveragesWithoutToday() {
        val totals = listOf(
            DayTotal(today.minusDays(2), 2000, entries = 4),
            DayTotal(today.minusDays(1), 2400, entries = 5),
            DayTotal(today, 500, entries = 1),
        )
        val s = History.summarize(today, 7, totals) { 2200 }
        assertEquals(7, s.days.size)
        assertEquals(today.minusDays(6), s.days.first().total.date)
        assertEquals(today, s.days.last().total.date)
        assertEquals(2200, s.average) // (2000 + 2400) / 2, heute zählt nicht
        assertEquals(3, s.loggedDays)
        assertEquals(1, s.overDays)
        assertFalse(s.days[0].logged)
    }

    @Test
    fun historyOnlyTodayUsesToday() {
        val s = History.summarize(today, 7, listOf(DayTotal(today, 800, entries = 2))) { 2000 }
        assertEquals(800, s.average)
    }

    @Test
    fun historyEmptyHasNoAverage() {
        assertNull(History.summarize(today, 14, emptyList()) { 2000 }.average)
    }

    @Test
    fun historyUsesWeekdayGoals() {
        val goals = mapOf(DayOfWeek.SATURDAY to 2800)
        val s = History.summarize(today.plusDays(1), 2, emptyList()) { goals[it.dayOfWeek] ?: 2000 }
        assertEquals(listOf(2000, 2800), s.days.map { it.goal })
    }

    @Test
    fun weekGoalsRoundTrip() {
        val m = mapOf(DayOfWeek.SATURDAY to 2600, DayOfWeek.MONDAY to 1900)
        val text = WeekGoals.format(m)
        assertEquals("MONDAY=1900;SATURDAY=2600", text)
        assertEquals(m, WeekGoals.parse(text))
        assertEquals(emptyMap<DayOfWeek, Int>(), WeekGoals.parse(null))
        assertEquals(mapOf(DayOfWeek.MONDAY to 1900), WeekGoals.parse("MONDAY=1900;FOO=1;TUESDAY=x"))
    }

    @Test
    fun quickCalc() {
        assertEquals(450, QuickCalc.parse("250+120+80"))
        assertEquals(300, QuickCalc.parse("2*150"))
        assertEquals(300, QuickCalc.parse("2x150"))
        assertEquals(400, QuickCalc.parse("500 - 100"))
        assertEquals(191, QuickCalc.parse("800 kJ"))
        assertEquals(650, QuickCalc.parse("650"))
        assertEquals(650, QuickCalc.parse("650 kcal"))
        assertEquals(125, QuickCalc.parse("62,5*2"))
        assertNull(QuickCalc.parse("abc"))
        assertNull(QuickCalc.parse("250+"))
        assertNull(QuickCalc.parse(""))
        assertTrue(QuickCalc.isCalculation("250+120"))
        assertTrue(QuickCalc.isCalculation("800 kJ"))
        assertFalse(QuickCalc.isCalculation("650"))
    }

    @Test
    fun portionGuesserUsesWordEnd() {
        // Reiswaffeln sind kein gekochter Reis
        assertTrue(PortionGuesser.portionsFor("Reiswaffeln", false).none { it.label.contains("gekocht") })
        assertTrue(PortionGuesser.portionsFor("Basmatireis, gekocht", false).any { it.label.contains("gekocht") })
        assertTrue(PortionGuesser.portionsFor("Vollkornnudeln", false).any { it.grams == 200.0 })
        assertTrue(PortionGuesser.portionsFor("Tomaten", false).any { it.grams == 80.0 })
        // „Brei“ endet auf „ei“, ist aber kein Ei
        assertTrue(PortionGuesser.portionsFor("Brei", false).none { it.grams == 58.0 })
        // eigene Portion steht vorne
        assertEquals("1 Stück", PortionGuesser.portionsFor("Reiswaffeln", false, Portion("1 Stück", 16.7)).first().label)
    }
}
