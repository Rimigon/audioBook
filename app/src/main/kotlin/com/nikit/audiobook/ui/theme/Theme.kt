package com.nikit.audiobook.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors =
    lightColorScheme(
        primary = Accent,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = AccentMuted,
        background = PaperLight,
        surface = PaperSurface,
        onBackground = InkPrimary,
        onSurface = InkPrimary,
        surfaceVariant = PaperLight,
        onSurfaceVariant = InkSecondary,
    )

private val DarkColors =
    darkColorScheme(
        primary = Accent,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = AccentMuted,
        background = SlateDark,
        surface = SlateSurface,
        onBackground = SlateOnSurface,
        onSurface = SlateOnSurface,
        surfaceVariant = SlateDark,
        onSurfaceVariant = SlateOnSurface,
    )

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun AudioBookTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark =
        when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = EditorialTypography,
        content = content,
    )
}
