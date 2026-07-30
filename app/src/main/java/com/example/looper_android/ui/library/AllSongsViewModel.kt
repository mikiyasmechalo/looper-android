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

import com.example.looper_android.data.models.SortOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AllSongsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryScanner: LibraryScanner,
    private val safScanner: SafScanner,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _songs = MutableStateFlow<List<SongEntity>>(emptyList())
    val songs: StateFlow<List<SongEntity>> = _songs.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DEFAULT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val sortedSongs: StateFlow<List<SongEntity>> = combine(_songs, _sortOption) { list, sort ->
        val sorted = list.toMutableList()
        when (sort) {
            SortOption.DEFAULT -> list
            SortOption.TITLE_ASC -> sorted.apply { sortBy { it.title.lowercase() } }
            SortOption.TITLE_DESC -> sorted.apply { sortByDescending { it.title.lowercase() } }
            SortOption.ARTIST_ASC -> sorted.apply { sortBy { (it.artist ?: "").lowercase() } }
            SortOption.ALBUM_ASC -> sorted.apply { sortBy { (it.album ?: "").lowercase() } }
            SortOption.DURATION -> sorted.apply { sortBy { it.duration } }
            SortOption.YEAR_NEWEST -> sorted.apply { sortByDescending { it.year ?: 0 } }
            SortOption.YEAR_OLDEST -> sorted.apply { sortBy { it.year ?: 0 } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun loadSongs() {
        if (_songs.value.isNotEmpty()) return
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
    
    fun playAll(startIndex: Int = 0, shuffled: Boolean = false) {
        val currentList = sortedSongs.value.ifEmpty { _songs.value }
        if (currentList.isNotEmpty()) {
            val safeIndex = startIndex.coerceIn(0, currentList.lastIndex)
            queueManager.createAndPlayQueue("All Songs", currentList, safeIndex, shuffled)
        }
    }

    fun shuffleAll() {
        val currentList = sortedSongs.value.ifEmpty { _songs.value }
        if (currentList.isNotEmpty()) {
            val randomIndex = currentList.indices.random()
            queueManager.createAndPlayQueue("Shuffled Library", currentList, startIndex = randomIndex, shuffled = true)
        }
    }

    fun updateSong(song: SongEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            libraryDao.updateSong(song)
        }
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(song.path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            libraryDao.deleteSongById(song.id)
        }
    }
}
