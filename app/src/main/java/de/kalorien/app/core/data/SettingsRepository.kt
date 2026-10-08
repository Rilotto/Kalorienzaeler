package de.kalorien.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Settings(val dailyGoal: Int = DEFAULT_GOAL, val showMacros: Boolean = true) {
    companion object {
        const val DEFAULT_GOAL = 2200
        const val MIN_GOAL = 800
        const val MAX_GOAL = 6000
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val goal = intPreferencesKey("daily_goal")
        val macros = booleanPreferencesKey("show_macros")
        val blsVersion = intPreferencesKey("bls_version")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            dailyGoal = p[Keys.goal] ?: Settings.DEFAULT_GOAL,
            showMacros = p[Keys.macros] ?: true,
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.goal] = goal.coerceIn(Settings.MIN_GOAL, Settings.MAX_GOAL) }
    }

    suspend fun setShowMacros(show: Boolean) {
        context.dataStore.edit { it[Keys.macros] = show }
    }

    suspend fun blsVersion(): Int = context.dataStore.data.first()[Keys.blsVersion] ?: 0

    suspend fun setBlsVersion(v: Int) {
        context.dataStore.edit { it[Keys.blsVersion] = v }
    }
}
