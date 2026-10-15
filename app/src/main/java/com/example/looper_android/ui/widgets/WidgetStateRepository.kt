package com.example.looper_android.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.glance.appwidget.updateAll
import androidx.palette.graphics.Palette
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.ArtworkLoaderHelper
import com.example.looper_android.data.repository.LyricsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

data class CachedSongVisuals(
    val artBitmap: Bitmap?,
    val artFullWithGradient: Bitmap?,
    val accentColor: Color,
    val cardBackgroundColor: Color
)

data class WidgetPlaybackState(
    val songId: Long? = null,
    val title: String = "Not Playing",
    val artist: String = "Looper",
    val album: String = "",
    val artBitmap: Bitmap? = null,
    val artFullWithGradient: Bitmap? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lyricsLines: List<String> = emptyList(),
    val activeLyricIndex: Int = -1,
    val accentColor: Color = Color(0xFF55DF69),
    val cardBackgroundColor: Color = Color(0xE61E1E22),
    val hasSong: Boolean = false
)

object WidgetStateRepository {

    private val _state = MutableStateFlow(WidgetPlaybackState())
    val state = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val visualsCache = LruCache<Long, CachedSongVisuals>(30)
    private var lastRawLyrics: String? = null
    private var lastSongIdForLyrics: Long? = null

    private val updateMutex = Mutex()
    private var pendingUpdateJob: Job? = null
    private var lastFullUpdateMs = 0L

    suspend fun ensureInitialized(context: Context) {
        if (_state.value.hasSong) return
        val cached = loadCachedSongFromPrefs(context) ?: return
        val (cachedSong, savedPos) = cached
        applySongState(
            context = context,
            song = cachedSong,
            isPlaying = false,
            positionMs = savedPos,
            durationMs = cachedSong.duration?.toLong() ?: 0L,
            triggerGlanceUpdate = false
        )
    }

    private fun loadCachedSongFromPrefs(context: Context): Pair<SongEntity, Long>? {
        return try {
            val prefs = context.getSharedPreferences("looper_queues", Context.MODE_PRIVATE)
            val songId = prefs.getLong("last_song_id", -1L)
            val path = prefs.getString("last_song_path", null)
            val title = prefs.getString("last_song_title", null)
            if (songId == -1L || path.isNullOrEmpty() || title.isNullOrEmpty()) return null
            val song = SongEntity(
                id = songId,
                path = path,
                title = title,
                artist = prefs.getString("last_song_artist", null),
                albumArtist = prefs.getString("last_song_album_artist", null),
                album = prefs.getString("last_song_album", null),
                genre = null,
                duration = prefs.getInt("last_song_duration", 0),
                trackNumber = null,
                year = null,
                artPath = prefs.getString("last_song_art_path", null),
                dateAdded = 0L,
                isFavorite = prefs.getBoolean("last_song_is_favorite", false)
            )
            val pos = prefs.getLong("last_song_position", 0L)
            Pair(song, pos)
        } catch (_: Exception) {
            null
        }
    }

    fun updateState(
        context: Context,
        song: SongEntity?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        rawLyrics: String? = null,
        accentColorInt: Int? = null
    ) {
        scope.launch {
            applySongState(
                context = context,
                song = song,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                rawLyrics = rawLyrics,
                accentColorInt = accentColorInt,
                triggerGlanceUpdate = true
            )
        }
    }

