package com.example.looper_android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class WidgetThemeMode {
    ADAPTIVE,
    DARK,
    LIGHT,
    SYSTEM
}

data class WidgetConfig(
    val themeMode: WidgetThemeMode = WidgetThemeMode.ADAPTIVE,
    val transparency: Float = 0.0f, // 0.0f = solid, 1.0f = transparent
    val showFavorite: Boolean = false,
    val circularCapsule: Boolean = false,
    val showOutline: Boolean = false
)

object WidgetSettingsManager {
    private const val PREFS_NAME = "looper_widget_prefs"

    private val _configVersion = MutableStateFlow(0L)
    val configVersion: StateFlow<Long> = _configVersion

    fun notifyConfigChanged() {
        _configVersion.value = System.currentTimeMillis()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context, widgetType: String, appWidgetId: Int = -1): WidgetConfig {
        val prefs = getPrefs(context)
        val defaultThemeMode = if (widgetType == "MINI_PLAYER") WidgetThemeMode.DARK else WidgetThemeMode.ADAPTIVE
        val defaultTransparency = if (widgetType == "MINI_PLAYER") 0.70f else 0.30f
        val defaultShowFav = if (widgetType == "FULL_ART" || widgetType == "SQUARE") true else false
        val defaultShowOutline = if (widgetType == "MINI_PLAYER") true else false

        var themeStr: String? = null
        var transparency: Float? = null
        var showFav: Boolean? = null
        var circularCapsule: Boolean? = null
        var showOutline: Boolean? = null

        if (appWidgetId != -1 && prefs.contains("w_${appWidgetId}_theme")) {
            themeStr = prefs.getString("w_${appWidgetId}_theme", null)
            if (prefs.contains("w_${appWidgetId}_transparency")) {
                transparency = prefs.getFloat("w_${appWidgetId}_transparency", defaultTransparency)
            }
            if (prefs.contains("w_${appWidgetId}_show_favorite")) {
                showFav = prefs.getBoolean("w_${appWidgetId}_show_favorite", defaultShowFav)
            }
            if (prefs.contains("w_${appWidgetId}_circular_capsule")) {
                circularCapsule = prefs.getBoolean("w_${appWidgetId}_circular_capsule", false)
            }
            if (prefs.contains("w_${appWidgetId}_show_outline")) {
                showOutline = prefs.getBoolean("w_${appWidgetId}_show_outline", defaultShowOutline)
            }
        }

        val typePrefix = "type_${widgetType}_"
        if (themeStr == null && prefs.contains("${typePrefix}theme")) {
            themeStr = prefs.getString("${typePrefix}theme", null)
        }
        if (transparency == null && prefs.contains("${typePrefix}transparency")) {
            transparency = prefs.getFloat("${typePrefix}transparency", defaultTransparency)
        }
        if (showFav == null && prefs.contains("${typePrefix}show_favorite")) {
            showFav = prefs.getBoolean("${typePrefix}show_favorite", defaultShowFav)
        }
        if (circularCapsule == null && prefs.contains("${typePrefix}circular_capsule")) {
            circularCapsule = prefs.getBoolean("${typePrefix}circular_capsule", false)
        }
        if (showOutline == null && prefs.contains("${typePrefix}show_outline")) {
            showOutline = prefs.getBoolean("${typePrefix}show_outline", defaultShowOutline)
        }

        val finalThemeStr = themeStr ?: defaultThemeMode.name
        val finalTransparency = transparency ?: defaultTransparency
        val finalShowFav = showFav ?: defaultShowFav
        val finalCircularCapsule = circularCapsule ?: false
        val finalShowOutline = showOutline ?: defaultShowOutline

        val mode = try { WidgetThemeMode.valueOf(finalThemeStr) } catch (_: Exception) { defaultThemeMode }
        return WidgetConfig(mode, finalTransparency, finalShowFav, finalCircularCapsule, finalShowOutline)
    }

    fun saveConfig(
        context: Context,
        widgetType: String,
        appWidgetId: Int,
        config: WidgetConfig,
        originWidgetType: String? = null
    ) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        // 1. Update type-level config
        val typePrefix = "type_${widgetType}_"
        editor.putString("${typePrefix}theme", config.themeMode.name)
        editor.putFloat("${typePrefix}transparency", config.transparency)
        editor.putBoolean("${typePrefix}show_favorite", config.showFavorite)
        editor.putBoolean("${typePrefix}circular_capsule", config.circularCapsule)
        editor.putBoolean("${typePrefix}show_outline", config.showOutline)

        // 2. Find ALL widget IDs on home screen belonging strictly to this widgetType and update them
        try {
            val manager = AppWidgetManager.getInstance(context)
            val receiverClass = when (widgetType) {
                "MINI_PLAYER" -> MiniPlayerWidgetReceiver::class.java
                "FULL_ART" -> FullAlbumArtWidgetReceiver::class.java
                "LYRICS" -> LyricsWidgetReceiver::class.java
                "SQUARE" -> SquareProgressWidgetReceiver::class.java
                "COMPACT" -> CompactWidgetReceiver::class.java
                else -> null
            }
            if (receiverClass != null) {
                val component = ComponentName(context, receiverClass)
                val ids = manager.getAppWidgetIds(component)
                for (id in ids) {
                    val p = "w_${id}_"
                    editor.putString("${p}theme", config.themeMode.name)
                    editor.putFloat("${p}transparency", config.transparency)
                    editor.putBoolean("${p}show_favorite", config.showFavorite)
                    editor.putBoolean("${p}circular_capsule", config.circularCapsule)
                    editor.putBoolean("${p}show_outline", config.showOutline)
                }
            }
        } catch (_: Exception) {}

        // 3. Only update calling appWidgetId if it belongs to this widgetType
        val isSameType = originWidgetType == null || originWidgetType == widgetType
        if (appWidgetId != -1 && isSameType) {
            val prefix = "w_${appWidgetId}_"
            editor.putString("${prefix}theme", config.themeMode.name)
            editor.putFloat("${prefix}transparency", config.transparency)
            editor.putBoolean("${prefix}show_favorite", config.showFavorite)
            editor.putBoolean("${prefix}circular_capsule", config.circularCapsule)
            editor.putBoolean("${prefix}show_outline", config.showOutline)
        }

        editor.commit()
        notifyConfigChanged()
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

        // Apply transparency: 0.0f = strictly 100% opaque solid, 1.0f = completely transparent
        val alphaFloat = (1.0f - config.transparency).coerceIn(0.0f, 1.0f)
        val alphaInt = if (config.transparency <= 0.005f) 255 else (alphaFloat * 255).toInt()
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

    fun isEffectiveLightMode(context: Context, config: WidgetConfig): Boolean {
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return when (config.themeMode) {
            WidgetThemeMode.LIGHT -> true
            WidgetThemeMode.SYSTEM -> !isSystemDark
            else -> false
        }
    }

    fun isDarkControlIcons(context: Context, config: WidgetConfig): Boolean {
        return isEffectiveLightMode(context, config) && config.transparency < 0.70f
    }

    fun getEffectiveControlIconColor(context: Context, config: WidgetConfig): Color {
        return if (isDarkControlIcons(context, config)) Color(0xFF111111) else Color.White
    }
}
