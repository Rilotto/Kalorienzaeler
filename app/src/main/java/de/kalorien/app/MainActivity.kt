package de.kalorien.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kalorien.app.core.data.Settings
import de.kalorien.app.core.data.ThemeMode
import de.kalorien.app.core.ui.theme.KalorienTheme
import de.kalorien.app.core.ui.theme.Palette
import de.kalorien.app.navigation.KalorienAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settingsFlow = (application as KalorienApp).container.settings.settings
        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null as Settings?)
            val dark = when (settings?.themeMode ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            KalorienTheme(darkTheme = dark, palette = settings?.palette ?: Palette.DEFAULT) {
                KalorienAppRoot()
            }
        }
    }
}
