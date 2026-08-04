package com.example.looper_android.ui.screens.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.domain.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.random.Random

data class GenreItem(
    val name: String,
    val songs: List<SongEntity>
)

data class HomeUiState(
    val quickPicks: List<SongEntity> = emptyList(),
    val quickPicksPool: List<SongEntity> = emptyList(),
    val topSongs: List<SongEntity> = emptyList(),
    val topSongsPool: List<SongEntity> = emptyList(),
    val artists: List<ArtistEntity> = emptyList(),
    val albums: List<AlbumEntity> = emptyList(),
    val genres: List<GenreItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val queueManager: QueueManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Fixed seed per app launch session so unplayed Quick Picks stay static while looking at screen
    private val sessionSeed: Long = System.currentTimeMillis()

    init {
        combine(
            libraryDao.getAllSongs(),
            libraryDao.getAllAlbums(),
            libraryDao.getAllArtists()
        ) { songs, albums, artists ->
            if (songs.isEmpty()) {
                return@combine HomeUiState(isLoading = false)
            }

            // 1. Quick Picks Pool (Up to 100 songs mix of recently played, top songs & random library songs)
            val recentlyPlayed = songs.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
            val topPlayed = songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(15)
            val randomMix = songs.shuffled(Random(sessionSeed))

            val quickPicksPool = (recentlyPlayed + topPlayed + randomMix).distinctBy { it.id }.take(100)
            // Shuffle the pool using sessionSeed so top/recent songs aren't all grouped at the front of cards
            val quickPicksDisplay = quickPicksPool.shuffled(Random(sessionSeed)).take(18)

            // 2. Top Songs Pool (Up to 100 songs sorted by playCount DESC, fallback dateAdded DESC)
            val topSongsPool = songs.sortedWith(
                compareByDescending<SongEntity> { it.playCount }
                    .thenByDescending { it.dateAdded }
            ).take(100)
            val topSongsDisplay = topSongsPool.take(8)

            // 3. Effective Artist Resolution (albumArtist preferred, fallback to artist)
            val songsByArtist = mutableMapOf<String, MutableList<SongEntity>>()
            for (song in songs) {
                val effectiveArtist = song.albumArtist?.trim()?.ifBlank { null }
                    ?: song.artist?.trim()?.ifBlank { null }
                    ?: "Unknown Artist"
                songsByArtist.getOrPut(effectiveArtist) { mutableListOf() }.add(song)
            }

            // Top Artists: Played artists ALWAYS rank above unplayed artists!
            val artistScoreMap = songsByArtist.mapValues { entry ->
                val totalPlays = entry.value.sumOf { it.playCount }
                val songCount = entry.value.size
                if (totalPlays > 0) {
                    100000 + (totalPlays * 1000) + songCount
                } else {
                    songCount
                }
            }

            val topArtists = artists.sortedByDescending { artist ->
                artistScoreMap[artist.name] ?: 0
            }

            // 4. Top Albums: Played albums ALWAYS rank above unplayed albums!
            val songsByAlbum = songs.groupBy { it.album }
            val albumScoreMap = songsByAlbum.mapValues { entry ->
                val totalPlays = entry.value.sumOf { it.playCount }
                val songCount = entry.value.size
                if (totalPlays > 0) {
                    100000 + (totalPlays * 1000) + songCount
                } else {
                    songCount
                }
            }

            val topAlbums = albums.sortedByDescending { album ->
                albumScoreMap[album.name] ?: 0
            }

            // 5. Genres: Group by genre and sort by track count DESC
            val genresMap = mutableMapOf<String, MutableList<SongEntity>>()
            for (song in songs) {
                val genre = song.genre?.trim()?.ifBlank { null } ?: "Unknown"
                genresMap.getOrPut(genre) { mutableListOf() }.add(song)
            }

            val genreScoreMap = genresMap.mapValues { entry ->
                val totalPlays = entry.value.sumOf { it.playCount }
                val songCount = entry.value.size
                if (totalPlays > 0) {
                    100000 + (totalPlays * 1000) + songCount
                } else {
                    songCount
                }
            }

            val genresList = genresMap.entries
                .sortedByDescending { genreScoreMap[it.key] ?: 0 }
                .map { GenreItem(name = it.key, songs = it.value) }

            HomeUiState(
                quickPicks = quickPicksDisplay,
                quickPicksPool = quickPicksPool,
                topSongs = topSongsDisplay,
                topSongsPool = topSongsPool,
                artists = topArtists,
                albums = topAlbums,
                genres = genresList,
                isLoading = false
            )
        }
        .flowOn(Dispatchers.Default)
        .onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun playQuickPicks(shuffled: Boolean = true) {
        val pool = _uiState.value.quickPicksPool
        if (pool.isNotEmpty()) {
            val startIndex = if (shuffled) (0 until pool.size).random() else 0
            queueManager.loadAndPlayQueue("Quick Picks", pool, startIndex, shuffled)
        }
    }

    fun playSongFromQuickPicks(index: Int) {
        val pool = _uiState.value.quickPicksPool
        if (pool.isNotEmpty() && index in pool.indices) {
            queueManager.loadAndPlayQueue("Quick Picks", pool, index, false)
        }
    }

    fun playTopSongs(shuffled: Boolean = true) {
        val pool = _uiState.value.topSongsPool
        if (pool.isNotEmpty()) {
            val startIndex = if (shuffled) (0 until pool.size).random() else 0
            queueManager.loadAndPlayQueue("Top Songs", pool, startIndex, shuffled)
        }
    }

    fun playAllTopSongs(startIndex: Int = 0) {
        val pool = _uiState.value.topSongsPool
        if (pool.isNotEmpty() && startIndex in pool.indices) {
            queueManager.loadAndPlayQueue("Top Songs", pool, startIndex, false)
        }
    }
}
