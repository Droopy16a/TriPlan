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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandDarkGreen,
    onPrimary = OffWhite,
    primaryContainer = BrandLightGreen,
    onPrimaryContainer = DeepGraphite,
    secondary = EmeraldGreen,
    onSecondary = OffWhite,
    tertiary = SkyBlue,
    onTertiary = DeepGraphite,
    background = OffWhite,
    onBackground = DeepGraphite,
    surface = CardBackground,
    onSurface = DeepGraphite,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = DeepGraphite,
    error = BrandDarkGreen,
    onError = OffWhite
)

private val DarkColors = darkColorScheme(
    primary = BrandDarkGreen,
    onPrimary = OffWhite,
    primaryContainer = BrandDarkGreen,
    onPrimaryContainer = OffWhite,
    secondary = EmeraldGreen,
    onSecondary = OffWhite,
    tertiary = SkyBlue,
    onTertiary = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DeepGraphite,
    onSurfaceVariant = DarkText,
    error = BrandDarkGreen,
    onError = OffWhite
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
