package de.kalorien.app.core.food

import de.kalorien.app.BuildConfig
import de.kalorien.app.core.domain.NutritionUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Ein Produkt aus Open Food Facts. Nährwerte je 100 g bzw. 100 ml; kcal kann fehlen. */
data class OffProduct(
    val ean: String,
    val name: String?,
    val brand: String?,
    val kcal100: Double?,
    val protein100: Double?,
    val carbs100: Double?,
    val fat100: Double?,
    val isLiquid: Boolean,
    val servingGrams: Double?,
    val servingLabel: String?,
) {
    /** Genug Daten, um ohne Nachfrage einzutragen? */
    val isComplete: Boolean get() = !name.isNullOrBlank() && kcal100 != null
}

/**
 * Abfrage bei Open Food Facts (offene Datenbank, ODbL). Nur der Barcode wird gesendet, sonst nichts.
 * Ergebnis `null` = Produkt unbekannt.
 */
object OpenFoodFacts {
    private const val FIELDS =
        "product_name,product_name_de,generic_name_de,brands,quantity,serving_size,serving_quantity,nutriments"

    suspend fun lookup(ean: String): OffProduct? = withContext(Dispatchers.IO) {
        val url = URL("https://world.openfoodfacts.org/api/v2/product/$ean.json?fields=$FIELDS")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "Kalorienzaehler/${BuildConfig.VERSION_NAME} (Android; github.com/Rilotto/Kalorienzaeler)")
        try {
            val code = conn.responseCode
            if (code == 404) return@withContext null
            if (code !in 200..299) throw IOException("Open Food Facts antwortet mit $code")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (json.optInt("status", 0) != 1) return@withContext null
            parse(ean, json.getJSONObject("product"))
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(ean: String, p: JSONObject): OffProduct {
        val n = p.optJSONObject("nutriments") ?: JSONObject()
        fun d(key: String): Double? = if (n.has(key) && !n.isNull(key)) n.optDouble(key).takeIf { !it.isNaN() } else null
        val kcal = d("energy-kcal_100g") ?: d("energy_100g")?.let(NutritionUnits::kjToKcal)
        val name = listOf("product_name_de", "product_name", "generic_name_de")
            .map { p.optString(it).trim() }.firstOrNull { it.isNotEmpty() }
        val brand = p.optString("brands").split(',').firstOrNull()?.trim()?.ifEmpty { null }
        val quantity = p.optString("quantity").ifEmpty { null }
        val servingQty = p.optDouble("serving_quantity").takeIf { !it.isNaN() && it > 0 }
        val servingText = p.optString("serving_size").trim().ifEmpty { null }
        return OffProduct(
            ean = ean,
            name = name,
            brand = brand,
            kcal100 = kcal,
            protein100 = d("proteins_100g"),
            carbs100 = d("carbohydrates_100g"),
            fat100 = d("fat_100g"),
            isLiquid = NutritionUnits.isLiquidQuantity(quantity) || NutritionUnits.isLiquidQuantity(servingText),
            servingGrams = servingQty,
            servingLabel = servingText?.take(30),
        )
    }
}
