package com.example.looper_android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    object Keys {
        val DYNAMIC_THEMING = booleanPreferencesKey("dynamic_theming")
        val DISABLE_BLUR = booleanPreferencesKey("disable_blur")
        val DYNAMIC_ACCENT_COLOR = booleanPreferencesKey("dynamic_accent_color")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val CUSTOM_ACCENT_COLOR = intPreferencesKey("custom_accent_color")
        val DYNAMIC_LYRICS = booleanPreferencesKey("dynamic_lyrics")
        val BLURRED_ARTWORK_FOR_LYRICS = booleanPreferencesKey("blurred_artwork_for_lyrics")
        val DYNAMIC_COLOR_ACTIVE_LYRICS = booleanPreferencesKey("dynamic_color_active_lyrics")
        val DISABLE_LYRIC_FONT_SCALING = booleanPreferencesKey("disable_lyric_font_scaling")
        val LYRICS_ALIGNMENT = stringPreferencesKey("lyrics_alignment")
        val LYRICS_SCREEN_DARKNESS = floatPreferencesKey("lyrics_screen_darkness")
        val PRIMARY_LYRICS_PROVIDER = stringPreferencesKey("primary_lyrics_provider")
        val DISABLE_SQUIGGLE = booleanPreferencesKey("disable_squiggle")
        val DISABLE_ANIMATED_DURATION = booleanPreferencesKey("disable_animated_duration")
        val SHOW_QUALITY_BADGE = booleanPreferencesKey("show_quality_badge")
        val ENABLE_PLAYER_GRADIENT = booleanPreferencesKey("enable_player_gradient")
        val KEEP_BACKGROUND_GRADIENT = booleanPreferencesKey("keep_background_gradient")
    }

    val dynamicTheming: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_THEMING] ?: false }
    val disableBlur: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_BLUR] ?: false }
    val dynamicAccentColor: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_ACCENT_COLOR] ?: true }
    val darkTheme: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DARK_THEME] ?: false }
    val customAccentColor: Flow<Int> = dataStore.data.map { prefs -> prefs[Keys.CUSTOM_ACCENT_COLOR] ?: 0xFF41C25E.toInt() }
    val dynamicLyrics: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_LYRICS] ?: true }
    val blurredArtworkForLyrics: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.BLURRED_ARTWORK_FOR_LYRICS] ?: true }
    val dynamicColorActiveLyrics: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_COLOR_ACTIVE_LYRICS] ?: true }
    val disableLyricFontScaling: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_LYRIC_FONT_SCALING] ?: true }
    val lyricsAlignment: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.LYRICS_ALIGNMENT] ?: "center" }
    val lyricsScreenDarkness: Flow<Float> = dataStore.data.map { prefs -> prefs[Keys.LYRICS_SCREEN_DARKNESS] ?: 0.6f }
    val primaryLyricsProvider: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.PRIMARY_LYRICS_PROVIDER] ?: "lrclib" }
    val disableSquiggle: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_SQUIGGLE] ?: false }
    val disableAnimatedDuration: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_ANIMATED_DURATION] ?: false }
    val showQualityBadge: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_QUALITY_BADGE] ?: true }
    val enablePlayerGradient: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.ENABLE_PLAYER_GRADIENT] ?: true }
    val keepBackgroundGradient: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.KEEP_BACKGROUND_GRADIENT] ?: false }

    suspend fun setDisableAnimatedDuration(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DISABLE_ANIMATED_DURATION] = value }
    }

    suspend fun setDynamicTheming(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_THEMING] = value }
    }

    suspend fun setDisableBlur(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DISABLE_BLUR] = value }
    }

    suspend fun setDynamicAccentColor(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_ACCENT_COLOR] = value }
    }

    suspend fun setDarkTheme(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DARK_THEME] = value }
    }

    suspend fun setCustomAccentColor(value: Int) {
        dataStore.edit { prefs -> prefs[Keys.CUSTOM_ACCENT_COLOR] = value }
    }

    suspend fun setDynamicLyrics(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_LYRICS] = value }
    }

    suspend fun setBlurredArtworkForLyrics(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.BLURRED_ARTWORK_FOR_LYRICS] = value }
    }

    suspend fun setDynamicColorActiveLyrics(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLOR_ACTIVE_LYRICS] = value }
    }

    suspend fun setDisableLyricFontScaling(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DISABLE_LYRIC_FONT_SCALING] = value }
    }

    suspend fun setLyricsAlignment(value: String) {
        dataStore.edit { prefs -> prefs[Keys.LYRICS_ALIGNMENT] = value }
    }

    suspend fun setLyricsScreenDarkness(value: Float) {
        dataStore.edit { prefs -> prefs[Keys.LYRICS_SCREEN_DARKNESS] = value }
    }

    suspend fun setPrimaryLyricsProvider(value: String) {
        dataStore.edit { prefs -> prefs[Keys.PRIMARY_LYRICS_PROVIDER] = value }
    }

    suspend fun setDisableSquiggle(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DISABLE_SQUIGGLE] = value }
    }

    suspend fun setShowQualityBadge(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_QUALITY_BADGE] = value }
    }

    suspend fun setEnablePlayerGradient(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ENABLE_PLAYER_GRADIENT] = value
            if (!value) {
                prefs[Keys.KEEP_BACKGROUND_GRADIENT] = false
            }
        }
    }

    suspend fun setKeepBackgroundGradient(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.KEEP_BACKGROUND_GRADIENT] = value }
    }
}
