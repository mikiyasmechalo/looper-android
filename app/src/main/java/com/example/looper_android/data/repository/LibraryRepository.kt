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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isDbLoaded = MutableStateFlow(false)
    val isDbLoaded: StateFlow<Boolean> = _isDbLoaded.asStateFlow()

    val allSongs: StateFlow<List<SongEntity>> = libraryDao.getAllSongs()
        .onEach { _isDbLoaded.value = true }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val allAlbums: StateFlow<List<AlbumEntity>> = libraryDao.getAllAlbums()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val allArtists: StateFlow<List<ArtistEntity>> = libraryDao.getAllArtists()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = libraryDao.getAllPlaylists()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = libraryDao.getFavoriteSongs()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
}
