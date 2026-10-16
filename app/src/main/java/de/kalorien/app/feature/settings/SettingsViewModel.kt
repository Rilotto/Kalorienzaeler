package de.kalorien.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import de.kalorien.app.core.data.BackupManager
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
) : ViewModel() {
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
