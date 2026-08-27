package com.example.myfin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = CardWhite,
    primaryContainer = AccentPurpleLight,
    onPrimaryContainer = AccentPurpleDark,
    secondary = SoftTeal,
    onSecondary = CardWhite,
    background = CanvasLight,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = CanvasLight,
    onSurfaceVariant = TextMuted,
    outline = BorderLight,
    error = SoftRed,
    onError = CardWhite
)

@Composable
fun MyfinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
