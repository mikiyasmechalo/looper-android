package com.example.looper_android.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTagReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class EmbeddedTags(
        val title: String?,
        val artist: String?,
        val album: String?,
        val albumArtist: String?,
        val durationMs: Int,
        val trackNumber: Int?,
        val year: Int?,
        val genre: String?,
        val lyrics: String?
    )

    suspend fun read(path: String): EmbeddedTags = withContext(Dispatchers.IO) {
        val jAudioTags = try {
            readInternal { AudioFileIO.read(File(path)) }
        } catch (_: Exception) {
            null
        }
        fallbackWithMediaMetadataRetriever(path, jAudioTags)
    }

    suspend fun read(uri: Uri): EmbeddedTags = withContext(Dispatchers.IO) {
        val jAudioTags = try {
            val ext = resolveAudioExtension(uri)
            val tempFile = File(context.cacheDir, "tag_read_${System.currentTimeMillis()}.$ext")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                readInternal { AudioFileIO.read(tempFile) }
            } finally {
                tempFile.delete()
            }
        } catch (_: Exception) {
            null
        }
        fallbackWithMediaMetadataRetriever(uri.toString(), jAudioTags)
    }

    private fun resolveAudioExtension(uri: Uri): String {
        val validExts = setOf("mp3", "flac", "m4a", "aac", "ogg", "wav", "mp4", "wma", "aiff")
        try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val ext = docFile?.name?.substringAfterLast('.', "")?.lowercase()
            if (!ext.isNullOrBlank() && ext in validExts) {
                return ext
            }
        } catch (_: Exception) {}

        try {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != null) {
                when {
                    mimeType.contains("mpeg") || mimeType.contains("mp3") -> return "mp3"
                    mimeType.contains("flac") -> return "flac"
                    mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("aac") -> return "m4a"
                    mimeType.contains("ogg") -> return "ogg"
                    mimeType.contains("wav") -> return "wav"
                }
            }
        } catch (_: Exception) {}

        val lastPath = uri.lastPathSegment
        if (lastPath != null) {
            val ext = lastPath.substringAfterLast('.', "").lowercase()
            if (ext in validExts) {
                return ext
            }
        }

        return "mp3"
    }

    private fun readInternal(openAudio: () -> org.jaudiotagger.audio.AudioFile): EmbeddedTags {
        val audioFile = openAudio()
        val tag: Tag? = audioFile.tag
        val header: AudioHeader = audioFile.audioHeader

        val trackStr = safeTag { tag?.getFirst(FieldKey.TRACK) }
        val trackNumber = trackStr?.substringBefore("/")?.toIntOrNull()

        // Try multiple year fields. Jaudiotagger maps FieldKey.YEAR differently per format,
        // but we also check raw IDs for edge cases.
        val yearCandidates = listOf(
            safeTag { tag?.getFirst(FieldKey.YEAR) },
            safeTag { tag?.getFirst("TDRC") },   // ID3v2.4 recording time
            safeTag { tag?.getFirst("TYER") },   // ID3v2.3 year
            safeTag { tag?.getFirst("TDRL") },   // ID3v2.4 release time
            safeTag { tag?.getFirst("DATE") }    // Vorbis comment fallback
        )
        val year = yearCandidates.firstNotNullOfOrNull { extractYear(it) }

        // Genre: raw string, no static ID3v1 lookup table.
        // If a file stores "(13)" we keep it as "(13)" or you can strip parens below.
        val genre = safeTag { tag?.getFirst(FieldKey.GENRE) }?.trim()

        // Lyrics: try standard LYRICS field, then common raw frame names.
        val lyrics = listOf(
            safeTag { tag?.getFirst(FieldKey.LYRICS) },
            safeTag { tag?.getFirst("USLT") },
            safeTag { tag?.getFirst("UNSYNCEDLYRICS") },
            safeTag { tag?.getFirst("SYLT") }
        ).firstNotNullOfOrNull { it?.trim()?.takeIf { s -> s.isNotBlank() } }

        return EmbeddedTags(
            title = safeTag { tag?.getFirst(FieldKey.TITLE) },
            artist = safeTag { tag?.getFirst(FieldKey.ARTIST) },
            album = safeTag { tag?.getFirst(FieldKey.ALBUM) },
            albumArtist = safeTag { tag?.getFirst(FieldKey.ALBUM_ARTIST) },
            durationMs = (header.trackLength * 1000).toInt(),
            trackNumber = trackNumber,
            year = year,
            genre = genre,
            lyrics = lyrics
        )
    }

    /** Runs a tag read inside a try-catch and returns null on any failure. */
    private inline fun safeTag(block: () -> String?): String? = try {
        block()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    /**
     * Extracts a reasonable 4-digit year from a raw date string.
     * Handles "2024", "2024-12-20", "12/20/2024", "20.12.2024", etc.
     */
    private fun extractYear(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null

        // Find every 4-digit sequence and return the first one that looks like a calendar year.
        Regex("""\d{4}""").findAll(raw).forEach { match ->
            val year = match.value.toIntOrNull() ?: return@forEach
            if (year in 1000..2100) return year
        }
        return null
    }

    private fun fallbackWithMediaMetadataRetriever(
        pathOrUri: String,
        existing: EmbeddedTags?
    ): EmbeddedTags {
        var title = existing?.title
        var artist = existing?.artist
        var album = existing?.album
        var albumArtist = existing?.albumArtist
        var durationMs = existing?.durationMs ?: 0
        var trackNumber = existing?.trackNumber
        var year = existing?.year
        var genre = existing?.genre
        var lyrics = existing?.lyrics

        val needsFallback = year == null || genre.isNullOrBlank() || lyrics.isNullOrBlank()
                || title == null || artist == null || album == null || durationMs <= 0

        if (!needsFallback) return existing!!

        try {
            val mmr = MediaMetadataRetriever()
            if (pathOrUri.startsWith("content://")) {
                mmr.setDataSource(context, Uri.parse(pathOrUri))
            } else {
                mmr.setDataSource(pathOrUri)
            }

            if (title == null) title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (artist == null) artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            if (album == null) album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            if (albumArtist == null) albumArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            if (durationMs <= 0) {
                durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
            }
            if (trackNumber == null) {
                trackNumber = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()
            }
            if (year == null) {
                val yrStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                year = extractYear(yrStr)
            }
            if (genre.isNullOrBlank()) {
                genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            }
            if (lyrics.isNullOrBlank()) {
                // 1000 == METADATA_KEY_LYRICS (hidden constant on some API levels)
                lyrics = mmr.extractMetadata(1000)
            }

            mmr.release()
        } catch (_: Exception) { }

        return EmbeddedTags(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            durationMs = durationMs,
            trackNumber = trackNumber,
            year = year,
            genre = genre,
            lyrics = lyrics
        )
    }
}