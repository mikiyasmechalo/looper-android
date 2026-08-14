package com.example.looper_android.ui.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.SongEntity
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

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    libraryCacheManager: LibraryCacheManager
) : ViewModel() {
    private val prefs = context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE)

    val isDbLoaded = libraryCacheManager.isDbLoaded

    private val initialSortOption = try {
        val saved = prefs.getString("collection_sort_option", SongSortOption.TITLE.name) ?: SongSortOption.TITLE.name
        SongSortOption.valueOf(saved)
    } catch (_: Exception) { SongSortOption.TITLE }

    private val initialIsAscending = prefs.getBoolean("collection_sort_ascending", true)

    private val _title = MutableStateFlow("")
    private val _type = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(initialSortOption)
    private val _isAscending = MutableStateFlow(initialIsAscending)

    val sortOption: StateFlow<SongSortOption> = _sortOption.asStateFlow()
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    fun initCollection(title: String, type: String) {
        _title.value = title
        _type.value = type
    }

    fun setSortOption(option: SongSortOption) {
        _sortOption.value = option
        prefs.edit().putString("collection_sort_option", option.name).apply()
    }

    fun toggleAscending() {
        val next = !_isAscending.value
        _isAscending.value = next
        prefs.edit().putBoolean("collection_sort_ascending", next).apply()
    }

    private data class CollectionConfig(
        val title: String,
        val type: String,
        val sort: SongSortOption,
        val asc: Boolean
    )

    val collectionSongs: StateFlow<List<SongEntity>> = combine(
        libraryCacheManager.songs,
        libraryCacheManager.playlists,
        combine(_title, _type, _sortOption, _isAscending) { title, type, sort, asc ->
            CollectionConfig(title, type, sort, asc)
        }
    ) { allSongs, playlists, config ->
        val title = config.title
        val type = config.type
        val sort = config.sort
        val asc = config.asc
        if (title.isBlank() || type.isBlank()) return@combine emptyList()

        val filtered = when (type.lowercase()) {
            "album" -> allSongs.filter {
                val effectiveAlbum = it.album?.trim()?.ifBlank { null } ?: "Unknown Album"
                effectiveAlbum.equals(title, ignoreCase = true)
            }
            "artist" -> allSongs.filter {
                val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                effectiveArtist.equals(title, ignoreCase = true) || (it.artist?.equals(title, ignoreCase = true) == true)
            }
            "genre" -> allSongs.filter {
                val effectiveGenre = it.genre?.trim()?.ifBlank { null } ?: "Unknown"
                effectiveGenre.equals(title, ignoreCase = true)
            }
            "folder" -> allSongs.filter {
                val parentName = java.io.File(it.path).parentFile?.name ?: ""
                parentName.equals(title, ignoreCase = true) || (java.io.File(it.path).parent ?: "").equals(title, ignoreCase = true)
            }
            "playlist" -> {
                val currentPlaylist = playlists.find { it.name.equals(title, ignoreCase = true) }
                if (currentPlaylist != null) {
                    val pathSet = try {
                        val json = org.json.JSONArray(currentPlaylist.songPaths)
                        val set = mutableSetOf<String>()
                        for (i in 0 until json.length()) {
                            set.add(json.optString(i))
                        }
                        set
                    } catch (_: Exception) {
                        emptySet()
                    }
                    allSongs.filter { pathSet.contains(it.path) }
                } else {
                    emptyList()
                }
            }
            else -> allSongs
        }

        val sorted = filtered.toMutableList()
        when (sort) {
            SongSortOption.TITLE -> sorted.sortBy { it.title.lowercase() }
            SongSortOption.ALBUM -> sorted.sortBy { (it.album ?: "").lowercase() }
            SongSortOption.TRACK_NUMBER -> sorted.sortBy { it.trackNumber ?: 0 }
            SongSortOption.DURATION -> sorted.sortByDescending { it.duration }
            SongSortOption.DATE_ADDED -> sorted.sortByDescending { it.dateAdded }
            SongSortOption.YEAR -> sorted.sortByDescending { it.year ?: 0 }
        }
        if (!asc) sorted.reverse()
        sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
