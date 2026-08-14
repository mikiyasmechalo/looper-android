package com.example.looper_android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.repository.ArtworkService
import com.example.looper_android.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val artworkService: ArtworkService
) : ViewModel() {

    val dynamicTheming: StateFlow<Boolean> = repository.dynamicTheming
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val disableBlur: StateFlow<Boolean> = repository.disableBlur
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicAccentColor: StateFlow<Boolean> = repository.dynamicAccentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val darkTheme: StateFlow<Boolean> = repository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customAccentColor: StateFlow<Int> = repository.customAccentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF41C25E.toInt())

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

    val disableSquiggle: StateFlow<Boolean> = repository.disableSquiggle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val disableAnimatedDuration: StateFlow<Boolean> = repository.disableAnimatedDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showQualityBadge: StateFlow<Boolean> = repository.showQualityBadge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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
}
