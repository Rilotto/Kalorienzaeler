package de.kalorien.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val updates: UpdateRepository,
) : ViewModel() {
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
