package de.kalorien.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** App-Theme. Hell/dunkel folgt dem System. Keine dynamischen Farben, damit die App überall gleich aussieht. */
@Composable
fun KalorienTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val t = if (darkTheme) DarkTokens else LightTokens
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = t.accent, onPrimary = t.bg, primaryContainer = t.accentSoft, onPrimaryContainer = t.accent,
            background = t.bg, onBackground = t.fg, surface = t.surface, onSurface = t.fg,
            surfaceVariant = t.bg, onSurfaceVariant = t.muted, outline = t.line, outlineVariant = t.line,
            error = t.over, errorContainer = t.overSoft, surfaceContainer = t.surface,
        )
    } else {
        lightColorScheme(
            primary = t.accent, onPrimary = t.surface, primaryContainer = t.accentSoft, onPrimaryContainer = t.accent,
            background = t.bg, onBackground = t.fg, surface = t.surface, onSurface = t.fg,
            surfaceVariant = t.bg, onSurfaceVariant = t.muted, outline = t.line, outlineVariant = t.line,
            error = t.over, errorContainer = t.overSoft, surfaceContainer = t.surface,
        )
    }
    CompositionLocalProvider(LocalKalorienColors provides t) {
        MaterialTheme(colorScheme = scheme, typography = KalorienTypography, content = content)
    }
}

/** Zugriff auf die Tokens: `Tokens.colors.accent`. */
object Tokens {
    val colors: KalorienColors
        @Composable @ReadOnlyComposable get() = LocalKalorienColors.current
}
