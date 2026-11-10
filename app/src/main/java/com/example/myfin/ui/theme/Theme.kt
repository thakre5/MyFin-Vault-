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
    primary = Primary600,
    onPrimary = CardWhite,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary900,
    secondary = SoftTeal,
    onSecondary = CardWhite,
    secondaryContainer = SoftTealLight,
    onSecondaryContainer = Primary900,
    background = Neutral50,
    onBackground = Neutral900,
    surface = CardWhite,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral500,
    outline = Neutral200,
    outlineVariant = Neutral300,
    error = Error500,
    onError = CardWhite,
    errorContainer = Error100,
    onErrorContainer = Error700
)

@Composable
fun MyfinTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = AndroidGraphicsColor.TRANSPARENT
            // true = dark text & icons (Neutral900) for all light screens across the app
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
