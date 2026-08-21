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

    private val _recentlyAddedSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val recentlyAddedSongs: StateFlow<List<SongEntity>> = _recentlyAddedSongs.asStateFlow()

    private val _notPlayedSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val notPlayedSongs: StateFlow<List<SongEntity>> = _notPlayedSongs.asStateFlow()

    private val _isDbLoaded = MutableStateFlow(false)
    val isDbLoaded: StateFlow<Boolean> = _isDbLoaded.asStateFlow()

    init {
        // Observe base tables
        libraryDao.getAllSongs().onEach { 
            _songs.value = it 
            _recentlyAddedSongs.value = it.sortedByDescending { s -> s.dateAdded }.take(25)
            _notPlayedSongs.value = it.filter { s -> s.playCount == 0 }.shuffled().take(25)
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
                counts[albumName.lowercase()] = (counts[albumName.lowercase()] ?: 0) + 1
            }
            _albumSongCounts.value = counts
            _albums.value = albumList.filter { album ->
                val count = counts[album.name.lowercase()] ?: 0
                count > 0
            }
        }.launchIn(scope)

        combine(_songs, _rawArtists, settingsRepository.useAlbumArtist) { songList, artistList, useAlbumArtist ->
            val artistMap = mutableMapOf<String, ArtistEntity>()
            for (a in artistList) {
                val lower = a.name.lowercase()
                val existing = artistMap[lower]
                if (existing == null || (!a.artistImageUrl.isNullOrBlank() && existing.artistImageUrl.isNullOrBlank())) {
                    artistMap[lower] = a
                }
            }

            // Case-insensitive artist grouping
            val artistNameVariants = mutableMapOf<String, MutableList<String>>()
            val artistSongCountMap = mutableMapOf<String, Int>()

            for (s in songList) {
                val artistName = if (useAlbumArtist) {
                    s.albumArtist?.trim()?.ifBlank { null } ?: s.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                } else {
                    s.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                }
                val lowerKey = artistName.lowercase()
                artistNameVariants.getOrPut(lowerKey) { mutableListOf() }.add(artistName)
                artistSongCountMap[lowerKey] = (artistSongCountMap[lowerKey] ?: 0) + 1
            }

            val computedArtists = artistNameVariants.entries.mapIndexed { index, (lowerKey, names) ->
                val existing = artistMap[lowerKey]
                // Pick canonical name:
                // 1. If existing artist in DB has an image, use its name
                // 2. Otherwise pick the most frequently used casing (or one with upper/mixed case)
                val bestName = if (existing != null && !existing.artistImageUrl.isNullOrBlank()) {
                    existing.name
                } else {
                    names.groupBy { it }.maxByOrNull { it.value.size }?.key
                        ?: existing?.name
                        ?: names.first()
                }

                ArtistEntity(
                    id = existing?.id ?: (index + 1).toLong(),
                    name = bestName,
                    artPath = existing?.artPath,
                    artistImageUrl = existing?.artistImageUrl
                )
            }.sortedBy { it.name.lowercase() }

            val finalCounts = mutableMapOf<String, Int>()
            for (artist in computedArtists) {
                val count = artistSongCountMap[artist.name.lowercase()] ?: 0
                finalCounts[artist.name] = count
                finalCounts[artist.name.lowercase()] = count
            }

            _artistSongCounts.value = finalCounts
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
        addSongsToPlaylist(playlist, listOf(songPath))
    }

    fun addSongsToPlaylist(playlist: PlaylistEntity, songPaths: List<String>) {
        if (songPaths.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val jsonArray = try {
                org.json.JSONArray(playlist.songPaths.ifBlank { "[]" })
            } catch (_: Exception) {
                org.json.JSONArray()
            }
            val existingSet = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                existingSet.add(jsonArray.optString(i))
            }
            var modified = false
            for (path in songPaths) {
                if (!existingSet.contains(path)) {
                    jsonArray.put(path)
                    existingSet.add(path)
                    modified = true
                }
            }
            if (modified) {
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