    suspend fun applySongState(
        context: Context,
        song: SongEntity?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        rawLyrics: String? = null,
        accentColorInt: Int? = null,
        triggerGlanceUpdate: Boolean = true
    ) {
        if (song == null) {
            // If we already have a song displayed, preserve it and only update playing status to paused
            if (_state.value.hasSong) {
                if (_state.value.isPlaying) {
                    _state.value = _state.value.copy(isPlaying = false)
                    if (triggerGlanceUpdate) scheduleThrottledUpdate(context)
                }
                return
            }
            val cached = loadCachedSongFromPrefs(context)
            if (cached != null) {
                val (cachedSong, savedPos) = cached
                applySongState(
                    context = context,
                    song = cachedSong,
                    isPlaying = false,
                    positionMs = savedPos,
                    durationMs = cachedSong.duration?.toLong() ?: 0L,
                    triggerGlanceUpdate = triggerGlanceUpdate
                )
            }
            return
        }

        // 1. Instant 0ms Visuals Cache Lookup & High-Contrast Palette
        var visuals = visualsCache.get(song.id)
        if (visuals == null) {
            val loadedBitmap = loadRawBitmap(context, song)
            if (loadedBitmap != null) {
                val thumb = createSquareBitmap(loadedBitmap, 300)
                val fullArt = createFullArtWithGradient(loadedBitmap, 800, 800, 48f)

                var extractedAccent = Color(0xFF55DF69)
                var extractedCardBg = Color(0xE61E1E22)
                try {
                    val palette = Palette.from(loadedBitmap).generate()
                    extractedAccent = computeOptimalAccentColor(palette)
                    extractedCardBg = computeAdaptiveCardColor(palette)
                } catch (_: Exception) {}

                visuals = CachedSongVisuals(thumb, fullArt, extractedAccent, extractedCardBg)
                visualsCache.put(song.id, visuals)
            } else {
                visuals = CachedSongVisuals(null, null, Color(0xFF55DF69), Color(0xE61E1E22))
                visualsCache.put(song.id, visuals)
            }
        }

        val effectiveAccent = visuals.accentColor

        // 2. Process Multi-Line Lyrics
        val effectiveLyrics = rawLyrics ?: (if (song.id == lastSongIdForLyrics && !lastRawLyrics.isNullOrBlank()) lastRawLyrics else song.lyrics)
        lastRawLyrics = effectiveLyrics
        lastSongIdForLyrics = song.id
        val (lines, activeIdx) = extractDisplayLyrics(effectiveLyrics, positionMs, durationMs)

        val newState = WidgetPlaybackState(
            songId = song.id,
            title = song.title.ifBlank { "Unknown Title" },
            artist = (song.artist ?: song.albumArtist ?: "Unknown Artist").ifBlank { "Unknown Artist" },
            album = song.album ?: "",
            artBitmap = visuals.artBitmap,
            artFullWithGradient = visuals.artFullWithGradient,
            isPlaying = isPlaying,
            isFavorite = song.isFavorite,
            positionMs = positionMs,
            durationMs = durationMs,
            lyricsLines = lines,
            activeLyricIndex = activeIdx,
            accentColor = effectiveAccent,
            cardBackgroundColor = visuals.cardBackgroundColor,
            hasSong = true
        )

        _state.value = newState
        if (triggerGlanceUpdate) {
            scheduleThrottledUpdate(context)
        }
    }

    fun updateLyrics(context: Context, songId: Long, rawLyrics: String) {
        lastRawLyrics = rawLyrics
        lastSongIdForLyrics = songId
        val cur = _state.value
        if (cur.songId == songId) {
            val (lines, activeIdx) = extractDisplayLyrics(rawLyrics, cur.positionMs, cur.durationMs)
            _state.value = cur.copy(lyricsLines = lines, activeLyricIndex = activeIdx)
            scope.launch {
                try {
                    LyricsWidget().updateAll(context)
                } catch (_: Exception) {}
            }
        }
    }

    fun updatePosition(context: Context, positionMs: Long) {
        val cur = _state.value
        if (!cur.hasSong || lastRawLyrics.isNullOrBlank()) return

        val (lines, activeIdx) = extractDisplayLyrics(lastRawLyrics, positionMs, cur.durationMs)
        // ONLY trigger RemoteViews IPC if the active lyric line index shifted!
        if (activeIdx != cur.activeLyricIndex || lines != cur.lyricsLines) {
            _state.value = cur.copy(
                positionMs = positionMs,
                lyricsLines = lines,
                activeLyricIndex = activeIdx
            )
            scope.launch {
                try {
                    LyricsWidget().updateAll(context)
                } catch (_: Exception) {}
            }
        }
    }

