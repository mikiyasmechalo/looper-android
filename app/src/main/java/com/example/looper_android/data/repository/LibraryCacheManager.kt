package com.example.looper_android.data.repository

import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.PlaylistEntity
import com.example.looper_android.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class LibraryCacheManager @Inject constructor(
    private val libraryDao: LibraryDao,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Raw sources
    private val _songs = MutableStateFlow<List<SongEntity>>(emptyList())
    val songs: StateFlow<List<SongEntity>> = _songs.asStateFlow()

    private val _rawAlbums = MutableStateFlow<List<AlbumEntity>>(emptyList())
    private val _rawArtists = MutableStateFlow<List<ArtistEntity>>(emptyList())
    
    val playlists: StateFlow<List<PlaylistEntity>> = libraryDao.getAllPlaylists()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Processed Data
    private val _albums = MutableStateFlow<List<AlbumEntity>>(emptyList())
    val albums: StateFlow<List<AlbumEntity>> = _albums.asStateFlow()

    private val _albumSongCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val albumSongCounts: StateFlow<Map<String, Int>> = _albumSongCounts.asStateFlow()

    private val _artists = MutableStateFlow<List<ArtistEntity>>(emptyList())
    val artists: StateFlow<List<ArtistEntity>> = _artists.asStateFlow()

    private val _artistSongCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val artistSongCounts: StateFlow<Map<String, Int>> = _artistSongCounts.asStateFlow()

    private val _genres = MutableStateFlow<List<GenreData>>(emptyList())
    val genres: StateFlow<List<GenreData>> = _genres.asStateFlow()

    private val _folderNodes = MutableStateFlow<List<FolderNode>>(emptyList())
    val folderNodes: StateFlow<List<FolderNode>> = _folderNodes.asStateFlow()

    private val _historySongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val historySongs: StateFlow<List<SongEntity>> = _historySongs.asStateFlow()

    private val _isDbLoaded = MutableStateFlow(false)
    val isDbLoaded: StateFlow<Boolean> = _isDbLoaded.asStateFlow()

    init {
        // Observe base tables
        libraryDao.getAllSongs().onEach { 
            _songs.value = it 
            _isDbLoaded.value = true
        }.launchIn(scope)

        libraryDao.getAllAlbums().onEach { _rawAlbums.value = it }.launchIn(scope)
        libraryDao.getAllArtists().onEach { _rawArtists.value = it }.launchIn(scope)

        // Process combined data asynchronously on Dispatchers.Default
        combine(_songs, _rawAlbums) { songList, albumList ->
            val counts = mutableMapOf<String, Int>()
            for (s in songList) {
                val albumName = s.album ?: "Unknown Album"
                counts[albumName] = (counts[albumName] ?: 0) + 1
            }
            _albumSongCounts.value = counts
            _albums.value = albumList
        }.launchIn(scope)

        combine(_songs, _rawArtists, settingsRepository.useAlbumArtist) { songList, artistList, useAlbumArtist ->
            val counts = mutableMapOf<String, Int>()
            val artistMap = mutableMapOf<String, ArtistEntity>()
            for (a in artistList) {
                artistMap[a.name.lowercase()] = a
            }

            for (s in songList) {
                val artistName = if (useAlbumArtist) {
                    s.albumArtist?.trim()?.ifBlank { null } ?: s.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                } else {
                    s.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                }
                counts[artistName] = (counts[artistName] ?: 0) + 1
            }
            _artistSongCounts.value = counts
            
            val computedArtists = counts.keys.mapIndexed { index, name ->
                val existing = artistMap[name.lowercase()]
                ArtistEntity(
                    id = existing?.id ?: (index + 1).toLong(),
                    name = name,
                    artPath = existing?.artPath,
                    artistImageUrl = existing?.artistImageUrl
                )
            }.sortedBy { it.name.lowercase() }
            _artists.value = computedArtists
        }.launchIn(scope)

        // Process Genres
        _songs.onEach { songList ->
            val genreMap = mutableMapOf<String, MutableList<SongEntity>>()
            for (s in songList) {
                val rawGenre = s.genre?.trim()?.ifBlank { null } ?: "Unknown Genre"
                genreMap.getOrPut(rawGenre) { mutableListOf() }.add(s)
            }
            _genres.value = genreMap.entries.map { GenreData(it.key, it.value) }
        }.launchIn(scope)

        // Process Folders
        _songs.onEach { songList ->
            val folderMap = mutableMapOf<String, MutableList<SongEntity>>()
            for (s in songList) {
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
        }.launchIn(scope)

        // Process History
        _songs.onEach { songList ->
            val history = songList.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
            _historySongs.value = history
        }.launchIn(scope)
    }

    // Proxy for creating/updating playlists
    fun createPlaylist(name: String) {
        scope.launch(Dispatchers.IO) {
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

    fun addSongToPlaylist(playlist: PlaylistEntity, songPath: String) {
        scope.launch(Dispatchers.IO) {
            val jsonArray = try {
                org.json.JSONArray(playlist.songPaths.ifBlank { "[]" })
            } catch (_: Exception) {
                org.json.JSONArray()
            }
            var exists = false
            for (i in 0 until jsonArray.length()) {
                if (jsonArray.optString(i) == songPath) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                jsonArray.put(songPath)
                val updated = playlist.copy(
                    songPaths = jsonArray.toString(),
                    dateModified = System.currentTimeMillis()
                )
                libraryDao.insertPlaylist(updated)
            }
        }
    }

    fun removeSongFromPlaylist(playlist: PlaylistEntity, songPath: String) {
        scope.launch(Dispatchers.IO) {
            val jsonArray = try {
                org.json.JSONArray(playlist.songPaths.ifBlank { "[]" })
            } catch (_: Exception) {
                org.json.JSONArray()
            }
            val newArray = org.json.JSONArray()
            for (i in 0 until jsonArray.length()) {
                val p = jsonArray.optString(i)
                if (p != songPath) {
                    newArray.put(p)
                }
            }
            val updated = playlist.copy(
                songPaths = newArray.toString(),
                dateModified = System.currentTimeMillis()
            )
            libraryDao.insertPlaylist(updated)
        }
    }

    fun deletePlaylist(id: Long) {
        scope.launch(Dispatchers.IO) {
            libraryDao.deletePlaylistById(id)
        }
    }
    
    fun clearHistory() {
        scope.launch(Dispatchers.IO) {
            libraryDao.clearHistory()
        }
    }
}
