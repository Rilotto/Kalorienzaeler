package de.kalorien.app.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.kalorien.app.core.domain.Per100

/** Herkunft eines Lebensmittels. Werte werden als Text gespeichert – nicht umbenennen. */
object FoodSource {
    const val BLS = "BLS"
    const val CUSTOM = "CUSTOM"
    const val QUICK = "QUICK"
    /** Open Food Facts (Barcode, Block 3) */
    const val OFF = "OFF"
    // später: RECIPE (Block 7)
}

@Entity(
    tableName = "food",
    indices = [
        Index("searchKey"),
        Index(value = ["source", "sourceId"], unique = true),
        Index("ean"),
    ],
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val source: String,
    /** z. B. BLS-Schlüssel */
    val sourceId: String? = null,
    val ean: String? = null,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    val isLiquid: Boolean = false,
    val favorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
    /** normalisierter Name + Marke für die Suche */
    val searchKey: String,
    /**
     * true = „Portions-Lebensmittel“ (z. B. aus einem Schnelleintrag gemerkt): Die Nährwerte in *100-Feldern
     * gelten für EINE Portion, gewählt wird die Anzahl Portionen statt Gramm. Seit Datenbank-Version 2.
     */
    @ColumnInfo(defaultValue = "0")
    val isPortionFood: Boolean = false,
    /** Packungs-/Portionsgröße, z. B. 330 (ml) für „1 Dose“ (Open Food Facts oder selbst eingegeben). Seit DB v3. */
    val servingGrams: Double? = null,
    /** Bezeichnung dazu, z. B. „1 Dose“; null = „1 Portion“. Seit DB v3. */
    val servingLabel: String? = null,
    /** Zucker je 100 g/ml (bzw. je Portion bei Portions-Lebensmitteln); null = unbekannt. Seit DB v4. */
    val sugar100: Double? = null,
    /** Vom Nutzer korrigiert – Datenbank-Updates (BLS) überschreiben die Werte dann nicht. Seit DB v4. */
    @ColumnInfo(defaultValue = "0")
    val isAdjusted: Boolean = false,
    /** Originalwerte vor der Korrektur (für „Auf Original zurücksetzen“). Seit DB v4. */
    val origKcal100: Double? = null,
    val origProtein100: Double? = null,
    val origCarbs100: Double? = null,
    val origFat100: Double? = null,
    val origSugar100: Double? = null,
) {
    val per100: Per100 get() = Per100(kcal100, protein100, carbs100, fat100, sugar100)
}

/**
 * Ein Tagebucheintrag. Nährwerte sind ein Snapshot zum Zeitpunkt des Eintragens,
 * damit spätere Änderungen am Lebensmittel alte Tage nicht verändern.
 */
@Entity(tableName = "entry", indices = [Index("date"), Index("foodId")])
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO-Datum yyyy-MM-dd */
    val date: String,
    /** [de.kalorien.app.core.domain.Meal.name] */
    val meal: String,
    /** null = Schnelleintrag ohne Lebensmittel */
    val foodId: Long?,
    val name: String,
    val brand: String? = null,
    /** null = Schnelleintrag ohne Menge */
    val grams: Double?,
    val isLiquid: Boolean = false,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAt: Long,
    /** Zucker in g; null = unbekannt (Einträge vor v0.5 oder Quelle ohne Zuckerangabe). Seit DB v4. */
    val sugar: Double? = null,
    /** Eingegebene Einheit, z. B. „Scheibe“ (null = in Gramm eingegeben). Seit DB v4. */
    val unitLabel: String? = null,
    /** Anzahl in dieser Einheit, z. B. 1,5 (bei Portions-Lebensmitteln: Anzahl Portionen). Seit DB v4. */
    val unitCount: Double? = null,
)
