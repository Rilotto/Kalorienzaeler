package de.kalorien.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import de.kalorien.app.core.data.BackupManager
import de.kalorien.app.core.data.FoodRepository
import de.kalorien.app.core.data.FoodEntity
import de.kalorien.app.core.data.TemplateRepository
import de.kalorien.app.core.data.TemplateWithItems
import de.kalorien.app.core.data.ThemeMode
import de.kalorien.app.core.CrashLog
import de.kalorien.app.core.domain.Per100
import java.time.DayOfWeek
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.data.SettingsRepository
import de.kalorien.app.core.update.ReleaseInfo
import de.kalorien.app.core.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Zustand des Update-Knopfs in den Einstellungen. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    /** Heruntergeladen; wartet auf Installation (ggf. erst Erlaubnis nötig) */
    data class Ready(val release: ReleaseInfo, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Zustand von „Zucker nachladen“. */
sealed interface SugarRefresh {
    data object Idle : SugarRefresh
    data object Working : SugarRefresh
    data object Offline : SugarRefresh
    data class Done(val found: Int) : SugarRefresh
}

/** Zustand der Sicherungs-Karte. */
sealed interface BackupState {
    data object Idle : BackupState
    data object Working : BackupState
    data class Saved(val fileName: String) : BackupState
    data class Restored(val entries: Int) : BackupState
    data class Failed(val message: String) : BackupState
}

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val updates: UpdateRepository,
    private val backup: BackupManager,
    private val foods: FoodRepository,
    private val templateRepo: TemplateRepository,
    private val crash: CrashLog,
) : ViewModel() {
    // --- v0.6: Vorlagen, eigene Lebensmittel, Darstellung, Fehlerprotokoll
    val templates: StateFlow<List<TemplateWithItems>> =
        templateRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ownFoods: StateFlow<List<FoodEntity>> =
        foods.observeOwn().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWeekdayGoals(goals: Map<DayOfWeek, Int>) = viewModelScope.launch { repo.setWeekdayGoals(goals) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setStartTab(tab: Int) = viewModelScope.launch { repo.setStartTab(tab) }

    fun deleteTemplate(t: TemplateWithItems) = viewModelScope.launch { templateRepo.delete(t.template.id) }
    fun restoreTemplate(t: TemplateWithItems) = viewModelScope.launch { templateRepo.restore(t) }
    fun renameTemplate(t: TemplateWithItems, name: String) = viewModelScope.launch { templateRepo.rename(t.template.id, name) }

    fun deleteFood(f: FoodEntity) = viewModelScope.launch { foods.deleteOwn(f) }
    fun restoreFood(f: FoodEntity) = viewModelScope.launch { foods.restoreDeleted(f) }
    fun correctFood(f: FoodEntity, per: Per100, name: String) = viewModelScope.launch { foods.correct(f, per, name) }
    fun resetFood(f: FoodEntity) = viewModelScope.launch { foods.resetToOriginal(f) }

    /** Zeitpunkt des letzten Absturzes, null = keiner aufgezeichnet. */
    private val _crash = MutableStateFlow(crash.lastCrash())
    val lastCrash: StateFlow<String?> = _crash
    fun crashShareUri(): Uri = crash.shareUri()
    fun clearCrashLog() { crash.clear(); _crash.value = null }

    private val _sugar = MutableStateFlow<SugarRefresh>(SugarRefresh.Idle)
    val sugarRefresh: StateFlow<SugarRefresh> = _sugar

    /** Zucker für früher gescannte Produkte bei Open Food Facts nachladen (nur auf Knopfdruck). */
    fun refreshSugar() {
        if (_sugar.value == SugarRefresh.Working) return
        _sugar.value = SugarRefresh.Working
        viewModelScope.launch {
            _sugar.value = try { SugarRefresh.Done(foods.refreshOffSugar()) } catch (e: Exception) { SugarRefresh.Offline }
        }
    }

    private val _backup = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backup

    fun setShowSugar(show: Boolean) = viewModelScope.launch { repo.setShowSugar(show) }
    fun setMacroGoals(protein: Int?, carbs: Int?, fat: Int?, sugar: Int?) =
        viewModelScope.launch { repo.setMacroGoals(protein, carbs, fat, sugar) }
    fun setAutoBackup(on: Boolean) = viewModelScope.launch { repo.setAutoBackup(on) }

    fun backupNow() = runBackup { BackupState.Saved(backup.backupNow()) }
    fun restore(uri: Uri) = runBackup { BackupState.Restored(backup.restore(uri)) }

    /** CSV erzeugen und an [onReady] (Teilen-Dialog) übergeben. */
    fun exportCsv(onReady: (Uri) -> Unit) = viewModelScope.launch {
        try {
            onReady(backup.exportCsv())
        } catch (e: Exception) {
            _backup.value = BackupState.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun runBackup(block: suspend () -> BackupState) {
        if (_backup.value is BackupState.Working) return
        _backup.value = BackupState.Working
        viewModelScope.launch {
            _backup.value = try { block() } catch (e: Exception) { BackupState.Failed(e.message ?: e.javaClass.simpleName) }
        }
    }
    val settings: StateFlow<Settings?> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update

    fun setGoal(goal: Int) = viewModelScope.launch { repo.setDailyGoal(goal) }
    fun setShowMacros(show: Boolean) = viewModelScope.launch { repo.setShowMacros(show) }
    fun setIncludeTestVersions(include: Boolean) = viewModelScope.launch { repo.setIncludeTestVersions(include) }

    fun checkForUpdate() {
        if (_update.value is UpdateState.Checking || _update.value is UpdateState.Downloading) return
        _update.value = UpdateState.Checking
        viewModelScope.launch {
            _update.value = try {
                val include = repo.settings.first().includeTestVersions
                updates.findUpdate(include)?.let { UpdateState.Available(it) } ?: UpdateState.UpToDate
            } catch (e: Exception) {
                UpdateState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun download(release: ReleaseInfo) {
        _update.value = UpdateState.Downloading(release, 0f)
        viewModelScope.launch {
            _update.value = try {
                val file = updates.download(release) { p -> _update.value = UpdateState.Downloading(release, p) }
                UpdateState.Ready(release, file)
            } catch (e: Exception) {
                UpdateState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
