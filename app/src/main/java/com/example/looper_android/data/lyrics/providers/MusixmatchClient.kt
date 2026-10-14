package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import com.example.looper_android.data.lyrics.formats.RichsyncLyricsParser
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object MusixmatchClient {

    private const val API_BASE = "https://apic.musixmatch.com/ws/1.1"
    private const val APP_ID = "android-player-v1.0"
    private const val MIN_GAP_MS = 500L
    private const val BACKOFF_MS = 10 * 60 * 1000L

    private var token: String? = null
    private var pausedUntil: Long = 0L
    private var lastRequestTime: Long = 0L
    private val mutex = Mutex()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() < pausedUntil) return@withContext null
        val cleanTitle = LyricsQuery.cleanTitle(title)

        try {
            if (token == null) {
                val tokenResp = callApi("token.get", mapOf("user_language" to "en"))
                token = tokenResp?.optString("user_token")
            }
            val curToken = token ?: return@withContext null

            val artistVariant = LyricsQuery.artistVariants(artist).lastOrNull() ?: artist
            val searchParams = mutableMapOf(
                "page_size" to "10",
                "page" to "1",
                "s_track_rating" to "desc",
                "q_track" to cleanTitle,
                "q_artist" to artistVariant,
                "usertoken" to curToken
            )
            if (durationSeconds > 0) {
                searchParams["q_duration"] = durationSeconds.toString()
            }

            val found = callApi("track.search", searchParams) ?: return@withContext null
            val trackList = found.optJSONArray("track_list") ?: return@withContext null

            for (i in 0 until trackList.length()) {
                val entry = trackList.optJSONObject(i) ?: continue
                val track = entry.optJSONObject("track") ?: continue
                val trackLen = track.optDouble("track_length", 0.0)

                val same = LyricsQuery.sameSong(
                    wantTitle = cleanTitle,
                    wantArtist = artist,
                    title = track.optString("track_name", ""),
                    artists = listOf(track.optString("artist_name", ""))
                ) && LyricsQuery.sameLength(if (trackLen > 0) trackLen else null, durationSeconds)

                val trackId = track.opt("track_id")?.toString()
                if (!same || trackId.isNullOrEmpty()) continue

                return@withContext lyricsFor(trackId, curToken)
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private suspend fun lyricsFor(trackId: String, token: String): String? {
        val body = callApi("macro.subtitles.get", mapOf(
            "namespace" to "lyrics_richsynched",
            "optional_calls" to "track.richsync",
            "subtitle_format" to "lrc",
            "track_id" to trackId,
            "f_subtitle_length_max_deviation" to "40",
            "usertoken" to token
        )) ?: return null

        val calls = body.optJSONObject("macro_calls") ?: return null

        fun answer(callName: String): JSONObject? {
            val call = calls.optJSONObject(callName) ?: return null
            val message = call.optJSONObject("message") ?: return null
            return message.optJSONObject("body")
        }

        val richsyncObj = answer("track.richsync.get")?.optJSONObject("richsync")
        val richsyncBody = richsyncObj?.optString("richsync_body", "")
        if (!richsyncBody.isNullOrEmpty()) {
            val lines = RichsyncLyricsParser.parse(richsyncBody)
            val lrc = lines?.let { TimedLrcWriter.write(it) }
            if (lrc != null) return lrc
        }

        val subtitleList = answer("track.subtitles.get")?.optJSONArray("subtitle_list")
        if (subtitleList != null && subtitleList.length() > 0) {
            val subtitle = subtitleList.optJSONObject(0)?.optJSONObject("subtitle")
            val lrc = subtitle?.optString("subtitle_body", "")?.trim()
            if (!lrc.isNullOrEmpty()) return lrc
        }

        val lyrics = answer("track.lyrics.get")?.optJSONObject("lyrics")
        val plain = lyrics?.optString("lyrics_body", "")?.trim()
        if (!plain.isNullOrEmpty()) return plain

        return null
    }

    private suspend fun callApi(method: String, params: Map<String, String>): JSONObject? {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < MIN_GAP_MS) {
                delay(MIN_GAP_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            val builder = Uri.parse("$API_BASE/$method").buildUpon()
            for ((k, v) in params) builder.appendQueryParameter(k, v)
            builder.appendQueryParameter("format", "json")
            builder.appendQueryParameter("app_id", APP_ID)
            builder.appendQueryParameter("t", System.currentTimeMillis().toString())

            val request = Request.Builder()
                .url(builder.build().toString())
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 14)")
                .header("Cookie", "AWSELB=0; AWSELBCORS=0")
                .header("Accept", "application/json")
                .build()

            val response = try { httpClient.newCall(request).execute() } catch (_: Exception) { return null }
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val root = try { JSONObject(body) } catch (_: Exception) { return null }
            val message = root.optJSONObject("message") ?: return null
            val header = message.optJSONObject("header")
            val status = header?.optInt("status_code", 0)

            if (status == 401) {
                token = null
                pausedUntil = System.currentTimeMillis() + BACKOFF_MS
                return null
            }
            if (status != 200) return null

            message.optJSONObject("body")
        }
    }
}
