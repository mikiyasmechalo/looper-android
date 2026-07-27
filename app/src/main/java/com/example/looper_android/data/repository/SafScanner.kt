package com.example.looper_android.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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
class SafScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao
) {
    suspend fun scanDirectory(treeUri: Uri) = withContext(Dispatchers.IO) {
        // Take persistable permission so we can read this folder across reboots
        try {
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
        val songs = mutableListOf<SongEntity>()
        val albumsMap = mutableMapOf<String, AlbumEntity>()
        val artistsMap = mutableMapOf<String, ArtistEntity>()

        val supportedExtensions = listOf("mp3", "flac", "opus", "aac", "m4a", "wav", "ogg")

        suspend fun traverse(directory: DocumentFile) {
            val files = directory.listFiles()
            for (file in files) {
                if (file.isDirectory) {
                    traverse(file)
                } else if (file.isFile) {
                    val ext = file.name?.substringAfterLast('.', "")?.lowercase()
                    if (ext != null && supportedExtensions.contains(ext)) {
                        extractAndAddSong(file.uri, songs, albumsMap, artistsMap)
                    }
                }
            }
        }

        traverse(rootDir)

        if (songs.isNotEmpty()) {
            libraryDao.insertSongs(songs)
            libraryDao.insertAlbums(albumsMap.values.toList())
            libraryDao.insertArtists(artistsMap.values.toList())
        }
    }

    private fun extractAndAddSong(
        uri: Uri,
        songs: MutableList<SongEntity>,
        albumsMap: MutableMap<String, AlbumEntity>,
        artistsMap: MutableMap<String, ArtistEntity>
    ) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown"
            val artistName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toIntOrNull() ?: 0
            val trackStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackStr?.substringBefore("/")?.toIntOrNull()
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = yearStr?.toIntOrNull()
            
            val dateAdded = System.currentTimeMillis()

            songs.add(
                SongEntity(
                    path = uri.toString(),
                    title = title,
                    artist = artistName,
                    albumArtist = artistName,
                    album = albumName,
                    genre = null,
                    duration = duration,
                    trackNumber = trackNumber,
                    year = year,
                    artPath = null, // Will need a separate content provider or caching mechanism for SAF art
                    dateAdded = dateAdded
                )
            )

            if (!albumsMap.containsKey(albumName)) {
                albumsMap[albumName] = AlbumEntity(
                    name = albumName,
                    artist = artistName,
                    artPath = null,
                    year = year,
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }
}
