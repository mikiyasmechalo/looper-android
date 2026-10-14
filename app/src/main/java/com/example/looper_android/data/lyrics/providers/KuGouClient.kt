package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import android.util.Base64
import com.example.looper_android.data.lyrics.formats.KrcLyricsParser
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import com.example.looper_android.data.lyrics.formats.TimedLyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object KuGouClient {

    private const val SEARCH_URL = "https://mobiles.kugou.com/api/v3/search/song"
    private const val LYRICS_BASE = "https://lyrics.kugou.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsQuery.cleanTitle(title)
        try {
            val name = LyricsQuery.artistVariants(artist).lastOrNull() ?: artist
            val found = getJson(SEARCH_URL, mapOf(
                "version" to "10000",
                "plat" to "0",
                "correct" to "1",
                "pagesize" to "10",
                "keyword" to "$name $cleanTitle"
            )) ?: return@withContext null

            val data = found.optJSONObject("data") ?: return@withContext null
            val songs = data.optJSONArray("info") ?: return@withContext null

            var asked = 0
            for (i in 0 until songs.length()) {
                val song = songs.optJSONObject(i) ?: continue
                val songName = song.optString("songname", "")
                val singerName = song.optString("singername", "")
                val durSec = song.optDouble("duration", 0.0)

                val same = LyricsQuery.sameSong(
                    wantTitle = cleanTitle,
                    wantArtist = artist,
                    title = songName,
                    artists = listOf(singerName)
                ) && LyricsQuery.sameLength(if (durSec > 0) durSec else null, durationSeconds)

                val hash = song.optString("hash", "")
                if (!same || hash.isEmpty()) continue
                if (asked++ >= 2) break

                val lrc = lyricsFor(hash, cleanTitle, durationSeconds)
                if (lrc != null) return@withContext lrc
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun lyricsFor(hash: String, title: String, durationSeconds: Int): String? {
        val found = getJson("$LYRICS_BASE/search", mapOf(
            "ver" to "1",
            "man" to "yes",
            "client" to "mobi",
            "hash" to hash
        )) ?: return null

        val candidates = found.optJSONArray("candidates") ?: return null
        for (i in 0 until candidates.length().coerceAtMost(2)) {
            val candidate = candidates.optJSONObject(i) ?: continue
            val durMs = candidate.optDouble("duration", 0.0)
            if (!LyricsQuery.sameLength(if (durMs > 0) durMs / 1000.0 else null, durationSeconds)) {
                continue
            }

            val id = candidate.opt("id")?.toString() ?: continue
            val accesskey = candidate.optString("accesskey", "")

            val file = getJson("$LYRICS_BASE/download", mapOf(
                "ver" to "1",
                "client" to "pc",
                "fmt" to "krc",
                "charset" to "utf8",
                "id" to id,
                "accesskey" to accesskey
            )) ?: continue

            val content = file.optString("content", "")
            if (content.isEmpty()) continue

            val fileBytes = try { Base64.decode(content, Base64.DEFAULT) } catch (_: Exception) { continue }
            val krc = KrcLyricsParser.decodeKrc(fileBytes) ?: continue
            val lines = TimedLyricsUtils.withoutCreditLines(KrcLyricsParser.parseKrc(krc), title)
            val lrc = TimedLrcWriter.write(lines)
            if (lrc != null) return lrc
        }
        return null
    }

    private fun getJson(url: String, params: Map<String, String>): JSONObject? {
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        val fullUrl = if (url.contains("?")) "$url&$query" else "$url?$query"

        val request = Request.Builder()
            .url(fullUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            JSONObject(body)
        } catch (_: Exception) {
            null
        }
    }
}
