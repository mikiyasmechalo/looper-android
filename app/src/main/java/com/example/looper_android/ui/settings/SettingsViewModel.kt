package com.example.looper_android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.repository.ArtworkService
import com.example.looper_android.data.repository.LibraryScanner
import com.example.looper_android.data.repository.LyricsFetcher
import com.example.looper_android.data.repository.SafPermissionRepository
import com.example.looper_android.data.repository.SafScanner
import com.example.looper_android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DiscoveredFolder(
    val folderName: String,
    val folderPath: String,
    val songCount: Int
)

@HiltViewModel
class SettingsViewModel @Inject constructor(

    private val repository: SettingsRepository,
    private val artworkService: ArtworkService,
    private val lyricsFetcher: LyricsFetcher,
    private val libraryScanner: LibraryScanner,
    private val safScanner: SafScanner,
    private val safPermissionRepo: SafPermissionRepository,
    private val libraryDao: LibraryDao,
    private val queueManager: com.example.looper_android.domain.QueueManager,
    private val updateService: com.example.looper_android.data.repository.UpdateService,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.excludedFolders.collect { excluded ->
                if (excluded.isNotEmpty()) {
                    queueManager.purgeExcludedSongs(excluded)
                }
            }
        }
    }

    val libraryFolders: StateFlow<Set<String>> = repository.libraryFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val excludedFolders: StateFlow<Set<String>> = repository.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val grantedSafUris: StateFlow<List<android.net.Uri>> = safPermissionRepo.grantedTreeUrisFlow

    val discoveredFolders: StateFlow<List<DiscoveredFolder>> = combine(
        libraryDao.getAllSongPathsFlow(),
        repository.excludedFolders
    ) { allPaths: List<String>, excluded: Set<String> ->
        val folderCounts = mutableMapOf<String, Int>()
        for (path in allPaths) {
            if (path.startsWith("content://")) continue // skip legacy content URIs
            val folderPath = if (path.endsWith("/")) path.dropLast(1) else path.substringBeforeLast('/', "")
            if (folderPath.isNotBlank()) {
                val isExcluded = excluded.isNotEmpty() && excluded.any { folderPath.startsWith(it, ignoreCase = true) }
                if (!isExcluded) {
                    folderCounts[folderPath] = (folderCounts[folderPath] ?: 0) + 1
                }
            }
        }
        folderCounts.map { (fPath, count) ->
            val name = fPath.substringAfterLast('/', fPath)
            DiscoveredFolder(
                folderName = name.ifBlank { "Music" },
                folderPath = fPath,
                songCount = count
            )
        }.sortedBy { it.folderName.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getInitialAccentColor(): Int = repository.getInitialAccentColor()
    fun getInitialSongArtPath(): String? = repository.getInitialSongArtPath()

    val dynamicTheming: StateFlow<Boolean> = repository.dynamicTheming
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val disableBlur: StateFlow<Boolean> = repository.disableBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicAccentColor: StateFlow<Boolean> = repository.dynamicAccentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val darkTheme: StateFlow<Boolean> = repository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customAccentColor: StateFlow<Int> = repository.customAccentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getInitialAccentColor())

    val dynamicLyrics: StateFlow<Boolean> = repository.dynamicLyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val blurredArtworkForLyrics: StateFlow<Boolean> = repository.blurredArtworkForLyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicColorActiveLyrics: StateFlow<Boolean> = repository.dynamicColorActiveLyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val disableLyricFontScaling: StateFlow<Boolean> = repository.disableLyricFontScaling
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lyricsAlignment: StateFlow<String> = repository.lyricsAlignment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "center")

    val lyricsScreenDarkness: StateFlow<Float> = repository.lyricsScreenDarkness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.6f)

    val primaryLyricsProvider: StateFlow<String> = repository.primaryLyricsProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "lrclib")

    val autoFallbackLyricsProviders: StateFlow<Boolean> = repository.autoFallbackLyricsProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lyricsSyncStyle: StateFlow<String> = repository.lyricsSyncStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "char")

    val lyricsProviderOrder: StateFlow<List<String>> = repository.lyricsProviderOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.looper_android.data.lyrics.providers.LyricsProviderInfo.ALL.map { it.id })

    val lyricsProvidersOff: StateFlow<Set<String>> = repository.lyricsProvidersOff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val disableSquiggle: StateFlow<Boolean> = repository.disableSquiggle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val disableAnimatedDuration: StateFlow<Boolean> = repository.disableAnimatedDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showQualityBadge: StateFlow<Boolean> = repository.showQualityBadge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoPowerSaveEnabled: StateFlow<Boolean> = repository.autoPowerSaveEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoPowerSaveThreshold: StateFlow<Int> = repository.autoPowerSaveThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val showHomeRecentlyAdded: StateFlow<Boolean> = repository.showHomeRecentlyAdded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHomeNotPlayed: StateFlow<Boolean> = repository.showHomeNotPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showLibraryRecentlyAdded: StateFlow<Boolean> = repository.showLibraryRecentlyAdded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showLibraryNotPlayed: StateFlow<Boolean> = repository.showLibraryNotPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAutoPowerSaveEnabled(enabled: Boolean) = viewModelScope.launch { repository.setAutoPowerSaveEnabled(enabled) }
    fun setAutoPowerSaveThreshold(threshold: Int) = viewModelScope.launch { repository.setAutoPowerSaveThreshold(threshold) }
    fun setShowHomeRecentlyAdded(enabled: Boolean) = viewModelScope.launch { repository.setShowHomeRecentlyAdded(enabled) }
    fun setShowHomeNotPlayed(enabled: Boolean) = viewModelScope.launch { repository.setShowHomeNotPlayed(enabled) }
    fun setShowLibraryRecentlyAdded(enabled: Boolean) = viewModelScope.launch { repository.setShowLibraryRecentlyAdded(enabled) }
    fun setShowLibraryNotPlayed(enabled: Boolean) = viewModelScope.launch { repository.setShowLibraryNotPlayed(enabled) }


    val enablePlayerGradient: StateFlow<Boolean> = repository.enablePlayerGradient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val keepBackgroundGradient: StateFlow<Boolean> = repository.keepBackgroundGradient
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val enableSlideGesture: StateFlow<Boolean> = repository.enableSlideGesture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val stopOnTaskRemoved: StateFlow<Boolean> = repository.stopOnTaskRemoved
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val enableInternet: StateFlow<Boolean> = repository.enableInternet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val downloadArtwork: StateFlow<Boolean> = repository.downloadArtwork
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showCardPlayButton: StateFlow<Boolean> = repository.showCardPlayButton
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showCardOptionsButton: StateFlow<Boolean> = repository.showCardOptionsButton
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appFontFamily: StateFlow<String> = repository.appFontFamily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DM Sans")

    val appFontWeightDelta: StateFlow<Int> = repository.appFontWeightDelta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val useCustomFontLyrics: StateFlow<Boolean> = repository.useCustomFontLyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lyricsFontFamily: StateFlow<String> = repository.lyricsFontFamily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sora")

    val lyricsFontWeightDelta: StateFlow<Int> = repository.lyricsFontWeightDelta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeLyricsFontWeightDelta: StateFlow<Int> = repository.activeLyricsFontWeightDelta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appLanguage: StateFlow<String> = repository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System Default")

    val pauseOnDisconnect: StateFlow<Boolean> = repository.pauseOnDisconnect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val resumeOnlyIfPausedByDisconnect: StateFlow<Boolean> = repository.resumeOnlyIfPausedByDisconnect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val resumeOnBluetoothConnect: StateFlow<Boolean> = repository.resumeOnBluetoothConnect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val resumeOnWiredConnect: StateFlow<Boolean> = repository.resumeOnWiredConnect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fadePlayPauseStop: StateFlow<Boolean> = repository.fadePlayPauseStop
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fadeLengthMs: StateFlow<Float> = repository.fadeLengthMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 300f)

    val persistQueue: StateFlow<Boolean> = repository.persistQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val audioFocus: StateFlow<Boolean> = repository.audioFocus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val audioFocusRequestOnPlay: StateFlow<Boolean> = repository.audioFocusRequestOnPlay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val audioFocusReleaseOnPause: StateFlow<Boolean> = repository.audioFocusReleaseOnPause
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val audioFocusStopOnOtherSession: StateFlow<Boolean> = repository.audioFocusStopOnOtherSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val audioFocusRestartOnGain: StateFlow<Boolean> = repository.audioFocusRestartOnGain
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val resumeOnStart: StateFlow<Boolean> = repository.resumeOnStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val permanentAudioFocusChange: StateFlow<Boolean> = repository.permanentAudioFocusChange
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val skipSilence: StateFlow<Boolean> = repository.skipSilence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSkipSilence(enabled)
        }
    }

    val enableCrossfade: StateFlow<Boolean> = repository.enableCrossfade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val crossfadeDuration: StateFlow<Float> = repository.crossfadeDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3f)

    val useAlbumArtist: StateFlow<Boolean> = repository.useAlbumArtist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultScreen: StateFlow<String> = repository.defaultScreen
        .stateIn(viewModelScope, SharingStarted.Eagerly, "home")

    val showHomeArtists: StateFlow<Boolean> = repository.showHomeArtists
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showHomeAlbums: StateFlow<Boolean> = repository.showHomeAlbums
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showHomeGenres: StateFlow<Boolean> = repository.showHomeGenres
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showUnknownGenre: StateFlow<Boolean> = repository.showUnknownGenre
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val homeSectionsOrder: StateFlow<List<String>> = repository.homeSectionsOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf("quick_picks", "songs", "artists", "albums", "genres"))

    val enableArtistBiography: StateFlow<Boolean> = repository.enableArtistBiography
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showCollectionTrackNumbers: StateFlow<Boolean> = repository.showCollectionTrackNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showLibraryRecentlyPlayed: StateFlow<Boolean> = repository.showLibraryRecentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setEnableArtistBiography(value: Boolean) = viewModelScope.launch { repository.setEnableArtistBiography(value) }
    fun setShowCollectionTrackNumbers(value: Boolean) = viewModelScope.launch { repository.setShowCollectionTrackNumbers(value) }
    fun setShowLibraryRecentlyPlayed(value: Boolean) = viewModelScope.launch { repository.setShowLibraryRecentlyPlayed(value) }

    fun setDynamicTheming(value: Boolean) = viewModelScope.launch { repository.setDynamicTheming(value) }
    fun setDisableBlur(value: Boolean) = viewModelScope.launch { repository.setDisableBlur(value) }
    fun setDynamicAccentColor(value: Boolean) = viewModelScope.launch { repository.setDynamicAccentColor(value) }
    fun setDarkTheme(value: Boolean) = viewModelScope.launch { repository.setDarkTheme(value) }
    fun setCustomAccentColor(value: Int) = viewModelScope.launch { repository.setCustomAccentColor(value) }
    fun setDynamicLyrics(value: Boolean) = viewModelScope.launch { repository.setDynamicLyrics(value) }
    fun setBlurredArtworkForLyrics(value: Boolean) = viewModelScope.launch { repository.setBlurredArtworkForLyrics(value) }
    fun setDynamicColorActiveLyrics(value: Boolean) = viewModelScope.launch { repository.setDynamicColorActiveLyrics(value) }
    fun setDisableLyricFontScaling(value: Boolean) = viewModelScope.launch { repository.setDisableLyricFontScaling(value) }
    fun setLyricsAlignment(value: String) = viewModelScope.launch { repository.setLyricsAlignment(value) }
    fun setLyricsScreenDarkness(value: Float) = viewModelScope.launch { repository.setLyricsScreenDarkness(value) }
    fun setPrimaryLyricsProvider(value: String) = viewModelScope.launch { repository.setPrimaryLyricsProvider(value) }
    fun setAutoFallbackLyricsProviders(value: Boolean) = viewModelScope.launch { repository.setAutoFallbackLyricsProviders(value) }
    fun setLyricsSyncStyle(style: String) = viewModelScope.launch { repository.setLyricsSyncStyle(style) }
    fun setLyricsProviderOrder(order: List<String>) = viewModelScope.launch { repository.setLyricsProviderOrder(order) }
    fun setLyricsProviderEnabled(id: String, enabled: Boolean) = viewModelScope.launch { repository.setLyricsProviderEnabled(id, enabled) }
    fun toggleLyricsProvider(id: String, enabled: Boolean) = setLyricsProviderEnabled(id, enabled)
    fun setDisableSquiggle(value: Boolean) = viewModelScope.launch { repository.setDisableSquiggle(value) }
    fun setDisableAnimatedDuration(value: Boolean) = viewModelScope.launch { repository.setDisableAnimatedDuration(value) }
    fun setShowQualityBadge(value: Boolean) = viewModelScope.launch { repository.setShowQualityBadge(value) }
    fun setEnablePlayerGradient(value: Boolean) = viewModelScope.launch { repository.setEnablePlayerGradient(value) }
    fun setKeepBackgroundGradient(value: Boolean) = viewModelScope.launch { repository.setKeepBackgroundGradient(value) }
    fun setEnableSlideGesture(value: Boolean) = viewModelScope.launch { repository.setEnableSlideGesture(value) }
    fun setStopOnTaskRemoved(value: Boolean) = viewModelScope.launch { repository.setStopOnTaskRemoved(value) }
    fun setEnableInternet(value: Boolean) = viewModelScope.launch {
        repository.setEnableInternet(value)
        if (value) {
            artworkService.downloadAllMissingArtistImages()
            if (downloadArtwork.value) {
                artworkService.downloadAllMissingArtworks()
            }
        }
    }
    fun setDownloadArtwork(value: Boolean) = viewModelScope.launch {
        repository.setDownloadArtwork(value)
        if (value && enableInternet.value) {
            artworkService.downloadAllMissingArtworks()
        }
    }
    fun setAppLanguage(value: String) = viewModelScope.launch { repository.setAppLanguage(value) }
    fun setPauseOnDisconnect(value: Boolean) = viewModelScope.launch { repository.setPauseOnDisconnect(value) }
    fun setResumeOnlyIfPausedByDisconnect(value: Boolean) =
        viewModelScope.launch { repository.setResumeOnlyIfPausedByDisconnect(value) }
    fun setResumeOnBluetoothConnect(value: Boolean) =
        viewModelScope.launch { repository.setResumeOnBluetoothConnect(value) }
    fun setResumeOnWiredConnect(value: Boolean) =
        viewModelScope.launch { repository.setResumeOnWiredConnect(value) }
    fun setFadePlayPauseStop(value: Boolean) = viewModelScope.launch { repository.setFadePlayPauseStop(value) }
    fun setFadeLengthMs(value: Float) = viewModelScope.launch { repository.setFadeLengthMs(value) }
    fun setPersistQueue(value: Boolean) = viewModelScope.launch { repository.setPersistQueue(value) }
    fun setAudioFocus(value: Boolean) = viewModelScope.launch { repository.setAudioFocus(value) }
    fun setAudioFocusRequestOnPlay(value: Boolean) =
        viewModelScope.launch { repository.setAudioFocusRequestOnPlay(value) }
    fun setAudioFocusReleaseOnPause(value: Boolean) =
        viewModelScope.launch { repository.setAudioFocusReleaseOnPause(value) }
    fun setAudioFocusStopOnOtherSession(value: Boolean) =
        viewModelScope.launch { repository.setAudioFocusStopOnOtherSession(value) }
    fun setAudioFocusRestartOnGain(value: Boolean) =
        viewModelScope.launch { repository.setAudioFocusRestartOnGain(value) }
    fun setResumeOnStart(value: Boolean) = viewModelScope.launch { repository.setResumeOnStart(value) }
    fun setPermanentAudioFocusChange(value: Boolean) =
        viewModelScope.launch { repository.setPermanentAudioFocusChange(value) }
    fun setEnableCrossfade(value: Boolean) = viewModelScope.launch { repository.setEnableCrossfade(value) }
    fun setCrossfadeDuration(value: Float) = viewModelScope.launch { repository.setCrossfadeDuration(value) }

    fun setUseAlbumArtist(value: Boolean) = viewModelScope.launch { repository.setUseAlbumArtist(value) }
    fun setShowCardPlayButton(value: Boolean) = viewModelScope.launch { repository.setShowCardPlayButton(value) }
    fun setShowCardOptionsButton(value: Boolean) = viewModelScope.launch { repository.setShowCardOptionsButton(value) }
    fun setAppFontFamily(value: String) = viewModelScope.launch { repository.setAppFontFamily(value) }
    fun setAppFontWeightDelta(value: Int) = viewModelScope.launch { repository.setAppFontWeightDelta(value) }
    fun setUseCustomFontLyrics(value: Boolean) = viewModelScope.launch { repository.setUseCustomFontLyrics(value) }
    fun setLyricsFontFamily(value: String) = viewModelScope.launch { repository.setLyricsFontFamily(value) }
    fun setLyricsFontWeightDelta(value: Int) = viewModelScope.launch { repository.setLyricsFontWeightDelta(value) }
    fun setActiveLyricsFontWeightDelta(value: Int) = viewModelScope.launch { repository.setActiveLyricsFontWeightDelta(value) }
    fun setDefaultScreen(value: String) = viewModelScope.launch { repository.setDefaultScreen(value) }
    fun setShowHomeArtists(value: Boolean) = viewModelScope.launch { repository.setShowHomeArtists(value) }
    fun setShowHomeAlbums(value: Boolean) = viewModelScope.launch { repository.setShowHomeAlbums(value) }
    fun setShowHomeGenres(value: Boolean) = viewModelScope.launch { repository.setShowHomeGenres(value) }
    fun setShowUnknownGenre(value: Boolean) = viewModelScope.launch { repository.setShowUnknownGenre(value) }
    fun setHomeSectionsOrder(order: List<String>) = viewModelScope.launch { repository.setHomeSectionsOrder(order) }
    fun moveHomeSection(fromIndex: Int, toIndex: Int) {
        val current = homeSectionsOrder.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            setHomeSectionsOrder(current)
        }
    }
    fun resetHomeSectionsOrder() {
        setHomeSectionsOrder(listOf("quick_picks", "songs", "artists", "albums", "genres"))
    }

    fun syncLyricsOffline() = viewModelScope.launch {
        lyricsFetcher.prefetchAllMissingLyrics()
    }

    fun rescanLibrary() = viewModelScope.launch {
        libraryScanner.scanMediaStore(forceDiskRead = true)
    }

    fun resetLibrary() = viewModelScope.launch {
        libraryScanner.resetLibrary()
    }

    fun excludeDiscoveredFolder(folderPath: String) = viewModelScope.launch(Dispatchers.IO) {
        val cleanPath = folderPath.trimEnd('/')
        repository.addExcludedFolder(cleanPath)
        libraryDao.deleteSongsByPathPrefix(cleanPath)
        queueManager.removeSongsByPathPrefix(cleanPath)
    }

    fun restoreExcludedFolder(folderPath: String) = viewModelScope.launch(Dispatchers.IO) {
        val cleanPath = folderPath.trimEnd('/')
        repository.removeExcludedFolder(cleanPath)
        libraryScanner.scanMediaStore(forceDiskRead = true)
    }

    fun grantSafPermission(uri: android.net.Uri) = viewModelScope.launch(Dispatchers.IO) {
        safPermissionRepo.saveGrantedTreeUri(uri)
        libraryDao.deleteContentUriSongs()
    }

    fun revokeSafPermission(uri: android.net.Uri) = viewModelScope.launch(Dispatchers.IO) {
        safPermissionRepo.removeGrantedTreeUri(uri)
    }

    fun addLibraryFolder(uri: android.net.Uri) = grantSafPermission(uri)

    fun removeLibraryFolder(uriString: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val uri = android.net.Uri.parse(uriString)
            revokeSafPermission(uri)
            queueManager.removeSongsByPathPrefix(uriString)
        } catch (_: Exception) {
            repository.removeLibraryFolder(uriString)
            libraryDao.deleteSongsByPathPrefix(uriString)
            queueManager.removeSongsByPathPrefix(uriString)
        }
    }

    val updateStatus = updateService.updateStatus
    val updateAvailable = updateService.updateAvailable
    val skippedUpdateVersion = updateService.skippedUpdateVersion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun checkForUpdates(manual: Boolean = true) = viewModelScope.launch {
        updateService.checkForUpdates(manual = manual)
    }

    fun skipUpdate(version: String) = viewModelScope.launch {
        updateService.skipUpdate(version)
    }

    fun snoozeOneWeek() = viewModelScope.launch {
        updateService.snoozeOneWeek()
    }

    fun resetSkippedUpdate() = viewModelScope.launch {
        updateService.resetSkippedUpdate()
    }

    fun dismissUpdate() {
        updateService.dismissUpdate()
    }

    fun openUpdateUrl(url: String) {
        updateService.openUrl(url)
    }
}
