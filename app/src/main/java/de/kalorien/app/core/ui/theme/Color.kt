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

/** Kurzform zum Anlegen weiterer Schemas: Makro-Farben werden vom Standard übernommen, wenn nicht angegeben. */
private fun light(
    bg: Long, line: Long, fg: Long, muted: Long, accent: Long, accentSoft: Long,
    over: Long = 0xFFB4432F, overSoft: Long = 0xFFF6DDD7, surface: Long = 0xFFFFFFFF,
    protein: Long = 0xFF3C6FA8, carbs: Long = 0xFFC08A1E, fat: Long = 0xFF8A4F9E, sugar: Long = 0xFFC2577A,
) = KalorienColors(
    Color(bg), Color(surface), Color(line), Color(fg), Color(muted), Color(accent), Color(accentSoft),
    Color(over), Color(overSoft), Color(protein), Color(carbs), Color(fat), Color(sugar),
)

private fun dark(
    bg: Long, surface: Long, line: Long, fg: Long, muted: Long, accent: Long, accentSoft: Long,
    over: Long = 0xFFE7826D, overSoft: Long = 0xFF3A221D,
    protein: Long = 0xFF7FA9DA, carbs: Long = 0xFFE0B456, fat: Long = 0xFFC595D6, sugar: Long = 0xFFE58FAE,
) = KalorienColors(
    Color(bg), Color(surface), Color(line), Color(fg), Color(muted), Color(accent), Color(accentSoft),
    Color(over), Color(overSoft), Color(protein), Color(carbs), Color(fat), Color(sugar),
)

/**
 * Farbschemas zur Auswahl (Einstellungen → Darstellung). Jedes gibt es hell und dunkel.
 * Regel: [KalorienColors.accent] muss mit [KalorienColors.surface] als Schrift gut lesbar sein (Knöpfe).
 * Neue Schemas hier ergänzen und in strings.xml benennen (palette_<name>).
 */
enum class Palette(val retro: Boolean = false) {
    /** Salbeigrün – das ursprüngliche Schema */
    DEFAULT,
    PEACH,
    LAVENDER,
    MINT,
    SKY,
    ROSE,
    /** 90er: Memphis-Design – Creme, Türkis, Gelb, Magenta, kräftige schwarze Linien */
    MEMPHIS(retro = true),
    /** 90er: Vaporwave – Lila, Neon-Pink und Cyan */
    VAPORWAVE(retro = true),
    ;

    fun colors(darkTheme: Boolean): KalorienColors = if (darkTheme) darkColors else lightColors

    val lightColors: KalorienColors by lazy {
        when (this) {
            DEFAULT -> LightTokens
            PEACH -> light(0xFFFFF6F1, 0xFFF2DED3, 0xFF2B1D17, 0xFF7A625A, 0xFFB5552F, 0xFFFBE3D7, over = 0xFFB23A48, overSoft = 0xFFF8DADF)
            LAVENDER -> light(0xFFF6F4FB, 0xFFE1DCEF, 0xFF1F1B2B, 0xFF6A6480, 0xFF6B57B0, 0xFFE6E0F6)
            MINT -> light(0xFFF1F8F6, 0xFFD5EAE4, 0xFF15231F, 0xFF5A716A, 0xFF1C7564, 0xFFD3EFE8)
            SKY -> light(0xFFF2F6FB, 0xFFD9E3EF, 0xFF172231, 0xFF5C6B7E, 0xFF2F6DA8, 0xFFDCE8F6, protein = 0xFF5B5FC7)
            ROSE -> light(0xFFFBF3F5, 0xFFEFDCE2, 0xFF2A1A20, 0xFF7A6068, 0xFFA3416A, 0xFFF6DEE6, over = 0xFFC0392B, overSoft = 0xFFF7DAD5, sugar = 0xFFD9822B)
            MEMPHIS -> light(
                0xFFFFF8E7, 0xFF1F1F1F, 0xFF111111, 0xFF55524A, 0xFF007C7C, 0xFFFFE45C,
                over = 0xFFD6006F, overSoft = 0xFFFFD1E8, protein = 0xFF3D5AFE, carbs = 0xFFFF8F00, fat = 0xFF7B2CBF, sugar = 0xFFD6006F,
            )
            VAPORWAVE -> light(
                0xFFFDF0FF, 0xFFF1C6F5, 0xFF2A1240, 0xFF7A5C8F, 0xFF8E2BC4, 0xFFD9F7FF,
                over = 0xFFE0197A, overSoft = 0xFFFFD6EA, protein = 0xFF0391B5, carbs = 0xFFE0951A, fat = 0xFFB15CF0, sugar = 0xFFF054B8,
            )
        }
    }

    val darkColors: KalorienColors by lazy {
        when (this) {
            DEFAULT -> DarkTokens
            PEACH -> dark(0xFF1A1311, 0xFF241A17, 0xFF3A2C27, 0xFFF5E9E3, 0xFFB9A39A, 0xFFF0A07F, 0xFF3D2820, over = 0xFFF08A96, overSoft = 0xFF3E2226)
            LAVENDER -> dark(0xFF13111A, 0xFF1C1926, 0xFF2E2A3D, 0xFFECE9F5, 0xFFA39DB8, 0xFFB5A6F0, 0xFF2C2645)
            MINT -> dark(0xFF0E1513, 0xFF16201D, 0xFF26352F, 0xFFE4F1EC, 0xFF93ABA3, 0xFF74D3BE, 0xFF1A3530)
            SKY -> dark(0xFF0F141B, 0xFF172029, 0xFF283444, 0xFFE6EEF7, 0xFF98A8BA, 0xFF86B8EA, 0xFF1E3147, protein = 0xFFA3A6F5)
            ROSE -> dark(0xFF1A1215, 0xFF241A1E, 0xFF3A2A30, 0xFFF5E8EC, 0xFFB89DA6, 0xFFEE9AB8, 0xFF3D2330, over = 0xFFF28B6E, sugar = 0xFFF0B070)
            MEMPHIS -> dark(
                0xFF141414, 0xFF1F1F1F, 0xFF4A4A4A, 0xFFF5F5F5, 0xFFA8A8A8, 0xFF2EE6D6, 0xFF3A3410,
                over = 0xFFFF4FA3, overSoft = 0xFF3D1A2C, protein = 0xFF8C9EFF, carbs = 0xFFFFB84D, fat = 0xFFC77DFF, sugar = 0xFFFF4FA3,
            )
            VAPORWAVE -> dark(
                0xFF1A0B2E, 0xFF241040, 0xFF3D2266, 0xFFF7E8FF, 0xFFB9A3D6, 0xFF01CDFE, 0xFF2B1F5C,
                over = 0xFFFF71CE, overSoft = 0xFF4A1840, protein = 0xFF7DF9FF, carbs = 0xFFFFFB96, fat = 0xFFB967FF, sugar = 0xFFFF71CE,
            )
        }
    }
}

