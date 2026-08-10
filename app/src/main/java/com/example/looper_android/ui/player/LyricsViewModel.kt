package com.example.looper_android.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.models.LyricLine
import com.example.looper_android.data.repository.LyricsFetcher
import com.example.looper_android.data.repository.LyricsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val isLoading: Boolean = false,
    val rawLyrics: String? = null,
    val parsedLines: List<LyricLine> = emptyList()
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsFetcher: LyricsFetcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private var currentSongId: Long = -1L

    fun loadLyricsForSong(song: SongEntity?) {
        if (song == null) {
            _uiState.value = LyricsUiState(isLoading = false, rawLyrics = null)
            currentSongId = -1L
            return
        }
        
        if (song.id == currentSongId) return
        currentSongId = song.id
        
        _uiState.value = LyricsUiState(isLoading = true)

        viewModelScope.launch {
            val raw = lyricsFetcher.fetchLyrics(song)
            if (raw != null) {
                val parsed = LyricsParser.parse(raw, (song.duration ?: 180000).toLong())
                _uiState.value = LyricsUiState(
                    isLoading = false,
                    rawLyrics = raw,
                    parsedLines = parsed
                )
            } else {
                _uiState.value = LyricsUiState(
                    isLoading = false,
                    rawLyrics = null,
                    parsedLines = emptyList()
                )
            }
        }
    }
}
