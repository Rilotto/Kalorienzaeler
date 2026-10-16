package de.kalorien.app.core.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import de.kalorien.app.BuildConfig
import de.kalorien.app.core.domain.Meal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Inhalt einer Sicherung. */
data class BackupData(val settings: Settings, val foods: List<FoodEntity>, val entries: List<EntryEntity>)

/**
 * Wandelt die Nutzerdaten in JSON um und zurück. Format-Version [FORMAT] – bei Änderungen erhöhen
 * und alte Versionen weiter lesen können. BLS-Lebensmittel werden nur mit Nutzungsdaten gesichert
 * (die Nährwerte stecken ohnehin in der App).
 */
object BackupCodec {
    const val FORMAT = 1

    fun encode(data: BackupData, createdAt: Long = System.currentTimeMillis()): String {
        val s = data.settings
        val root = JSONObject()
            .put("format", FORMAT)
            .put("app", "Kalorienzaehler")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("createdAt", createdAt)
            .put(
                "settings",
                JSONObject().put("dailyGoal", s.dailyGoal).put("showMacros", s.showMacros).put("showSugar", s.showSugar)
                    .putOpt("proteinGoal", s.proteinGoal).putOpt("carbsGoal", s.carbsGoal)
                    .putOpt("fatGoal", s.fatGoal).putOpt("sugarLimit", s.sugarLimit),
            )
        root.put("foods", JSONArray().apply { data.foods.forEach { put(food(it)) } })
        root.put("entries", JSONArray().apply { data.entries.forEach { put(entry(it)) } })
        return root.toString()
    }

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        require(root.optString("app") == "Kalorienzaehler") { "Das ist keine Sicherung dieser App." }
        require(root.optInt("format") in 1..FORMAT) { "Die Sicherung ist von einer neueren App-Version." }
        val st = root.optJSONObject("settings") ?: JSONObject()
        val settings = Settings(
            dailyGoal = st.optInt("dailyGoal", Settings.DEFAULT_GOAL),
            showMacros = st.optBoolean("showMacros", true),
            showSugar = st.optBoolean("showSugar", true),
            proteinGoal = st.optIntOrNull("proteinGoal"), carbsGoal = st.optIntOrNull("carbsGoal"),
            fatGoal = st.optIntOrNull("fatGoal"), sugarLimit = st.optIntOrNull("sugarLimit"),
        )
        val foods = root.optJSONArray("foods").objects().map(::food)
        val entries = root.optJSONArray("entries").objects().map(::entry)
        return BackupData(settings, foods, entries)
    }

    private fun food(f: FoodEntity) = JSONObject()
        .put("id", f.id).put("name", f.name).putOpt("brand", f.brand).put("source", f.source)
        .putOpt("sourceId", f.sourceId).putOpt("ean", f.ean)
        .put("kcal100", f.kcal100).put("protein100", f.protein100).put("carbs100", f.carbs100).put("fat100", f.fat100)
        .putOpt("sugar100", f.sugar100).put("isLiquid", f.isLiquid).put("favorite", f.favorite)
        .putOpt("lastUsedAt", f.lastUsedAt).put("useCount", f.useCount).put("isPortionFood", f.isPortionFood)
        .putOpt("servingGrams", f.servingGrams).putOpt("servingLabel", f.servingLabel).put("isAdjusted", f.isAdjusted)
        .putOpt("origKcal100", f.origKcal100).putOpt("origProtein100", f.origProtein100)
        .putOpt("origCarbs100", f.origCarbs100).putOpt("origFat100", f.origFat100).putOpt("origSugar100", f.origSugar100)

    private fun food(o: JSONObject): FoodEntity {
        val name = o.getString("name")
        val brand = o.optStringOrNull("brand")
        return FoodEntity(
            id = o.optLong("id"), name = name, brand = brand, source = o.getString("source"),
            sourceId = o.optStringOrNull("sourceId"), ean = o.optStringOrNull("ean"),
            kcal100 = o.optDouble("kcal100", 0.0), protein100 = o.optDouble("protein100", 0.0),
            carbs100 = o.optDouble("carbs100", 0.0), fat100 = o.optDouble("fat100", 0.0),
            sugar100 = o.optDoubleOrNull("sugar100"), isLiquid = o.optBoolean("isLiquid"), favorite = o.optBoolean("favorite"),
            lastUsedAt = o.optLongOrNull("lastUsedAt"), useCount = o.optInt("useCount"),
            searchKey = FoodRepository.searchKey(name, brand), isPortionFood = o.optBoolean("isPortionFood"),
            servingGrams = o.optDoubleOrNull("servingGrams"), servingLabel = o.optStringOrNull("servingLabel"),
            isAdjusted = o.optBoolean("isAdjusted"), origKcal100 = o.optDoubleOrNull("origKcal100"),
            origProtein100 = o.optDoubleOrNull("origProtein100"), origCarbs100 = o.optDoubleOrNull("origCarbs100"),
            origFat100 = o.optDoubleOrNull("origFat100"), origSugar100 = o.optDoubleOrNull("origSugar100"),
        )
    }

    private fun entry(e: EntryEntity) = JSONObject()
        .put("date", e.date).put("meal", e.meal).putOpt("foodId", e.foodId).put("name", e.name).putOpt("brand", e.brand)
        .putOpt("grams", e.grams).put("isLiquid", e.isLiquid).put("kcal", e.kcal).put("protein", e.protein)
        .put("carbs", e.carbs).put("fat", e.fat).putOpt("sugar", e.sugar).put("createdAt", e.createdAt)
        .putOpt("unitLabel", e.unitLabel).putOpt("unitCount", e.unitCount)

    private fun entry(o: JSONObject) = EntryEntity(
        date = o.getString("date"), meal = o.optString("meal", Meal.SNACK.name), foodId = o.optLongOrNull("foodId"),
        name = o.getString("name"), brand = o.optStringOrNull("brand"), grams = o.optDoubleOrNull("grams"),
        isLiquid = o.optBoolean("isLiquid"), kcal = o.optInt("kcal"), protein = o.optDouble("protein", 0.0),
        carbs = o.optDouble("carbs", 0.0), fat = o.optDouble("fat", 0.0), sugar = o.optDoubleOrNull("sugar"),
        createdAt = o.optLong("createdAt"), unitLabel = o.optStringOrNull("unitLabel"), unitCount = o.optDoubleOrNull("unitCount"),
    )

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.optStringOrNull(k: String) = if (has(k) && !isNull(k)) getString(k) else null
    private fun JSONObject.optDoubleOrNull(k: String) = if (has(k) && !isNull(k)) getDouble(k) else null
    private fun JSONObject.optLongOrNull(k: String) = if (has(k) && !isNull(k)) getLong(k) else null
    private fun JSONObject.optIntOrNull(k: String) = if (has(k) && !isNull(k)) getInt(k) else null
}

