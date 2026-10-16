package de.kalorien.app.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.kalorien.app.KalorienApp
import de.kalorien.app.feature.add.AddViewModel
import de.kalorien.app.feature.settings.SettingsViewModel
import de.kalorien.app.feature.today.TodayViewModel

/** Eine Fabrik für alle ViewModels. Neue ViewModels hier eintragen. */
object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { val c = app().container; TodayViewModel(c.entries, c.foods, c.settings) }
        initializer { val c = app().container; AddViewModel(c.foods, c.entries, c.blsImporter) }
        initializer { val c = app().container; SettingsViewModel(c.settings, c.updates, c.backup) }
    }

    private fun CreationExtras.app(): KalorienApp =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KalorienApp
}
