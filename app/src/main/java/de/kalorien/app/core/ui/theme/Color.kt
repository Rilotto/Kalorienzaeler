package de.kalorien.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design-Tokens aus dem Prototyp. Farben NUR hier ändern, nirgends sonst Farbwerte hart kodieren.
 */
@Immutable
data class KalorienColors(
    val bg: Color,
    val surface: Color,
    val line: Color,
    val fg: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color,
    val over: Color,
    val overSoft: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val sugar: Color,
)

val LightTokens = KalorienColors(
    bg = Color(0xFFF3F5F2),
    surface = Color(0xFFFFFFFF),
    line = Color(0xFFDDE3DC),
    fg = Color(0xFF18201B),
    muted = Color(0xFF5E6B63),
    accent = Color(0xFF2F6B4F),
    accentSoft = Color(0xFFDCEBE2),
    over = Color(0xFFB4432F),
    overSoft = Color(0xFFF6DDD7),
    protein = Color(0xFF3C6FA8),
    carbs = Color(0xFFC08A1E),
    fat = Color(0xFF8A4F9E),
    sugar = Color(0xFFC2577A),
)

val DarkTokens = KalorienColors(
    bg = Color(0xFF0F1411),
    surface = Color(0xFF18201B),
    line = Color(0xFF2A342E),
    fg = Color(0xFFE8EEE9),
    muted = Color(0xFF95A39A),
    accent = Color(0xFF6FBF93),
    accentSoft = Color(0xFF1F3328),
    over = Color(0xFFE7826D),
    overSoft = Color(0xFF3A221D),
    protein = Color(0xFF7FA9DA),
    carbs = Color(0xFFE0B456),
    fat = Color(0xFFC595D6),
    sugar = Color(0xFFE58FAE),
)

val LocalKalorienColors = staticCompositionLocalOf { LightTokens }
