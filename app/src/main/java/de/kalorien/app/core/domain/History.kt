package de.kalorien.app.core.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/** Summe eines Tages (aus der Datenbank). */
data class DayTotal(
    val date: LocalDate,
    val kcal: Int,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val entries: Int = 0,
)

/** Ein Tag im Verlauf, mit Ziel. Tage ohne Einträge haben [logged] = false. */
data class HistoryDay(val total: DayTotal, val goal: Int) {
    val logged: Boolean get() = total.entries > 0
    val over: Boolean get() = logged && total.kcal > goal
}

data class HistorySummary(
    /** älteste zuerst, lückenlos bis heute */
    val days: List<HistoryDay>,
    /** Durchschnitt der Tage mit Einträgen, ohne heute (null = keine solchen Tage) */
    val average: Int?,
    val averageGoal: Int,
    val loggedDays: Int,
    val overDays: Int,
) {
    val maxKcal: Int get() = (days.maxOfOrNull { maxOf(it.total.kcal, it.goal) } ?: 0)
}

object History {
    val ranges = listOf(7, 14, 30)

    /**
     * Verlauf für die letzten [rangeDays] Tage bis einschließlich [today] zusammenstellen.
     * Fehlende Tage werden mit 0 aufgefüllt. Der laufende Tag zählt nicht zum Durchschnitt,
     * außer es gibt sonst keinen Tag mit Einträgen.
     */
    fun summarize(today: LocalDate, rangeDays: Int, totals: List<DayTotal>, goalFor: (LocalDate) -> Int): HistorySummary {
        require(rangeDays > 0)
        val byDate = totals.associateBy { it.date }
        val days = (rangeDays - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            HistoryDay(byDate[d] ?: DayTotal(d, 0), goalFor(d))
        }
        val logged = days.filter { it.logged }
        val past = logged.filter { it.total.date != today }
        val base = past.ifEmpty { logged }
        return HistorySummary(
            days = days,
            average = if (base.isEmpty()) null else base.map { it.total.kcal }.average().roundToInt(),
            averageGoal = (if (base.isEmpty()) days else base).map { it.goal }.average().roundToInt(),
            loggedDays = logged.size,
            overDays = logged.count { it.over },
        )
    }
}

/** Ziel pro Wochentag, gespeichert als Text „MONDAY=2000;SATURDAY=2600“. Fehlende Tage = Standardziel. */
object WeekGoals {
    fun parse(text: String?): Map<DayOfWeek, Int> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.split(';').mapNotNull { part ->
            val (k, v) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val day = runCatching { DayOfWeek.valueOf(k.trim()) }.getOrNull() ?: return@mapNotNull null
            val goal = v.trim().toIntOrNull() ?: return@mapNotNull null
            day to goal
        }.toMap()
    }

    fun format(goals: Map<DayOfWeek, Int>): String =
        goals.entries.sortedBy { it.key }.joinToString(";") { "${it.key.name}=${it.value}" }
}

/**
 * Schnelleintrag: einfache Rechnung erlauben, z. B. „250+120+80“ oder „2*150“,
 * und Kilojoule umrechnen („800 kJ“ → 191 kcal). Ergebnis gerundet, null = nicht lesbar.
 */
object QuickCalc {
    const val KJ_PER_KCAL = 4.184

    fun parse(input: String): Int? {
        var s = input.trim().lowercase().replace(" ", "").replace(',', '.')
        if (s.isEmpty()) return null
        val kj = s.endsWith("kj")
        s = s.removeSuffix("kj").removeSuffix("kcal")
        if (s.isEmpty() || !s.all { it.isDigit() || it in "+-*×x." }) return null
        val value = sum(s.replace('×', '*').replace('x', '*')) ?: return null
        val kcal = if (kj) value / KJ_PER_KCAL else value
        return kcal.roundToInt().takeIf { it in 0..20_000 }
    }

    /** „a+b-c“, jedes Glied darf „x*y“ sein. */
    private fun sum(s: String): Double? {
        var total = 0.0
        var sign = 1
        var term = StringBuilder()
        fun flush(): Boolean {
            val t = term.toString()
            if (t.isEmpty()) return false
            val factors = t.split('*').map { it.toDoubleOrNull() ?: return false }
            total += sign * factors.fold(1.0) { a, b -> a * b }
            term = StringBuilder()
            return true
        }
        for (ch in s) {
            if (ch == '+' || ch == '-') {
                if (!flush()) return null
                sign = if (ch == '+') 1 else -1
            } else term.append(ch)
        }
        if (!flush()) return null
        return total
    }