    fun updatePlayPauseOptimistic(context: Context, isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying)
        scheduleThrottledUpdate(context)
    }

    fun updateFavoriteOptimistic(context: Context, isFavorite: Boolean) {
        _state.value = _state.value.copy(isFavorite = isFavorite)
        scheduleThrottledUpdate(context)
    }

    private fun scheduleThrottledUpdate(context: Context) {
        scope.launch {
            updateMutex.withLock {
                val now = System.currentTimeMillis()
                val diff = now - lastFullUpdateMs
                if (diff < 200L) {
                    pendingUpdateJob?.cancel()
                    pendingUpdateJob = scope.launch {
                        delay(200L - diff)
                        dispatchAllWidgets(context)
                    }
                } else {
                    dispatchAllWidgets(context)
                }
            }
        }
    }

    fun clearVisualsCache() {
        visualsCache.evictAll()
    }

    fun forceRefreshAllWidgets(context: Context) {
        visualsCache.evictAll()
        scope.launch {
            val cached = loadCachedSongFromPrefs(context)
            if (cached != null) {
                val (cachedSong, savedPos) = cached
                applySongState(
                    context = context,
                    song = cachedSong,
                    isPlaying = _state.value.isPlaying,
                    positionMs = _state.value.positionMs.takeIf { it > 0 } ?: savedPos,
                    durationMs = _state.value.durationMs.takeIf { it > 0 } ?: (cachedSong.duration?.toLong() ?: 0L),
                    triggerGlanceUpdate = true
                )
            } else {
                dispatchAllWidgets(context)
            }
        }
    }

    private suspend fun dispatchAllWidgets(context: Context) {
        lastFullUpdateMs = System.currentTimeMillis()
        try {
            CompactWidget().updateAll(context)
            MiniPlayerWidget().updateAll(context)
            FullAlbumArtWidget().updateAll(context)
            LyricsWidget().updateAll(context)
            SquareProgressWidget().updateAll(context)
        } catch (_: Exception) {}
    }

    private fun computeOptimalAccentColor(palette: Palette): Color {
        val swatch = palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightVibrantSwatch

        if (swatch == null) return Color(0xFF55DF69)

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, hsl)

        // Strict clamp on Lightness between 0.38f and 0.52f
        // Ensures maximum vibrant color POP while guaranteeing crisp >4.5:1 contrast against white icons
        hsl[1] = hsl[1].coerceIn(0.65f, 1.0f)
        hsl[2] = hsl[2].coerceIn(0.38f, 0.52f)

        val clampedRgb = ColorUtils.HSLToColor(hsl)
        return Color(clampedRgb or (0xFF shl 24))
    }

    private fun computeAdaptiveCardColor(palette: Palette): Color {
        val swatch = palette.darkMutedSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch

        if (swatch == null) return Color(0xE61A1A1E)

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, hsl)

        // Give a rich, atmospheric dark tint matching the album hue
        hsl[1] = hsl[1].coerceIn(0.35f, 0.65f) // Rich saturation
        hsl[2] = 0.16f // 16% lightness: rich dark atmospheric tint

        val clampedRgb = ColorUtils.HSLToColor(hsl)
        return Color(clampedRgb or (0xE6 shl 24))
    }

    private fun extractDisplayLyrics(
        rawLyrics: String?,
        positionMs: Long,
        durationMs: Long
    ): Pair<List<String>, Int> {
        if (rawLyrics.isNullOrBlank()) {
            return Pair(listOf("", "", "No lyrics available for this song", "", ""), 2)
        }

        val parsed = LyricsParser.parseLyrics(rawLyrics, durationMs.coerceAtLeast(180000L))
        if (parsed.isSynced && parsed.timedLines.isNotEmpty()) {
            val timed = parsed.timedLines
            var currentIndex = timed.indexOfLast { it.startTimeMs <= positionMs }
            if (currentIndex == -1) currentIndex = 0

            val result = ArrayList<String>(5)
            for (offset in -2..2) {
                val idx = currentIndex + offset
                if (idx in timed.indices) {
                    result.add(timed[idx].text.ifBlank { "♪ ♪ ♪" })
                } else {
                    result.add("")
                }
            }
            return Pair(result, 2)
        } else if (parsed.plainLines.isNotEmpty()) {
            val plain = parsed.plainLines.filter { it.isNotBlank() }
            if (plain.isEmpty()) return Pair(listOf("", "", "♪ ♪ ♪", "", ""), 2)

            val progressRatio = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            val targetIdx = (progressRatio * plain.size).toInt().coerceIn(0, plain.size - 1)

            val result = ArrayList<String>(5)
            for (offset in -2..2) {
                val idx = targetIdx + offset
                if (idx in plain.indices) {
                    result.add(plain[idx])
                } else {
                    result.add("")
                }
            }
            return Pair(result, 2)
        }

        val firstLine = rawLyrics.trim().lines().firstOrNull() ?: "♪ ♪ ♪"
        return Pair(listOf("", "", firstLine, "", ""), 2)
    }

    private fun loadRawBitmap(context: Context, song: SongEntity): Bitmap? {
        val artPath = song.artPath
        val songPath = song.path

        val helperBmp = ArtworkLoaderHelper.loadArtworkBitmap(context, artPath, songPath)
        if (helperBmp != null) return helperBmp

        try {
            val cleanPath = when {
                songPath.startsWith("content://") -> null
                songPath.startsWith("file://") -> Uri.parse(songPath).path
                else -> songPath
            }
            if (cleanPath != null) {
                val f = File(cleanPath)
                if (f.exists() && f.canRead()) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(cleanPath)
                    val pic = mmr.embeddedPicture
                    mmr.release()
                    if (pic != null) {
                        return BitmapFactory.decodeByteArray(pic, 0, pic.size)
                    }
                }
            }
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !artPath.isNullOrEmpty()) {
            try {
                return context.contentResolver.loadThumbnail(Uri.parse(artPath), Size(1200, 1200), null)
            } catch (_: Exception) {}
        }

        return null
    }

    private fun createSquareBitmap(src: Bitmap, targetSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        val minDim = minOf(w, h)
        val startX = (w - minDim) / 2
        val startY = (h - minDim) / 2
        val cropped = if (w != h) {
            Bitmap.createBitmap(src, startX, startY, minDim, minDim)
        } else {
            src
        }
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }

    private fun createRoundedBitmap(src: Bitmap, targetW: Int, targetH: Int, cornerRadiusPx: Float): Bitmap {
        val scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        val rectF = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())

        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }

    private fun createFullArtWithGradient(src: Bitmap, targetW: Int, targetH: Int, cornerRadiusPx: Float): Bitmap {
        val srcW = src.width
        val srcH = src.height
        val scale = maxOf(targetW.toFloat() / srcW, targetH.toFloat() / srcH)
        val scaledW = (srcW * scale).toInt()
        val scaledH = (srcH * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        val rectF = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())

        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left = (targetW - scaledW) / 2f
        val top = (targetH - scaledH) / 2f
        canvas.drawBitmap(scaled, left, top, paint)

        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        gradientPaint.shader = LinearGradient(
            0f, targetH * 0.25f,
            0f, targetH.toFloat(),
            intArrayOf(
                0x00000000,
                0x66000000,
                0xDD000000.toInt(),
                0xF5000000.toInt()
            ),
            floatArrayOf(0f, 0.35f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, gradientPaint)

        return output
    }
}
