package com.ramble.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = RambleGreen,
    onPrimary = Color.White,
    primaryContainer = RambleGreen.copy(alpha = 0.12f),
    onPrimaryContainer = RambleGreenDark,
    secondary = RambleGreenDark,
    onSecondary = Color.White,
    tertiary = RambleNavy,
    onTertiary = RambleLight,
    background = RambleLight,
    onBackground = RambleNavy,
    surface = Color.White,
    onSurface = RambleNavy,
    surfaceVariant = RambleLight,
    onSurfaceVariant = RambleNavy,
    error = Color(0xFFB00020),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = RambleGreen,
    onPrimary = RambleNavy,
    primaryContainer = RambleGreenDark,
    onPrimaryContainer = RambleLight,
    secondary = RambleGreenDark,
    onSecondary = RambleNavy,
    tertiary = RambleLight,
    onTertiary = RambleNavy,
    background = RambleNavy,
    onBackground = RambleLight,
    surface = RambleNavy,
    onSurface = RambleLight,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = RambleLight,
    error = Color(0xFFCF6679),
    onError = RambleNavy
)

@Composable
fun RambleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Keep false by default to enforce our premium brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RambleTypography,
        shapes = RambleShapes,
        content = content
    )
}
