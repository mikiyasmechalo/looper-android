package com.example.looper_android.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.AppDatabase
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.LibraryDao
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

    suspend fun scanMediaStore(
        forceDiskRead: Boolean = false,
        onProgress: ((processed: Int, total: Int, phase: String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
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

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val songsToEnrich = mutableListOf<SongEntity>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            val totalSongs = cursor.count
            var scannedCount = 0

            db.withTransaction {
                val processedAlbums = mutableSetOf<String>()
                val processedArtists = mutableSetOf<String>()

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn) ?: continue
                    val existing = libraryDao.getSongByPath(path)

                    val mediaStoreTitle = cursor.getString(titleColumn) ?: "Unknown"
                    val mediaStoreArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val mediaStoreAlbum = cursor.getString(albumColumn) ?: "Unknown Album"
                    val mediaStoreDuration = cursor.getInt(durationColumn)
                    val mediaStoreTrack = cursor.getInt(trackColumn)
                    val mediaStoreYear = cursor.getInt(yearColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
                    val albumId = cursor.getLong(albumIdColumn)

                    val albumArtUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumId
                    ).toString()

                    val songTitle = if (mediaStoreTitle.isNotBlank() && !mediaStoreTitle.equals("<unknown>", ignoreCase = true)) mediaStoreTitle else (existing?.title ?: mediaStoreTitle)
                    val songArtist = if (mediaStoreArtist.isNotBlank() && !mediaStoreArtist.equals("<unknown>", ignoreCase = true)) mediaStoreArtist else (existing?.artist ?: mediaStoreArtist)
                    val songAlbum = if (mediaStoreAlbum.isNotBlank() && !mediaStoreAlbum.equals("<unknown>", ignoreCase = true)) mediaStoreAlbum else (existing?.album ?: mediaStoreAlbum)

                    val effectiveArtist = existing?.albumArtist?.trim()?.ifBlank { null } ?: songArtist

                    val song = SongEntity(
                        id = existing?.id ?: 0,
                        path = path,
                        title = songTitle,
                        artist = songArtist,
                        albumArtist = effectiveArtist,
                        album = songAlbum,
                        genre = existing?.genre,
                        duration = if (mediaStoreDuration > 0) mediaStoreDuration else (existing?.duration ?: 0),
                        trackNumber = if (mediaStoreTrack > 0) mediaStoreTrack else existing?.trackNumber,
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
                        libraryDao.insertSong(song)
                    } else {
                        libraryDao.updateSong(song)
                    }

                    songsToEnrich.add(song)
                    scannedCount++
                    onProgress?.invoke(scannedCount, totalSongs, "Discovering songs...")

                    if (songAlbum !in processedAlbums) {
                        processedAlbums.add(songAlbum)
                        libraryDao.insertAlbum(
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
                        libraryDao.insertArtist(
                            ArtistEntity(
                                name = effectiveArtist,
                                artPath = null,
                                artistImageUrl = null
                            )
                        )
                    }
                }
            }
        }

        // Asynchronously enrich ID3 metadata in background scope (non-blocking)
        if (songsToEnrich.isNotEmpty()) {
            externalScope.launch {
                enrichMetadataInBackground(songsToEnrich, forceDiskRead)
            }
        }
    }

    private suspend fun enrichMetadataInBackground(songs: List<SongEntity>, forceDiskRead: Boolean = false) = withContext(Dispatchers.IO) {
        for (song in songs) {
            try {
                if (forceDiskRead || song.genre == null || song.lyrics == null || song.year == null || song.albumArtist == null) {
                    val embedded = tagReader.read(song.path)
                    val updatedAlbumArtist = embedded.albumArtist?.trim()?.ifBlank { null } ?: song.albumArtist ?: song.artist
                    val updated = song.copy(
                        title = embedded.title ?: song.title,
                        artist = embedded.artist ?: song.artist,
                        album = embedded.album ?: song.album,
                        albumArtist = updatedAlbumArtist,
                        genre = embedded.genre ?: song.genre,
                        lyrics = embedded.lyrics ?: song.lyrics,
                        year = embedded.year ?: song.year
                    )
                    if (updated != song) {
                        libraryDao.updateSong(updated)
                        // Also insert artist entity for new albumArtist if needed
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
    }
}