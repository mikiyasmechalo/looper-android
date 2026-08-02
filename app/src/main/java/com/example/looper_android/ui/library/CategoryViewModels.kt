package com.example.looper_android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.PlaylistEntity
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

enum class ViewMode { GRID_2, GRID_3, LIST }
enum class AlbumSortOption { NAME, ARTIST, SONG_COUNT, DATE_ADDED, YEAR }
enum class ArtistSortOption { NAME, SONG_COUNT }
enum class GenreSortOption { NAME, SONG_COUNT }

data class FolderNode(
    val path: String,
    val name: String,
    val songs: List<SongEntity>,
    val subFolders: List<FolderNode>
)

data class GenreData(
    val name: String,
    val songs: List<SongEntity>
)


// --- ALBUMS VIEW MODEL ---
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("ui_view_modes", Context.MODE_PRIVATE)

    private val _albums = MutableStateFlow<List<AlbumEntity>>(emptyList())
    val albums: StateFlow<List<AlbumEntity>> = _albums.asStateFlow()

    private val _albumSongCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val albumSongCounts: StateFlow<Map<String, Int>> = _albumSongCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val initialMode = prefs.getString("albums_view_mode", ViewMode.GRID_2.name)?.let {
        runCatching { ViewMode.valueOf(it) }.getOrNull()
    } ?: ViewMode.GRID_2

    private val _viewMode = MutableStateFlow(initialMode)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortOption = MutableStateFlow(AlbumSortOption.NAME)
    val sortOption: StateFlow<AlbumSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    init {
        combine(libraryRepository.allSongs, libraryRepository.allAlbums, _sortOption, _isAscending) { songs, rawAlbums, option, asc ->
            _isLoading.value = false
            val counts = mutableMapOf<String, Int>()
            for (s in songs) {
                val albumName = s.album ?: "Unknown Album"
                counts[albumName] = (counts[albumName] ?: 0) + 1
            }
            _albumSongCounts.value = counts

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
        }.onEach { _albums.value = it }.launchIn(viewModelScope)
    }

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
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("ui_view_modes", Context.MODE_PRIVATE)

    private val _artists = MutableStateFlow<List<ArtistEntity>>(emptyList())
    val artists: StateFlow<List<ArtistEntity>> = _artists.asStateFlow()

    private val _artistSongCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val artistSongCounts: StateFlow<Map<String, Int>> = _artistSongCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val initialMode = prefs.getString("artists_view_mode", ViewMode.GRID_2.name)?.let {
        runCatching { ViewMode.valueOf(it) }.getOrNull()
    } ?: ViewMode.GRID_2

    private val _viewMode = MutableStateFlow(initialMode)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortOption = MutableStateFlow(ArtistSortOption.NAME)
    val sortOption: StateFlow<ArtistSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    init {
        combine(libraryRepository.allSongs, libraryRepository.allArtists, _sortOption, _isAscending) { songs, rawArtists, option, asc ->
            _isLoading.value = false
            val counts = mutableMapOf<String, Int>()
            for (s in songs) {
                val artistName = s.albumArtist?.trim()?.ifBlank { null } ?: s.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                counts[artistName] = (counts[artistName] ?: 0) + 1
            }
            _artistSongCounts.value = counts

            val filteredArtists = rawArtists.filter { (counts[it.name] ?: 0) > 0 }.toMutableList()
            when (option) {
                ArtistSortOption.NAME -> filteredArtists.sortBy { it.name.lowercase() }
                ArtistSortOption.SONG_COUNT -> filteredArtists.sortByDescending { counts[it.name] ?: 0 }
            }
            if (!asc) filteredArtists.reverse()
            filteredArtists
        }.onEach { _artists.value = it }.launchIn(viewModelScope)
    }

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
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    init {
        libraryRepository.allPlaylists.onEach { _playlists.value = it }.launchIn(viewModelScope)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    songPaths = "[]",
                    dateCreated = System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis()
                )
            )
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.deletePlaylistById(id)
        }
    }
}

// --- GENRES VIEW MODEL ---
@HiltViewModel
class GenresViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    private val _genres = MutableStateFlow<List<GenreData>>(emptyList())
    val genres: StateFlow<List<GenreData>> = _genres.asStateFlow()

    private val _sortOption = MutableStateFlow(GenreSortOption.SONG_COUNT)
    val sortOption: StateFlow<GenreSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    init {
        combine(libraryRepository.allSongs, _sortOption, _isAscending) { songs, option, asc ->
            val genreMap = mutableMapOf<String, MutableList<SongEntity>>()
            for (s in songs) {
                val genre = s.genre?.trim()?.ifBlank { null } ?: "Unknown"
                genreMap.getOrPut(genre) { mutableListOf() }.add(s)
            }

            val list = genreMap.entries.map { GenreData(it.key, it.value) }.toMutableList()
            when (option) {
                GenreSortOption.NAME -> list.sortBy { it.name.lowercase() }
                GenreSortOption.SONG_COUNT -> list.sortByDescending { it.songs.size }
            }
            if (!asc) list.reverse()
            list
        }.onEach { _genres.value = it }.launchIn(viewModelScope)
    }

    fun setSortOption(option: GenreSortOption) { _sortOption.value = option }
    fun toggleAscending() { _isAscending.value = !_isAscending.value }
}

// --- FOLDERS VIEW MODEL ---
@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    private val _folderNodes = MutableStateFlow<List<FolderNode>>(emptyList())
    val folderNodes: StateFlow<List<FolderNode>> = _folderNodes.asStateFlow()

    init {
        libraryRepository.allSongs.onEach { songs ->
            val folderMap = mutableMapOf<String, MutableList<SongEntity>>()
            for (s in songs) {
                val parentPath = java.io.File(s.path).parent ?: "Root"
                folderMap.getOrPut(parentPath) { mutableListOf() }.add(s)
            }

            val nodes = folderMap.entries.map { entry ->
                FolderNode(
                    path = entry.key,
                    name = java.io.File(entry.key).name.ifBlank { entry.key },
                    songs = entry.value,
                    subFolders = emptyList()
                )
            }.sortedBy { it.name.lowercase() }

            _folderNodes.value = nodes
        }.launchIn(viewModelScope)
    }
}

// --- HISTORY VIEW MODEL ---
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository
) : ViewModel() {
    private val _historySongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val historySongs: StateFlow<List<SongEntity>> = _historySongs.asStateFlow()

    init {
        libraryRepository.allSongs.onEach { songs ->
            val history = songs.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
            _historySongs.value = history
        }.launchIn(viewModelScope)
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            libraryDao.clearHistory()
        }
    }
}
