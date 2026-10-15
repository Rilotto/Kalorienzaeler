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
        Rule(listOf("nudel", "spaghetti", "pasta", "teigwaren", "reis"), listOf(Portion("1 Portion gekocht", 200.0))),
        Rule(listOf("zucker", "honig", "marmelade", "konfituere"), listOf(Portion("1 TL", 7.0), Portion("1 EL", 20.0))),
        Rule(listOf("nuss", "nuesse", "mandel"), listOf(Portion("1 Handvoll", 25.0))),
        Rule(listOf("schokolade"), listOf(Portion("1 Riegel", 25.0), Portion("1 Tafel", 100.0))),
        Rule(listOf("pizza"), listOf(Portion("1 Pizza", 350.0), Portion("½ Pizza", 175.0))),
        Rule(listOf("doener", "kebab", "duerueum"), listOf(Portion("1 Stück", 350.0))),
        Rule(listOf("brustfilet", "schnitzel", "steak"), listOf(Portion("1 Stück", 150.0))),
        Rule(listOf("lachs", "fischfilet", "seelachs"), listOf(Portion("1 Filet", 125.0))),
        Rule(listOf("pommes"), listOf(Portion("kleine Portion", 120.0), Portion("große Portion", 200.0))),
        Rule(listOf("bratwurst", "currywurst", "wiener", "wuerstchen"), listOf(Portion("1 Stück", 100.0))),
    )

    private val liquidDefaults = listOf(Portion("1 Tasse", 200.0), Portion("1 Glas", 250.0), Portion("0,5 l", 500.0))

    /** Wenn keine Regel passt: einfache Standardmengen statt nur „100 g“. */
    private val solidDefaults = listOf(Portion("50 g", 50.0), Portion("150 g", 150.0), Portion("200 g", 200.0))

    /** [serving]: bekannte Packungs-/Portionsgröße (z. B. „1 Dose“ 330 ml) – steht dann ganz vorne. */
    fun portionsFor(name: String, isLiquid: Boolean, serving: Portion? = null): List<Portion> {
        // Nur das Hauptwort am Anfang zählt (sonst bekäme „Pizza mit Tomatensauce“ Tomaten-Portionen).
        // Passt dort nichts, die ersten zwei Wörter zusammengeschrieben: BLS schreibt „Hafer Flocken“.
        val words = TextNormalizer.tokens(name)
        fun rulesFor(head: String): List<Portion> {
            val key = " $head "
            return rules.filter { r -> r.keywords.any { key.contains(if (it.endsWith(" ")) " $it" else it) } }
                .flatMap { it.portions }
        }
        val found: List<Portion> = when {
            isLiquid || words.isEmpty() -> emptyList()
            else -> rulesFor(words[0]).ifEmpty { if (words.size > 1) rulesFor(words[0] + words[1]) else emptyList() }
        }
        val unit = if (isLiquid) "ml" else "g"
        val hundred = Portion("100 $unit", 100.0)
        val list = when {
            isLiquid -> liquidDefaults + hundred
            found.isEmpty() -> listOf(solidDefaults[0], hundred) + solidDefaults.drop(1)
            else -> found + hundred
        }
        return (listOfNotNull(serving) + list).distinctBy { it.grams }.take(5)
    }
}
