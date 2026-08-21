package com.example.looper_android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
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
        val AUTO_FALLBACK_LYRICS_PROVIDERS = booleanPreferencesKey("auto_fallback_lyrics_providers")
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
        val USE_ALBUM_ARTIST = booleanPreferencesKey("use_album_artist")
        val LIBRARY_FOLDERS = stringSetPreferencesKey("library_folders")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
        val DEFAULT_SCREEN = stringPreferencesKey("default_screen")
        val SHOW_HOME_ARTISTS = booleanPreferencesKey("show_home_artists")
        val SHOW_HOME_ALBUMS = booleanPreferencesKey("show_home_albums")
        val SHOW_HOME_GENRES = booleanPreferencesKey("show_home_genres")
        val HOME_SECTIONS_ORDER = stringPreferencesKey("home_sections_order")
        val SHOW_CARD_PLAY_BUTTON = booleanPreferencesKey("show_card_play_button")
        val SHOW_CARD_OPTIONS_BUTTON = booleanPreferencesKey("show_card_options_button")

        // Typography & Font settings
        val APP_FONT_FAMILY = stringPreferencesKey("app_font_family")
        val APP_FONT_WEIGHT_DELTA = intPreferencesKey("app_font_weight_delta")
        val USE_CUSTOM_FONT_LYRICS = booleanPreferencesKey("use_custom_font_lyrics")
        val LYRICS_FONT_FAMILY = stringPreferencesKey("lyrics_font_family")
        val LYRICS_FONT_WEIGHT_DELTA = intPreferencesKey("lyrics_font_weight_delta")
        val ACTIVE_LYRICS_FONT_WEIGHT_DELTA = intPreferencesKey("active_lyrics_font_weight_delta")

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
        val ENABLE_CROSSFADE = booleanPreferencesKey("enable_crossfade")
        val CROSSFADE_DURATION = floatPreferencesKey("crossfade_duration")
    }

    private val syncPrefs = context.getSharedPreferences("settings_sync_cache", android.content.Context.MODE_PRIVATE)

    fun getInitialAccentColor(): Int {
        val isDynamic = syncPrefs.getBoolean("cached_dynamic_accent_color_enabled", true)
        val isDynamicTheming = syncPrefs.getBoolean("cached_dynamic_theming_enabled", false)
        if (isDynamic || isDynamicTheming) {
            val cachedDynamic = syncPrefs.getInt("cached_dynamic_accent_color", -1)
            if (cachedDynamic != -1 && cachedDynamic != 0 && (cachedDynamic and 0xFF000000.toInt()) != 0) {
                return cachedDynamic
            }
            val cachedLast = syncPrefs.getInt("cached_last_accent_color", -1)
            if (cachedLast != -1 && cachedLast != 0 && (cachedLast and 0xFF000000.toInt()) != 0) {
                return cachedLast
            }
        }
        val cachedCustom = syncPrefs.getInt("cached_custom_accent_color", 0xFF55DF69.toInt())
        if (cachedCustom != 0 && (cachedCustom and 0xFF000000.toInt()) != 0) {
            return cachedCustom
        }
        return 0xFF55DF69.toInt()
    }

    fun getInitialSongArtPath(): String? {
        return syncPrefs.getString("cached_last_song_art_path", null)
    }

    fun saveCachedAccentColor(color: Int, isDynamic: Boolean) {
        if (color == 0 || (color and 0xFF000000.toInt()) == 0) return // Guard against transparent/0
        if (isDynamic) {
            syncPrefs.edit()
                .putInt("cached_dynamic_accent_color", color)
                .putInt("cached_last_accent_color", color)
                .apply()
        } else {
            syncPrefs.edit()
                .putInt("cached_custom_accent_color", color)
                .putInt("cached_last_accent_color", color)
                .apply()
        }
    }

    fun saveCachedSongArtPath(artPath: String?) {
        if (!artPath.isNullOrEmpty()) {
            syncPrefs.edit().putString("cached_last_song_art_path", artPath).apply()
        }
    }

    fun saveCachedDynamicAccentColorEnabled(enabled: Boolean) {
        syncPrefs.edit().putBoolean("cached_dynamic_accent_color_enabled", enabled).apply()
    }

    fun saveCachedDynamicThemingEnabled(enabled: Boolean) {
        syncPrefs.edit().putBoolean("cached_dynamic_theming_enabled", enabled).apply()
    }

    val dynamicTheming: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_THEMING] ?: false }
    val disableBlur: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DISABLE_BLUR] ?: false }
    val dynamicAccentColor: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_ACCENT_COLOR] ?: true }
    val darkTheme: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.DARK_THEME] ?: false }
    val customAccentColor: Flow<Int> =
        dataStore.data.map { prefs ->
            val stored = prefs[Keys.CUSTOM_ACCENT_COLOR]
            if (stored != null && stored != 0 && (stored and 0xFF000000.toInt()) != 0) {
                stored
            } else {
                getInitialAccentColor()
            }
        }
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
    val autoFallbackLyricsProviders: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.AUTO_FALLBACK_LYRICS_PROVIDERS] ?: true }
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
    val useAlbumArtist: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.USE_ALBUM_ARTIST] ?: true }
    val libraryFolders: Flow<Set<String>> = dataStore.data.map { prefs -> prefs[Keys.LIBRARY_FOLDERS] ?: emptySet() }
    val excludedFolders: Flow<Set<String>> = dataStore.data.map { prefs -> prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet() }
    val defaultScreen: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.DEFAULT_SCREEN] ?: "home" }
    val showHomeArtists: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_HOME_ARTISTS] ?: true }
    val showHomeAlbums: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_HOME_ALBUMS] ?: true }
    val showHomeGenres: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_HOME_GENRES] ?: true }
    val homeSectionsOrder: Flow<List<String>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.HOME_SECTIONS_ORDER]
        if (raw.isNullOrBlank()) {
            listOf("quick_picks", "songs", "artists", "albums", "genres")
        } else {
            raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    val showCardPlayButton: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_CARD_PLAY_BUTTON] ?: true }
    val showCardOptionsButton: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.SHOW_CARD_OPTIONS_BUTTON] ?: true }

    val appFontFamily: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.APP_FONT_FAMILY] ?: "DM Sans" }
    val appFontWeightDelta: Flow<Int> = dataStore.data.map { prefs -> prefs[Keys.APP_FONT_WEIGHT_DELTA] ?: 0 }
    val useCustomFontLyrics: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.USE_CUSTOM_FONT_LYRICS] ?: false }
    val lyricsFontFamily: Flow<String> = dataStore.data.map { prefs -> prefs[Keys.LYRICS_FONT_FAMILY] ?: "Sora" }
    val lyricsFontWeightDelta: Flow<Int> = dataStore.data.map { prefs -> prefs[Keys.LYRICS_FONT_WEIGHT_DELTA] ?: 0 }
    val activeLyricsFontWeightDelta: Flow<Int> = dataStore.data.map { prefs -> prefs[Keys.ACTIVE_LYRICS_FONT_WEIGHT_DELTA] ?: 0 }

    val pauseOnDisconnect: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.PAUSE_ON_DISCONNECT] ?: true }
    val resumeOnlyIfPausedByDisconnect: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.RESUME_ONLY_IF_PAUSED_BY_DISCONNECT] ?: false }
    val resumeOnBluetoothConnect: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.RESUME_ON_BLUETOOTH_CONNECT] ?: false }
    val resumeOnWiredConnect: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.RESUME_ON_WIRED_CONNECT] ?: false }
    val fadePlayPauseStop: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.FADE_PLAY_PAUSE_STOP] ?: true }
    val fadeLengthMs: Flow<Float> = dataStore.data.map { prefs -> prefs[Keys.FADE_LENGTH_MS] ?: 150f }
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
    val enableCrossfade: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.ENABLE_CROSSFADE] ?: false }
    val crossfadeDuration: Flow<Float> = dataStore.data.map { prefs -> prefs[Keys.CROSSFADE_DURATION] ?: 3f } // Default 3s

    suspend fun setDisableAnimatedDuration(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DISABLE_ANIMATED_DURATION] = value }
    }

    suspend fun setDynamicTheming(value: Boolean) {
        saveCachedDynamicThemingEnabled(value)
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_THEMING] = value }
    }

    suspend fun setDisableBlur(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DISABLE_BLUR] = value }
    }

    suspend fun setDynamicAccentColor(value: Boolean) {
        saveCachedDynamicAccentColorEnabled(value)
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_ACCENT_COLOR] = value }
    }

    suspend fun setDarkTheme(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DARK_THEME] = value }
    }

    suspend fun setCustomAccentColor(value: Int) {
        syncPrefs.edit().putInt("cached_custom_accent_color", value).apply()
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

    suspend fun setAutoFallbackLyricsProviders(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUTO_FALLBACK_LYRICS_PROVIDERS] = value }
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

    suspend fun setEnableCrossfade(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ENABLE_CROSSFADE] = value }
    }

    suspend fun setCrossfadeDuration(value: Float) {
        dataStore.edit { prefs -> prefs[Keys.CROSSFADE_DURATION] = value }
    }

    suspend fun setUseAlbumArtist(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.USE_ALBUM_ARTIST] = value }
    }

    suspend fun setDefaultScreen(screen: String) {
        context.getSharedPreferences("app_settings_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("default_screen", screen).apply()
        dataStore.edit { prefs -> prefs[Keys.DEFAULT_SCREEN] = screen }
    }

    suspend fun setShowHomeArtists(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_HOME_ARTISTS] = value }
    }

    suspend fun setShowHomeAlbums(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_HOME_ALBUMS] = value }
    }

    suspend fun setShowHomeGenres(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_HOME_GENRES] = value }
    }

    suspend fun setHomeSectionsOrder(order: List<String>) {
        dataStore.edit { prefs -> prefs[Keys.HOME_SECTIONS_ORDER] = order.joinToString(",") }
    }

    suspend fun addLibraryFolder(folderUri: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.LIBRARY_FOLDERS] ?: emptySet()
            prefs[Keys.LIBRARY_FOLDERS] = current + folderUri
        }
    }

    suspend fun removeLibraryFolder(folderUri: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.LIBRARY_FOLDERS] ?: emptySet()
            prefs[Keys.LIBRARY_FOLDERS] = current - folderUri
        }
    }

    suspend fun setLibraryFolders(folders: Set<String>) {
        dataStore.edit { prefs -> prefs[Keys.LIBRARY_FOLDERS] = folders }
    }

    suspend fun addExcludedFolder(folderPath: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet()
            prefs[Keys.EXCLUDED_FOLDERS] = current + folderPath
        }
    }

    suspend fun removeExcludedFolder(folderPath: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_FOLDERS] ?: emptySet()
            prefs[Keys.EXCLUDED_FOLDERS] = current - folderPath
        }
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        dataStore.edit { prefs -> prefs[Keys.EXCLUDED_FOLDERS] = folders }
    }

    suspend fun setShowCardPlayButton(show: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_CARD_PLAY_BUTTON] = show }
    }

    suspend fun setShowCardOptionsButton(show: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_CARD_OPTIONS_BUTTON] = show }
    }

    suspend fun setAppFontFamily(family: String) {
        dataStore.edit { prefs -> prefs[Keys.APP_FONT_FAMILY] = family }
    }

    suspend fun setAppFontWeightDelta(delta: Int) {
        dataStore.edit { prefs -> prefs[Keys.APP_FONT_WEIGHT_DELTA] = delta }
    }

    suspend fun setUseCustomFontLyrics(use: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.USE_CUSTOM_FONT_LYRICS] = use }
    }

    suspend fun setLyricsFontFamily(family: String) {
        dataStore.edit { prefs -> prefs[Keys.LYRICS_FONT_FAMILY] = family }
    }

    suspend fun setLyricsFontWeightDelta(delta: Int) {
        dataStore.edit { prefs -> prefs[Keys.LYRICS_FONT_WEIGHT_DELTA] = delta }
    }

    suspend fun setActiveLyricsFontWeightDelta(delta: Int) {
        dataStore.edit { prefs -> prefs[Keys.ACTIVE_LYRICS_FONT_WEIGHT_DELTA] = delta }
    }
}
