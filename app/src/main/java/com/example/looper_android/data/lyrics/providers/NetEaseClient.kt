package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import com.example.looper_android.data.lyrics.formats.KrcLyricsParser
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import com.example.looper_android.data.lyrics.formats.TimedLyricLine
import com.example.looper_android.data.lyrics.formats.TimedLyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NetEaseClient {

    private const val API_BASE = "https://music.163.com/api"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val LRC_LINE = Regex("""^\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\](.*)$""")

    suspend fun fetch(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsQuery.cleanTitle(title)
        try {
            val name = LyricsQuery.artistVariants(artist).lastOrNull() ?: artist
            val found = getJson("/cloudsearch/pc", mapOf(
                "s" to "$name $cleanTitle",
                "type" to "1",
                "limit" to "10"
            )) ?: return@withContext null

            val result = found.optJSONObject("result") ?: return@withContext null
            val songs = result.optJSONArray("songs") ?: return@withContext null

            var asked = 0
            for (i in 0 until songs.length()) {
                val song = songs.optJSONObject(i) ?: continue
                val songName = song.optString("name", "")
                val dt = song.optDouble("dt", 0.0)
                val lengthSec = if (dt > 0) dt / 1000.0 else null

                val artistsList = mutableListOf<String>()
                val ar = song.optJSONArray("ar")
                if (ar != null) {
                    for (j in 0 until ar.length()) {
                        val a = ar.optJSONObject(j) ?: continue
                        val n = a.optString("name", "")
                        if (n.isNotEmpty()) artistsList.add(n)
                    }
                }

                val same = LyricsQuery.sameSong(
                    wantTitle = cleanTitle,
                    wantArtist = artist,
                    title = songName,
                    artists = artistsList
                ) && LyricsQuery.sameLength(lengthSec, durationSeconds)

                val id = song.opt("id")?.toString()
                if (!same || id.isNullOrEmpty()) continue
                if (asked++ >= 2) break

                val lyric = getJson("/song/lyric", mapOf(
                    "id" to id,
                    "lv" to "1",
                    "yv" to "1"
                )) ?: continue

                val lrc = toLrc(lyric, cleanTitle)
                if (lrc != null) return@withContext lrc
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun toLrc(lyric: JSONObject, title: String): String? {
        fun text(key: String): String {
            val obj = lyric.optJSONObject(key)
            return obj?.optString("lyric", "") ?: ""
        }

        var lines = KrcLyricsParser.parseYrc(text("yrc"))
        if (lines.isEmpty()) lines = parseLrc(text("lrc"))
        if (lines.isEmpty()) return null

        val filtered = TimedLyricsUtils.withoutCreditLines(lines, title)
        return TimedLrcWriter.write(filtered)
    }

    private fun parseLrc(lrc: String): List<TimedLyricLine> {
        val lines = mutableListOf<TimedLyricLine>()
        for (raw in lrc.split(Regex("""\r?\n"""))) {
            val match = LRC_LINE.find(raw.trim()) ?: continue
            val text = match.groupValues[4].trim()
            if (text.isEmpty()) continue

            val mins = match.groupValues[1].toLongOrNull() ?: 0L
            val secs = match.groupValues[2].toLongOrNull() ?: 0L
            val frac = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0L
            val ms = mins * 60000L + secs * 1000L + frac

            lines.add(TimedLyricLine(startMs = ms, endMs = ms, text = text))
        }
        return lines
    }

    private fun getJson(path: String, params: Map<String, String>): JSONObject? {
        val builder = Uri.parse("$API_BASE$path").buildUpon()
        for ((k, v) in params) builder.appendQueryParameter(k, v)

        val request = Request.Builder()
            .url(builder.build().toString())
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Referer", "https://music.163.com/")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        return try { JSONObject(body) } catch (_: Exception) { null }
    }
}
