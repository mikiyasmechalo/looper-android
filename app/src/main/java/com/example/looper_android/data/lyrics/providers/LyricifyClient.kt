package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import android.util.Base64
import com.example.looper_android.data.lyrics.formats.LyricifyLyricsParser
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import com.example.looper_android.data.lyrics.formats.TimedLyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Random
import java.util.concurrent.TimeUnit

object LyricifyClient {

    private const val API_URL = "https://api.lyricify.app/lyrics/get/mobile/new"
    private const val CREDITS_RESOLVE_URL = "https://api.credits.fm/v1/resolve/track"
    private val random = Random()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsQuery.cleanTitle(title)
        val artists = LyricsQuery.artistVariants(artist)

        try {
            var isrcs = MusicBrainzIsrc.find(
                title = cleanTitle,
                artists = artists,
                durationSeconds = durationSeconds
            )
            if (isrcs.isEmpty()) {
                val isrc = isrcFromCredits(cleanTitle, artists.lastOrNull() ?: artist)
                if (isrc != null) isrcs = listOf(isrc)
            }

            for (isrc in isrcs.take(3)) {
                val lines = lyricsFor(isrc) ?: continue
                if (!TimedLyricsUtils.fitsDuration(lines, durationSeconds)) continue
                val filtered = TimedLyricsUtils.withoutCreditLines(lines, cleanTitle)
                val lrc = TimedLrcWriter.write(filtered)
                if (lrc != null) return@withContext lrc
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun lyricsFor(isrc: String): List<com.example.looper_android.data.lyrics.formats.TimedLyricLine>? {
        val randomUser = "user${100000 + random.nextInt(900000)}"
        val encodedIsrc = Base64.encodeToString(isrc.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val url = Uri.parse(API_URL).buildUpon()
            .appendQueryParameter("username", randomUser)
            .appendQueryParameter("isrc", encodedIsrc)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val json = try { JSONObject(body) } catch (_: Exception) { return null }
        if (json.optBoolean("isInstrumental", false)) return null

        val text = json.optString("text", "")
        if (text.isEmpty()) return null
        val offset = json.optLong("offset", 0L)

        val lines = LyricifyLyricsParser.parse(text, offset)
        return if (lines.isEmpty()) null else lines
    }

    private fun isrcFromCredits(title: String, artist: String): String? {
        if (artist.equals("unknown artist", ignoreCase = true)) return null

        val payload = JSONObject().apply {
            put("name", title)
            put("artist", artist)
        }.toString()

        val request = Request.Builder()
            .url(CREDITS_RESOLVE_URL)
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val response = try { httpClient.newCall(request).execute() } catch (_: Exception) { return null }
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val json = try { JSONObject(body) } catch (_: Exception) { return null }
        if (json.optString("match_status") != "matched") return null

        val recTitle = json.optString("recording_title", "")
        if (LyricsQuery.key(recTitle) != LyricsQuery.key(title)) return null

        val isrc = json.optString("isrc", "")
        return if (isrc.isNotEmpty()) isrc else null
    }
}