/**
 * Sicherung in „Download/Kalorienzaehler“ (bleibt nach dem Deinstallieren erhalten), Wiederherstellen und CSV-Export.
 * Keine Berechtigung nötig (Android 10+ MediaStore). Es werden die letzten [KEEP] Sicherungen behalten.
 */
class BackupManager(
    private val context: Context,
    private val db: KalorienDatabase,
    private val settings: SettingsRepository,
) {
    private val resolver get() = context.contentResolver
    private val collection: Uri get() = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    /** Einmal pro Tag automatisch (beim App-Start), wenn eingeschaltet und es Einträge gibt. */
    suspend fun autoBackupIfDue() = withContext(Dispatchers.IO) {
        runCatching {
            val s = settings.current()
            val today = LocalDate.now().toString()
            if (!s.autoBackup || s.lastBackup == today) return@runCatching
            if (db.entryDao().all().isEmpty()) return@runCatching
            backupNow()
        }
    }

    /** Sicherung jetzt schreiben (überschreibt die von heute). Gibt den Dateinamen zurück. */
    suspend fun backupNow(): String = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val json = BackupCodec.encode(collect())
        val name = "$PREFIX${today}.json"
        deleteOwn { it == name }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("Sicherung konnte nicht angelegt werden")
        resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } ?: error("Sicherung konnte nicht geschrieben werden")
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        pruneOld()
        settings.setLastBackup(today.toString())
        name
    }

    /** Sicherung aus einer Datei einspielen. Ersetzt alle Einträge und eigenen Lebensmittel. Gibt die Anzahl Einträge zurück. */
    suspend fun restore(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Datei konnte nicht gelesen werden")
        val data = BackupCodec.decode(json)
        val foodDao = db.foodDao()
        val entryDao = db.entryDao()
        db.withTransaction {
            entryDao.deleteAll()
            foodDao.deleteNonBls()
            foodDao.resetBlsUsage()
            val idMap = HashMap<Long, Long>()
            data.foods.forEach { f ->
                if (f.source == FoodSource.BLS && f.sourceId != null) {
                    val existing = foodDao.findBls(f.sourceId) ?: return@forEach
                    foodDao.update(
                        existing.copy(
                            favorite = f.favorite, useCount = f.useCount, lastUsedAt = f.lastUsedAt,
                            isAdjusted = f.isAdjusted,
                            kcal100 = if (f.isAdjusted) f.kcal100 else existing.kcal100,
                            protein100 = if (f.isAdjusted) f.protein100 else existing.protein100,
                            carbs100 = if (f.isAdjusted) f.carbs100 else existing.carbs100,
                            fat100 = if (f.isAdjusted) f.fat100 else existing.fat100,
                            sugar100 = if (f.isAdjusted) f.sugar100 else existing.sugar100,
                            origKcal100 = f.origKcal100, origProtein100 = f.origProtein100, origCarbs100 = f.origCarbs100,
                            origFat100 = f.origFat100, origSugar100 = f.origSugar100,
                        ),
                    )
                    idMap[f.id] = existing.id
                } else {
                    idMap[f.id] = foodDao.insert(f.copy(id = 0))
                }
            }
            entryDao.insertAll(data.entries.map { e -> e.copy(id = 0, foodId = e.foodId?.let { idMap[it] }) })
        }
        settings.restore(data.settings)
        data.entries.size
    }

    /** CSV aller Einträge in den App-Cache schreiben; Rückgabe ist eine teilbare Adresse (für „Teilen“). */
    suspend fun exportCsv(): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "kalorienzaehler-${LocalDate.now()}.csv")
        val fmt = { d: Double? -> d?.let { String.format(java.util.Locale.GERMANY, "%.1f", it) }.orEmpty() }
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿") // damit Excel die Umlaute richtig liest
            w.write("Datum;Mahlzeit;Lebensmittel;Menge;Einheit;kcal;Eiweiß (g);Kohlenhydrate (g);Fett (g);Zucker (g)\n")
            db.entryDao().all().forEach { e ->
                val amount = when {
                    e.unitLabel != null && e.unitCount != null -> "${fmt(e.unitCount)};${e.unitLabel}"
                    e.grams != null -> "${fmt(e.grams)};${if (e.isLiquid) "ml" else "g"}"
                    else -> ";"
                }
                val name = e.name.replace(";", ",")
                w.write("${e.date};${mealName(e.meal)};$name;$amount;${e.kcal};${fmt(e.protein)};${fmt(e.carbs)};${fmt(e.fat)};${fmt(e.sugar)}\n")
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
    }

    private fun mealName(m: String) = when (Meal.fromDb(m)) {
        Meal.BREAKFAST -> "Frühstück"; Meal.LUNCH -> "Mittagessen"; Meal.DINNER -> "Abendessen"; Meal.SNACK -> "Snacks"
    }

    private suspend fun collect() = BackupData(settings.current(), db.foodDao().userRelevant(), db.entryDao().all())

    /** Eigene Sicherungsdateien (nur die, die diese App angelegt hat): Name → Uri */
    private fun ownBackups(): List<Pair<String, Uri>> {
        val out = mutableListOf<Pair<String, Uri>>()
        resolver.query(
            collection, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("$FOLDER/", "$PREFIX%"), "${MediaStore.MediaColumns.DISPLAY_NAME} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                out += c.getString(1) to Uri.withAppendedPath(collection, id.toString())
            }
        }
        return out
    }

    private fun deleteOwn(predicate: (String) -> Boolean) {
        ownBackups().filter { predicate(it.first) }.forEach { runCatching { resolver.delete(it.second, null, null) } }
    }

    private fun pruneOld() {
        ownBackups().drop(KEEP).forEach { runCatching { resolver.delete(it.second, null, null) } }
    }

    companion object {
        const val FOLDER = "Download/Kalorienzaehler"
        const val PREFIX = "kalorienzaehler-backup-"
        const val KEEP = 7
        val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
