package de.kalorien.app

import android.app.Application

/**
 * Einstiegspunkt der App. Hält ab Block 1 den [AppContainer] (manuelle Dependency Injection,
 * bewusst ohne Hilt, damit der Build schlank und robust bleibt).
 */
class KalorienApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Sammelt alle app-weiten Abhängigkeiten (Datenbank, Repositories, Einstellungen). */
class AppContainer(@Suppress("unused") private val app: Application)
