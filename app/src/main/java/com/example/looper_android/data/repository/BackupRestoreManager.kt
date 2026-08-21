package com.example.looper_android.data.repository

import android.content.Context
import android.net.Uri
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.PlaylistEntity
import com.example.looper_android.data.db.QueueDao
import com.example.looper_android.data.db.QueueEntity
import com.example.looper_android.data.db.QueueItemEntity
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

data class BackupReport(
    val playlistCount: Int,
    val favoritesCount: Int,
    val customEqualizersCount: Int,
    val savedQueuesCount: Int,
    val timestamp: Long
)

data class RestoreReport(
    val playlistsRestored: Int,
    val favoritesRestored: Int,
    val statsRestored: Int,
    val queuesRestored: Int,
    val settingsRestored: Boolean
)

@Singleton
class BackupRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val queueDao: QueueDao,
    private val libraryCacheManager: LibraryCacheManager,
    private val settingsRepository: SettingsRepository
) {

    suspend fun createBackup(uri: Uri): Result<BackupReport> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            val now = System.currentTimeMillis()
            root.put("app", "looper_android")
            root.put("version", 1)
            root.put("timestamp", now)

            // 1. Playlists
            val allPlaylists = libraryDao.getAllPlaylists().firstOrNull() ?: emptyList()
            val playlistsArray = JSONArray()
            for (p in allPlaylists) {
                val pObj = JSONObject().apply {
                    put("name", p.name)
                    put("songPaths", p.songPaths)
                    put("dateCreated", p.dateCreated)
                    put("dateModified", p.dateModified)
                }
                playlistsArray.put(pObj)
            }
            root.put("playlists", playlistsArray)

            // 2. Song User Metadata (Favorites, playCount, lastPlayed, equalizerGains, lyrics)
            val allSongs = libraryCacheManager.songs.value.ifEmpty { libraryDao.getAllSongsList() }
            val favoritesArray = JSONArray()
            val songMetaArray = JSONArray()
            var eqCount = 0
            var favCount = 0

            for (song in allSongs) {
                if (song.isFavorite) {
                    favCount++
                    favoritesArray.put(JSONObject().apply {
                        put("path", song.path)
                        put("title", song.title)
                        put("artist", song.artist ?: "")
                        put("album", song.album ?: "")
                    })
                }

                if (song.playCount > 0 || song.lastPlayed != null || song.hasCustomEqualizer || !song.lyrics.isNullOrBlank()) {
                    if (song.hasCustomEqualizer) eqCount++
                    songMetaArray.put(JSONObject().apply {
                        put("path", song.path)
                        put("title", song.title)
                        put("artist", song.artist ?: "")
                        put("playCount", song.playCount)
                        put("lastPlayed", song.lastPlayed ?: 0L)
                        put("hasCustomEqualizer", song.hasCustomEqualizer)
                        put("equalizerGains", song.equalizerGains ?: "")
                        put("lyrics", song.lyrics ?: "")
                    })
                }
            }
            root.put("favorites", favoritesArray)
            root.put("songMetadata", songMetaArray)

            // 3. Saved Queues
            val allQueues = queueDao.getAllQueues().firstOrNull() ?: emptyList()
            val queuesArray = JSONArray()
            for (q in allQueues) {
                val qObj = JSONObject().apply {
                    put("name", q.name)
                    put("currentItemIndex", q.currentItemIndex)
                    put("currentPlaybackPositionMs", q.currentPlaybackPositionMs)
                    put("shuffleState", q.shuffleState)
                    put("lastPlayedAt", q.lastPlayedAt)
                    put("isPermanent", q.isPermanent)
                }
                val items = queueDao.getItemsForQueue(q.id).firstOrNull() ?: emptyList()
                val itemsArray = JSONArray()
                for (item in items) {
                    itemsArray.put(JSONObject().apply {
                        put("mediaId", item.mediaId)
                        put("title", item.title)
                        put("artist", item.artist)
                        put("album", item.album)
                        put("durationMs", item.durationMs)
                        put("uri", item.uri)
                        put("albumArtUri", item.albumArtUri ?: "")
                        put("positionInQueue", item.positionInQueue)
                    })
                }
                qObj.put("items", itemsArray)
                queuesArray.put(qObj)
            }
            root.put("queues", queuesArray)

            // 4. Settings / Preferences
            val settingsObj = JSONObject().apply {
                put("dynamicTheming", settingsRepository.dynamicTheming.first())
                put("disableBlur", settingsRepository.disableBlur.first())
                put("dynamicAccentColor", settingsRepository.dynamicAccentColor.first())
                put("darkTheme", settingsRepository.darkTheme.first())
                put("customAccentColor", settingsRepository.customAccentColor.first())
                put("dynamicLyrics", settingsRepository.dynamicLyrics.first())
                put("blurredArtworkForLyrics", settingsRepository.blurredArtworkForLyrics.first())
                put("dynamicColorActiveLyrics", settingsRepository.dynamicColorActiveLyrics.first())
                put("disableLyricFontScaling", settingsRepository.disableLyricFontScaling.first())
                put("lyricsAlignment", settingsRepository.lyricsAlignment.first())
                put("lyricsScreenDarkness", settingsRepository.lyricsScreenDarkness.first().toDouble())
                put("primaryLyricsProvider", settingsRepository.primaryLyricsProvider.first())
                put("autoFallbackLyricsProviders", settingsRepository.autoFallbackLyricsProviders.first())
                put("disableSquiggle", settingsRepository.disableSquiggle.first())
                put("disableAnimatedDuration", settingsRepository.disableAnimatedDuration.first())
                put("showQualityBadge", settingsRepository.showQualityBadge.first())
                put("enablePlayerGradient", settingsRepository.enablePlayerGradient.first())
                put("keepBackgroundGradient", settingsRepository.keepBackgroundGradient.first())
                put("autoPowerSaveEnabled", settingsRepository.autoPowerSaveEnabled.first())
                put("autoPowerSaveThreshold", settingsRepository.autoPowerSaveThreshold.first())
                put("repeatMode", settingsRepository.repeatMode.first())
                put("skipSilence", settingsRepository.skipSilence.first())
                put("appFontFamily", settingsRepository.appFontFamily.first())
                put("lyricsFontFamily", settingsRepository.lyricsFontFamily.first())
            }
            root.put("settings", settingsObj)

            // Write to OutputStream
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                    writer.flush()
                }
            } ?: throw IllegalStateException("Unable to open output stream for backup")

            BackupReport(
                playlistCount = allPlaylists.size,
                favoritesCount = favCount,
                customEqualizersCount = eqCount,
                savedQueuesCount = allQueues.size,
                timestamp = now
            )
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<RestoreReport> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: throw IllegalStateException("Unable to open backup file stream")

            val root = JSONObject(content)
            if (!root.has("app") || root.optString("app") != "looper_android") {
                // Also accept generic Looper backup
                if (!root.has("playlists") && !root.has("settings")) {
                    throw IllegalArgumentException("Invalid Looper backup archive format")
                }
            }

            var playlistsRestored = 0
            var favoritesRestored = 0
            var statsRestored = 0
            var queuesRestored = 0
            var settingsRestored = false

            val allSongs = libraryCacheManager.songs.value.ifEmpty { libraryDao.getAllSongsList() }
            val pathMap = allSongs.associateBy { it.path }
            val titleArtistMap = allSongs.groupBy { "${it.title.trim().lowercase()}###${(it.artist ?: "").trim().lowercase()}" }

            // 1. Restore Playlists
            val playlistsArray = root.optJSONArray("playlists")
            if (playlistsArray != null) {
                for (i in 0 until playlistsArray.length()) {
                    val pObj = playlistsArray.optJSONObject(i) ?: continue
                    val name = pObj.optString("name")
                    val rawPaths = pObj.optString("songPaths")
                    if (name.isNotBlank() && rawPaths.isNotBlank()) {
                        val parsedPaths = try {
                            val arr = JSONArray(rawPaths)
                            (0 until arr.length()).map { arr.getString(it) }
                        } catch (_: Exception) {
                            rawPaths.split(",").map { it.trim() }
                        }
                        libraryCacheManager.createPlaylist(name, parsedPaths)
                        playlistsRestored++
                    }
                }
            }

            // 2. Restore Favorites
            val favsArray = root.optJSONArray("favorites")
            if (favsArray != null) {
                for (i in 0 until favsArray.length()) {
                    val fObj = favsArray.optJSONObject(i) ?: continue
                    val path = fObj.optString("path")
                    val title = fObj.optString("title").trim().lowercase()
                    val artist = fObj.optString("artist").trim().lowercase()

                    val targetSong = pathMap[path] ?: titleArtistMap["$title###$artist"]?.firstOrNull()
                    if (targetSong != null && !targetSong.isFavorite) {
                        libraryDao.updateFavorite(targetSong.id, true)
                        favoritesRestored++
                    }
                }
            }

            // 3. Restore Song Metadata (Play Count, History, Equalizer, Lyrics)
            val metaArray = root.optJSONArray("songMetadata")
            if (metaArray != null) {
                for (i in 0 until metaArray.length()) {
                    val mObj = metaArray.optJSONObject(i) ?: continue
                    val path = mObj.optString("path")
                    val title = mObj.optString("title").trim().lowercase()
                    val artist = mObj.optString("artist").trim().lowercase()

                    val targetSong = pathMap[path] ?: titleArtistMap["$title###$artist"]?.firstOrNull()
                    if (targetSong != null) {
                        val playCount = mObj.optInt("playCount", targetSong.playCount)
                        val lastPlayed = if (mObj.has("lastPlayed")) mObj.optLong("lastPlayed") else targetSong.lastPlayed
                        val hasEq = mObj.optBoolean("hasCustomEqualizer", targetSong.hasCustomEqualizer)
                        val eqGains = mObj.optString("equalizerGains", targetSong.equalizerGains ?: "")
                        val lyrics = mObj.optString("lyrics", targetSong.lyrics ?: "")

                        val updated = targetSong.copy(
                            playCount = playCount,
                            lastPlayed = if (lastPlayed != 0L) lastPlayed else targetSong.lastPlayed,
                            hasCustomEqualizer = hasEq,
                            equalizerGains = eqGains.ifBlank { null },
                            lyrics = lyrics.ifBlank { null }
                        )
                        libraryDao.updateSong(updated)
                        statsRestored++
                    }
                }
            }

            // 4. Restore Saved Queues
            val queuesArray = root.optJSONArray("queues")
            if (queuesArray != null) {
                for (i in 0 until queuesArray.length()) {
                    val qObj = queuesArray.optJSONObject(i) ?: continue
                    val name = qObj.optString("name")
                    if (name.isBlank()) continue

                    val currentItemIndex = qObj.optInt("currentItemIndex", 0)
                    val currentPlaybackPositionMs = qObj.optLong("currentPlaybackPositionMs", 0L)
                    val shuffleState = qObj.optBoolean("shuffleState", false)
                    val lastPlayedAt = qObj.optLong("lastPlayedAt", System.currentTimeMillis())
                    val isPermanent = qObj.optBoolean("isPermanent", false)

                    val existing = queueDao.getQueueByName(name)
                    val queueId = existing?.id ?: queueDao.insertQueue(
                        QueueEntity(
                            name = name,
                            currentItemIndex = currentItemIndex,
                            currentPlaybackPositionMs = currentPlaybackPositionMs,
                            shuffleState = shuffleState,
                            lastPlayedAt = lastPlayedAt,
                            isPermanent = isPermanent
                        )
                    )

                    val itemsArray = qObj.optJSONArray("items")
                    if (itemsArray != null && itemsArray.length() > 0) {
                        queueDao.deleteQueueItems(queueId)
                        val itemsList = mutableListOf<QueueItemEntity>()
                        for (j in 0 until itemsArray.length()) {
                            val itmObj = itemsArray.optJSONObject(j) ?: continue
                            itemsList.add(
                                QueueItemEntity(
                                    queueId = queueId,
                                    mediaId = itmObj.optString("mediaId", "0"),
                                    title = itmObj.optString("title"),
                                    artist = itmObj.optString("artist"),
                                    album = itmObj.optString("album"),
                                    durationMs = itmObj.optLong("durationMs", 0L),
                                    uri = itmObj.optString("uri"),
                                    albumArtUri = itmObj.optString("albumArtUri").ifBlank { null },
                                    positionInQueue = itmObj.optInt("positionInQueue", j)
                                )
                            )
                        }
                        queueDao.insertQueueItems(itemsList)
                    }
                    queuesRestored++
                }
            }

            // 5. Restore Settings
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                if (settingsObj.has("dynamicTheming")) settingsRepository.setDynamicTheming(settingsObj.optBoolean("dynamicTheming"))
                if (settingsObj.has("disableBlur")) settingsRepository.setDisableBlur(settingsObj.optBoolean("disableBlur"))
                if (settingsObj.has("dynamicAccentColor")) settingsRepository.setDynamicAccentColor(settingsObj.optBoolean("dynamicAccentColor"))
                if (settingsObj.has("darkTheme")) settingsRepository.setDarkTheme(settingsObj.optBoolean("darkTheme"))
                if (settingsObj.has("customAccentColor")) settingsRepository.setCustomAccentColor(settingsObj.optInt("customAccentColor"))
                if (settingsObj.has("dynamicLyrics")) settingsRepository.setDynamicLyrics(settingsObj.optBoolean("dynamicLyrics"))
                if (settingsObj.has("blurredArtworkForLyrics")) settingsRepository.setBlurredArtworkForLyrics(settingsObj.optBoolean("blurredArtworkForLyrics"))
                if (settingsObj.has("dynamicColorActiveLyrics")) settingsRepository.setDynamicColorActiveLyrics(settingsObj.optBoolean("dynamicColorActiveLyrics"))
                if (settingsObj.has("disableLyricFontScaling")) settingsRepository.setDisableLyricFontScaling(settingsObj.optBoolean("disableLyricFontScaling"))
                if (settingsObj.has("lyricsAlignment")) settingsRepository.setLyricsAlignment(settingsObj.optString("lyricsAlignment"))
                if (settingsObj.has("lyricsScreenDarkness")) settingsRepository.setLyricsScreenDarkness(settingsObj.optDouble("lyricsScreenDarkness").toFloat())
                if (settingsObj.has("primaryLyricsProvider")) settingsRepository.setPrimaryLyricsProvider(settingsObj.optString("primaryLyricsProvider"))
                if (settingsObj.has("autoFallbackLyricsProviders")) settingsRepository.setAutoFallbackLyricsProviders(settingsObj.optBoolean("autoFallbackLyricsProviders"))
                if (settingsObj.has("disableSquiggle")) settingsRepository.setDisableSquiggle(settingsObj.optBoolean("disableSquiggle"))
                if (settingsObj.has("disableAnimatedDuration")) settingsRepository.setDisableAnimatedDuration(settingsObj.optBoolean("disableAnimatedDuration"))
                if (settingsObj.has("showQualityBadge")) settingsRepository.setShowQualityBadge(settingsObj.optBoolean("showQualityBadge"))
                if (settingsObj.has("enablePlayerGradient")) settingsRepository.setEnablePlayerGradient(settingsObj.optBoolean("enablePlayerGradient"))
                if (settingsObj.has("keepBackgroundGradient")) settingsRepository.setKeepBackgroundGradient(settingsObj.optBoolean("keepBackgroundGradient"))
                if (settingsObj.has("autoPowerSaveEnabled")) settingsRepository.setAutoPowerSaveEnabled(settingsObj.optBoolean("autoPowerSaveEnabled"))
                if (settingsObj.has("autoPowerSaveThreshold")) settingsRepository.setAutoPowerSaveThreshold(settingsObj.optInt("autoPowerSaveThreshold"))
                if (settingsObj.has("repeatMode")) settingsRepository.setRepeatMode(settingsObj.optString("repeatMode"))
                if (settingsObj.has("skipSilence")) settingsRepository.setSkipSilence(settingsObj.optBoolean("skipSilence"))
                if (settingsObj.has("appFontFamily")) settingsRepository.setAppFontFamily(settingsObj.optString("appFontFamily"))
                if (settingsObj.has("lyricsFontFamily")) settingsRepository.setLyricsFontFamily(settingsObj.optString("lyricsFontFamily"))
                settingsRestored = true
            }

            RestoreReport(
                playlistsRestored = playlistsRestored,
                favoritesRestored = favoritesRestored,
                statsRestored = statsRestored,
                queuesRestored = queuesRestored,
                settingsRestored = settingsRestored
            )
        }
    }
}
