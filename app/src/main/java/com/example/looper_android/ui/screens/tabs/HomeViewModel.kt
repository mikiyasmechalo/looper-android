package com.example.looper_android.ui.screens.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.domain.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val queueManager: QueueManager,
    val libraryCacheManager: com.example.looper_android.data.repository.LibraryCacheManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("home_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val sessionSeed: Long = System.currentTimeMillis()

    private var cachedQuickPicksPool: List<SongEntity>? = null
    private var cachedQuickPicksDisplay: List<SongEntity>? = null
    private var cachedDate: String? = null

    init {
        combine(
            libraryDao.getAllSongs(),
            libraryDao.getAllAlbums(),
            libraryDao.getAllArtists()
        ) { songs, albums, artists ->
            if (songs.isEmpty()) {
                return@combine HomeUiState(isLoading = false)
            }

            // 1. Quick Picks: stable per session and daily cache
            val today = java.time.LocalDate.now().toString()
            val savedDate = prefs.getString("quick_picks_date", null)
            val savedPoolIds = prefs.getString("quick_picks_pool_ids", null)?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
            val savedDisplayIds = prefs.getString("quick_picks_display_ids", null)?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

            val songsMap = songs.associateBy { it.id }

            val (quickPicksPool, quickPicksDisplay) = if (
                cachedDate == today && cachedQuickPicksPool != null && cachedQuickPicksDisplay != null
            ) {
                val updatedPool = cachedQuickPicksPool!!.mapNotNull { songsMap[it.id] }
                val updatedDisplay = cachedQuickPicksDisplay!!.mapNotNull { songsMap[it.id] }
                if (updatedDisplay.isNotEmpty()) {
                    Pair(updatedPool, updatedDisplay)
                } else {
                    generateNewQuickPicks(songs, today)
                }
            } else if (savedDate == today && savedPoolIds.isNotEmpty() && savedDisplayIds.isNotEmpty()) {
                val restoredPool = savedPoolIds.mapNotNull { songsMap[it] }
                val restoredDisplay = savedDisplayIds.mapNotNull { songsMap[it] }
                if (restoredDisplay.isNotEmpty()) {
                    cachedDate = today
                    cachedQuickPicksPool = restoredPool
                    cachedQuickPicksDisplay = restoredDisplay
                    Pair(restoredPool, restoredDisplay)
                } else {
                    generateNewQuickPicks(songs, today)
                }
            } else {
                generateNewQuickPicks(songs, today)
            }

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
                songsByArtist.getOrPut(effectiveArtist.lowercase()) { mutableListOf() }.add(song)
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

            val topArtists = artists
                .distinctBy { it.name.lowercase() }
                .sortedByDescending { artist ->
                    artistScoreMap[artist.name.lowercase()] ?: 0
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

            // 5. Genres: Group by genre (filtering out unknown/blank) and sort by track count DESC
            val genresMap = mutableMapOf<String, MutableList<SongEntity>>()
            for (song in songs) {
                val rawGenre = song.genre?.trim()
                if (!rawGenre.isNullOrBlank() &&
                    !rawGenre.equals("unknown", ignoreCase = true) &&
                    !rawGenre.equals("unknown genre", ignoreCase = true) &&
                    !rawGenre.equals("<unknown>", ignoreCase = true) &&
                    !rawGenre.equals("null", ignoreCase = true) &&
                    !rawGenre.equals("undefined", ignoreCase = true)) {
                    genresMap.getOrPut(rawGenre) { mutableListOf() }.add(song)
                }
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

            val topGenres = genresMap.entries
                .sortedByDescending { genreScoreMap[it.key] ?: 0 }
                .map { GenreItem(name = it.key, songs = it.value) }

            allSongsList = songs

            HomeUiState(
                quickPicks = quickPicksDisplay,
                quickPicksPool = quickPicksPool,
                topSongs = topSongsDisplay,
                topSongsPool = topSongsPool,
                artists = topArtists,
                albums = topAlbums,
                genres = topGenres,
                isLoading = false
            )
        }
        .flowOn(Dispatchers.Default)
        .onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    private var allSongsList: List<SongEntity> = emptyList()

    private fun generateNewQuickPicks(songs: List<SongEntity>, today: String): Pair<List<SongEntity>, List<SongEntity>> {
        val recentlyPlayed = songs.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }
        val topPlayed = songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(15)
        val randomMix = songs.shuffled(Random(sessionSeed))

        val pool = (recentlyPlayed + topPlayed + randomMix).distinctBy { it.id }.take(100)
        val display = pool.shuffled(Random(sessionSeed)).take(18)

        cachedDate = today
        cachedQuickPicksPool = pool
        cachedQuickPicksDisplay = display

        prefs.edit()
            .putString("quick_picks_date", today)
            .putString("quick_picks_pool_ids", pool.joinToString(",") { it.id.toString() })
            .putString("quick_picks_display_ids", display.joinToString(",") { it.id.toString() })
            .apply()

        return Pair(pool, display)
    }

    fun playQuickPicks(shuffled: Boolean = true) {
        val pool = _uiState.value.quickPicksPool
        if (pool.isNotEmpty()) {
            val startIndex = if (shuffled) (0 until pool.size).random() else 0
            queueManager.loadAndPlayQueue("Quick Picks", pool, startIndex, shuffled)
        }
    }

    fun playSongFromQuickPicks(song: SongEntity) {
        val pool = _uiState.value.quickPicksPool
        val reordered = if (pool.isNotEmpty()) {
            listOf(song) + pool.filter { it.id != song.id }
        } else {
            listOf(song)
        }
        queueManager.loadAndPlayQueue("Quick Picks", reordered, 0, false)
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

    fun playArtistsShuffle() {
        val topArtists = _uiState.value.artists.take(15).map { it.name }.toSet()
        val songs = if (topArtists.isNotEmpty()) {
            allSongsList.filter { (it.albumArtist ?: it.artist) in topArtists }
        } else allSongsList
        if (songs.isNotEmpty()) {
            val startIndex = (0 until songs.size).random()
            queueManager.loadAndPlayQueue("Top Artists", songs, startIndex, true)
        }
    }

    fun playAlbumsShuffle() {
        val topAlbums = _uiState.value.albums.take(15).map { it.name }.toSet()
        val songs = if (topAlbums.isNotEmpty()) {
            allSongsList.filter { it.album in topAlbums }
        } else allSongsList
        if (songs.isNotEmpty()) {
            val startIndex = (0 until songs.size).random()
            queueManager.loadAndPlayQueue("Top Albums", songs, startIndex, true)
        }
    }

    fun playGenresShuffle() {
        val allGenreSongs = _uiState.value.genres.flatMap { it.songs }.distinctBy { it.id }
        val songs = if (allGenreSongs.isNotEmpty()) allGenreSongs else allSongsList
        if (songs.isNotEmpty()) {
            val startIndex = (0 until songs.size).random()
            queueManager.loadAndPlayQueue("Top Genres", songs, startIndex, true)
        }
    }
}
