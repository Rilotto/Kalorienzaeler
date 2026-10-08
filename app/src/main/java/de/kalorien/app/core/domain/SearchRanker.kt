package de.kalorien.app.core.domain

/** Was die Suche über ein Lebensmittel wissen muss. */
data class SearchCandidate(
    val id: Long,
    /** normalisierter Name (+ Marke), siehe [TextNormalizer] */
    val name: String,
    val favorite: Boolean,
    val useCount: Int,
    val isOwn: Boolean,
)

/**
 * Sortiert Suchtreffer. Alle Suchwörter müssen vorkommen (auch zusammengeschrieben: „haferflocken“ findet
 * „Hafer Flocken“). Vorne stehen Favoriten, oft Genutztes, eigene Lebensmittel und Grundprodukte
 * („Banane roh“ vor „Bananenkuchen“). Getestet mit den echten BLS-Namen.
 */
object SearchRanker {
    /** Alltagswörter → so heißt es im BLS. Leicht erweiterbar. */
    private val synonyms = mapOf(
        "nudel" to listOf("teigwaren"), "nudeln" to listOf("teigwaren"), "pasta" to listOf("teigwaren"),
        "spaghetti" to listOf("teigwaren"), "penne" to listOf("teigwaren"), "makkaroni" to listOf("teigwaren"),
        "ei" to listOf("huehnerei"), "eier" to listOf("huehnerei"), "spiegelei" to listOf("huehnerei gebraten"),
        "quark" to listOf("speisequark"), "magerquark" to listOf("speisequark magerstufe"),
        "milch" to listOf("vollmilch"),
        "pommes" to listOf("pommes frites"), "kaffee" to listOf("kaffee getraenk"), "toast" to listOf("toastbrot"),
    )
    private val baseWords = setOf("roh", "frisch", "gekocht", "pasteurisiert", "natur")
    private val sideWords = listOf("dip", "dressing", "sauce", "creme", "kuchen", "torte", "plunder", "riegel", "kekse", "plaetzchen", "auflauf", "suppe")

    fun alternatives(token: String): List<String> = listOf(token) + synonyms[token].orEmpty()

    /** Begriffe für die Datenbank-Vorauswahl (längstes Suchwort und seine Synonyme). */
    fun prefilterTerms(query: String): List<String> {
        val tokens = TextNormalizer.tokens(query)
        if (tokens.isEmpty()) return emptyList()
        return alternatives(tokens.maxBy { it.length }).map { alt -> alt.split(' ').maxBy { it.length } }.distinct()
    }

    fun rank(query: String, candidates: List<SearchCandidate>, limit: Int = 60): List<SearchCandidate> {
        val tokens = TextNormalizer.tokens(query)
        if (tokens.isEmpty()) return emptyList()
        return candidates
            .mapNotNull { c ->
                val compact = c.name.replace(" ", "")
                val mapped = tokens.map { t -> match(t, compact) ?: return@mapNotNull null }
                c to score(tokens, mapped, c, compact)
            }
            .sortedWith(compareByDescending<Pair<SearchCandidate, Int>> { it.second }.thenBy { it.first.name.length })
            .take(limit)
            .map { it.first }
    }

    /** Synonyme zuerst prüfen, damit „Ei“ bei „Hühnerei gekocht“ als Hühnerei zählt. */
    private fun match(token: String, compact: String): String? =
        (synonyms[token].orEmpty() + token).firstOrNull { alt -> alt.split(' ').all { compact.contains(it) } }

    internal fun score(tokens: List<String>, mapped: List<String>, c: SearchCandidate, compact: String): Int {
        val words = c.name.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return Int.MIN_VALUE
        var s = 0
        if (c.favorite) s += 1000
        if (c.isOwn) s += 300
        s += (c.useCount * 20).coerceAtMost(600)

        if (compact == tokens.joinToString("") || compact == mapped.joinToString("").replace(" ", "")) s += 600
        val firstAlt = mapped.first().split(' ').first()
        s += when {
            words[0] == firstAlt && mapped.first() != tokens.first() -> 320 // Synonym trifft das Hauptwort
            words[0] == tokens.first() || words[0] == firstAlt -> 220
            compact.startsWith(firstAlt) || compact.startsWith(tokens.first()) -> 90
            else -> 0
        }
        mapped.forEach { m ->
            m.split(' ').forEach { p ->
                s += when {
                    p in words -> 60
                    words.any { it.startsWith(p) } -> 30
                    else -> 0
                }
            }
        }
        if (words.size <= 4 && words.any { it in baseWords }) s += 50
        if ("mit" in words && "mit" !in tokens) s -= 80
        val isSide = words.any { w -> sideWords.any { w.contains(it) } }
        val wantsSide = tokens.any { t -> sideWords.any { t.contains(it) } }
        if (isSide && !wantsSide) s -= 60
        s -= words.size * 12
        return s
    }
}
