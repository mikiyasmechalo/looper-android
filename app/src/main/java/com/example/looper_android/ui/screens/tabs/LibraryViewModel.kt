package com.example.looper_android.ui.screens.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.domain.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class LibraryUiState(
    val recentlyPlayed: List<SongEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        libraryDao.getAllSongs().map { songs ->
            songs.filter { it.lastPlayed != null }
                 .sortedByDescending { it.lastPlayed }
        }
        .flowOn(Dispatchers.Default)
        .onEach { recent ->
            _uiState.value = LibraryUiState(
                recentlyPlayed = recent,
                isLoading = false
            )
        }.launchIn(viewModelScope)
    }

    fun playRecentlyPlayedSong(index: Int) {
        val pool = _uiState.value.recentlyPlayed
        if (pool.isNotEmpty() && index in pool.indices) {
            queueManager.loadAndPlayQueue("Recently Played", pool, index, false)
        }
    }
}
