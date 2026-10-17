package com.noto.app.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supported Theme Modes.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED,
    EYE_CARE
}

/**
 * Supported Custom Font Families.
 */
enum class AppFontFamily {
    SYSTEM_SANS,
    SERIF,
    MONOSPACE
}

/**
 * Supported Font Size Scaling.
 */
enum class FontSizeScale(val scaleFactor: Float) {
    SMALL(0.88f),
    DEFAULT(1.0f),
    LARGE(1.15f)
}

/**
 * Lightweight settings storage using SharedPreferences.
 */
class ThemeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("noto_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadSavedTheme())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _fontFamily = MutableStateFlow(loadSavedFontFamily())
    val fontFamily: StateFlow<AppFontFamily> = _fontFamily.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(loadSavedFontSizeScale())
    val fontSizeScale: StateFlow<FontSizeScale> = _fontSizeScale.asStateFlow()

    private fun loadSavedTheme(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private fun loadSavedFontFamily(): AppFontFamily {
        val name = prefs.getString("app_font_family", AppFontFamily.SYSTEM_SANS.name)
        return runCatching { AppFontFamily.valueOf(name ?: AppFontFamily.SYSTEM_SANS.name) }.getOrDefault(AppFontFamily.SYSTEM_SANS)
    }

    private fun loadSavedFontSizeScale(): FontSizeScale {
        val name = prefs.getString("font_size_scale", FontSizeScale.DEFAULT.name)
        return runCatching { FontSizeScale.valueOf(name ?: FontSizeScale.DEFAULT.name) }.getOrDefault(FontSizeScale.DEFAULT)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setFontFamily(font: AppFontFamily) {
        prefs.edit().putString("app_font_family", font.name).apply()
        _fontFamily.value = font
    }

    fun setFontSizeScale(scale: FontSizeScale) {
        prefs.edit().putString("font_size_scale", scale.name).apply()
        _fontSizeScale.value = scale
    }

    fun toggleTheme() {
        val next = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.OLED
            ThemeMode.OLED -> ThemeMode.EYE_CARE
            ThemeMode.EYE_CARE -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(next)
    }
}
