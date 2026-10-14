package com.example.looper_android.data.repository

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.lyrics.providers.AmllClient
import com.example.looper_android.data.lyrics.providers.KuGouClient
import com.example.looper_android.data.lyrics.providers.LrcRedClient
import com.example.looper_android.data.lyrics.providers.LyricifyClient
import com.example.looper_android.data.lyrics.providers.LyricsPlusClient
import com.example.looper_android.data.lyrics.providers.LyricsProviderInfo
import com.example.looper_android.data.lyrics.providers.MusixmatchClient
import com.example.looper_android.data.lyrics.providers.NetEaseClient
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsFetcher @Inject constructor(
    private val libraryDao: LibraryDao,
    private val audioTagReader: AudioTagReader,
    private val settingsRepository: SettingsRepository,
    private val lyricsCache: LyricsCache
) {
    companion object {
        private const val TAG = "LyricsFetcher"

        val ALL_ONLINE_PROVIDERS = LyricsProviderInfo.ALL.map { it.id }

        val ALL_PROVIDERS = ALL_ONLINE_PROVIDERS + listOf(
            "Local",
            "Embedded"
        )
    }

    // In-memory per-song source cache (songId -> Map of normalized provider names -> lyrics)
    private val sessionLyricsCache = ConcurrentHashMap<Long, ConcurrentHashMap<String, String>>()
    private val upgradingSongs = ConcurrentHashMap<Long, Boolean>()

    fun isLyricsSynced(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return Regex("""\[\d{1,3}\s*:\s*\d{1,2}""").containsMatchIn(raw)
    }

    fun hasWordTiming(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return raw.contains("\n[w:") || Regex("""<\d{1,3}\s*:\s*\d{1,2}""").containsMatchIn(raw)
    }

    fun levelOf(raw: String?): Int {
        if (raw.isNullOrBlank() || raw.contains("[source:not_found]")) return 0
        if (!isLyricsSynced(raw)) return 1
        if (!hasWordTiming(raw)) return 2
        return 3
    }

    fun lyricsPriorityScore(lyrics: String?): Int {
        if (lyrics.isNullOrBlank() || lyrics.contains("[source:not_found]")) return 0
        val isUserPicked = lyrics.contains("[picked:user]")
        val isSynced = isLyricsSynced(lyrics)
        val hasWord = hasWordTiming(lyrics)

        return when {
            isUserPicked && hasWord -> 100
            isUserPicked && isSynced -> 90
            isUserPicked -> 80
            hasWord -> 50
            isSynced -> 40
            else -> 10
        }
    }

    fun selectBestLyrics(candidates: Iterable<String?>): String? {
        return candidates
            .filterNotNull()
            .filter { it.isNotBlank() && !it.contains("[source:not_found]") }
            .maxByOrNull { lyricsPriorityScore(it) }
    }

    fun canUpgrade(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        if (raw.contains("[picked:user]")) return false
        if (raw.contains("[source:custom]", ignoreCase = true)) return false
        if (raw.contains("[source:embedded]", ignoreCase = true)) return false
        if (raw.contains("[source:local", ignoreCase = true)) return false
        val level = levelOf(raw)
        return level == 1
    }

    fun getCachedLyricsSync(song: SongEntity): String? {
        val candidates = mutableListOf<String>()

        sessionLyricsCache[song.id]?.values?.let { candidates.addAll(it) }
        candidates.addAll(lyricsCache.getAllProviderLyrics(song.id, song.artist, song.title))
        lyricsCache.get(song.id, song.artist, song.title)?.let { candidates.add(it) }
        song.lyrics?.let { if (it.isNotBlank()) candidates.add(it) }

        val best = selectBestLyrics(candidates)
        if (best != null) {
            val src = extractLyricsSource(best).uppercase()
            cacheInMemory(song.id, src, best)
            return best
        }
        return null
    }

    private fun cacheInMemory(songId: Long, provider: String, lyrics: String) {
        sessionLyricsCache.computeIfAbsent(songId) { ConcurrentHashMap() }[provider.uppercase()] = lyrics
    }

    private suspend fun cacheInMemoryAndDb(song: SongEntity, provider: String, lyrics: String) {
        cacheInMemory(song.id, provider, lyrics)
        lyricsCache.saveProvider(song.id, song.artist, song.title, provider, lyrics)
        lyricsCache.save(song.id, song.artist, song.title, lyrics)
        if (song.id > 0L) {
            libraryDao.updateSongLyrics(song.id, lyrics)
        }
        if (song.path.isNotBlank()) {
            libraryDao.updateSongLyricsByPath(song.path, lyrics)
        }
    }

    private suspend fun getFromMemoryOrFetch(song: SongEntity, provider: String): String? {
        val cached = sessionLyricsCache[song.id]?.get(provider.uppercase())
        if (!cached.isNullOrEmpty()) return cached

        val disk = lyricsCache.getProvider(song.id, song.artist, song.title, provider)
        if (!disk.isNullOrEmpty()) {
            cacheInMemory(song.id, provider.uppercase(), disk)
            return disk
        }

        val fetched = fetchFromOnlineProvider(song, provider.uppercase())
        if (fetched != null) {
            cacheInMemory(song.id, provider.uppercase(), fetched)
            lyricsCache.saveProvider(song.id, song.artist, song.title, provider, fetched)
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

            if (url.contains("uddg=")) {
                val uddg = Uri.parse(url).getQueryParameter("uddg")
                if (uddg != null) {
                    val decoded = try { URLDecoder.decode(uddg, "UTF-8") } catch (_: Exception) { null }
                    if (decoded != null) url = decoded
                }
            }

            if (url.contains("/RU=")) {
                val ru = url.substringAfter("/RU=").substringBefore("/RK=").substringBefore("\"").substringBefore("'")
                val decodedRu = try { URLDecoder.decode(ru, "UTF-8") } catch (_: Exception) { null }
                if (decodedRu != null) url = decodedRu
            }

            if (url.contains("/url?q=")) {
                val gq = url.substringAfter("/url?q=").substringBefore("&")
                val decodedGq = try { URLDecoder.decode(gq, "UTF-8") } catch (_: Exception) { null }
                if (decodedGq != null) url = decodedGq
            }

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
                val response = httpGet(searchEngineUrl) ?: continue
                for (match in hrefRegex.findAll(response)) {
                    val candidate = extractCandidate(match.groupValues[1])
                    if (candidate != null) return candidate
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
        val sourceMatch = Regex("^\\[source:([a-zA-Z0-9_.-]+)\\]", RegexOption.IGNORE_CASE).find(firstLine)
        return if (sourceMatch != null) {
            val src = sourceMatch.groupValues[1].uppercase()
            LyricsProviderInfo.of(src)?.label ?: when (src) {
                "EMBEDDED" -> "Embedded"
                "LOCAL", "SIDECAR" -> "Local"
                "CUSTOM" -> "Custom LRC"
                else -> src.lowercase().replaceFirstChar { it.uppercase() }
            }
        } else {
            val hasTimestamps = Regex("""\[\d{1,3}\s*:\s*\d{1,2}""").containsMatchIn(rawLyrics)
            if (hasTimestamps) "Local" else "Plain Text"
        }
    }

    suspend fun fetchLyrics(song: SongEntity): String? = withContext(Dispatchers.IO) {
        // Priority 1: Check Local Synced Lyrics FIRST.
        // Rule: "make sure we only fetch lyrics from online if the local isn't synced."
        val embedded = tryEmbeddedLyrics(song)
        if (isLyricsSynced(embedded) && embedded != null) {
            cacheInMemoryAndDb(song, "EMBEDDED", embedded)
            return@withContext embedded
        }

        val sidecar = tryLocalSidecarLyrics(song)
        if (isLyricsSynced(sidecar) && sidecar != null) {
            cacheInMemoryAndDb(song, "LOCAL", sidecar)
            return@withContext sidecar
        }

        // Fast Check 1: In-memory session cache for USER-PICKED or SYNCED lyrics
        val inMemBest = selectBestLyrics(sessionLyricsCache[song.id]?.values ?: emptyList())
        if (inMemBest != null && (inMemBest.contains("[picked:user]") || isLyricsSynced(inMemBest))) {
            return@withContext inMemBest
        }

        // Fast Check 2: Persistent disk cache for USER-PICKED or SYNCED lyrics
        val diskCandidates = lyricsCache.getAllProviderLyrics(song.id, song.artist, song.title)
        val diskBest = selectBestLyrics(diskCandidates)
        if (diskBest != null && (diskBest.contains("[picked:user]") || isLyricsSynced(diskBest))) {
            val src = extractLyricsSource(diskBest).uppercase()
            cacheInMemory(song.id, src, diskBest)
            return@withContext diskBest
        }

        // Fast Check 3: Database cache for USER-PICKED or SYNCED lyrics
        val dbCached = (if (song.id > 0L) libraryDao.getSongLyricsById(song.id) else null)
            ?: libraryDao.getSongLyricsByPath(song.path)
        if (!dbCached.isNullOrEmpty() && (dbCached.contains("[picked:user]") || isLyricsSynced(dbCached))) {
            val src = extractLyricsSource(dbCached).uppercase()
            cacheInMemory(song.id, src, dbCached)
            lyricsCache.save(song.id, song.artist, song.title, dbCached)
            return@withContext dbCached
        }

        // Priority 2: Online Providers for SYNCED lyrics (Iterating user's prioritized order)
        var lineSyncedFallback: String? = null
        var onlineUnsyncedFallback: String? = null

        val enableInternet = settingsRepository.enableInternet.first()
        if (enableInternet) {
            val order = settingsRepository.lyricsProviderOrder.first()
            val off = settingsRepository.lyricsProvidersOff.first()
            val autoFallback = settingsRepository.autoFallbackLyricsProviders.first()

            val enabledProviders = order.filter { !off.contains(it) }

            for (provider in enabledProviders) {
                val lyrics = try {
                    getFromMemoryOrFetch(song, provider)
                } catch (e: Exception) {
                    Log.e(TAG, "Provider $provider failed in fetchLyrics loop", e)
                    null
                }
                if (lyrics != null) {
                    val level = levelOf(lyrics)
                    if (level >= 3) {
                        // Word- or letter-synced lyrics: best possible level, return immediately!
                        val src = extractLyricsSource(lyrics).uppercase()
                        cacheInMemoryAndDb(song, src, lyrics)
                        return@withContext lyrics
                    } else if (level == 2 && lineSyncedFallback == null) {
                        lineSyncedFallback = lyrics
                    } else if (level == 1 && onlineUnsyncedFallback == null) {
                        onlineUnsyncedFallback = lyrics
                    }
                }

                if (!autoFallback && (lineSyncedFallback != null || onlineUnsyncedFallback != null)) break
            }
        }

        // If we found line-synced lyrics online, save and return immediately
        if (lineSyncedFallback != null) {
            val src = extractLyricsSource(lineSyncedFallback).uppercase()
            cacheInMemoryAndDb(song, src, lineSyncedFallback)
            return@withContext lineSyncedFallback
        }

        // 4. Fallback to UNSYNCED lyrics if no synced lyrics could be found anywhere
        val unsyncedCandidate = selectBestLyrics(listOf(
            embedded,
            sidecar,
            dbCached,
            song.lyrics,
            onlineUnsyncedFallback,
            inMemBest,
            diskBest
        ))

        if (unsyncedCandidate != null) {
            val src = extractLyricsSource(unsyncedCandidate).uppercase()
            cacheInMemory(song.id, src, unsyncedCandidate)
            return@withContext unsyncedCandidate
        }

        return@withContext null
    }

    /**
     * Checks in the background if higher-level (word/letter-synced) lyrics exist
     * for a song that currently only has line-synced or plain-text lyrics.
     */
    suspend fun upgradeLyrics(song: SongEntity, current: String): String? = withContext(Dispatchers.IO) {
        if (!canUpgrade(current)) return@withContext null
        if (upgradingSongs.putIfAbsent(song.id, true) != null) return@withContext null

        try {
            val enableInternet = settingsRepository.enableInternet.first()
            if (!enableInternet) return@withContext null

            val currentLevel = levelOf(current)
            val order = settingsRepository.lyricsProviderOrder.first()
            val off = settingsRepository.lyricsProvidersOff.first()
            val candidates = order.filter { id ->
                !off.contains(id) && (LyricsProviderInfo.of(id)?.bestLevel ?: 2) > currentLevel
            }

            for (provider in candidates) {
                val candidateLyrics = try {
                    getFromMemoryOrFetch(song, provider)
                } catch (e: Exception) {
                    null
                } ?: continue

                val candidateLevel = levelOf(candidateLyrics)
                if (candidateLevel > currentLevel) {
                    val src = extractLyricsSource(candidateLyrics).uppercase()
                    cacheInMemoryAndDb(song, src, candidateLyrics)
                    return@withContext candidateLyrics
                }
            }
        } finally {
            upgradingSongs.remove(song.id)
        }
        return@withContext null
    }

    /**
     * Explicitly fetches lyrics from a specific provider (used by provider switcher in bottom sheet).
     */
    suspend fun fetchFromSpecificProvider(song: SongEntity, provider: String): String? = withContext(Dispatchers.IO) {
        val norm = provider.uppercase()
        val cached = sessionLyricsCache[song.id]?.get(norm)
            ?: lyricsCache.getProvider(song.id, song.artist, song.title, norm)
        if (!cached.isNullOrEmpty()) {
            val pinned = if (cached.contains("[picked:user]")) cached else "$cached\n[picked:user]"
            cacheInMemoryAndDb(song, norm, pinned)
            return@withContext pinned
        }

        val result = try {
            when (norm) {
                "EMBEDDED" -> tryEmbeddedLyrics(song)
                "LOCAL", "LOCAL SIDECAR" -> tryLocalSidecarLyrics(song)
                else -> {
                    val enableInternet = settingsRepository.enableInternet.first()
                    if (enableInternet) {
                        fetchFromOnlineProvider(song, norm)
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchFromSpecificProvider for $provider", e)
            null
        }

        if (result != null) {
            val pinned = if (result.contains("[picked:user]")) result else "$result\n[picked:user]"
            cacheInMemoryAndDb(song, norm, pinned)
            return@withContext pinned
        }
        null
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

    private suspend fun fetchFromOnlineProvider(song: SongEntity, provider: String): String? {
        return try {
            val title = song.title.trim()
            val artist = (song.artist ?: "Unknown Artist").trim()
            val durationSeconds = ((song.duration ?: 0) / 1000).toInt()

            when (provider.uppercase()) {
                "LRC.RED" -> LrcRedClient.fetch(title, artist, durationSeconds)?.let { "[source:LRC.RED]\n$it" }
                "LYRICSPLUS" -> LyricsPlusClient.fetch(title, artist, durationSeconds)?.let { "[source:LYRICSPLUS]\n$it" }
                "AMLL" -> AmllClient.fetch(title, artist, durationSeconds)?.let { "[source:AMLL]\n$it" }
                "NETEASE" -> NetEaseClient.fetch(title, artist, durationSeconds)?.let { "[source:NETEASE]\n$it" }
                "KUGOU" -> KuGouClient.fetch(title, artist, durationSeconds)?.let { "[source:KUGOU]\n$it" }
                "MUSIXMATCH" -> MusixmatchClient.fetch(title, artist, durationSeconds)?.let { "[source:MUSIXMATCH]\n$it" }
                "LYRICIFY" -> LyricifyClient.fetch(title, artist, durationSeconds)?.let { "[source:LYRICIFY]\n$it" }
                "LRCLIB" -> fetchFromLrcLib(song)
                "GENIUS" -> fetchFromGenius(title, artist)
                "AZLYRICS" -> fetchFromAZLyrics(title, artist)
                "LYRICSMINT" -> fetchFromLyricsMint(title, artist)
                "LYRICFIND" -> fetchFromLyricFind(title, artist)
                else -> fetchFromLrcLib(song)
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchFromOnlineProvider failed for $provider", e)
            null
        }
    }

    private fun JSONObject.getNullableString(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key, "").trim()
        return if (v.isEmpty() || v.equals("null", ignoreCase = true)) null else v
    }

    // --- LRCLIB ---
    private fun fetchFromLrcLib(song: SongEntity): String? {
        val trackName = song.title.trim()
        val artistName = (song.artist ?: "").trim()
        val durationSeconds = ((song.duration ?: 0) / 1000).toInt()

        val cleanTrack = sanitize(trackName)
        val cleanArtist = sanitize(artistName)

        var fallbackPlain: String? = null

        try {
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
                if (responseBody != null) {
                    val results = JSONArray(responseBody)
                    if (results.length() > 0) {
                        if (durationSeconds > 0) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val synced = item.getNullableString("syncedLyrics")
                                if (!synced.isNullOrBlank()) {
                                    val itemDur = item.optInt("duration", 0)
                                    if (Math.abs(itemDur - durationSeconds) < 15) {
                                        return "[source:lrclib]\n$synced"
                                    }
                                }
                            }
                        }

                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val synced = item.getNullableString("syncedLyrics")
                            if (!synced.isNullOrBlank()) {
                                return "[source:lrclib]\n$synced"
                            }
                        }

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
                return "[source:lrclib]\n$fallbackPlain"
            }
        } catch (e: Exception) {
            Log.e(TAG, "LRCLIB error", e)
        }
        return null
    }

    // --- GENIUS ---
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
                pageUrl = searchUrlOnWeb("genius.com", q)
                if (pageUrl != null) break
            }

            if (pageUrl == null) return null
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

    // --- AZLYRICS ---
    private fun fetchFromAZLyrics(trackName: String, artistName: String): String? {
        try {
            val cleanArtist = artistName.lowercase().replace(Regex("[^a-z0-9]"), "")
            val cleanTrack = trackName.lowercase().replace(Regex("[^a-z0-9]"), "")

            var pageUrl: String? = "https://www.azlyrics.com/lyrics/$cleanArtist/$cleanTrack.html"
            var responseBody = if (pageUrl != null) httpGet(pageUrl) else null

            if (responseBody == null || !responseBody.contains("<!-- Usage of azlyrics.com content")) {
                if (cleanTrack.length > 4 && cleanTrack.last().isDigit()) {
                    val trimmed = cleanTrack.dropLast(1)
                    pageUrl = "https://www.azlyrics.com/lyrics/$cleanArtist/$trimmed.html"
                    responseBody = httpGet(pageUrl)
                }
            }

            if (responseBody == null || !responseBody.contains("<!-- Usage of azlyrics.com content")) {
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
                val startTag = "<!-- Usage of azlyrics.com content by any third-party lyrics provider is prohibited by our licensing agreement. Sorry about that. -->"
                val startIdx = responseBody.indexOf(startTag)
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

    // --- LYRICSMINT ---
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

    // --- LYRICFIND ---
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
            val html = httpGet(pageUrl) ?: return null

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
        for (song in songsToFetch) {
            fetchLyrics(song)
            val internetEnabled = settingsRepository.enableInternet.first()
            if (internetEnabled) {
                kotlinx.coroutines.delay(250)
            }
        }
    }
}
