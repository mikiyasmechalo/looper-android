package com.example.looper_android.ui.library

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.models.SortOption
import com.example.looper_android.data.repository.AudioFileDeleter
import com.example.looper_android.data.repository.DeleteResult
import com.example.looper_android.data.repository.DeleteBatchResult
import com.example.looper_android.data.repository.LibraryScanner
import com.example.looper_android.data.repository.SafScanner
import com.example.looper_android.domain.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PendingDeletePermission(
    val intentSender: IntentSender,
    val pendingSong: SongEntity
)

data class PendingBatchDeletePermission(
    val intentSender: IntentSender,
    val pendingSongIds: List<Long>
)

@HiltViewModel
class AllSongsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryScanner: LibraryScanner,
    private val safScanner: SafScanner,
    private val audioFileDeleter: AudioFileDeleter,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _pendingDeletePermission = MutableStateFlow<PendingDeletePermission?>(null)
    val pendingDeletePermission: StateFlow<PendingDeletePermission?> = _pendingDeletePermission.asStateFlow()

    private val _pendingBatchDeletePermission = MutableStateFlow<PendingBatchDeletePermission?>(null)
    val pendingBatchDeletePermission: StateFlow<PendingBatchDeletePermission?> = _pendingBatchDeletePermission.asStateFlow()

    private val _songs = MutableStateFlow<List<SongEntity>>(emptyList())
    val songs: StateFlow<List<SongEntity>> = _songs.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    val sortedSongs: StateFlow<List<SongEntity>> = combine(_songs, _sortOption, _isAscending) { list, sort, asc ->
        val sorted = list.toMutableList()
        when (sort) {
            SortOption.DATE_ADDED -> if (asc) sorted.sortBy { it.dateAdded } else sorted.sortByDescending { it.dateAdded }
            SortOption.TITLE -> if (asc) sorted.sortBy { it.title.lowercase() } else sorted.sortByDescending { it.title.lowercase() }
            SortOption.ARTIST -> if (asc) sorted.sortBy { (it.artist ?: "").lowercase() } else sorted.sortByDescending { (it.artist ?: "").lowercase() }
            SortOption.ALBUM -> if (asc) sorted.sortBy { (it.album ?: "").lowercase() } else sorted.sortByDescending { (it.album ?: "").lowercase() }
            SortOption.GENRE -> if (asc) sorted.sortBy { (it.genre ?: "").lowercase() } else sorted.sortByDescending { (it.genre ?: "").lowercase() }
            SortOption.DURATION -> if (asc) sorted.sortBy { it.duration } else sorted.sortByDescending { it.duration }
            SortOption.YEAR -> if (asc) sorted.sortBy { it.year ?: 0 } else sorted.sortByDescending { it.year ?: 0 }
            SortOption.MOST_PLAYED -> if (asc) sorted.sortBy { it.playCount } else sorted.sortByDescending { it.playCount }
            SortOption.RECENTLY_PLAYED -> if (asc) sorted.sortBy { it.lastPlayed ?: 0L } else sorted.sortByDescending { it.lastPlayed ?: 0L }
        }
        sorted
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

    fun toggleAscending() {
        _isAscending.value = !_isAscending.value
    }

    fun playAll(startIndex: Int = 0, shuffled: Boolean = false) {
        val currentList = sortedSongs.value
        if (currentList.isNotEmpty()) {
            queueManager.loadAndPlayQueue("All Songs", currentList, startIndex, shuffled)
        }
    }

    fun shuffleAll() {
        val currentList = sortedSongs.value
        if (currentList.isNotEmpty()) {
            val randomIndex = (0 until currentList.size).random()
            queueManager.loadAndPlayQueue("All Songs", currentList, randomIndex, true)
        }
    }

    fun updateSong(song: SongEntity) {
        viewModelScope.launch {
            libraryDao.updateSong(song)
        }
    }

    fun deleteSong(song: SongEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = audioFileDeleter.delete(song.path)
            withContext(Dispatchers.Main) {
                when (result) {
                    is DeleteResult.Success -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            libraryDao.deleteSongById(song.id)
                        }
                        queueManager.removeSong(song.id)
                        onComplete?.invoke(true)
                    }
                    is DeleteResult.RequiresPermission -> {
                        _pendingDeletePermission.value = PendingDeletePermission(
                            intentSender = result.intentSender,
                            pendingSong = song
                        )
                        onComplete?.invoke(false)
                    }
                    is DeleteResult.Failure -> {
                        onComplete?.invoke(false)
                    }
                }
            }
        }
    }

    fun onDeletePermissionGranted() {
        val pending = _pendingDeletePermission.value ?: return
        _pendingDeletePermission.value = null
        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.deleteSongById(pending.pendingSong.id)
            queueManager.removeSong(pending.pendingSong.id)
        }
    }

    fun onDeletePermissionDenied() {
        _pendingDeletePermission.value = null
    }

    fun deleteSongs(songsToDelete: List<SongEntity>, onComplete: ((Boolean) -> Unit)? = null) {
        if (songsToDelete.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val result = audioFileDeleter.deleteBatch(songsToDelete.map { it.path })
                when (result) {
                    is DeleteBatchResult.Success -> {
                        val deletedPaths = result.deletedPaths.toSet()
                        val deletedSongs = if (deletedPaths.isEmpty()) songsToDelete else songsToDelete.filter { it.path in deletedPaths }
                        deletedSongs.forEach { song ->
                            libraryDao.deleteSongById(song.id)
                            queueManager.removeSong(song.id)
                        }
                        onComplete?.invoke(true)
                    }
                    is DeleteBatchResult.RequiresPermission -> {
                        _pendingBatchDeletePermission.value = PendingBatchDeletePermission(
                            intentSender = result.intentSender,
                            pendingSongIds = songsToDelete.map { it.id }
                        )
                        onComplete?.invoke(false)
                    }
                    is DeleteBatchResult.Failure -> {
                        onComplete?.invoke(false)
                    }
                }
            }
        }
    }

    fun onBatchDeletePermissionGranted() {
        val pending = _pendingBatchDeletePermission.value ?: return
        _pendingBatchDeletePermission.value = null
        viewModelScope.launch(Dispatchers.IO) {
            pending.pendingSongIds.forEach { songId ->
                libraryDao.deleteSongById(songId)
                queueManager.removeSong(songId)
            }
        }
    }

    fun onBatchDeletePermissionDenied() {
        _pendingBatchDeletePermission.value = null
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                libraryScanner.scanMediaStore(forceDiskRead = true)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
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
}
