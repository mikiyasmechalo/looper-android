package com.example.looper_android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.LibraryScanner
import com.example.looper_android.data.repository.SafScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.looper_android.domain.QueueManager

@HiltViewModel
class AllSongsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryScanner: LibraryScanner,
    private val safScanner: SafScanner,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _songs = MutableStateFlow<List<SongEntity>>(emptyList())
    val songs: StateFlow<List<SongEntity>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDbLoaded = MutableStateFlow(false)
    val isDbLoaded: StateFlow<Boolean> = _isDbLoaded.asStateFlow()

    init {
        libraryDao.getAllSongs()
            .onEach { 
                _songs.value = it 
                _isDbLoaded.value = true
            }
            .catch { it.printStackTrace() }
            .launchIn(viewModelScope)
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                libraryScanner.scanMediaStore()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun scanFolder(uri: android.net.Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                safScanner.scanDirectory(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun playAll(startIndex: Int) {
        val currentSongs = _songs.value
        if (currentSongs.isNotEmpty()) {
            queueManager.createAndPlayQueue("All Songs", currentSongs, startIndex)
        }
    }

    fun shuffleAll() {
        val currentSongs = _songs.value.shuffled()
        if (currentSongs.isNotEmpty()) {
            queueManager.createAndPlayQueue("Shuffled Library", currentSongs)
        }
    }
}
