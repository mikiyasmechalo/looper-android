package com.example.looper_android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.repository.GenreData
import com.example.looper_android.data.repository.LibraryCacheManager
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchResults(
    val topResult: Any? = null,
    val songs: List<SongEntity> = emptyList(),
    val albums: List<AlbumEntity> = emptyList(),
    val artists: List<ArtistEntity> = emptyList(),
    val genres: List<GenreData> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryCache: LibraryCacheManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchContext = MutableStateFlow("global")
    val searchContext: StateFlow<String> = _searchContext.asStateFlow()

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun updateSearchContext(context: String) {
        _searchContext.value = context
    }

    private class LibraryData(
        val songs: List<SongEntity>,
        val albums: List<AlbumEntity>,
        val artists: List<ArtistEntity>,
        val genres: List<GenreData>
    )

    private val libraryDataFlow = combine(
        libraryCache.songs,
        libraryCache.albums,
        libraryCache.artists,
        libraryCache.genres
    ) { songs, albums, artists, genres ->
        LibraryData(songs, albums, artists, genres)
    }

    val searchResults: StateFlow<SearchResults> = combine(
        _query,
        _searchContext,
        libraryDataFlow
    ) { query, context, data ->
        val allSongs = data.songs
        val allAlbums = data.albums
        val allArtists = data.artists
        val allGenres = data.genres

        if (query.isBlank()) {
            return@combine SearchResults()
        }

        val lowerQuery = query.lowercase().trim()

        // 1. Filter Songs with Flutter's scoring system
        val scoredSongs = allSongs.mapNotNull { song ->
            val title = song.title.lowercase()
            val lyrics = (song.lyrics ?: "").lowercase()
            val artist = (song.artist ?: "").lowercase()
            val album = (song.album ?: "").lowercase()

            var score = 0
            if (title == lowerQuery) score = 100
            else if (title.startsWith(lowerQuery)) score = 90
            else if (title.contains(lowerQuery)) score = 80
            else if (lyrics.contains(lowerQuery)) score = 50
            else if (artist.contains(lowerQuery) || album.contains(lowerQuery)) score = 30

            if (score > 0) Pair(song, score) else null
        }
        val sortedSongs = scoredSongs
            .sortedWith(compareByDescending<Pair<SongEntity, Int>> { it.second }.thenBy { it.first.title.lowercase() })
            .map { it.first }

        // 2. Filter Albums
        val filteredAlbums = allAlbums.filter {
            it.name.lowercase().contains(lowerQuery)
        }

        // 3. Filter Artists
        val filteredArtists = allArtists.filter {
            it.name.lowercase().contains(lowerQuery)
        }

        // 4. Filter Genres
        val filteredGenres = allGenres.filter {
            it.name.lowercase().contains(lowerQuery)
        }

        // 5. Determine Top Result and Order based on context
        var topResult: Any? = null
        
        when (context) {
            "albums" -> {
                if (filteredAlbums.isNotEmpty()) {
                    topResult = filteredAlbums.first()
                } else if (sortedSongs.isNotEmpty()) {
                    topResult = sortedSongs.first()
                } else if (filteredArtists.isNotEmpty()) {
                    topResult = filteredArtists.first()
                }
                SearchResults(
                    topResult = topResult,
                    albums = if (topResult is AlbumEntity) filteredAlbums.drop(1) else filteredAlbums,
                    songs = if (topResult is SongEntity) sortedSongs.drop(1) else sortedSongs,
                    artists = if (topResult is ArtistEntity) filteredArtists.drop(1) else filteredArtists,
                    genres = emptyList() // No genres in non-genre pages
                )
            }
            "artists" -> {
                if (filteredArtists.isNotEmpty()) {
                    topResult = filteredArtists.first()
                } else if (sortedSongs.isNotEmpty()) {
                    topResult = sortedSongs.first()
                } else if (filteredAlbums.isNotEmpty()) {
                    topResult = filteredAlbums.first()
                }
                SearchResults(
                    topResult = topResult,
                    artists = if (topResult is ArtistEntity) filteredArtists.drop(1) else filteredArtists,
                    songs = if (topResult is SongEntity) sortedSongs.drop(1) else sortedSongs,
                    albums = if (topResult is AlbumEntity) filteredAlbums.drop(1) else filteredAlbums,
                    genres = emptyList()
                )
            }
            "genres" -> {
                if (filteredGenres.isNotEmpty()) {
                    topResult = filteredGenres.first()
                } else if (sortedSongs.isNotEmpty()) {
                    topResult = sortedSongs.first()
                }
                SearchResults(
                    topResult = topResult,
                    genres = if (topResult is GenreData) filteredGenres.drop(1) else filteredGenres,
                    songs = if (topResult is SongEntity) sortedSongs.drop(1) else sortedSongs,
                    albums = emptyList(),
                    artists = emptyList()
                )
            }
            else -> { // global or songs
                if (sortedSongs.isNotEmpty()) {
                    topResult = sortedSongs.first()
                } else if (filteredAlbums.isNotEmpty()) {
                    topResult = filteredAlbums.first()
                } else if (filteredArtists.isNotEmpty()) {
                    topResult = filteredArtists.first()
                }
                SearchResults(
                    topResult = topResult,
                    songs = if (topResult is SongEntity) sortedSongs.drop(1) else sortedSongs,
                    albums = if (topResult is AlbumEntity) filteredAlbums.drop(1) else filteredAlbums,
                    artists = if (topResult is ArtistEntity) filteredArtists.drop(1) else filteredArtists,
                    genres = emptyList() // User requested not to include genres in results in other pages
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchResults()
    )
}
