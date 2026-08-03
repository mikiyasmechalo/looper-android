package com.example.looper_android.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.PlaylistEntity
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.FolderNode
import com.example.looper_android.data.repository.GenreData
import com.example.looper_android.data.repository.LibraryCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ViewMode { GRID_2, GRID_3, LIST }
enum class AlbumSortOption { NAME, ARTIST, SONG_COUNT, DATE_ADDED, YEAR }
enum class ArtistSortOption { NAME, SONG_COUNT }
enum class GenreSortOption { NAME, SONG_COUNT }

// --- ALBUMS VIEW MODEL ---
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    private val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)

    val isDbLoaded = libraryCacheManager.isDbLoaded
    val albumSongCounts = libraryCacheManager.albumSongCounts

    private val initialViewMode = try {
        val saved = prefs.getString("albums_view_mode", ViewMode.GRID_2.name) ?: ViewMode.GRID_2.name
        ViewMode.valueOf(saved)
    } catch (_: Exception) { ViewMode.GRID_2 }

    private val _viewMode = MutableStateFlow(initialViewMode)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortOption = MutableStateFlow(AlbumSortOption.NAME)
    val sortOption: StateFlow<AlbumSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    val albums: StateFlow<List<AlbumEntity>> = combine(
        libraryCacheManager.albums,
        albumSongCounts,
        _sortOption,
        _isAscending
    ) { rawAlbums, counts, option, asc ->
        val sorted = rawAlbums.toMutableList()
        when (option) {
            AlbumSortOption.NAME -> sorted.sortBy { it.name.lowercase() }
            AlbumSortOption.ARTIST -> sorted.sortBy { (it.artist ?: "").lowercase() }
            AlbumSortOption.SONG_COUNT -> sorted.sortByDescending { counts[it.name] ?: 0 }
            AlbumSortOption.DATE_ADDED -> sorted.sortByDescending { it.dateAdded }
            AlbumSortOption.YEAR -> sorted.sortByDescending { it.year ?: 0 }
        }
        if (!asc) sorted.reverse()
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cycleViewMode() {
        val nextMode = when (_viewMode.value) {
            ViewMode.GRID_2 -> ViewMode.GRID_3
            ViewMode.GRID_3 -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.GRID_2
        }
        _viewMode.value = nextMode
        prefs.edit().putString("albums_view_mode", nextMode.name).apply()
    }

    fun setSortOption(option: AlbumSortOption) { _sortOption.value = option }
    fun toggleAscending() { _isAscending.value = !_isAscending.value }
}

// --- ARTISTS VIEW MODEL ---
@HiltViewModel
class ArtistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    private val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)

    val isDbLoaded = libraryCacheManager.isDbLoaded
    val artistSongCounts = libraryCacheManager.artistSongCounts

    private val initialViewMode = try {
        val saved = prefs.getString("artists_view_mode", ViewMode.GRID_2.name) ?: ViewMode.GRID_2.name
        ViewMode.valueOf(saved)
    } catch (_: Exception) { ViewMode.GRID_2 }

    private val _viewMode = MutableStateFlow(initialViewMode)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortOption = MutableStateFlow(ArtistSortOption.NAME)
    val sortOption: StateFlow<ArtistSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    val artists: StateFlow<List<ArtistEntity>> = combine(
        libraryCacheManager.artists,
        artistSongCounts,
        _sortOption,
        _isAscending
    ) { rawArtists, counts, option, asc ->
        val sorted = rawArtists.toMutableList()
        when (option) {
            ArtistSortOption.NAME -> sorted.sortBy { it.name.lowercase() }
            ArtistSortOption.SONG_COUNT -> sorted.sortByDescending { counts[it.name] ?: 0 }
        }
        if (!asc) sorted.reverse()
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cycleViewMode() {
        val nextMode = when (_viewMode.value) {
            ViewMode.GRID_2 -> ViewMode.GRID_3
            ViewMode.GRID_3 -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.GRID_2
        }
        _viewMode.value = nextMode
        prefs.edit().putString("artists_view_mode", nextMode.name).apply()
    }

    fun setSortOption(option: ArtistSortOption) { _sortOption.value = option }
    fun toggleAscending() { _isAscending.value = !_isAscending.value }
}

// --- PLAYLISTS VIEW MODEL ---
@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistEntity>> = libraryCacheManager.playlists

    fun createPlaylist(name: String) = libraryCacheManager.createPlaylist(name)
    fun deletePlaylist(id: Long) = libraryCacheManager.deletePlaylist(id)
}

// --- GENRES VIEW MODEL ---
@HiltViewModel
class GenresViewModel @Inject constructor(
    libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    private val _sortOption = MutableStateFlow(GenreSortOption.SONG_COUNT)
    val sortOption: StateFlow<GenreSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    val genres: StateFlow<List<GenreData>> = combine(
        libraryCacheManager.genres,
        _sortOption,
        _isAscending
    ) { rawGenres, option, asc ->
        val list = rawGenres.toMutableList()
        when (option) {
            GenreSortOption.NAME -> list.sortBy { it.name.lowercase() }
            GenreSortOption.SONG_COUNT -> list.sortByDescending { it.songs.size }
        }
        if (!asc) list.reverse()
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOption(option: GenreSortOption) { _sortOption.value = option }
    fun toggleAscending() { _isAscending.value = !_isAscending.value }
}

// --- FOLDERS VIEW MODEL ---
@HiltViewModel
class FoldersViewModel @Inject constructor(
    libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    val folderNodes = libraryCacheManager.folderNodes
}

// --- HISTORY VIEW MODEL ---
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    val historySongs = libraryCacheManager.historySongs

    fun clearHistory() = libraryCacheManager.clearHistory()
}
