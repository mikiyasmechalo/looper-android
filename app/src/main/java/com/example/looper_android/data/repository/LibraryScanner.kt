package com.example.looper_android.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.looper_android.data.db.AlbumEntity
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
    private val libraryDao: LibraryDao
) {

    suspend fun scanMediaStore() = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()
        val albumsMap = mutableMapOf<String, AlbumEntity>()
        val artistsMap = mutableMapOf<String, ArtistEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA, // Path
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

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: continue
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artistName = cursor.getString(artistColumn) ?: "Unknown Artist"
                val albumName = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getInt(durationColumn)
                val trackNumber = cursor.getInt(trackColumn)
                val year = cursor.getInt(yearColumn)
                val dateAdded = cursor.getLong(dateAddedColumn) * 1000L // convert to ms
                val albumId = cursor.getLong(albumIdColumn)

                val albumArtUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                songs.add(
                    SongEntity(
                        path = path,
                        title = title,
                        artist = artistName,
                        albumArtist = artistName,
                        album = albumName,
                        genre = null,
                        duration = duration,
                        trackNumber = trackNumber,
                        year = if (year > 0) year else null,
                        artPath = albumArtUri,
                        dateAdded = dateAdded
                    )
                )

                if (!albumsMap.containsKey(albumName)) {
                    albumsMap[albumName] = AlbumEntity(
                        name = albumName,
                        artist = artistName,
                        artPath = albumArtUri,
                        year = if (year > 0) year else null,
                        dateAdded = dateAdded
                    )
                }

                if (!artistsMap.containsKey(artistName)) {
                    artistsMap[artistName] = ArtistEntity(
                        name = artistName,
                        artPath = null,
                        artistImageUrl = null
                    )
                }
            }
        }

        // Batch insert into DB using REPLACE on conflict (no empty DB flash)
        if (songs.isNotEmpty()) {
            libraryDao.insertSongs(songs)
            libraryDao.insertAlbums(albumsMap.values.toList())
            libraryDao.insertArtists(artistsMap.values.toList())
        }
    }
}
