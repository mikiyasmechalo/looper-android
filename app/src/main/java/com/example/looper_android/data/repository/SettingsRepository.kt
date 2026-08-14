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
        val ENABLE_SLIDE_GESTURE = booleanPreferencesKey("enable_slide_gesture")
        val STOP_ON_TASK_REMOVED = booleanPreferencesKey("stop_on_task_removed")
        val ENABLE_INTERNET = booleanPreferencesKey("enable_internet")
        val DOWNLOAD_ARTWORK = booleanPreferencesKey("download_artwork")
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        // Audio & Playback + Headphone settings
        val PAUSE_ON_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
        val RESUME_ONLY_IF_PAUSED_BY_DISCONNECT = booleanPreferencesKey("resume_only_if_paused_by_disconnect")
        val RESUME_ON_BLUETOOTH_CONNECT = booleanPreferencesKey("resume_on_bluetooth_connect")
        val RESUME_ON_WIRED_CONNECT = booleanPreferencesKey("resume_on_wired_connect")
        val FADE_PLAY_PAUSE_STOP = booleanPreferencesKey("fade_play_pause_stop")
        val FADE_LENGTH_MS = floatPreferencesKey("fade_length_ms")
        val PERSIST_QUEUE = booleanPreferencesKey("persist_queue")
        val AUDIO_FOCUS = booleanPreferencesKey("audio_focus")
        val AUDIO_FOCUS_REQUEST_ON_PLAY = booleanPreferencesKey("audio_focus_request_on_play")
        val AUDIO_FOCUS_RELEASE_ON_PAUSE = booleanPreferencesKey("audio_focus_release_on_pause")
        val AUDIO_FOCUS_STOP_ON_OTHER_SESSION = booleanPreferencesKey("audio_focus_stop_on_other_session")
        val AUDIO_FOCUS_RESTART_ON_GAIN = booleanPreferencesKey("audio_focus_restart_on_gain")
        val RESUME_ON_START = booleanPreferencesKey("resume_on_start")
        val PERMANENT_AUDIO_FOCUS_CHANGE = booleanPreferencesKey("permanent_audio_focus_change")
    }

    val dynamicTheming: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_THEMING] ?: false }
    val disableBlur: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_BLUR] ?: false }
    val dynamicAccentColor: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_ACCENT_COLOR] ?: true }
    val darkTheme: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DARK_THEME] ?: false }
    val customAccentColor: Flow<Int> =
        dataStore.data.map { prefs -> prefs[Keys.CUSTOM_ACCENT_COLOR] ?: 0xFF41C25E.toInt() }
    val dynamicLyrics: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_LYRICS] ?: true }
    val blurredArtworkForLyrics: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.BLURRED_ARTWORK_FOR_LYRICS] ?: true }
    val dynamicColorActiveLyrics: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_COLOR_ACTIVE_LYRICS] ?: true }
    val disableLyricFontScaling: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.DISABLE_LYRIC_FONT_SCALING] ?: true }
    val lyricsAlignment: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.LYRICS_ALIGNMENT] ?: "center" }
    val lyricsScreenDarkness: Flow<Float> = dataStore.data.map { prefs -> prefs[Keys.LYRICS_SCREEN_DARKNESS] ?: 0.6f }
    val primaryLyricsProvider: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.PRIMARY_LYRICS_PROVIDER] ?: "lrclib" }
    val disableSquiggle: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_SQUIGGLE] ?: false }
    val disableAnimatedDuration: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.DISABLE_ANIMATED_DURATION] ?: false }
    val showQualityBadge: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_QUALITY_BADGE] ?: true }
    val enablePlayerGradient: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.ENABLE_PLAYER_GRADIENT] ?: true }
    val keepBackgroundGradient: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.KEEP_BACKGROUND_GRADIENT] ?: false }
    val enableSlideGesture: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.ENABLE_SLIDE_GESTURE] ?: true }
    val stopOnTaskRemoved: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.STOP_ON_TASK_REMOVED] ?: false }
    val enableInternet: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.ENABLE_INTERNET] ?: false } // to be turned true before relase.
    val downloadArtwork: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DOWNLOAD_ARTWORK] ?: true }
    val appLanguage: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.APP_LANGUAGE] ?: "System Default" }

    val pauseOnDisconnect: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.PAUSE_ON_DISCONNECT] ?: true }
    val resumeOnlyIfPausedByDisconnect: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.RESUME_ONLY_IF_PAUSED_BY_DISCONNECT] ?: false }
    val resumeOnBluetoothConnect: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.RESUME_ON_BLUETOOTH_CONNECT] ?: false }
    val resumeOnWiredConnect: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.RESUME_ON_WIRED_CONNECT] ?: false }
    val fadePlayPauseStop: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.FADE_PLAY_PAUSE_STOP] ?: true }
    val fadeLengthMs: Flow<Float> = dataStore.data.map { prefs -> prefs[Keys.FADE_LENGTH_MS] ?: 300f }
    val persistQueue: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.PERSIST_QUEUE] ?: true }
    val audioFocus: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.AUDIO_FOCUS] ?: true }
    val audioFocusRequestOnPlay: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.AUDIO_FOCUS_REQUEST_ON_PLAY] ?: true }
    val audioFocusReleaseOnPause: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.AUDIO_FOCUS_RELEASE_ON_PAUSE] ?: true }
    val audioFocusStopOnOtherSession: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.AUDIO_FOCUS_STOP_ON_OTHER_SESSION] ?: false }
    val audioFocusRestartOnGain: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.AUDIO_FOCUS_RESTART_ON_GAIN] ?: true }
    val resumeOnStart: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.RESUME_ON_START] ?: false }
    val permanentAudioFocusChange: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.PERMANENT_AUDIO_FOCUS_CHANGE] ?: false }

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

    suspend fun setEnableSlideGesture(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ENABLE_SLIDE_GESTURE] = value }
    }

    suspend fun setStopOnTaskRemoved(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.STOP_ON_TASK_REMOVED] = value }
    }

    suspend fun setEnableInternet(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ENABLE_INTERNET] = value }
    }

    suspend fun setDownloadArtwork(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DOWNLOAD_ARTWORK] = value }
    }

    suspend fun setAppLanguage(value: String) {
        dataStore.edit { prefs -> prefs[Keys.APP_LANGUAGE] = value }
    }

    suspend fun setPauseOnDisconnect(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PAUSE_ON_DISCONNECT] = value }
    }

    suspend fun setResumeOnlyIfPausedByDisconnect(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.RESUME_ONLY_IF_PAUSED_BY_DISCONNECT] = value }
    }

    suspend fun setResumeOnBluetoothConnect(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.RESUME_ON_BLUETOOTH_CONNECT] = value }
    }

    suspend fun setResumeOnWiredConnect(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.RESUME_ON_WIRED_CONNECT] = value }
    }

    suspend fun setFadePlayPauseStop(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.FADE_PLAY_PAUSE_STOP] = value }
    }

    suspend fun setFadeLengthMs(value: Float) {
        dataStore.edit { prefs -> prefs[Keys.FADE_LENGTH_MS] = value }
    }

    suspend fun setPersistQueue(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PERSIST_QUEUE] = value }
    }

    suspend fun setAudioFocus(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUDIO_FOCUS] = value }
    }

    suspend fun setAudioFocusRequestOnPlay(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUDIO_FOCUS_REQUEST_ON_PLAY] = value }
    }

    suspend fun setAudioFocusReleaseOnPause(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUDIO_FOCUS_RELEASE_ON_PAUSE] = value }
    }

    suspend fun setAudioFocusStopOnOtherSession(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUDIO_FOCUS_STOP_ON_OTHER_SESSION] = value }
    }

    suspend fun setAudioFocusRestartOnGain(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUDIO_FOCUS_RESTART_ON_GAIN] = value }
    }

    suspend fun setResumeOnStart(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.RESUME_ON_START] = value }
    }

    suspend fun setPermanentAudioFocusChange(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PERMANENT_AUDIO_FOCUS_CHANGE] = value }
    }
}
