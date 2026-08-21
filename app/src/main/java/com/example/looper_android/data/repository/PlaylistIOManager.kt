package com.example.looper_android.data.repository

import android.content.Context
import android.net.Uri
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.PlaylistEntity
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistImportResult(
    val playlistName: String,
    val totalTracks: Int,
    val matchedTracks: Int,
    val unmatchedTracks: Int
)

@Singleton
class PlaylistIOManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val libraryCacheManager: LibraryCacheManager
) {

    suspend fun exportPlaylistToM3u(uri: Uri, playlistName: String, songs: List<SongEntity>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("#EXTM3U\n")
                    writer.write("#PLAYLIST:$playlistName\n\n")

                    for (song in songs) {
                        val durationSeconds = (song.duration ?: 0) / 1000
                        val artist = song.artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
                        val title = song.title.ifBlank { "Unknown Title" }
                        writer.write("#EXTINF:$durationSeconds,$artist - $title\n")
                        writer.write("${song.path}\n")
                    }
                    writer.flush()
                }
            } ?: throw IllegalStateException("Unable to open output stream for export")
            songs.size
        }
    }

    suspend fun exportPlaylistToJson(uri: Uri, playlistName: String, songs: List<SongEntity>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("version", 1)
            root.put("type", "looper_playlist")
            root.put("name", playlistName)
            root.put("createdAt", System.currentTimeMillis())

            val tracksArray = JSONArray()
            for (song in songs) {
                val item = JSONObject().apply {
                    put("title", song.title)
                    put("artist", song.artist ?: "")
                    put("album", song.album ?: "")
                    put("path", song.path)
                    put("duration", song.duration ?: 0)
                }
                tracksArray.put(item)
            }
            root.put("tracks", tracksArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                    writer.flush()
                }
            } ?: throw IllegalStateException("Unable to open output stream for export")
            songs.size
        }
    }

    suspend fun exportAllPlaylistsToJson(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val allLibrarySongs = libraryCacheManager.songs.value.ifEmpty {
                libraryDao.getAllSongsList()
            }
            val pathMap = allLibrarySongs.associateBy { it.path }
            val playlists = libraryDao.getAllPlaylistsList()

            val root = JSONObject()
            root.put("version", 1)
            root.put("type", "looper_all_playlists")
            root.put("createdAt", System.currentTimeMillis())

            val playlistsArray = JSONArray()
            for (p in playlists) {
                val pObj = JSONObject().apply {
                    put("name", p.name)
                    put("dateCreated", p.dateCreated)
                    put("dateModified", p.dateModified)
                }
                val rawPaths = try {
                    val arr = JSONArray(p.songPaths)
                    (0 until arr.length()).map { arr.getString(it) }
                } catch (_: Exception) {
                    p.songPaths.split(",").map { it.trim() }
                }
                val tracksArray = JSONArray()
                for (path in rawPaths) {
                    val s = pathMap[path]
                    val item = JSONObject().apply {
                        put("path", path)
                        put("title", s?.title ?: "")
                        put("artist", s?.artist ?: "")
                        put("album", s?.album ?: "")
                        put("duration", s?.duration ?: 0)
                    }
                    tracksArray.put(item)
                }
                pObj.put("tracks", tracksArray)
                playlistsArray.put(pObj)
            }
            root.put("playlists", playlistsArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                    writer.flush()
                }
            } ?: throw IllegalStateException("Unable to open output stream for export")
            playlists.size
        }
    }

    suspend fun importPlaylist(uri: Uri, customName: String? = null): Result<PlaylistImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val allLibrarySongs = libraryCacheManager.songs.value.ifEmpty {
                libraryDao.getAllSongsList()
            }
            val pathMap = allLibrarySongs.associateBy { it.path }
            val titleArtistMap = allLibrarySongs.groupBy { "${it.title.trim().lowercase()}###${(it.artist ?: "").trim().lowercase()}" }

            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: throw IllegalStateException("Unable to open input stream")

            var playlistName = customName ?: getFileName(uri).removeSuffix(".m3u8").removeSuffix(".m3u").removeSuffix(".json")
            if (playlistName.isBlank()) playlistName = "Imported Playlist"

            val matchedSongPaths = mutableListOf<String>()
            var totalCount = 0

            if (content.trimStart().startsWith("{")) {
                // JSON Playlist
                val root = JSONObject(content)
                if (customName == null && root.has("name")) {
                    val n = root.optString("name")
                    if (n.isNotBlank()) playlistName = n
                }
                val tracks = root.optJSONArray("tracks") ?: JSONArray()
                totalCount = tracks.length()

                for (i in 0 until tracks.length()) {
                    val trackObj = tracks.optJSONObject(i) ?: continue
                    val path = trackObj.optString("path")
                    val title = trackObj.optString("title").trim().lowercase()
                    val artist = trackObj.optString("artist").trim().lowercase()

                    val match = pathMap[path] ?: titleArtistMap["$title###$artist"]?.firstOrNull()
                    if (match != null) {
                        matchedSongPaths.add(match.path)
                    } else if (path.isNotBlank()) {
                        matchedSongPaths.add(path)
                    }
                }
            } else {
                // M3U / M3U8 Playlist
                val lines = content.lines()
                var currentExtInfTitle = ""
                var currentExtInfArtist = ""

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    if (trimmed.startsWith("#PLAYLIST:", ignoreCase = true) && customName == null) {
                        val n = trimmed.substringAfter(":").trim()
                        if (n.isNotBlank()) playlistName = n
                        continue
                    }

                    if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                        val info = trimmed.substringAfter(":")
                        val trackInfo = info.substringAfter(",")
                        if (trackInfo.contains(" - ")) {
                            currentExtInfArtist = trackInfo.substringBefore(" - ").trim().lowercase()
                            currentExtInfTitle = trackInfo.substringAfter(" - ").trim().lowercase()
                        } else {
                            currentExtInfArtist = ""
                            currentExtInfTitle = trackInfo.trim().lowercase()
                        }
                        continue
                    }

                    if (trimmed.startsWith("#")) continue

                    // This is a file path / URL
                    totalCount++
                    val path = trimmed
                    val match = pathMap[path] 
                        ?: (if (currentExtInfTitle.isNotEmpty()) titleArtistMap["$currentExtInfTitle###$currentExtInfArtist"]?.firstOrNull() else null)
                        ?: allLibrarySongs.firstOrNull { it.path.endsWith(path.substringAfterLast("/").substringAfterLast("\\"), ignoreCase = true) }

                    if (match != null) {
                        matchedSongPaths.add(match.path)
                    } else {
                        matchedSongPaths.add(path)
                    }

                    currentExtInfTitle = ""
                    currentExtInfArtist = ""
                }
            }

            if (matchedSongPaths.isEmpty() && totalCount == 0) {
                throw IllegalStateException("No tracks found in playlist file")
            }

            libraryCacheManager.createPlaylist(playlistName, matchedSongPaths)

            PlaylistImportResult(
                playlistName = playlistName,
                totalTracks = totalCount,
                matchedTracks = matchedSongPaths.size,
                unmatchedTracks = (totalCount - matchedSongPaths.size).coerceAtLeast(0)
            )
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "Playlist_${System.currentTimeMillis()}"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex) ?: name
                }
            }
        }
        return name
    }
}
