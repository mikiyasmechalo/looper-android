package com.example.looper_android.ui.widgets

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

enum class WidgetThemeMode {
    ADAPTIVE,
    DARK,
    LIGHT,
    SYSTEM
}

data class WidgetConfig(
    val themeMode: WidgetThemeMode = WidgetThemeMode.ADAPTIVE,
    val transparency: Float = 0.1f, // 0.0f = solid, 1.0f = transparent
    val showFavorite: Boolean = false
)

object WidgetSettingsManager {
    private const val PREFS_NAME = "looper_widget_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context, widgetType: String, appWidgetId: Int = -1): WidgetConfig {
        val prefs = getPrefs(context)
        val prefix = if (appWidgetId != -1) "w_${appWidgetId}_" else "type_${widgetType}_"

        val defaultShowFav = if (widgetType == "FULL_ART" || widgetType == "SQUARE") true else false
        val themeStr = prefs.getString("${prefix}theme", prefs.getString("default_theme", WidgetThemeMode.ADAPTIVE.name)) ?: WidgetThemeMode.ADAPTIVE.name
        val transparency = prefs.getFloat("${prefix}transparency", prefs.getFloat("default_transparency", 0.1f))
        val showFav = prefs.getBoolean("${prefix}show_favorite", prefs.getBoolean("default_show_favorite", defaultShowFav))

        val mode = try { WidgetThemeMode.valueOf(themeStr) } catch (_: Exception) { WidgetThemeMode.ADAPTIVE }
        return WidgetConfig(mode, transparency, showFav)
    }

    fun saveConfig(context: Context, widgetType: String, appWidgetId: Int, config: WidgetConfig) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        if (appWidgetId != -1) {
            val prefix = "w_${appWidgetId}_"
            editor.putString("${prefix}theme", config.themeMode.name)
            editor.putFloat("${prefix}transparency", config.transparency)
            editor.putBoolean("${prefix}show_favorite", config.showFavorite)
        }

        // Also update type default
        val typePrefix = "type_${widgetType}_"
        editor.putString("${typePrefix}theme", config.themeMode.name)
        editor.putFloat("${typePrefix}transparency", config.transparency)
        editor.putBoolean("${typePrefix}show_favorite", config.showFavorite)

        editor.apply()
    }

    fun getEffectiveCardColor(context: Context, config: WidgetConfig, adaptiveColor: Color): Color {
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val baseColorInt = when (config.themeMode) {
            WidgetThemeMode.ADAPTIVE -> {
                val a = adaptiveColor
                val r = (a.red * 255).toInt()
                val g = (a.green * 255).toInt()
                val b = (a.blue * 255).toInt()
                android.graphics.Color.rgb(r, g, b)
            }
            WidgetThemeMode.DARK -> android.graphics.Color.rgb(18, 18, 18)
            WidgetThemeMode.LIGHT -> android.graphics.Color.rgb(238, 238, 240)
            WidgetThemeMode.SYSTEM -> if (isSystemDark) android.graphics.Color.rgb(18, 18, 18) else android.graphics.Color.rgb(238, 238, 240)
        }

        // Apply transparency (alpha: 1.0f - transparency)
        val alphaFloat = (1.0f - config.transparency).coerceIn(0.0f, 1.0f)
        val alphaInt = (alphaFloat * 255).toInt()
        val argb = (alphaInt shl 24) or (baseColorInt and 0x00FFFFFF)
        return Color(argb)
    }

    fun getEffectiveTextColor(context: Context, config: WidgetConfig): Color {
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isLight = when (config.themeMode) {
            WidgetThemeMode.LIGHT -> true
            WidgetThemeMode.SYSTEM -> !isSystemDark
            else -> false
        }
        // If transparency is very high (>= 0.7), force white text so it is legible on wallpaper
        if (config.transparency >= 0.7f) return Color.White
        return if (isLight) Color(0xFF111111) else Color.White
    }

    fun getEffectiveSecondaryTextColor(context: Context, config: WidgetConfig): Color {
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isLight = when (config.themeMode) {
            WidgetThemeMode.LIGHT -> true
            WidgetThemeMode.SYSTEM -> !isSystemDark
            else -> false
        }
        if (config.transparency >= 0.7f) return Color(0xB3FFFFFF)
        return if (isLight) Color(0x99000000) else Color(0x99FFFFFF)
    }
}
