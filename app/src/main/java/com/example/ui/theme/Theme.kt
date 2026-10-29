package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NoorDarkColorScheme = darkColorScheme(
    primary = ElegantGold,
    onPrimary = OnGoldDark,
    primaryContainer = GoldContainerDark,
    onPrimaryContainer = RadiantAmberGlow,
    secondary = EmeraldCompleted,
    onSecondary = DeepSlateBlue,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldCompleted,
    tertiary = RadiantAmberGlow,
    background = DeepSlateBlue,
    onBackground = CrispOffWhite,
    surface = ElevatedSlateSurface,
    onSurface = CrispOffWhite,
    surfaceVariant = ElevatedSlateCard,
    onSurfaceVariant = MutedPeriwinkle,
    outline = SlateStrokeBorder,
    error = CoralAlert
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NoorDarkColorScheme,
        typography = Typography,
        content = content
    )
}
