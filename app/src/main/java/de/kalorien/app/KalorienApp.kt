package de.kalorien.app

import android.app.Application
import de.kalorien.app.core.data.BlsImporter
import de.kalorien.app.core.data.EntryRepository
import de.kalorien.app.core.data.FoodRepository
import de.kalorien.app.core.data.KalorienDatabase
import de.kalorien.app.core.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Einstiegspunkt der App. Hält den [AppContainer] (manuelle Dependency Injection, bewusst ohne Hilt). */
class KalorienApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.blsImporter.importIfNeeded() }
    }
}

/** Sammelt alle app-weiten Abhängigkeiten. Neue Repositories hier ergänzen. */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob())
    val database: KalorienDatabase = KalorienDatabase.create(app)
    val settings = SettingsRepository(app)
    val entries = EntryRepository(database)
    val foods = FoodRepository(database)
    val blsImporter = BlsImporter(app, database, settings)
}
