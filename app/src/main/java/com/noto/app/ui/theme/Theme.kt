package com.noto.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightOnBackground,
    onPrimary = LightBackground,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkOnBackground,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val OledColorScheme = darkColorScheme(
    primary = OledOnBackground,
    onPrimary = OledBackground,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    surfaceContainer = OledSurfaceContainer,
    outline = OledOutline,
    outlineVariant = OledOutlineVariant
)

private val EyeCareColorScheme = lightColorScheme(
    primary = EyeCareOnBackground,
    onPrimary = EyeCareBackground,
    background = EyeCareBackground,
    onBackground = EyeCareOnBackground,
    surface = EyeCareSurface,
    onSurface = EyeCareOnSurface,
    surfaceVariant = EyeCareSurfaceVariant,
    onSurfaceVariant = EyeCareOnSurfaceVariant,
    surfaceContainer = EyeCareSurfaceContainer,
    outline = EyeCareOutline,
    outlineVariant = EyeCareOutlineVariant
)

@Composable
fun NotoTheme(
    themeMode: com.noto.app.data.repository.ThemeMode = com.noto.app.data.repository.ThemeMode.SYSTEM,
    fontFamily: com.noto.app.data.repository.AppFontFamily = com.noto.app.data.repository.AppFontFamily.SYSTEM_SANS,
    fontSizeScale: com.noto.app.data.repository.FontSizeScale = com.noto.app.data.repository.FontSizeScale.DEFAULT,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        com.noto.app.data.repository.ThemeMode.SYSTEM -> isSystemDark
        com.noto.app.data.repository.ThemeMode.LIGHT -> false
        com.noto.app.data.repository.ThemeMode.DARK -> true
        com.noto.app.data.repository.ThemeMode.OLED -> true
        com.noto.app.data.repository.ThemeMode.EYE_CARE -> false
    }

    val colorScheme = when (themeMode) {
        com.noto.app.data.repository.ThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        com.noto.app.data.repository.ThemeMode.LIGHT -> LightColorScheme
        com.noto.app.data.repository.ThemeMode.DARK -> DarkColorScheme
        com.noto.app.data.repository.ThemeMode.OLED -> OledColorScheme
        com.noto.app.data.repository.ThemeMode.EYE_CARE -> EyeCareColorScheme
    }

    val composeFont = when (fontFamily) {
        com.noto.app.data.repository.AppFontFamily.SYSTEM_SANS -> androidx.compose.ui.text.font.FontFamily.Default
        com.noto.app.data.repository.AppFontFamily.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
        com.noto.app.data.repository.AppFontFamily.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
    }

    val typography = getAppTypography(composeFont, fontSizeScale.scaleFactor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.DisposableEffect(isDark, themeMode) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