    /** Zeigt die Rechnung an, wenn mehr als eine Zahl eingegeben wurde („= 450 kcal“). */
    fun isCalculation(input: String): Boolean {
        val s = input.lowercase()
        return s.contains("kj") || s.trim().drop(1).any { it in "+-*×x" }
    }
}

/** Ansicht im Verlauf: einzelne Tage, Wochen (Mo–So) oder Kalendermonate. */
enum class HistoryGrouping(val counts: List<Int>, val defaultCount: Int) {
    DAYS(listOf(7, 14, 30), 7),
    WEEKS(listOf(4, 8, 12), 8),
    MONTHS(listOf(3, 6, 12), 6),
}

/** Diagrammart im Verlauf. */
enum class ChartType { BARS, LINE, AREA }

/**
 * Ein Abschnitt im Verlauf (ein Tag, eine Woche oder ein Monat).
 * [kcal] = Durchschnitt pro Tag mit Einträgen (beim Tag: die Summe), null = keine Einträge.
 */
data class HistoryBucket(
    val start: LocalDate,
    val end: LocalDate,
    val kcal: Int?,
    val goal: Int,
    val loggedDays: Int,
    val days: Int,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
) {
    val logged: Boolean get() = kcal != null
    val over: Boolean get() = kcal != null && kcal > goal
}

object HistoryBuckets {
    /**
     * [count] Abschnitte bis einschließlich [today]. Wochen beginnen montags, Monate am 1.
     * Pro Abschnitt: Durchschnitt der Tage mit Einträgen; der laufende Tag zählt nur, wenn er der einzige ist.
     */
    fun build(
        today: LocalDate,
        grouping: HistoryGrouping,
        count: Int,
        totals: List<DayTotal>,
        goalFor: (LocalDate) -> Int,
    ): List<HistoryBucket> {
        require(count > 0)
        val byDate = totals.associateBy { it.date }
        val ranges: List<Pair<LocalDate, LocalDate>> = when (grouping) {
            HistoryGrouping.DAYS -> (count - 1 downTo 0).map { today.minusDays(it.toLong()).let { d -> d to d } }
            HistoryGrouping.WEEKS -> {
                val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                (count - 1 downTo 0).map { back -> monday.minusWeeks(back.toLong()).let { it to it.plusDays(6) } }
            }
            HistoryGrouping.MONTHS -> {
                val first = today.withDayOfMonth(1)
                (count - 1 downTo 0).map { back ->
                    first.minusMonths(back.toLong()).let { it to it.plusMonths(1).minusDays(1) }
                }
            }
        }
        return ranges.map { (start, endFull) ->
            val end = if (endFull > today) today else endFull
            val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()
            val logged = dates.mapNotNull { byDate[it]?.takeIf { t -> t.entries > 0 } }
            val base = logged.filter { it.date != today }.ifEmpty { logged }
            HistoryBucket(
                start = start, end = end,
                kcal = if (base.isEmpty()) null else base.map { it.kcal }.average().roundToInt(),
                goal = dates.map(goalFor).average().roundToInt(),
                loggedDays = logged.size, days = dates.size,
                protein = if (base.isEmpty()) 0.0 else base.map { it.protein }.average(),
                carbs = if (base.isEmpty()) 0.0 else base.map { it.carbs }.average(),
                fat = if (base.isEmpty()) 0.0 else base.map { it.fat }.average(),
            )
        }
    }

    /** Erster Tag, der für [grouping]/[count] geladen werden muss. */
    fun firstDay(today: LocalDate, grouping: HistoryGrouping, count: Int): LocalDate = when (grouping) {
        HistoryGrouping.DAYS -> today.minusDays(count - 1L)
        HistoryGrouping.WEEKS -> today.minusDays((today.dayOfWeek.value - 1).toLong()).minusWeeks(count - 1L)
        HistoryGrouping.MONTHS -> today.withDayOfMonth(1).minusMonths(count - 1L)
    }
}
