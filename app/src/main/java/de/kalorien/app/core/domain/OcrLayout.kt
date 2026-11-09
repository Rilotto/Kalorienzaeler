package de.kalorien.app.core.domain

import kotlin.math.abs

/** Eine erkannte Textzeile mit Position im Bild (Pixel). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY: Double get() = (top + bottom) / 2.0
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

/**
 * Setzt Tabellen wieder zeilenweise zusammen.
 * Die Texterkennung liest weit auseinanderstehende Spalten oft als getrennte Blöcke
 * (erst alle Beschriftungen, dann alle Zahlen). Hier werden Zeilen, die auf gleicher Höhe liegen,
 * links nach rechts zu einer Tabellenzeile verbunden – „Fett   3,9 g   25,6 g“.
 */
object OcrLayout {
    /** Zeilen gelten als gleiche Tabellenzeile, wenn ihre Mitten weniger als dieser Anteil der Zeilenhöhe auseinander liegen. */
    private const val SAME_ROW = 0.75

    fun rows(lines: List<OcrLine>): String {
        if (lines.isEmpty()) return ""
        val h = lines.map { it.height }.sorted()[lines.size / 2].toDouble()
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            val rowCenter = row?.let { r -> r.sumOf { it.centerY } / r.size }
            if (row != null && rowCenter != null && abs(line.centerY - rowCenter) < h * SAME_ROW) row.add(line)
            else rows.add(mutableListOf(line))
        }
        return rows.joinToString("\n") { r -> r.sortedBy { it.left }.joinToString("   ") { it.text.trim() } }
    }
}
