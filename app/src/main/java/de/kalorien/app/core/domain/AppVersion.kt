package de.kalorien.app.core.domain

/**
 * Versionsnummer wie „0.3.0“ oder „0.3.0-rc.2“ (auch mit führendem „v“).
 * Reihenfolge: 0.3.0-rc.1 < 0.3.0-rc.2 < 0.3.0 < 0.3.1.
 */
data class AppVersion(val numbers: List<Int>, val preRelease: String?) : Comparable<AppVersion> {

    val isPreRelease: Boolean get() = preRelease != null

    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(numbers.size, other.numbers.size)) {
            val d = numbers.getOrElse(i) { 0 } - other.numbers.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return when {
            preRelease == null && other.preRelease == null -> 0
            preRelease == null -> 1 // fertige Version ist neuer als ihre Testversion
            other.preRelease == null -> -1
            else -> comparePre(preRelease, other.preRelease)
        }
    }

    private fun comparePre(a: String, b: String): Int {
        val pa = a.split('.')
        val pb = b.split('.')
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrNull(i) ?: return -1
            val y = pb.getOrNull(i) ?: return 1
            val xi = x.toIntOrNull()
            val yi = y.toIntOrNull()
            val d = if (xi != null && yi != null) xi - yi else x.compareTo(y)
            if (d != 0) return d
        }
        return 0
    }

    companion object {
        /** null, wenn der Text keine Version ist (z. B. Entwickler-Builds „0.0.0-dev“ gelten als sehr alt). */
        fun parse(text: String): AppVersion? {
            val t = text.trim().removePrefix("v").removePrefix("V")
            val core = t.substringBefore('-')
            val pre = t.substringAfter('-', "").ifEmpty { null }
            val nums = core.split('.').map { it.toIntOrNull() ?: return null }
            if (nums.isEmpty()) return null
            return AppVersion(nums, pre)
        }
    }
}
