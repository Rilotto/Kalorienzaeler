package de.kalorien.app.core.domain

/** Eine wählbare Portion, z. B. „1 Scheibe“ = 50 g. */
data class Portion(val label: String, val grams: Double)

/**
 * Schlägt typische Portionen anhand des Namens vor (die Lebensmitteldaten enthalten keine Portionen).
 * Die Liste ist bewusst klein und leicht erweiterbar: Regel = Schlüsselwort im normalisierten Namen.
 */
object PortionGuesser {
    private data class Rule(val keywords: List<String>, val portions: List<Portion>)

    private val rules = listOf(
        Rule(listOf("brot", "toast"), listOf(Portion("1 Scheibe", 50.0))),
        Rule(listOf("broetchen", "semmel", "croissant"), listOf(Portion("1 Stück", 60.0))),
        Rule(listOf("ei ", "huehnerei", "eier"), listOf(Portion("1 Stück (M)", 58.0))),
        Rule(listOf("banane"), listOf(Portion("1 Stück", 120.0))),
        Rule(listOf("apfel"), listOf(Portion("1 Stück", 180.0))),
        Rule(listOf("birne", "orange", "pfirsich"), listOf(Portion("1 Stück", 160.0))),
        Rule(listOf("kiwi", "mandarine", "clementine"), listOf(Portion("1 Stück", 75.0))),
        Rule(listOf("tomate"), listOf(Portion("1 Stück", 80.0))),
        Rule(listOf("kartoffel"), listOf(Portion("1 mittelgroße", 90.0))),
        Rule(listOf("butter", "margarine"), listOf(Portion("1 TL", 5.0), Portion("dünn aufs Brot", 8.0))),
        Rule(listOf("oel", "olivenoel", "rapsoel"), listOf(Portion("1 TL", 5.0), Portion("1 EL", 10.0))),
        Rule(listOf("kaese", "gouda", "emmentaler"), listOf(Portion("1 Scheibe", 25.0))),
        Rule(listOf("schinken", "salami", "wurst", "aufschnitt"), listOf(Portion("1 Scheibe", 20.0))),
        Rule(listOf("joghurt", "quark", "skyr"), listOf(Portion("1 Becher", 150.0), Portion("2 EL", 60.0))),
        Rule(listOf("haferflocken", "muesli", "cornflakes"), listOf(Portion("1 EL", 10.0), Portion("1 Portion", 50.0))),
        Rule(listOf("nudel", "spaghetti", "pasta", "reis"), listOf(Portion("1 Portion gekocht", 200.0))),
        Rule(listOf("zucker", "honig", "marmelade", "konfituere"), listOf(Portion("1 TL", 7.0), Portion("1 EL", 20.0))),
        Rule(listOf("nuss", "nuesse", "mandel"), listOf(Portion("1 Handvoll", 25.0))),
        Rule(listOf("schokolade"), listOf(Portion("1 Riegel", 25.0), Portion("1 Tafel", 100.0))),
    )

    private val liquidDefaults = listOf(Portion("1 Tasse", 200.0), Portion("1 Glas", 250.0), Portion("0,5 l", 500.0))

    fun portionsFor(name: String, isLiquid: Boolean): List<Portion> {
        val key = " " + TextNormalizer.normalize(name) + " "
        val found: List<Portion> = if (isLiquid) emptyList() else rules.filter { r -> r.keywords.any { key.contains(if (it.endsWith(" ")) " $it" else it) } }
            .flatMap { it.portions }
        val base = if (isLiquid) liquidDefaults else emptyList()
        val unit = if (isLiquid) "ml" else "g"
        return (found + base + Portion("100 $unit", 100.0)).distinctBy { it.grams to it.label }.take(5)
    }
}
