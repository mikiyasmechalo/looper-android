package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import com.example.looper_android.data.lyrics.formats.TtmlLyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object LrcRedClient {

    private const val API_BASE = "https://lrc.red"
    private const val MAX_DURATION_DRIFT_SEC = 4
    private const val TTML_TIMEOUT_SEC = 4L

    private var searchDownUntil: Long = 0L
    private val isSearchUp: Boolean
        get() = System.currentTimeMillis() > searchDownUntil

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TTML_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(TTML_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(
        title: String,
        artist: String,
        durationSeconds: Int = 0,
        isrc: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsQuery.cleanTitle(title)
        val artists = LyricsQuery.artistVariants(artist)
        val tried = mutableSetOf<String>()

        suspend fun fromIsrcs(isrcs: Iterable<String>): String? {
            val batch = isrcs.filter { tried.add(it) }.take(3)
            val deferreds = batch.map { candidate ->
                async { fetchTtml(candidate) }
            }
            val results = deferreds.awaitAll()
            return results.firstOrNull { it != null }
        }

        if (!isrc.isNullOrBlank()) {
            val direct = fromIsrcs(listOf(isrc.trim()))
            if (direct != null) return@withContext direct
        }

        if (isSearchUp) {
            try {
                for (name in artists.take(2)) {
                    val params = mutableMapOf(
                        "track" to cleanTitle,
                        "artist" to name
                    )
                    if (durationSeconds > 0) {
                        params["duration"] = durationSeconds.toString()
                    }
                    val hits = queryHits("/match.json", params)
                    val ranked = rankHits(byArtist(hits, artists), durationSeconds)
                    val lrc = fromIsrcs(ranked)
                    if (lrc != null) return@withContext lrc
                }
            } catch (_: Exception) {
                searchDownUntil = System.currentTimeMillis() + 10 * 60 * 1000L
            }
        }

        val mbIsrcs = MusicBrainzIsrc.find(
            title = cleanTitle,
            artists = artists,
            durationSeconds = durationSeconds
        )
        val viaMb = fromIsrcs(mbIsrcs)
        if (viaMb != null) return@withContext viaMb

        if (isSearchUp) {
            try {
                val queryArtist = if (artists.size > 1) artists[1] else artist
                val hits = queryHits("/search.json", mapOf("q" to "$queryArtist $cleanTitle"))
                val ranked = rankHits(byArtist(hits, artists), durationSeconds)
                val lrc = fromIsrcs(ranked)
                if (lrc != null) return@withContext lrc
            } catch (_: Exception) {
                searchDownUntil = System.currentTimeMillis() + 10 * 60 * 1000L
            }
        }

        return@withContext null
    }

    private fun byArtist(hits: List<JSONObject>, artists: List<String>): List<JSONObject> {
        val wanted = artists.map { LyricsQuery.key(it) }.filter { it.isNotEmpty() }
        return hits.filter { hit ->
            val credit = hit.optString("artist", hit.optString("artistName", ""))
            if (credit.isEmpty()) true
            else {
                val key = LyricsQuery.key(credit)
                wanted.any { key.contains(it) || it.contains(key) }
            }
        }
    }

    private fun rankHits(hits: List<JSONObject>, durationSeconds: Int): List<String> {
        val candidates = hits.mapNotNull {
            val isrc = it.optString("isrc", "")
            if (isrc.isNotEmpty()) it else null
        }
        if (durationSeconds <= 0) {
            return candidates.map { it.optString("isrc") }
        }

        val scored = candidates.mapNotNull { hit ->
            val d = hit.optDouble("duration", -1.0)
            val drift = if (d < 0) MAX_DURATION_DRIFT_SEC.toDouble() else abs(d - durationSeconds)
            if (drift <= MAX_DURATION_DRIFT_SEC) {
                Pair(hit.optString("isrc"), drift)
            } else null
        }.sortedBy { it.second }

        return scored.map { it.first }
    }

    private fun queryHits(path: String, params: Map<String, String>): List<JSONObject> {
        val builder = Uri.parse("$API_BASE$path").buildUpon()
        for ((k, v) in params) builder.appendQueryParameter(k, v)

        val request = Request.Builder()
            .url(builder.build().toString())
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val hits = json.optJSONArray("hits") ?: return emptyList()

        val out = mutableListOf<JSONObject>()
        for (i in 0 until hits.length()) {
            val obj = hits.optJSONObject(i) ?: continue
            out.add(obj)
        }
        return out
    }

    private fun fetchTtml(isrc: String): String? {
        val url = "$API_BASE/s/${Uri.encode(isrc)}.ttml"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val ttml = response.body?.string() ?: return null
        val lines = TtmlLyricsParser.parse(ttml) ?: return null
        return TimedLrcWriter.write(lines)
    }
}
