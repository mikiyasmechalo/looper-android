package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object MusicBrainzIsrc {

    private const val ENDPOINT = "https://musicbrainz.org/ws/2/recording"
    private const val MAX_DURATION_DRIFT_SEC = 4
    private const val MIN_GAP_MS = 1100L

    private val mutex = Mutex()
    private var lastRequestTime = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun find(
        title: String,
        artists: List<String>,
        durationSeconds: Int = 0
    ): List<String> = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsQuery.cleanTitle(title)
        val titleKey = LyricsQuery.key(cleanTitle)
        if (titleKey.isEmpty()) return@withContext emptyList()

        val names = (if (artists.size > 1) artists.drop(1) else artists)
            .filter { !it.equals("unknown artist", ignoreCase = true) }
        if (names.isEmpty()) return@withContext emptyList()

        fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

        val artistQuery = names.joinToString(" OR ") { "artist:\"${escape(it)}\"" }
        val clauses = mutableListOf(
            "recording:\"${escape(cleanTitle)}\"",
            "($artistQuery)"
        )

        if (durationSeconds > 0) {
            val lo = (durationSeconds - MAX_DURATION_DRIFT_SEC) * 1000
            val hi = (durationSeconds + MAX_DURATION_DRIFT_SEC) * 1000
            clauses.add("dur:[$lo TO $hi]")
        }

        val url = Uri.parse(ENDPOINT).buildUpon()
            .appendQueryParameter("query", clauses.joinToString(" AND "))
            .appendQueryParameter("fmt", "json")
            .appendQueryParameter("limit", "10")
            .build()
            .toString()

        val body = throttledGet(url) ?: return@withContext emptyList()

        try {
            val root = JSONObject(body)
            val recordings = root.optJSONArray("recordings") ?: return@withContext emptyList()
            val artistKeys = names.map { LyricsQuery.key(it) }.toSet()
            val found = mutableListOf<String>()

            for (i in 0 until recordings.length()) {
                val r = recordings.optJSONObject(i) ?: continue
                val isrcs = r.optJSONArray("isrcs") ?: continue
                if (isrcs.length() == 0) continue

                val recTitle = r.optString("title", "")
                if (LyricsQuery.key(recTitle) != titleKey) continue

                val lengthMs = r.optDouble("length", 0.0)
                if (durationSeconds > 0 && lengthMs > 0) {
                    val lengthSec = lengthMs / 1000.0
                    if (abs(lengthSec - durationSeconds) > MAX_DURATION_DRIFT_SEC) continue
                }

                val artistCredit = r.optJSONArray("artist-credit")
                var credited = false
                if (artistCredit != null) {
                    for (j in 0 until artistCredit.length()) {
                        val a = artistCredit.optJSONObject(j) ?: continue
                        val name = a.optString("name", "")
                        if (artistKeys.contains(LyricsQuery.key(name))) {
                            credited = true
                            break
                        }
                    }
                }

                if (credited) {
                    for (k in 0 until isrcs.length()) {
                        val isrc = isrcs.optString(k, "")
                        if (isrc.isNotEmpty() && !found.contains(isrc)) {
                            found.add(isrc)
                        }
                    }
                }
            }
            found
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun throttledGet(url: String): String? {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < MIN_GAP_MS) {
                delay(MIN_GAP_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "LooperPlayer/1.0 (https://github.com/SthrNilshaaa/looper)")
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) response.body?.string() else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
