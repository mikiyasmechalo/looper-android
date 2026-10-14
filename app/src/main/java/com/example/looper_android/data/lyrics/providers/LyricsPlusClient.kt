package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import com.example.looper_android.data.lyrics.formats.KpoeLyricsParser
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object LyricsPlusClient {

    private val MIRRORS = listOf(
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev"
    )

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
        var lineSyncedFallback: String? = null

        for (name in LyricsQuery.artistVariants(artist)) {
            for (base in MIRRORS) {
                try {
                    val builder = Uri.parse("$base/v2/lyrics/get").buildUpon()
                        .appendQueryParameter("title", cleanTitle)
                        .appendQueryParameter("artist", name)
                    if (durationSeconds > 0) {
                        builder.appendQueryParameter("duration", durationSeconds.toString())
                    }

                    val request = Request.Builder()
                        .url(builder.build().toString())
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Accept", "application/json")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.code == 404) {
                        break // mirrors share catalogue, try next artist credit
                    }
                    if (!response.isSuccessful) continue

                    val payload = response.body?.string() ?: continue
                    val lines = KpoeLyricsParser.parse(payload) ?: continue
                    val lrc = TimedLrcWriter.write(lines) ?: continue

                    if (lines.any { it.isWordSynced }) {
                        return@withContext lrc
                    }
                    if (lineSyncedFallback == null) {
                        lineSyncedFallback = lrc
                    }
                    break
                } catch (_: Exception) {}
            }
            if (lineSyncedFallback != null) break
        }

        return@withContext lineSyncedFallback
    }
}
