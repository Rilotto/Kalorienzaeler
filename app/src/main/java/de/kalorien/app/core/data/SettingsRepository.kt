package de.kalorien.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import de.kalorien.app.core.domain.WeekGoals
import java.time.DayOfWeek
import java.time.LocalDate

data class Settings(
    val dailyGoal: Int = DEFAULT_GOAL,
    val showMacros: Boolean = true,
    /** Auch Testversionen (rc) als Update anbieten */
    val includeTestVersions: Boolean = true,
    /** Zucker-Balken in der Tagesansicht */
    val showSugar: Boolean = true,
    /** Eigene Tagesziele in Gramm; null = automatisch aus dem Kalorienziel */
    val proteinGoal: Int? = null,
    val carbsGoal: Int? = null,
    val fatGoal: Int? = null,
    val sugarLimit: Int? = null,
    /** Täglich automatisch eine Sicherung in den Download-Ordner schreiben */
    val autoBackup: Boolean = true,
    /** Datum (yyyy-MM-dd) der letzten Sicherung, null = noch nie */
    val lastBackup: String? = null,
    /** Abweichendes Ziel an einzelnen Wochentagen (z. B. Samstag 2600) */
    val weekdayGoals: Map<DayOfWeek, Int> = emptyMap(),
    /** Hell/Dunkel: System, immer hell, immer dunkel */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Reiter, der beim Eintragen zuerst offen ist (0 Suchen, 1 Scannen, 2 Foto, 3 Schnell) */
    val startTab: Int = 0,
) {
    /** Tagesziel für ein Datum (Wochentag-Ziel oder Standard). */
    fun goalFor(date: LocalDate): Int = weekdayGoals[date.dayOfWeek] ?: dailyGoal

    companion object {
        const val DEFAULT_GOAL = 2200
        const val MIN_GOAL = 800
        const val MAX_GOAL = 6000
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val goal = intPreferencesKey("daily_goal")
        val macros = booleanPreferencesKey("show_macros")
        val blsVersion = intPreferencesKey("bls_version")
        val testVersions = booleanPreferencesKey("include_test_versions")
        val sugar = booleanPreferencesKey("show_sugar")
        val proteinGoal = intPreferencesKey("protein_goal")
        val carbsGoal = intPreferencesKey("carbs_goal")
        val fatGoal = intPreferencesKey("fat_goal")
        val sugarLimit = intPreferencesKey("sugar_limit")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val lastBackup = stringPreferencesKey("last_backup")
        val weekdayGoals = stringPreferencesKey("weekday_goals")
        val theme = stringPreferencesKey("theme_mode")
        val startTab = intPreferencesKey("start_tab")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            dailyGoal = p[Keys.goal] ?: Settings.DEFAULT_GOAL,
            showMacros = p[Keys.macros] ?: true,
            includeTestVersions = p[Keys.testVersions] ?: true,
            showSugar = p[Keys.sugar] ?: true,
            proteinGoal = p[Keys.proteinGoal],
            carbsGoal = p[Keys.carbsGoal],
            fatGoal = p[Keys.fatGoal],
            sugarLimit = p[Keys.sugarLimit],
            autoBackup = p[Keys.autoBackup] ?: true,
            lastBackup = p[Keys.lastBackup],
            weekdayGoals = WeekGoals.parse(p[Keys.weekdayGoals]),
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            startTab = (p[Keys.startTab] ?: 0).coerceIn(0, 3),
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.goal] = goal.coerceIn(Settings.MIN_GOAL, Settings.MAX_GOAL) }
    }

    suspend fun setShowMacros(show: Boolean) {
        context.dataStore.edit { it[Keys.macros] = show }
    }

    suspend fun setShowSugar(show: Boolean) {
        context.dataStore.edit { it[Keys.sugar] = show }
    }

    /** Eigene Makro-Ziele setzen; null-Werte = automatisch. */
    suspend fun setMacroGoals(protein: Int?, carbs: Int?, fat: Int?, sugar: Int?) {
        context.dataStore.edit { p ->
            fun set(key: Preferences.Key<Int>, v: Int?) { if (v == null) p.remove(key) else p[key] = v.coerceIn(0, 2000) }
            set(Keys.proteinGoal, protein)
            set(Keys.carbsGoal, carbs)
            set(Keys.fatGoal, fat)
            set(Keys.sugarLimit, sugar)
        }
    }

    /** Ziel für einen Wochentag; null = Standardziel verwenden. */
    suspend fun setWeekdayGoal(day: DayOfWeek, goal: Int?) {
        context.dataStore.edit { p ->
            val m = WeekGoals.parse(p[Keys.weekdayGoals]).toMutableMap()
            if (goal == null) m.remove(day) else m[day] = goal.coerceIn(Settings.MIN_GOAL, Settings.MAX_GOAL)
            p[Keys.weekdayGoals] = WeekGoals.format(m)
        }
    }

    suspend fun setWeekdayGoals(goals: Map<DayOfWeek, Int>) {
        context.dataStore.edit { it[Keys.weekdayGoals] = WeekGoals.format(goals) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setStartTab(tab: Int) {
        context.dataStore.edit { it[Keys.startTab] = tab.coerceIn(0, 3) }
    }

    suspend fun setAutoBackup(on: Boolean) {
        context.dataStore.edit { it[Keys.autoBackup] = on }
    }

    suspend fun setLastBackup(date: String) {
        context.dataStore.edit { it[Keys.lastBackup] = date }
    }

    suspend fun current(): Settings = settings.first()

    /** Für die Wiederherstellung einer Sicherung. */
    suspend fun restore(s: Settings) {
        setDailyGoal(s.dailyGoal)
        setShowMacros(s.showMacros)
        setShowSugar(s.showSugar)
        setMacroGoals(s.proteinGoal, s.carbsGoal, s.fatGoal, s.sugarLimit)
        setWeekdayGoals(s.weekdayGoals)
        setThemeMode(s.themeMode)
        setStartTab(s.startTab)
    }

    suspend fun setIncludeTestVersions(include: Boolean) {
        context.dataStore.edit { it[Keys.testVersions] = include }
    }

    suspend fun blsVersion(): Int = context.dataStore.data.first()[Keys.blsVersion] ?: 0

    suspend fun setBlsVersion(v: Int) {
        context.dataStore.edit { it[Keys.blsVersion] = v }
    }
}
