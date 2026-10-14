package com.example.looper_android.data.lyrics.providers

import android.net.Uri
import com.example.looper_android.data.lyrics.formats.TimedLrcWriter
import com.example.looper_android.data.lyrics.formats.TimedLyricsUtils
import com.example.looper_android.data.lyrics.formats.TtmlLyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AmllClient {

    private const val API_BASE = "https://api.amll.dev/v1/lyrics"

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
            for (name in LyricsQuery.artistVariants(artist, max = 2)) {
                val found = getJson("/search", mapOf("musicName" to cleanTitle, "artistName" to name)) ?: continue
                val items = found.optJSONArray("items") ?: continue

                for (i in 0 until items.length().coerceAtMost(3)) {
                    val item = items.optJSONObject(i) ?: continue
                    val musicNames = jsonArrayToStringList(item.optJSONArray("musicNames"))
                    val artistNames = jsonArrayToStringList(item.optJSONArray("artistNames"))

                    val same = musicNames.any { t ->
                        LyricsQuery.sameSong(
                            wantTitle = cleanTitle,
                            wantArtist = artist,
                            title = t,
                            artists = artistNames
                        )
                    }
                    val id = item.opt("id")?.toString()
                    if (!same || id.isNullOrEmpty()) continue

                    val data = getJson("/get", mapOf("id" to id, "format" to "ttml")) ?: continue
                    val ttml = data.optString("lyrics", "")
                    if (ttml.isEmpty()) continue

                    val lines = TtmlLyricsParser.parse(ttml) ?: continue
                    if (!TimedLyricsUtils.fitsDuration(lines, durationSeconds)) continue

                    val lrc = TimedLrcWriter.write(lines)
                    if (lrc != null) return@withContext lrc
                }
                if (items.length() > 0) break
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun getJson(path: String, params: Map<String, String>): JSONObject? {
        val builder = Uri.parse("$API_BASE$path").buildUpon()
        for ((k, v) in params) builder.appendQueryParameter(k, v)

        val request = Request.Builder()
            .url(builder.build().toString())
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val root = JSONObject(body)
        return root.optJSONObject("data")
    }

    private fun jsonArrayToStringList(array: org.json.JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val s = array.optString(i, "")
            if (s.isNotEmpty()) list.add(s)
        }
        return list
    }
}
