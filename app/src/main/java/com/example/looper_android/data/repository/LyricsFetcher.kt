package com.example.looper_android.data.repository

import android.util.Log
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.first

@Singleton
class LyricsFetcher @Inject constructor(
    private val libraryDao: LibraryDao,
    private val audioTagReader: AudioTagReader,
    private val settingsRepository: SettingsRepository
) {
    suspend fun fetchLyrics(song: SongEntity): String? = withContext(Dispatchers.IO) {
        // 1. Try DB cache
        val dbCached = libraryDao.getSongLyricsById(song.id) ?: libraryDao.getSongLyricsByPath(song.path)
        if (!dbCached.isNullOrEmpty()) {
            return@withContext dbCached
        }

        var lrc: String? = null
        val primaryProvider = settingsRepository.primaryLyricsProvider.first().lowercase()

        val tryEmbedded = suspend {
            try {
                val embedded = audioTagReader.read(song.path).lyrics
                if (!embedded.isNullOrEmpty()) "[source:embedded]\n$embedded" else null
            } catch (e: Exception) {
                Log.e("LyricsFetcher", "Failed to read embedded lyrics for ${song.path}", e)
                null
            }
        }

        val tryLocal = suspend {
            var localRes: String? = null
            val file = File(song.path)
            val dir = file.parentFile
            if (dir != null && dir.exists()) {
                val baseName = file.nameWithoutExtension
                val searchFiles = mutableListOf(
                    File(dir, "$baseName.lrc"),
                    File(dir, "$baseName.LRC"),
                    File(dir, "$baseName.txt"),
                    File(dir, "$baseName.TXT"),
                    File(dir, "lyrics/$baseName.lrc"),
                    File(dir, "lyrics/$baseName.txt"),
                    File(dir, "Lyrics/$baseName.lrc"),
                    File(dir, "Lyrics/$baseName.txt")
                )
                try {
                    dir.listFiles()?.forEach { f ->
                        if (f.name.equals("$baseName.lrc", ignoreCase = true) ||
                            f.name.equals("$baseName.txt", ignoreCase = true)
                        ) {
                            searchFiles.add(f)
                        }
                    }
                } catch (_: Exception) {}

                for (f in searchFiles) {
                    if (f.exists() && f.isFile) {
                        try {
                            val content = f.readText()
                            if (content.isNotBlank()) {
                                localRes = "[source:local]\n$content"
                                break
                            }
                        } catch (e: Exception) {
                            Log.e("LyricsFetcher", "Error reading local lyric file ${f.absolutePath}", e)
                        }
                    }
                }
            }
            localRes
        }

        val enableInternet = settingsRepository.enableInternet.first()
        val tryNetwork = suspend { if (enableInternet) fetchFromLrcLib(song) else null }

        val providers: List<suspend () -> String?> = listOf(tryEmbedded, tryLocal, tryNetwork)

        for (provider in providers) {
            lrc = provider()
            if (lrc != null) break
        }

        // Save to DB if found
        if (lrc != null) {
            libraryDao.updateSongLyrics(song.id, lrc)
        }

        return@withContext lrc
    }

    private fun sanitize(text: String): String {
        return text
            .replace(Regex("\\((feat|with|ft)\\.?.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(feat|with|ft)\\.?.*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\((Remastered|Live|Official|Video).*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(Remastered|Live|Official|Video).*?\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun fetchFromLrcLib(song: SongEntity): String? {
        val rawTitle = song.title.trim()
        val rawArtist = (song.artist ?: "").trim()
        val cleanTitle = sanitize(rawTitle)
        val cleanArtist = sanitize(rawArtist)
        val durationSecs = ((song.duration ?: 0) / 1000).toInt()

        // 1. Try /api/get with sanitized track/artist
        var result = queryLrcLibGet(cleanTitle, cleanArtist, durationSecs)
        if (result != null) return result

        // 2. Try /api/get with raw title/artist if different
        if (cleanTitle != rawTitle || cleanArtist != rawArtist) {
            result = queryLrcLibGet(rawTitle, rawArtist, durationSecs)
            if (result != null) return result
        }

        // 3. Fallback to /api/search with multiple search queries
        val searchQueries = listOf(
            "$cleanArtist - $cleanTitle",
            "$rawArtist - $rawTitle",
            "$cleanTitle $cleanArtist",
            cleanTitle
        ).distinct()

        for (q in searchQueries) {
            if (q.isBlank()) continue
            result = queryLrcLibSearch(q, durationSecs)
            if (result != null) return result
        }

        return null
    }

    private fun queryLrcLibGet(trackName: String, artistName: String, durationSecs: Int): String? {
        try {
            val titleEnc = URLEncoder.encode(trackName, "UTF-8")
            val artistEnc = URLEncoder.encode(artistName, "UTF-8")
            var urlString = "https://lrclib.net/api/get?track_name=$titleEnc&artist_name=$artistEnc"
            if (durationSecs > 0) {
                urlString += "&duration=$durationSecs"
            }

            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "LooperAndroid/1.0 (https://github.com/justmikiy/looper-android)")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val synced = json.optString("syncedLyrics", "")
                val plain = json.optString("plainLyrics", "")
                val raw = if (synced.isNotEmpty()) synced else plain
                if (raw.isNotEmpty()) {
                    return "[source:lrclib]\n$raw"
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsFetcher", "LRCLIB /get failed for $trackName - $artistName", e)
        }
        return null
    }

    private fun queryLrcLibSearch(query: String, durationSecs: Int): String? {
        try {
            val queryEnc = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://lrclib.net/api/search?q=$queryEnc"
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "LooperAndroid/1.0 (https://github.com/justmikiy/looper-android)")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    var bestMatch: JSONObject? = null

                    // Try finding match with close duration (+/- 10s)
                    if (durationSecs > 0) {
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val itemDur = item.optInt("duration", 0)
                            if (Math.abs(itemDur - durationSecs) < 10) {
                                bestMatch = item
                                break
                            }
                        }
                    }

                    // Fallback to first item with synced lyrics or first item overall
                    if (bestMatch == null) {
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            if (item.optString("syncedLyrics", "").isNotEmpty()) {
                                bestMatch = item
                                break
                            }
                        }
                    }
                    if (bestMatch == null) {
                        bestMatch = jsonArray.getJSONObject(0)
                    }

                    val synced = bestMatch.optString("syncedLyrics", "")
                    val plain = bestMatch.optString("plainLyrics", "")
                    val raw = if (synced.isNotEmpty()) synced else plain
                    if (raw.isNotEmpty()) {
                        return "[source:lrclib]\n$raw"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsFetcher", "LRCLIB /search failed for $query", e)
        }
        return null
    }
}
