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
import kotlinx.coroutines.Dispatchers
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

    suspend fun scanMediaStore() = withContext(Dispatchers.IO) {
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
                        android.net.Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val songTitle = existing?.title ?: mediaStoreTitle
                    val songArtist = existing?.artist ?: mediaStoreArtist
                    val songAlbum = existing?.album ?: mediaStoreAlbum

                    val song = SongEntity(
                        id = existing?.id ?: 0,
                        path = path,
                        title = songTitle,
                        artist = songArtist,
                        albumArtist = existing?.albumArtist ?: songArtist,
                        album = songAlbum,
                        genre = existing?.genre,
                        duration = mediaStoreDuration,
                        trackNumber = existing?.trackNumber ?: mediaStoreTrack,
                        year = existing?.year ?: if (mediaStoreYear > 0) mediaStoreYear else null,
                        artPath = albumArtUri,
                        dateAdded = dateAdded,
                        playCount = existing?.playCount ?: 0,
                        lastPlayed = existing?.lastPlayed,
                        isFavorite = existing?.isFavorite ?: false,
                        lyrics = existing?.lyrics,
                        hasCustomEqualizer = existing?.hasCustomEqualizer ?: false,
                        equalizerGains = existing?.equalizerGains
                    )

                    if (existing == null) {
                        libraryDao.insertSong(song)
                    } else {
                        libraryDao.updateSong(song)
                    }

                    songsToEnrich.add(song)

                    if (songAlbum !in processedAlbums) {
                        processedAlbums.add(songAlbum)
                        libraryDao.insertAlbum(
                            AlbumEntity(
                                name = songAlbum,
                                artist = songArtist,
                                artPath = albumArtUri,
                                year = song.year,
                                dateAdded = dateAdded
                            )
                        )
                    }

                    if (songArtist !in processedArtists) {
                        processedArtists.add(songArtist)
                        libraryDao.insertArtist(
                            ArtistEntity(
                                name = songArtist,
                                artPath = null,
                                artistImageUrl = null
                            )
                        )
                    }
                }
            }
        }

        // Asynchronously enrich Jaudiotagger metadata (genre, lyrics, full tags) in background
        if (songsToEnrich.isNotEmpty()) {
            enrichMetadataInBackground(songsToEnrich)
        }
    }

    private suspend fun enrichMetadataInBackground(songs: List<SongEntity>) = withContext(Dispatchers.IO) {
        for (song in songs) {
            try {
                if (song.genre == null || song.lyrics == null || song.year == null) {
                    val embedded = tagReader.read(song.path)
                    val updated = song.copy(
                        title = embedded.title ?: song.title,
                        artist = embedded.artist ?: song.artist,
                        album = embedded.album ?: song.album,
                        albumArtist = embedded.albumArtist ?: song.albumArtist,
                        genre = song.genre ?: embedded.genre,
                        lyrics = song.lyrics ?: embedded.lyrics,
                        year = song.year ?: embedded.year
                    )
                    if (updated != song) {
                        libraryDao.updateSong(updated)
                    }
                }
            } catch (_: Exception) { }
        }
    }
}