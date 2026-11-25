package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalNoorColors = staticCompositionLocalOf<NoorThemePalette> {
    AppleOledDarkPalette
}

object NoorTheme {
    val colors: NoorThemePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalNoorColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalNoorColors.current.isDark
}

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 240),
        label = label
    )
    return color
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val target = if (darkTheme) AppleOledDarkPalette else AppleWhiteLightPalette

    val animatedPalette = NoorThemePalette(
        isDark = darkTheme,
        canvas = animateThemeColor(target.canvas, "theme_canvas"),
        canvasElevated = animateThemeColor(target.canvasElevated, "theme_canvas_elev"),
        surfaceCard = animateThemeColor(target.surfaceCard, "theme_surface_card"),
        surfaceCardHighlight = animateThemeColor(target.surfaceCardHighlight, "theme_surface_hi"),
        surfaceNested = animateThemeColor(target.surfaceNested, "theme_surface_nested"),
        hairline = animateThemeColor(target.hairline, "theme_hairline"),
        hairlineStrong = animateThemeColor(target.hairlineStrong, "theme_hairline_strong"),
        inkPrimary = animateThemeColor(target.inkPrimary, "theme_ink_primary"),
        inkSecondary = animateThemeColor(target.inkSecondary, "theme_ink_secondary"),
        inkTertiary = animateThemeColor(target.inkTertiary, "theme_ink_tertiary"),
        accentGold = animateThemeColor(target.accentGold, "theme_accent_gold"),
        accentGoldBright = animateThemeColor(target.accentGoldBright, "theme_accent_gold_bright"),
        accentGoldWash = animateThemeColor(target.accentGoldWash, "theme_accent_gold_wash"),
        onAccentGold = animateThemeColor(target.onAccentGold, "theme_on_accent_gold"),
        accentEmerald = animateThemeColor(target.accentEmerald, "theme_accent_emerald"),
        accentEmeraldWash = animateThemeColor(target.accentEmeraldWash, "theme_accent_emerald_wash"),
        onAccentEmerald = animateThemeColor(target.onAccentEmerald, "theme_on_accent_emerald"),
        accentCoral = animateThemeColor(target.accentCoral, "theme_accent_coral"),
        accentCoralWash = animateThemeColor(target.accentCoralWash, "theme_accent_coral_wash"),
        dockGlassSurface = animateThemeColor(target.dockGlassSurface, "theme_dock_glass"),
        dockGlassBorder = animateThemeColor(target.dockGlassBorder, "theme_dock_border"),
        dockActivePill = animateThemeColor(target.dockActivePill, "theme_dock_active"),
        segmentedTrack = animateThemeColor(target.segmentedTrack, "theme_seg_track"),
        segmentedThumb = animateThemeColor(target.segmentedThumb, "theme_seg_thumb"),
        scrimGlow = animateThemeColor(target.scrimGlow, "theme_scrim")
    )

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = animatedPalette.accentGold,
            onPrimary = animatedPalette.onAccentGold,
            primaryContainer = animatedPalette.accentGoldWash,
            onPrimaryContainer = animatedPalette.accentGoldBright,
            secondary = animatedPalette.accentEmerald,
            onSecondary = animatedPalette.onAccentEmerald,
            secondaryContainer = animatedPalette.accentEmeraldWash,
            onSecondaryContainer = animatedPalette.accentEmerald,
            tertiary = animatedPalette.accentGoldBright,
            background = animatedPalette.canvas,
            onBackground = animatedPalette.inkPrimary,
            surface = animatedPalette.surfaceCard,
            onSurface = animatedPalette.inkPrimary,
            surfaceVariant = animatedPalette.surfaceNested,
            onSurfaceVariant = animatedPalette.inkSecondary,
            outline = animatedPalette.hairline,
            error = animatedPalette.accentCoral
        )
    } else {
        lightColorScheme(
            primary = animatedPalette.accentGold,
            onPrimary = animatedPalette.onAccentGold,
            primaryContainer = animatedPalette.accentGoldWash,
            onPrimaryContainer = animatedPalette.accentGold,
            secondary = animatedPalette.accentEmerald,
            onSecondary = animatedPalette.onAccentEmerald,
            secondaryContainer = animatedPalette.accentEmeraldWash,
            onSecondaryContainer = animatedPalette.accentEmerald,
            tertiary = animatedPalette.accentGoldBright,
            background = animatedPalette.canvas,
            onBackground = animatedPalette.inkPrimary,
            surface = animatedPalette.surfaceCard,
            onSurface = animatedPalette.inkPrimary,
            surfaceVariant = animatedPalette.surfaceNested,
            onSurfaceVariant = animatedPalette.inkSecondary,
            outline = animatedPalette.hairline,
            error = animatedPalette.accentCoral
        )
    }

    CompositionLocalProvider(LocalNoorColors provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
