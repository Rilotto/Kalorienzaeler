package de.kalorien.app.core.domain

/** Was die Suche über ein Lebensmittel wissen muss. */
data class SearchCandidate(
    val id: Long,
    /** normalisierter Suchtext, siehe [TextNormalizer] */
    val key: String,
    val favorite: Boolean,
    val useCount: Int,
    val isOwn: Boolean,
)

/**
 * Sortiert Suchtreffer: Alle Suchwörter müssen vorkommen. Vorne stehen Favoriten, oft Genutztes,
 * eigene Lebensmittel und Namen, die mit dem Suchwort beginnen. Kurze Namen vor langen.
 */
object SearchRanker {
    fun rank(query: String, candidates: List<SearchCandidate>, limit: Int = 60): List<SearchCandidate> {
        val tokens = TextNormalizer.tokens(query)
        if (tokens.isEmpty()) return emptyList()
        return candidates
            .filter { c -> tokens.all { c.key.contains(it) } }
            .map { it to score(tokens, it) }
            .sortedWith(compareByDescending<Pair<SearchCandidate, Int>> { it.second }.thenBy { it.first.key.length })
            .take(limit)
            .map { it.first }
    }

    internal fun score(tokens: List<String>, c: SearchCandidate): Int {
        var s = 0
        if (c.favorite) s += 1000
        if (c.isOwn) s += 300
        s += (c.useCount * 20).coerceAtMost(600)
        val words = c.key.split(' ')
        if (c.key.startsWith(tokens.first())) s += 250
        tokens.forEach { t ->
            when {
                words.any { it == t } -> s += 80
                words.any { it.startsWith(t) } -> s += 50
            }
        }
        s -= words.size * 5
        return s
    }
}
