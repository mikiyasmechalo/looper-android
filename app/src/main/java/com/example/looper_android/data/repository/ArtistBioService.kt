package com.example.looper_android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistBioService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val prefs = context.getSharedPreferences("artist_biographies", Context.MODE_PRIVATE)

    private val collaborationSplitRegex = Regex(
        """\s+(?:ft\.?|feat\.?|featuring|vs\.?|with|and|&|\+|\/|x)\s+|,\s*""",
        RegexOption.IGNORE_CASE
    )

    suspend fun getArtistBiography(rawArtistName: String): String? = withContext(Dispatchers.IO) {
        val cleanName = rawArtistName.trim()
        if (cleanName.isBlank() || cleanName.equals("unknown artist", ignoreCase = true) || cleanName.equals("unknown", ignoreCase = true)) {
            return@withContext null
        }

        val primaryArtist = cleanName.split(collaborationSplitRegex).firstOrNull()?.trim()?.ifBlank { null } ?: cleanName
        val cacheKey = primaryArtist.lowercase()
        if (prefs.contains(cacheKey)) {
            val cached = prefs.getString(cacheKey, null)
            if (!cached.isNullOrBlank()) return@withContext cached
        }

        val internetEnabled = settingsRepository.enableInternet.first()
        val bioEnabled = settingsRepository.enableArtistBiography.first()
        if (!internetEnabled || !bioEnabled) {
            return@withContext null
        }

        // 1. Try Last.fm Music Database (100% coverage for music artists)
        var bioText: String? = fetchLastFmBio(cleanName)
        if (bioText.isNullOrBlank() && primaryArtist != cleanName) {
            bioText = fetchLastFmBio(primaryArtist)
        }

        // 2. Try Wikipedia Summary & Search API
        if (bioText.isNullOrBlank()) {
            bioText = fetchWikipediaBio(cleanName) ?: fetchWikipediaBio(primaryArtist)
        }

        // 3. Fallback to TheAudioDB API
        if (bioText.isNullOrBlank()) {
            bioText = fetchTheAudioDbBio(cleanName) ?: fetchTheAudioDbBio(primaryArtist)
        }

        if (!bioText.isNullOrBlank()) {
            prefs.edit().putString(cacheKey, bioText).apply()
        }

        return@withContext bioText
    }

    private fun cleanHtmlAndLinks(text: String): String {
        return text
            .replace(Regex("<.*?>"), "")
            .replace("Read more on Last.fm", "")
            .replace("User-contributed text is available under the Creative Commons By-SA License; additional terms may apply.", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun fetchLastFmBio(artistName: String): String? {
        return try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val url = URL("https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=$encoded&api_key=b25b959554ed76058ac220b7b2e0a026&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "LooperApp/1.0")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val artistObj = obj.optJSONObject("artist")
                val bioObj = artistObj?.optJSONObject("bio")
                val summary = bioObj?.optString("summary") ?: bioObj?.optString("content")
                if (!summary.isNullOrBlank() && !summary.startsWith("Incorrect tag")) {
                    val cleaned = cleanHtmlAndLinks(summary)
                    if (cleaned.isNotBlank()) return cleaned
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchWikipediaBio(artistName: String): String? {
        return try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val sumUrl = URL("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
            val conn = sumUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "LooperApp/1.0 (Android; music)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val type = obj.optString("type")
                if (type != "disambiguation") {
                    val extract = obj.optString("extract")
                    if (extract.isNotBlank() && !extract.contains("may refer to:", ignoreCase = true)) {
                        return extract.trim()
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchTheAudioDbBio(artistName: String): String? {
        return try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val url = URL("https://theaudiodb.com/api/v1/json/2/artist.php?s=$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val artists = obj.optJSONArray("artists")
                if (artists != null && artists.length() > 0) {
                    val first = artists.getJSONObject(0)
                    val bio = first.optString("strBiographyEN")
                    if (bio.isNotBlank()) {
                        return bio.trim()
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
