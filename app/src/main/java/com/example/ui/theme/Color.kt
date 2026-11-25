package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * User-selectable appearance mode persisted in DataStore.
 */
enum class AppThemeMode(val shortLabel: String, val displayName: String) {
    SYSTEM_AUTO("Auto", "System Auto"),
    APPLE_WHITE_LIGHT("White", "Apple White"),
    APPLE_OLED_DARK("OLED", "OLED Dark");

    fun nextCycle(): AppThemeMode = when (this) {
        SYSTEM_AUTO -> APPLE_WHITE_LIGHT
        APPLE_WHITE_LIGHT -> APPLE_OLED_DARK
        APPLE_OLED_DARK -> SYSTEM_AUTO
    }

    companion object {
        fun fromStoredName(raw: String?): AppThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM_AUTO
    }
}

/**
 * User-selectable Prayers Hero presentation style persisted in DataStore.
 */
enum class HeroCardStyle(val shortLabel: String, val displayName: String) {
    WAVE_CARD("Wave Card", "Wave Card"),
    MINIMAL_EDITORIAL("Minimal", "Minimal Editorial");

    companion object {
        fun fromStoredName(raw: String?): HeroCardStyle =
            entries.firstOrNull { it.name == raw } ?: WAVE_CARD
    }
}

/**
 * Editorial Swiss Minimal dual-theme palette (Warm Architectural White Light + True OLED Dark)
 * built for calm monochrome-first hierarchy (90%+ canvas & ink) with restrained Gold, Emerald,
 * and Vermilion functional accents.
 */
@Immutable
data class NoorThemePalette(
    val isDark: Boolean,
    val canvas: Color,
    val canvasElevated: Color,
    val surfaceCard: Color,
    val surfaceCardHighlight: Color,
    val surfaceNested: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val accentGold: Color,
    val accentGoldBright: Color,
    val accentGoldWash: Color,
    val onAccentGold: Color,
    val accentEmerald: Color,
    val accentEmeraldWash: Color,
    val onAccentEmerald: Color,
    val accentCoral: Color,
    val accentCoralWash: Color,
    val dockGlassSurface: Color,
    val dockGlassBorder: Color,
    val dockActivePill: Color,
    val segmentedTrack: Color,
    val segmentedThumb: Color,
    val scrimGlow: Color
)

val AppleWhiteLightPalette = NoorThemePalette(
    isDark = false,
    canvas = Color(0xFFF6F6F4),              // Architectural warm gallery off-white
    canvasElevated = Color(0xFFFAFAF8),
    surfaceCard = Color(0xFFFFFFFF),         // Pure crisp flat inset surface
    surfaceCardHighlight = Color(0xFFFAF8F2), // Whisper-subtle active prayer row tint
    surfaceNested = Color(0xFFF0F0EC),       // Quiet neutral recess for tracks & controls
    hairline = Color(0xFFE4E4DF),            // Razor-sharp 0.5dp Swiss hairline
    hairlineStrong = Color(0xFFCFCFC8),
    inkPrimary = Color(0xFF111111),          // Deep Swiss editorial ink
    inkSecondary = Color(0xFF666660),        // Calm warm slate secondary ink
    inkTertiary = Color(0xFF96968E),         // Quiet stone tertiary ink
    accentGold = Color(0xFF9E7012),          // Restrained burnished gold (WCAG AA on white)
    accentGoldBright = Color(0xFFB5821A),
    accentGoldWash = Color(0xFFF6F0E1),      // Subtle champagne tint
    onAccentGold = Color(0xFFFFFFFF),
    accentEmerald = Color(0xFF0E7C54),       // Calm editorial emerald (completed confirmations)
    accentEmeraldWash = Color(0xFFE9F4EF),
    onAccentEmerald = Color(0xFFFFFFFF),
    accentCoral = Color(0xFFC63A32),         // Restrained vermilion (urgent / Qaza only)
    accentCoralWash = Color(0xFFFBECEB),
    dockGlassSurface = Color(0xF5FFFFFF),    // Crisp frosted gallery bar
    dockGlassBorder = Color(0xFFE2E2DC),
    dockActivePill = Color(0xFFF2F1EC),
    segmentedTrack = Color(0xFFEBEBE6),
    segmentedThumb = Color(0xFFFFFFFF),
    scrimGlow = Color(0x0D000000)
)

val AppleOledDarkPalette = NoorThemePalette(
    isDark = true,
    canvas = Color(0xFF000000),              // True pure OLED black
    canvasElevated = Color(0xFF0A0A0C),
    surfaceCard = Color(0xFF111113),         // Flat obsidian inset surface
    surfaceCardHighlight = Color(0xFF18181B), // Whisper-subtle active prayer row tint
    surfaceNested = Color(0xFF1C1C20),       // Quiet dark recess for tracks & controls
    hairline = Color(0xFF232327),            // Razor-sharp OLED 0.5dp hairline
    hairlineStrong = Color(0xFF34343A),
    inkPrimary = Color(0xFFF4F4F0),          // Crisp porcelain ink
    inkSecondary = Color(0xFF8E8E93),        // Calm cool gray secondary ink
    inkTertiary = Color(0xFF5C5C62),         // Muted graphite tertiary ink
    accentGold = Color(0xFFDFB256),          // Refined champagne gold for OLED
    accentGoldBright = Color(0xFFECC876),
    accentGoldWash = Color(0xFF211B0F),
    onAccentGold = Color(0xFF111113),
    accentEmerald = Color(0xFF34C78E),       // Crisp OLED emerald
    accentEmeraldWash = Color(0xFF0D231B),
    onAccentEmerald = Color(0xFF061610),
    accentCoral = Color(0xFFFF6259),         // Restrained OLED vermilion
    accentCoralWash = Color(0xFF261213),
    dockGlassSurface = Color(0xF2121215),    // Smoked obsidian dock
    dockGlassBorder = Color(0xFF27272C),
    dockActivePill = Color(0xFF1D1D22),
    segmentedTrack = Color(0xFF18181B),
    segmentedThumb = Color(0xFF2A2A30),
    scrimGlow = Color(0x33000000)
)
