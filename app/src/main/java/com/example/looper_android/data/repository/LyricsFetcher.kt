package com.example.looper_android.data.repository

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsFetcher @Inject constructor(
    private val libraryDao: LibraryDao,
    private val audioTagReader: AudioTagReader,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "LyricsFetcher"

        val ALL_PROVIDERS = listOf(
            "LRCLIB",
            "Genius",
            "LyricFind",
            "Musixmatch",
            "AZLyrics",
            "LyricsMINT",
            "Local",
            "Embedded"
        )

        val ALL_ONLINE_PROVIDERS = listOf(
            "LRCLIB",
            "Genius",
            "LyricFind",
            "Musixmatch",
            "AZLyrics",
            "LyricsMINT"
        )
    }

    // In-memory per-song source cache (songId -> Map of normalized provider names -> lyrics)
    private val sessionLyricsCache = java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.ConcurrentHashMap<String, String>>()

    fun isLyricsSynced(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return Regex("""\[\d{1,3}\s*:\s*\d{1,2}""").containsMatchIn(raw)
    }

    fun getCachedLyricsSync(song: SongEntity): String? {
        val inMem = sessionLyricsCache[song.id]?.values?.firstOrNull { it.isNotBlank() }
        if (!inMem.isNullOrBlank()) return inMem
        if (!song.lyrics.isNullOrBlank()) {
            cacheInMemory(song.id, extractLyricsSource(song.lyrics).uppercase(), song.lyrics)
            return song.lyrics
        }
        return null
    }

    private fun cacheInMemory(songId: Long, provider: String, lyrics: String) {
        sessionLyricsCache.computeIfAbsent(songId) { java.util.concurrent.ConcurrentHashMap() }[provider.uppercase()] = lyrics
    }

    private suspend fun cacheInMemoryAndDb(songId: Long, provider: String, lyrics: String) {
        cacheInMemory(songId, provider, lyrics)
        libraryDao.updateSongLyrics(songId, lyrics)
    }

    private fun getFromMemoryOrFetch(song: SongEntity, provider: String): String? {
        val cached = sessionLyricsCache[song.id]?.get(provider.uppercase())
        if (!cached.isNullOrEmpty()) return cached
        val fetched = fetchFromOnlineProvider(song, provider.uppercase())
        if (fetched != null) {
            cacheInMemory(song.id, provider.uppercase(), fetched)
        }
        return fetched
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun httpGet(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            Log.d(TAG, "httpGet() url=$url, code=$code")
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "httpGet failed for $url", e)
            null
        }
    }

    private fun sanitize(text: String): String {
        return text
            .replace(Regex("""\((feat|with|ft)\.?.*?\)|\[(feat|with|ft)\.?.*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\((Remastered|Live|Official|Video).*?\)|\[(Remastered|Live|Official|Video).*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b(feat|ft|with|featuring)\.?\s+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun searchUrlOnWeb(site: String, query: String): String? {
        val cleanQ = query.replace(Regex("""\s{2,}"""), " ").trim()
        val encodedQ = try { URLEncoder.encode("site:$site $cleanQ", "UTF-8") } catch (_: Exception) { cleanQ }

        fun extractCandidate(raw: String): String? {
            var url = raw.replace("&amp;", "&")
            if (url.startsWith("//")) url = "https:$url"

            // 1. DuckDuckGo uddg parameter
            if (url.contains("uddg=")) {
                val uddg = Uri.parse(url).getQueryParameter("uddg")
                if (uddg != null) {
                    val decoded = try { URLDecoder.decode(uddg, "UTF-8") } catch (_: Exception) { null }
                    if (decoded != null) url = decoded
                }
            }

            // 2. Yahoo RU parameter
            if (url.contains("/RU=")) {
                val ru = url.substringAfter("/RU=").substringBefore("/RK=").substringBefore("\"").substringBefore("'")
                val decodedRu = try { URLDecoder.decode(ru, "UTF-8") } catch (_: Exception) { null }
                if (decodedRu != null) url = decodedRu
            }

            // 3. Google /url?q= parameter
            if (url.contains("/url?q=")) {
                val gq = url.substringAfter("/url?q=").substringBefore("&")
                val decodedGq = try { URLDecoder.decode(gq, "UTF-8") } catch (_: Exception) { null }
                if (decodedGq != null) url = decodedGq
            }

            // 4. Bing base64 u=a1 parameter
            if (url.contains("u=a1")) {
                val b64 = url.substringAfter("u=a1").substringBefore("&").substringBefore("\"").substringBefore("'")
                val decodedB64 = try { String(Base64.decode(b64, Base64.DEFAULT)) } catch (_: Exception) { null }
                if (decodedB64 != null) url = decodedB64
            }

            if ((url.startsWith("http://") || url.startsWith("https://")) &&
                url.contains(site, ignoreCase = true) &&
                !url.contains("search", ignoreCase = true) &&
                !url.contains("google.com", ignoreCase = true) &&
                !url.contains("bing.com", ignoreCase = true) &&
                !url.contains("yahoo.com", ignoreCase = true) &&
                !url.contains("duckduckgo.com", ignoreCase = true)
            ) {
                return url
            }
            return null
        }

        val searchEngines = listOf(
            "https://search.yahoo.com/search?p=$encodedQ",
            "https://www.bing.com/search?q=$encodedQ",
            "https://html.duckduckgo.com/html/?q=site:$site+$cleanQ",
            "https://www.google.com/search?q=$encodedQ"
        )

        val hrefRegex = Regex("""href=["']([^"']+)["']""")

        for (searchEngineUrl in searchEngines) {
            try {
                Log.d(TAG, "searchUrlOnWeb() trying: $searchEngineUrl")
                val response = httpGet(searchEngineUrl) ?: continue
                for (match in hrefRegex.findAll(response)) {
                    val candidate = extractCandidate(match.groupValues[1])
                    if (candidate != null) {
                        Log.d(TAG, "searchUrlOnWeb() matched target URL: $candidate")
                        return candidate
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchUrlOnWeb engine error for $searchEngineUrl", e)
            }
        }
        return null
    }

    /**
     * Extracts normalized source label from lyrics text (e.g. "[source:lrclib]" -> "LRCLIB")
     */
    fun extractLyricsSource(rawLyrics: String?): String {
        if (rawLyrics.isNullOrBlank()) return "None"
        val firstLine = rawLyrics.trim().lines().firstOrNull() ?: ""
        val sourceMatch = Regex("^\\[source:([a-zA-Z0-9_-]+)\\]", RegexOption.IGNORE_CASE).find(firstLine)
        return if (sourceMatch != null) {
            when (sourceMatch.groupValues[1].lowercase()) {
                "lrclib" -> "LRCLIB"
                "genius" -> "Genius"
                "musixmatch" -> "Musixmatch"
                "azlyrics" -> "AZLyrics"
                "lyricsmint" -> "LyricsMINT"
                "lyricfind" -> "LyricFind"
                "embedded" -> "Embedded"
                "local", "sidecar" -> "Local"
                "custom" -> "Custom LRC"
                else -> sourceMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            }
        } else {
            val hasTimestamps = Regex("""\[\d{1,3}\s*:\s*\d{1,2}""").containsMatchIn(rawLyrics)
            if (hasTimestamps) "Local" else "Plain Text"
        }
    }

    suspend fun fetchLyrics(song: SongEntity): String? = withContext(Dispatchers.IO) {
        // Fast Check: If in-memory cache already has lyrics, return immediately
        val inMem = sessionLyricsCache[song.id]?.values?.firstOrNull { it.isNotBlank() }
        if (!inMem.isNullOrBlank()) return@withContext inMem

        // Check DB Cache
        val dbCached = libraryDao.getSongLyricsById(song.id) ?: libraryDao.getSongLyricsByPath(song.path)
        if (!dbCached.isNullOrEmpty()) {
            val src = extractLyricsSource(dbCached).uppercase()
            cacheInMemory(song.id, src, dbCached)
            return@withContext dbCached
        }

        // 1. Check Embedded Metadata
        val embedded = tryEmbeddedLyrics(song)
        val isEmbeddedSynced = isLyricsSynced(embedded)

        // Rule 1: Embedded SYNCED lyrics have highest priority
        if (isEmbeddedSynced && embedded != null) {
            cacheInMemoryAndDb(song.id, "EMBEDDED", embedded)
            return@withContext embedded
        }

        // 2. Check Local Sidecar (.lrc / .txt)
        val sidecar = tryLocalSidecarLyrics(song)
        val isSidecarSynced = isLyricsSynced(sidecar)

        // Rule 2: Local Sidecar (.lrc) SYNCED lyrics
        if (isSidecarSynced && sidecar != null) {
            cacheInMemoryAndDb(song.id, "LOCAL", sidecar)
            return@withContext sidecar
        }

        // 3. Online Providers (Primary + Fallbacks) - Search for SYNCED lyrics first
        var onlineUnsynced: String? = null
        val enableInternet = settingsRepository.enableInternet.first()
        if (enableInternet) {
            val primaryProvider = settingsRepository.primaryLyricsProvider.first().uppercase()
            val autoFallback = settingsRepository.autoFallbackLyricsProviders.first()

            val primaryLyrics = getFromMemoryOrFetch(song, primaryProvider)
            if (isLyricsSynced(primaryLyrics) && primaryLyrics != null) {
                cacheInMemoryAndDb(song.id, extractLyricsSource(primaryLyrics).uppercase(), primaryLyrics)
                return@withContext primaryLyrics
            } else if (primaryLyrics != null && onlineUnsynced == null) {
                onlineUnsynced = primaryLyrics
            }

            if (autoFallback) {
                val remainingProviders = ALL_ONLINE_PROVIDERS.filter { !it.equals(primaryProvider, ignoreCase = true) }
                for (fallback in remainingProviders) {
                    val fallbackLyrics = getFromMemoryOrFetch(song, fallback)
                    if (isLyricsSynced(fallbackLyrics) && fallbackLyrics != null) {
                        cacheInMemoryAndDb(song.id, extractLyricsSource(fallbackLyrics).uppercase(), fallbackLyrics)
                        return@withContext fallbackLyrics
                    } else if (fallbackLyrics != null && onlineUnsynced == null) {
                        onlineUnsynced = fallbackLyrics
                    }
                }
            }
        }

        // Rule 4: Embedded UNSYNCED lyrics (if available)
        if (!embedded.isNullOrEmpty()) {
            cacheInMemoryAndDb(song.id, "EMBEDDED", embedded)
            return@withContext embedded
        }

        // Rule 5: Local Sidecar UNSYNCED lyrics (if available)
        if (!sidecar.isNullOrEmpty()) {
            cacheInMemoryAndDb(song.id, "LOCAL", sidecar)
            return@withContext sidecar
        }

        // Rule 6: Online UNSYNCED lyrics (if available)
        if (onlineUnsynced != null) {
            cacheInMemoryAndDb(song.id, extractLyricsSource(onlineUnsynced).uppercase(), onlineUnsynced)
            return@withContext onlineUnsynced
        }

        return@withContext null
    }

    /**
     * Explicitly fetches lyrics from a specific provider (used by provider switcher in bottom sheet).
     */
    suspend fun fetchFromSpecificProvider(song: SongEntity, provider: String): String? = withContext(Dispatchers.IO) {
        val norm = provider.uppercase()
        // Check in-memory cache first for instant switching
        val cached = sessionLyricsCache[song.id]?.get(norm)
        if (!cached.isNullOrEmpty()) {
            libraryDao.updateSongLyrics(song.id, cached)
            return@withContext cached
        }

        val result = when (norm) {
            "EMBEDDED" -> tryEmbeddedLyrics(song)
            "LOCAL", "LOCAL SIDECAR" -> tryLocalSidecarLyrics(song)
            else -> {
                val enableInternet = settingsRepository.enableInternet.first()
                if (enableInternet) {
                    fetchFromOnlineProvider(song, norm)
                } else null
            }
        }

        if (result != null) {
            cacheInMemoryAndDb(song.id, norm, result)
        }
        result
    }

    private suspend fun tryEmbeddedLyrics(song: SongEntity): String? {
        return try {
            val embedded = audioTagReader.read(song.path).lyrics
            if (!embedded.isNullOrEmpty()) {
                "[source:embedded]\n$embedded"
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryLocalSidecarLyrics(song: SongEntity): String? {
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
                            return "[source:local]\n$content"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading local lyric file ${f.absolutePath}", e)
                    }
                }
            }
        }
        return null
    }

    private fun fetchFromOnlineProvider(song: SongEntity, provider: String): String? {
        val title = song.title.trim()
        val artist = (song.artist ?: "Unknown Artist").trim()

        return when (provider.uppercase()) {
            "GENIUS" -> fetchFromGenius(title, artist)
            "LYRICFIND" -> fetchFromLyricFind(title, artist)
            "MUSIXMATCH" -> fetchFromMusixmatch(title, artist)
            "AZLYRICS" -> fetchFromAZLyrics(title, artist)
            "LYRICSMINT" -> fetchFromLyricsMint(title, artist)
            "LRCLIB" -> fetchFromLrcLib(song)
            else -> fetchFromLrcLib(song)
        }
    }

    private fun JSONObject.getNullableString(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key, "").trim()
        return if (v.isEmpty() || v.equals("null", ignoreCase = true)) null else v
    }

    // --- 1. LRCLIB (Direct port of Flutter) ---
    private fun fetchFromLrcLib(song: SongEntity): String? {
        val trackName = song.title.trim()
        val artistName = (song.artist ?: "").trim()
        val durationSeconds = ((song.duration ?: 0) / 1000).toInt()

        val cleanTrack = sanitize(trackName)
        val cleanArtist = sanitize(artistName)

        var fallbackPlain: String? = null

        try {
            // 1. Try /api/get with sanitized track/artist
            val getUriBuilder = Uri.parse("https://lrclib.net/api/get").buildUpon()
                .appendQueryParameter("track_name", cleanTrack)
                .appendQueryParameter("artist_name", cleanArtist)
            if (durationSeconds > 0) {
                getUriBuilder.appendQueryParameter("duration", durationSeconds.toString())
            }

            var responseBody = httpGet(getUriBuilder.build().toString())
            if (responseBody != null) {
                val json = JSONObject(responseBody)
                val synced = json.getNullableString("syncedLyrics")
                if (!synced.isNullOrBlank()) return "[source:lrclib]\n$synced"
                val plain = json.getNullableString("plainLyrics")
                if (!plain.isNullOrBlank() && fallbackPlain == null) fallbackPlain = plain
            }

            // 2. Try with raw track/artist if sanitized differed
            if (cleanTrack != trackName || cleanArtist != artistName) {
                val rawUriBuilder = Uri.parse("https://lrclib.net/api/get").buildUpon()
                    .appendQueryParameter("track_name", trackName)
                    .appendQueryParameter("artist_name", artistName)
                if (durationSeconds > 0) {
                    rawUriBuilder.appendQueryParameter("duration", durationSeconds.toString())
                }
                responseBody = httpGet(rawUriBuilder.build().toString())
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val synced = json.getNullableString("syncedLyrics")
                    if (!synced.isNullOrBlank()) return "[source:lrclib]\n$synced"
                    val plain = json.getNullableString("plainLyrics")
                    if (!plain.isNullOrBlank() && fallbackPlain == null) fallbackPlain = plain
                }
            }

            // 3. Fallback search queries to find synced lyrics
            val searchQueries = mutableListOf(
                "$cleanArtist - $cleanTrack",
                "$cleanTrack $cleanArtist",
                cleanTrack,
                "$artistName - $trackName",
                trackName
            )
            if (cleanTrack.length > 3 && cleanTrack.last().isDigit()) {
                val trimmed = cleanTrack.dropLast(1)
                searchQueries.add("$cleanArtist - $trimmed")
                searchQueries.add(trimmed)
            }

            for (q in searchQueries.distinct()) {
                val searchUri = Uri.parse("https://lrclib.net/api/search").buildUpon()
                    .appendQueryParameter("q", q)
                    .build()
                    .toString()

                responseBody = httpGet(searchUri)
                Log.d(TAG, "queryLrcLibSearch() q='$q', body preview: ${responseBody?.take(200)}")
                if (responseBody != null) {
                    val results = JSONArray(responseBody)
                    Log.d(TAG, "queryLrcLibSearch() results count=${results.length()}")
                    if (results.length() > 0) {
                        // 3a. First search for exact synced match by duration
                        if (durationSeconds > 0) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val synced = item.getNullableString("syncedLyrics")
                                if (!synced.isNullOrBlank()) {
                                    val itemDur = item.optInt("duration", 0)
                                    if (Math.abs(itemDur - durationSeconds) < 15) {
                                        Log.d(TAG, "queryLrcLibSearch() matched synced by duration: $itemDur")
                                        return "[source:lrclib]\n$synced"
                                    }
                                }
                            }
                        }

                        // 3b. Then any synced match
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val synced = item.getNullableString("syncedLyrics")
                            if (!synced.isNullOrBlank()) {
                                Log.d(TAG, "queryLrcLibSearch() matched any synced lyrics")
                                return "[source:lrclib]\n$synced"
                            }
                        }

                        // 3c. Capture plain lyrics if no fallback yet
                        if (fallbackPlain == null) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val plain = item.getNullableString("plainLyrics")
                                if (!plain.isNullOrBlank()) {
                                    fallbackPlain = plain
                                    break
                                }
                            }
                        }
                    }
                }
            }

            if (!fallbackPlain.isNullOrBlank()) {
                Log.d(TAG, "fetchFromLrcLib() returning fallback plain lyrics")
                return "[source:lrclib]\n$fallbackPlain"
            }
        } catch (e: Exception) {
            Log.e(TAG, "LRCLIB error", e)
        }
        return null
    }

    // --- 2. GENIUS (Direct API + Port of Flutter) ---
    private fun fetchFromGenius(trackName: String, artistName: String): String? {
        try {
            val cleanT = sanitize(trackName)
            val cleanA = sanitize(artistName)
            val queries = listOf(
                "$cleanA $cleanT",
                "$artistName $trackName",
                cleanT
            ).distinct()

            var pageUrl: String? = null

            for (q in queries) {
                // 1. Direct Genius API search
                try {
                    val apiQuery = URLEncoder.encode(q, "UTF-8")
                    val apiSearchUrl = "https://genius.com/api/search/multi?q=$apiQuery"
                    val apiJsonStr = httpGet(apiSearchUrl)
                    if (apiJsonStr != null) {
                        val root = JSONObject(apiJsonStr)
                        val sections = root.optJSONObject("response")?.optJSONArray("sections")
                        if (sections != null) {
                            for (i in 0 until sections.length()) {
                                val sec = sections.getJSONObject(i)
                                val hits = sec.optJSONArray("hits") ?: continue
                                for (j in 0 until hits.length()) {
                                    val hit = hits.getJSONObject(j)
                                    val result = hit.optJSONObject("result")
                                    val hitUrl = result?.optString("url", "")
                                    if (!hitUrl.isNullOrBlank() && hitUrl.contains("genius.com", ignoreCase = true)) {
                                        pageUrl = hitUrl
                                        break
                                    }
                                }
                                if (pageUrl != null) break
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (pageUrl != null) break

                // 2. Web Search Fallback
                pageUrl = searchUrlOnWeb("genius.com", q)
                if (pageUrl != null) break
            }

            if (pageUrl == null) {
                Log.d(TAG, "Genius page not found on search")
                return null
            }

            Log.d(TAG, "Fetching Genius page: $pageUrl")
            val responseBody = httpGet(pageUrl) ?: return null

            val regex = Regex("""<div[^>]*data-lyrics-container="true"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val matches = regex.findAll(responseBody).toList()
            if (matches.isNotEmpty()) {
                val buffer = StringBuilder()
                for (m in matches) {
                    buffer.append(cleanHtml(m.groupValues[1])).append("\n")
                }
                return "[source:genius]\n${buffer.toString().trim()}"
            }

            val regexOld = Regex("""<div[^>]*class="lyrics"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val matchOld = regexOld.find(responseBody)
            if (matchOld != null) {
                return "[source:genius]\n${cleanHtml(matchOld.groupValues[1])}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Genius error", e)
        }
        return null
    }

    // --- 3. MUSIXMATCH (Synced Lyrics + Plain Text Fallback) ---
    private fun fetchFromMusixmatch(trackName: String, artistName: String): String? {
        try {
            val cleanT = sanitize(trackName)
            val cleanA = sanitize(artistName)
            val queries = listOf(
                "$cleanA $cleanT",
                "$artistName $trackName",
                cleanT
            ).distinct()

            var pageUrl: String? = null
            for (q in queries) {
                pageUrl = searchUrlOnWeb("musixmatch.com", q)
                if (pageUrl != null) break
            }

            if (pageUrl == null) {
                Log.d(TAG, "Musixmatch page not found on search")
                return null
            }

            Log.d(TAG, "Fetching Musixmatch page: $pageUrl")
            val responseBody = httpGet(pageUrl) ?: return null

            val nextDataRegex = Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            val match = nextDataRegex.find(responseBody)
            if (match != null) {
                val rootJson = JSONObject(match.groupValues[1])
                val trackInfoData = rootJson.optJSONObject("props")
                    ?.optJSONObject("pageProps")
                    ?.optJSONObject("data")
                    ?.optJSONObject("trackInfo")
                    ?.optJSONObject("data")

                Log.d(TAG, "Musixmatch raw subtitle field: ${trackInfoData?.opt("subtitle")}")
                Log.d(TAG, "Musixmatch raw lyrics field: ${trackInfoData?.opt("lyrics")}")

                val syncedLyrics = extractMusixmatchSyncedLyrics(rootJson)
                if (!syncedLyrics.isNullOrBlank()) {
                    Log.d(TAG, "fetchFromMusixmatch() successfully extracted synced lyrics (${syncedLyrics.length} chars)")
                    return "[source:musixmatch]\n$syncedLyrics"
                }

                // Plain text fallback
                val lyricsObj = trackInfoData?.optJSONObject("lyrics")?.optJSONObject("data") ?: trackInfoData?.optJSONObject("lyrics")
                val lyricsBody = lyricsObj?.optString("body", "")
                if (!lyricsBody.isNullOrBlank()) {
                    Log.d(TAG, "fetchFromMusixmatch() plain text fallback (${lyricsBody.length} chars)")
                    return "[source:musixmatch]\n${lyricsBody.trim()}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Musixmatch error", e)
        }
        return null
    }

    private fun extractMusixmatchSyncedLyrics(root: JSONObject): String? {
        fun checkSubtitleBody(obj: JSONObject?): String? {
            if (obj == null) return null
            val unwrapped = obj.optJSONObject("data") ?: obj
            val subBody = unwrapped.optString("subtitle_body", unwrapped.optString("body", ""))
            if (subBody.isNotBlank() && subBody.contains("[")) return subBody.trim()
            
            val lines = unwrapped.optJSONArray("lines") ?: unwrapped.optJSONArray("subtitles")
            if (lines != null && lines.length() > 0) {
                val parsed = parseLinesArray(lines)
                if (parsed != null) return parsed
            }
            return null
        }

        val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
        val data = pageProps?.optJSONObject("data")
        val trackInfoData = data?.optJSONObject("trackInfo")?.optJSONObject("data")

        if (trackInfoData != null) {
            // 1. Check subtitle object inside trackInfo
            val subObj = trackInfoData.optJSONObject("subtitle") ?: trackInfoData.optJSONObject("subtitles")
            val subResult = checkSubtitleBody(subObj)
            if (subResult != null) return subResult

            // 2. Check lyrics object inside trackInfo
            val lyricsObj = trackInfoData.optJSONObject("lyrics")
            val lyricsSubResult = checkSubtitleBody(lyricsObj?.optJSONObject("subtitle") ?: lyricsObj?.optJSONObject("subtitles"))
            if (lyricsSubResult != null) return lyricsSubResult
            val lyricsDirectResult = checkSubtitleBody(lyricsObj)
            if (lyricsDirectResult != null) return lyricsDirectResult
        }

        // 3. Deep search fallback across whole JSON
        fun deepSearchLyrics(obj: Any?): String? {
            when (obj) {
                is JSONObject -> {
                    val unwrapped = obj.optJSONObject("data") ?: obj
                    val subBody = unwrapped.optString("subtitle_body", unwrapped.optString("body", ""))
                    if (subBody.isNotBlank() && subBody.contains("[")) return subBody.trim()

                    val lines = unwrapped.optJSONArray("lines") ?: unwrapped.optJSONArray("subtitles")
                    if (lines != null && lines.length() > 0) {
                        val parsed = parseLinesArray(lines)
                        if (parsed != null) return parsed
                    }

                    for (key in obj.keys()) {
                        val child = obj.opt(key)
                        val res = deepSearchLyrics(child)
                        if (res != null) return res
                    }
                }
                is JSONArray -> {
                    val parsed = parseLinesArray(obj)
                    if (parsed != null) return parsed
                    for (i in 0 until obj.length()) {
                        val res = deepSearchLyrics(obj.opt(i))
                        if (res != null) return res
                    }
                }
                is String -> {
                    if (obj.contains("[00:") || obj.contains("[01:") || obj.contains("[02:")) {
                        if (obj.length > 50) return obj.trim()
                    }
                }
            }
            return null
        }

        return deepSearchLyrics(root)
    }

    private fun parseLinesArray(linesArray: JSONArray): String? {
        if (linesArray.length() == 0) return null
        val lrcBuilder = StringBuilder()
        var hasValidTimestamp = false
        for (i in 0 until linesArray.length()) {
            val lineObj = linesArray.optJSONObject(i) ?: continue
            val text = lineObj.optString("text", lineObj.optString("body", lineObj.optString("val", ""))).trim()
            val timeObj = lineObj.optJSONObject("time")
            val totalSecs = if (timeObj != null) {
                timeObj.optDouble("total", 0.0)
            } else {
                val directTime = lineObj.optDouble("time", lineObj.optDouble("startTime", lineObj.optDouble("start", -1.0)))
                if (directTime > 1000) directTime / 1000.0 else directTime
            }
            if (totalSecs >= 0) {
                hasValidTimestamp = true
                val mins = (totalSecs / 60).toInt()
                val secs = totalSecs % 60
                val timestamp = String.format(java.util.Locale.US, "[%02d:%05.2f]", mins, secs)
                lrcBuilder.append(timestamp).append(" ").append(text).append("\n")
            } else if (text.isNotEmpty()) {
                lrcBuilder.append(text).append("\n")
            }
        }
        val synced = lrcBuilder.toString().trim()
        return if (hasValidTimestamp && synced.contains("[")) synced else null
    }

    // --- 4. AZLYRICS ---
    private fun fetchFromAZLyrics(trackName: String, artistName: String): String? {
        try {
            val cleanArtist = artistName.lowercase().replace(Regex("[^a-z0-9]"), "")
            val cleanTrack = trackName.lowercase().replace(Regex("[^a-z0-9]"), "")
            
            var pageUrl: String? = "https://www.azlyrics.com/lyrics/$cleanArtist/$cleanTrack.html"
            Log.d(TAG, "Trying direct AZLyrics URL: $pageUrl")

            var responseBody = if (pageUrl != null) httpGet(pageUrl) else null
            Log.d(TAG, "Direct AZLyrics response length=${responseBody?.length}, text=${responseBody?.take(1000)}")
            if (responseBody == null || !responseBody.contains("<!-- Usage of azlyrics.com content")) {
                if (cleanTrack.length > 4 && cleanTrack.last().isDigit()) {
                    val trimmed = cleanTrack.dropLast(1)
                    pageUrl = "https://www.azlyrics.com/lyrics/$cleanArtist/$trimmed.html"
                    Log.d(TAG, "Trying trimmed direct AZLyrics URL: $pageUrl")
                    responseBody = httpGet(pageUrl)
                    Log.d(TAG, "Trimmed AZLyrics response length=${responseBody?.length}, text=${responseBody?.take(1000)}")
                }
            }

            if (responseBody == null || !responseBody.contains("<!-- Usage of azlyrics.com content")) {
                Log.d(TAG, "Direct AZLyrics failed, searching via DuckDuckGo")
                val cleanT = sanitize(trackName)
                val cleanA = sanitize(artistName)
                val queries = listOf(
                    "$cleanA $cleanT",
                    "$artistName $trackName",
                    cleanT
                ).distinct()

                for (q in queries) {
                    pageUrl = searchUrlOnWeb("azlyrics.com", q)
                    if (pageUrl != null) {
                        responseBody = httpGet(pageUrl)
                        if (responseBody != null && responseBody.contains("<!-- Usage of azlyrics.com content")) break
                    }
                }
            }

            if (responseBody != null) {
                Log.d(TAG, "AZLyrics full HTML response: ${responseBody.take(1500)}")
                val startTag = "<!-- Usage of azlyrics.com content by any third-party lyrics provider is prohibited by our licensing agreement. Sorry about that. -->"
                val startIdx = responseBody.indexOf(startTag)
                Log.d(TAG, "AZLyrics startIdx=$startIdx, contains startTag=${responseBody.contains("Usage of azlyrics")}")
                if (startIdx != -1) {
                    val endIdx = responseBody.indexOf("</div>", startIdx + startTag.length)
                    if (endIdx != -1) {
                        val rawLyrics = responseBody.substring(startIdx + startTag.length, endIdx)
                        return "[source:azlyrics]\n${cleanHtml(rawLyrics)}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AZLyrics error", e)
        }
        return null
    }

    // --- 5. LYRICSMINT (Direct port of Flutter) ---
    private fun fetchFromLyricsMint(trackName: String, artistName: String): String? {
        try {
            val cleanT = sanitize(trackName)
            val cleanA = sanitize(artistName)
            val queries = listOf(
                "$cleanA $cleanT",
                "$artistName $trackName",
                cleanT
            ).distinct()

            var pageUrl: String? = null
            for (q in queries) {
                pageUrl = searchUrlOnWeb("lyricsmint.com", q)
                if (pageUrl != null) break
            }

            if (pageUrl == null) return null

            Log.d(TAG, "Fetching LyricsMINT page: $pageUrl")
            val responseBody = httpGet(pageUrl) ?: return null

            val lyricRegex = Regex("""<div[^>]*class="[^"]*text-base[^"]*lg:text-lg[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val lyricMatch = lyricRegex.find(responseBody)
            if (lyricMatch != null) {
                val content = lyricMatch.groupValues[1]
                    .replace(Regex("<p>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
                    .trim()
                return "[source:lyricsmint]\n${cleanHtml(content)}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "LyricsMINT error", e)
        }
        return null
    }

    // --- 6. LYRICFIND (Schema.org JSON-LD + HTML Fallback) ---
    private fun fetchFromLyricFind(trackName: String, artistName: String): String? {
        try {
            val cleanT = sanitize(trackName)
            val cleanA = sanitize(artistName)
            val queries = listOf(
                "$cleanA $cleanT",
                "$artistName $trackName",
                cleanT
            ).distinct()

            var pageUrl: String? = null
            for (q in queries) {
                pageUrl = searchUrlOnWeb("lyricfind.com", q)
                if (pageUrl != null) break
            }

            if (pageUrl == null) return null

            Log.d(TAG, "Fetching LyricFind page: $pageUrl")
            val html = httpGet(pageUrl) ?: return null

            // 1. JSON-LD
            val jsonLdRegex = Regex("""<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            for (match in jsonLdRegex.findAll(html)) {
                val jsonStr = match.groupValues[1].trim()
                try {
                    if (jsonStr.startsWith("{")) {
                        val json = JSONObject(jsonStr)
                        val text = extractLyricsFromJsonLd(json)
                        if (!text.isNullOrBlank()) {
                            return "[source:lyricfind]\n${cleanHtml(text)}"
                        }
                    } else if (jsonStr.startsWith("[")) {
                        val array = JSONArray(jsonStr)
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val text = extractLyricsFromJsonLd(obj)
                            if (!text.isNullOrBlank()) {
                                return "[source:lyricfind]\n${cleanHtml(text)}"
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. HTML Fallback
            val containerPatterns = listOf(
                Regex("""<div[^>]*class="[^"]*lyrics-text[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL),
                Regex("""<div[^>]*class="[^"]*lyrics[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL),
                Regex("""<div[^>]*data-testid="lyrics"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL),
                Regex("""<p[^>]*class="[^"]*lyrics-text[^"]*"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
            )

            for (pattern in containerPatterns) {
                val match = pattern.find(html)
                if (match != null) {
                    val raw = cleanHtml(match.groupValues[1])
                    if (raw.length > 20) {
                        return "[source:lyricfind]\n$raw"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LyricFind error", e)
        }
        return null
    }

    private fun extractLyricsFromJsonLd(json: JSONObject): String? {
        val lyricsObj = json.opt("lyrics")
        if (lyricsObj is String && lyricsObj.isNotBlank()) return lyricsObj
        if (lyricsObj is JSONObject) {
            val text = lyricsObj.optString("text", "")
            if (text.isNotBlank()) return text
        }
        val textProp = json.optString("text", "")
        if (textProp.isNotBlank()) return textProp
        return null
    }

    suspend fun prefetchAllMissingLyrics() = withContext(Dispatchers.IO) {
        val songsToFetch = libraryDao.getSongsWithMissingLyrics()
        Log.d(TAG, "Prefetching offline lyrics for ${songsToFetch.size} songs")
        for (song in songsToFetch) {
            fetchLyrics(song)
            val internetEnabled = settingsRepository.enableInternet.first()
            if (internetEnabled) {
                kotlinx.coroutines.delay(250)
            }
        }
    }
}
