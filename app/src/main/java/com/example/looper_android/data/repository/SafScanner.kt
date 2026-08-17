package com.example.looper_android.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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

import kotlinx.coroutines.flow.first

@Singleton
class SafScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val db: AppDatabase,
    private val tagReader: AudioTagReader,
    private val settingsRepository: SettingsRepository
) {
    private val externalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supportedExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")

    suspend fun scanDirectory(
        folderUri: Uri,
        onProgress: ((processed: Int, total: Int, phase: String) -> Unit)? = null
    ) = scanFolder(folderUri, onProgress)

    suspend fun scanFolder(
        folderUri: Uri,
        onProgress: ((processed: Int, total: Int, phase: String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val rootDir = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext
        val processedAlbums = mutableSetOf<String>()
        val processedArtists = mutableSetOf<String>()
        val songsToEnrich = mutableListOf<SongEntity>()
        val excludedFolders = try { settingsRepository.excludedFolders.first() } catch (_: Exception) { emptySet() }

        onProgress?.invoke(0, 0, "Scanning SAF folder...")

        db.withTransaction {
            suspend fun traverse(directory: DocumentFile) {
                for (file in directory.listFiles()) {
                    if (file.isDirectory) {
                        val dirPath = file.uri.toString()
                        if (excludedFolders.isEmpty() || !excludedFolders.any { dirPath.startsWith(it, ignoreCase = true) }) {
                            traverse(file)
                        }
                    } else if (file.isFile) {
                        val ext = file.name?.substringAfterLast('.', "")?.lowercase()
                        if (ext != null && supportedExtensions.contains(ext)) {
                            val path = file.uri.toString()
                            if (excludedFolders.isNotEmpty() && excludedFolders.any { path.startsWith(it, ignoreCase = true) }) {
                                continue
                            }
                            val existing = libraryDao.getSongByPath(path)
                            val dateAdded = System.currentTimeMillis()
                            val fileNameWithoutExt = file.name?.substringBeforeLast('.') ?: "Unknown"

                            val songTitle = existing?.title ?: fileNameWithoutExt
                            val songArtist = existing?.artist ?: "Unknown Artist"
                            val songAlbum = existing?.album ?: "Unknown Album"

                            val song = SongEntity(
                                id = existing?.id ?: 0,
                                path = path,
                                title = songTitle,
                                artist = songArtist,
                                albumArtist = existing?.albumArtist ?: songArtist,
                                album = songAlbum,
                                genre = existing?.genre,
                                duration = existing?.duration ?: 0,
                                trackNumber = existing?.trackNumber,
                                year = existing?.year,
                                lyrics = existing?.lyrics,
                                artPath = null,
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

                            if (songAlbum !in processedAlbums) {
                                processedAlbums.add(songAlbum)
                                libraryDao.insertAlbum(
                                    AlbumEntity(
                                        name = songAlbum,
                                        artist = songArtist,
                                        artPath = null,
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

                            onProgress?.invoke(songsToEnrich.size, songsToEnrich.size, "Scanning SAF folder...")
                        }
                    }
                }
            }

            traverse(rootDir)
        }

        // Asynchronously enrich metadata in background scope (non-blocking for folder scan)
        if (songsToEnrich.isNotEmpty()) {
            externalScope.launch {
                enrichMetadataInBackground(songsToEnrich)
            }
        }
    }

    private suspend fun enrichMetadataInBackground(songs: List<SongEntity>) = withContext(Dispatchers.IO) {
        for (song in songs) {
            try {
                if (song.genre == null || song.lyrics == null || song.year == null) {
                    val uri = Uri.parse(song.path)
                    val embedded = tagReader.read(uri)
                    val updated = song.copy(
                        title = embedded.title ?: song.title,
                        artist = embedded.artist ?: song.artist,
                        album = embedded.album ?: song.album,
                        albumArtist = embedded.albumArtist ?: song.albumArtist,
                        duration = if (song.duration == 0) embedded.durationMs else song.duration,
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

    companion object {
        fun getDisplayNameAndPath(context: Context, uriString: String): Pair<String, String> {
            return try {
                val uri = Uri.parse(uriString)
                val docFile = DocumentFile.fromTreeUri(context, uri)
                val docName = docFile?.name
                val decoded = Uri.decode(uriString)
                val treeIndex = decoded.indexOf("/tree/")
                if (treeIndex != -1) {
                    val rawPart = decoded.substring(treeIndex + 6)
                    val colonIndex = rawPart.indexOf(':')
                    if (colonIndex != -1) {
                        val storageId = rawPart.substring(0, colonIndex)
                        val relativePath = rawPart.substring(colonIndex + 1).trimStart('/')
                        val fullPath = if (storageId.equals("primary", ignoreCase = true)) {
                            "/storage/emulated/0" + if (relativePath.isNotEmpty()) "/$relativePath" else ""
                        } else {
                            "/storage/$storageId" + if (relativePath.isNotEmpty()) "/$relativePath" else ""
                        }
                        val folderName = docName ?: (relativePath.split('/').lastOrNull()?.ifEmpty { null } ?: fullPath.split('/').last())
                        return Pair(folderName, fullPath)
                    }
                }
                val fallbackName = docName ?: uri.lastPathSegment ?: "Music Folder"
                Pair(fallbackName, decoded)
            } catch (e: Exception) {
                Pair("Music Folder", uriString)
            }
        }
    }
}