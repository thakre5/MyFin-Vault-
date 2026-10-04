package com.example.myfin.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidGraphicsColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = AndroidGraphicsColor.TRANSPARENT
            // true = dark text & icons (black/dark gray) for all light screens across the app
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
