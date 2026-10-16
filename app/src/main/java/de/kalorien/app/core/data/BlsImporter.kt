package de.kalorien.app.core.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import de.kalorien.app.core.domain.BlsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Spielt die eingebaute BLS-Tabelle (assets/bls.tsv) in die Datenbank ein.
 * Läuft beim ersten Start und erneut, wenn [ASSET_VERSION] erhöht wurde.
 * Favoriten und „zuletzt verwendet“ bleiben dabei erhalten (Update statt Neu-Anlegen).
 */
class BlsImporter(
    private val context: Context,
    private val db: KalorienDatabase,
    private val settings: SettingsRepository,
) {
    enum class State { Idle, Running, Done, Missing }

    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state

    suspend fun importIfNeeded() = withContext(Dispatchers.IO) {
        if (settings.blsVersion() >= ASSET_VERSION) {
            _state.value = State.Done
            return@withContext
        }
        val lines = try {
            context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).readLines()
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Keine BLS-Datei in den Assets", e)
            _state.value = State.Missing
            return@withContext
        }
        _state.value = State.Running
        val rows = lines.mapNotNull(BlsRow::parse)
        val dao = db.foodDao()
        db.withTransaction {
            rows.forEach { r ->
                val key = FoodRepository.searchKey(r.name, null)
                val p = r.per100
                val updated = dao.updateBls(r.code, r.name, p.kcal, p.protein, p.carbs, p.fat, p.sugar, r.isLiquid, key)
                // vom Nutzer korrigierte Lebensmittel: Korrektur bleibt, nur das „Original“ wird aktualisiert
                val adjusted = updated == 0 && dao.updateBlsOriginal(r.code, p.kcal, p.protein, p.carbs, p.fat, p.sugar) > 0
                if (updated == 0 && !adjusted) {
                    dao.insert(
                        FoodEntity(
                            name = r.name, source = FoodSource.BLS, sourceId = r.code,
                            kcal100 = r.per100.kcal, protein100 = r.per100.protein, carbs100 = r.per100.carbs,
                            fat100 = r.per100.fat, sugar100 = r.per100.sugar, isLiquid = r.isLiquid, searchKey = key,
                        ),
                    )
                }
            }
        }
        settings.setBlsVersion(ASSET_VERSION)
        Log.i(TAG, "BLS importiert: ${rows.size} Lebensmittel")
        _state.value = State.Done
    }

    companion object {
        private const val TAG = "BlsImporter"
        const val ASSET = "bls.tsv"

        /** Erhöhen, wenn assets/bls.tsv neu erzeugt wurde. (2 = mit Zucker, v0.5.0) */
        const val ASSET_VERSION = 2
    }
}
