package de.kalorien.app.core.domain

import java.util.Locale

/** Macht Texte vergleichbar: klein, ohne Umlaute/Sonderzeichen. „Käse, Gouda“ → „kaese gouda“. */
object TextNormalizer {
    private val nonWord = Regex("[^a-z0-9]+")

    fun normalize(text: String): String = text
        .lowercase(Locale.GERMAN)
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .replace("é", "e").replace("è", "e").replace("à", "a").replace("ç", "c")
        .replace(nonWord, " ")
        .trim()

    fun tokens(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }
}
