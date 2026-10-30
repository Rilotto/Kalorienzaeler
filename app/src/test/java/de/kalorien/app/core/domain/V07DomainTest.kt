package de.kalorien.app.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class V07DomainTest {
    private val today = LocalDate.of(2026, 10, 21) // Mittwoch

    @Test
    fun weeksStartMondayAndAverageLoggedDays() {
        val totals = listOf(
            DayTotal(LocalDate.of(2026, 10, 19), 2000, entries = 3), // Mo diese Woche
            DayTotal(LocalDate.of(2026, 10, 20), 2400, entries = 3), // Di
            DayTotal(today, 300, entries = 1),                         // heute (läuft noch)
            DayTotal(LocalDate.of(2026, 10, 12), 1800, entries = 2),   // Mo letzte Woche
        )
        val b = HistoryBuckets.build(today, HistoryGrouping.WEEKS, 3, totals) { 2200 }
        assertEquals(3, b.size)
        assertEquals(LocalDate.of(2026, 10, 19), b.last().start)
        assertEquals(today, b.last().end) // laufende Woche endet heute
        assertEquals(3, b.last().days)
        assertEquals(2200, b.last().kcal) // (2000 + 2400) / 2, heute nicht
        assertEquals(3, b.last().loggedDays)
        assertEquals(1800, b[1].kcal)
        assertNull(b[0].kcal)
        assertFalse(b[0].logged)
        assertEquals(LocalDate.of(2026, 10, 5), HistoryBuckets.firstDay(today, HistoryGrouping.WEEKS, 3))
    }

    @Test
    fun monthsUseCalendarMonthsAndGoalAverage() {
        val totals = listOf(DayTotal(LocalDate.of(2026, 9, 30), 2600, entries = 4))
        val b = HistoryBuckets.build(today, HistoryGrouping.MONTHS, 2, totals) { if (it.dayOfWeek.value >= 6) 2600 else 2000 }
        assertEquals(LocalDate.of(2026, 9, 1), b[0].start)
        assertEquals(LocalDate.of(2026, 9, 30), b[0].end)
        assertEquals(30, b[0].days)
        assertEquals(2600, b[0].kcal)
        assertTrue(b[0].goal in 2001..2599) // Mischung aus Wochentag- und Wochenendziel
        assertEquals(21, b[1].days)
        assertEquals(LocalDate.of(2026, 9, 1), HistoryBuckets.firstDay(today, HistoryGrouping.MONTHS, 2))
    }

    @Test
    fun daysAreSingleDays() {
        val b = HistoryBuckets.build(today, HistoryGrouping.DAYS, 7, listOf(DayTotal(today, 900, entries = 2))) { 2000 }
        assertEquals(7, b.size)
        assertTrue(b.all { it.start == it.end && it.days == 1 })
        assertEquals(900, b.last().kcal)
    }
}
