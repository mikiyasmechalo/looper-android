package com.example.looper_android.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.AppDatabase
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongDiffData
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val db: AppDatabase,
    private val tagReader: AudioTagReader
) {
    private val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fast lookup for MediaStore genres across all audio files on device.
     */
    private fun fetchMediaStoreGenreMap(): Map<Long, String> {
        val genreMap = mutableMapOf<Long, String>()
        try {
            val uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (cursor.moveToNext()) {
                    val genreId = cursor.getLong(idCol)
                    val genreName = cursor.getString(nameCol) ?: continue
                    if (genreName.isNotBlank() && !genreName.equals("<unknown>", ignoreCase = true)) {
                        try {
                            val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                            val membersProj = arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID)
                            context.contentResolver.query(membersUri, membersProj, null, null, null)?.use { mCursor ->
                                val audioIdCol = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                                while (mCursor.moveToNext()) {
                                    val audioId = mCursor.getLong(audioIdCol)
                                    genreMap[audioId] = genreName
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.GENRE)
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val genreCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val genre = cursor.getString(genreCol)
                        if (!genre.isNullOrBlank() && !genre.equals("<unknown>", ignoreCase = true)) {
                            genreMap[id] = genre
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return genreMap
    }

    suspend fun scanMediaStore(
        forceDiskRead: Boolean = false,
        onProgress: ((processed: Int, total: Int, phase: String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val mediaStoreGenreMap = fetchMediaStoreGenreMap()

        val projection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ARTIST
            )
        } else {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED
            )
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        // OPTIMIZATION: Load all existing songs into memory as a map (1 query instead of N queries)
        val existingByPath: Map<String, SongDiffData> = libraryDao.getAllSongsForDiff()
            .associateBy { it.path }

        val songsToInsert = mutableListOf<SongEntity>()
        val songsToUpdate = mutableListOf<SongEntity>()
        val albumsToInsert = mutableListOf<AlbumEntity>()
        val artistsToInsert = mutableListOf<ArtistEntity>()
        val processedAlbums = mutableSetOf<String>()
        val processedArtists = mutableSetOf<String>()
        val songsToEnrich = mutableListOf<SongEntity>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumArtistColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
            } else {
                -1
            }

            val totalSongs = cursor.count
            var scannedCount = 0

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idColumn)
                val path = cursor.getString(dataColumn) ?: continue
                val existing = existingByPath[path]

                val mediaStoreTitle = cursor.getString(titleColumn) ?: "Unknown"
                val mediaStoreArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val mediaStoreAlbum = cursor.getString(albumColumn) ?: "Unknown Album"
                val mediaStoreDuration = cursor.getInt(durationColumn)
                val mediaStoreTrack = cursor.getInt(trackColumn)
                val mediaStoreYear = cursor.getInt(yearColumn)
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
                val albumId = cursor.getLong(albumIdColumn)
                val mediaStoreAlbumArtist = if (albumArtistColumn >= 0) {
                    cursor.getString(albumArtistColumn)?.trim()?.ifBlank { null }
                } else null

                val discoveredGenre = mediaStoreGenreMap[mediaId] ?: existing?.genre

                val albumArtUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    albumId
                ).toString()

                val songTitle = if (mediaStoreTitle.isNotBlank() && !mediaStoreTitle.equals("<unknown>", ignoreCase = true)) mediaStoreTitle else "Unknown"
                val songArtist = if (mediaStoreArtist.isNotBlank() && !mediaStoreArtist.equals("<unknown>", ignoreCase = true)) mediaStoreArtist else "Unknown Artist"
                val songAlbum = if (mediaStoreAlbum.isNotBlank() && !mediaStoreAlbum.equals("<unknown>", ignoreCase = true)) mediaStoreAlbum else "Unknown Album"

                val effectiveAlbumArtist = existing?.albumArtist?.trim()?.ifBlank { null } ?: mediaStoreAlbumArtist
                val effectiveArtist = effectiveAlbumArtist ?: songArtist

                val song = SongEntity(
                    id = existing?.id ?: 0,
                    path = path,
                    title = songTitle,
                    artist = songArtist,
                    albumArtist = effectiveAlbumArtist,
                    album = songAlbum,
                    genre = discoveredGenre,
                    duration = if (mediaStoreDuration > 0) mediaStoreDuration else 0,
                    trackNumber = if (mediaStoreTrack > 0) mediaStoreTrack else null,
                    year = if (mediaStoreYear > 0) mediaStoreYear else existing?.year,
                    lyrics = existing?.lyrics,
                    artPath = existing?.artPath ?: albumArtUri,
                    dateAdded = dateAdded,
                    playCount = existing?.playCount ?: 0,
                    lastPlayed = existing?.lastPlayed,
                    isFavorite = existing?.isFavorite ?: false,
                    hasCustomEqualizer = existing?.hasCustomEqualizer ?: false,
                    equalizerGains = existing?.equalizerGains
                )

                if (existing == null) {
                    songsToInsert.add(song)
                } else {
                    songsToUpdate.add(song)
                }

                songsToEnrich.add(song)
                scannedCount++
                onProgress?.invoke(scannedCount, totalSongs, "Discovering songs...")

                if (songAlbum !in processedAlbums) {
                    processedAlbums.add(songAlbum)
                    albumsToInsert.add(
                        AlbumEntity(
                            name = songAlbum,
                            artist = effectiveArtist,
                            artPath = albumArtUri,
                            year = song.year,
                            dateAdded = dateAdded
                        )
                    )
                }

                if (effectiveArtist !in processedArtists) {
                    processedArtists.add(effectiveArtist)
                    artistsToInsert.add(
                        ArtistEntity(
                            name = effectiveArtist,
                            artPath = null,
                            artistImageUrl = null
                        )
                    )
                }
            }
        }

        // OPTIMIZATION: Single bulk transaction for all DB writes
        db.withTransaction {
            if (songsToInsert.isNotEmpty()) libraryDao.insertSongs(songsToInsert)
            if (songsToUpdate.isNotEmpty()) libraryDao.updateSongs(songsToUpdate)
            if (albumsToInsert.isNotEmpty()) libraryDao.insertAlbums(albumsToInsert)
            if (artistsToInsert.isNotEmpty()) libraryDao.insertArtists(artistsToInsert)
        }

        // Asynchronously enrich ID3 metadata in background scope (non-blocking batch update)
        if (songsToEnrich.isNotEmpty()) {
            externalScope.launch {
                enrichMetadataInBackground(songsToEnrich, forceDiskRead)
            }
        }
    }


    private suspend fun enrichMetadataInBackground(songs: List<SongEntity>, forceDiskRead: Boolean = false) = withContext(Dispatchers.IO) {
        val updatedBatch = mutableListOf<SongEntity>()

        for (song in songs) {
            try {
                if (forceDiskRead || song.albumArtist == null || song.genre == null || song.lyrics == null || song.year == null) {
                    val embedded = tagReader.read(song.path)
                    val rawAlbumArtist = embedded.albumArtist?.trim()?.ifBlank { null }
                    val updatedAlbumArtist = rawAlbumArtist ?: song.albumArtist ?: song.artist
                    val updated = song.copy(
                        title = embedded.title?.trim()?.ifBlank { null } ?: song.title,
                        artist = embedded.artist?.trim()?.ifBlank { null } ?: song.artist,
                        album = embedded.album?.trim()?.ifBlank { null } ?: song.album,
                        albumArtist = rawAlbumArtist ?: song.albumArtist,
                        genre = embedded.genre?.trim()?.ifBlank { null } ?: song.genre,
                        lyrics = embedded.lyrics?.trim()?.ifBlank { null } ?: song.lyrics,
                        year = embedded.year ?: song.year
                    )
                    if (updated != song) {
                        updatedBatch.add(updated)
                        if (updatedAlbumArtist != null) {
                            libraryDao.insertArtist(
                                ArtistEntity(
                                    name = updatedAlbumArtist,
                                    artPath = null,
                                    artistImageUrl = null
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        if (updatedBatch.isNotEmpty()) {
            db.withTransaction {
                libraryDao.updateSongs(updatedBatch)
            }
        }
    }

    suspend fun resetLibrary() = withContext(Dispatchers.IO) {
        val savedArtistsWithImages = libraryDao.getArtistsWithImages()
        db.withTransaction {
            libraryDao.clearSongs()
            libraryDao.clearAlbums()
            libraryDao.clearArtists()
            if (savedArtistsWithImages.isNotEmpty()) {
                libraryDao.insertArtists(savedArtistsWithImages)
            }
        }
    }
}