package com.example.looper_android.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTagWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioTagWriter"
        private val VALID_AUDIO_EXTENSIONS = setOf("mp3", "flac", "m4a", "aac", "ogg", "wav", "mp4", "wma", "aiff")
    }

    data class TagEdit(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val albumArtist: String? = null,
        val year: Int? = null,
        val genre: String? = null,
        val lyrics: String? = null,
        val trackNumber: Int? = null
    )

    /**
     * For MediaStore files where you have a direct filesystem path.
     * Includes fallback to MediaStore Content Uri if Scoped Storage blocks direct File writing.
     */
    suspend fun write(path: String, edit: TagEdit): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist at path: $path")
            return@withContext false
        }

        // Try direct file system write first
        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            applyTagEdit(tag, edit)
            AudioFileIO.write(audioFile)

            refreshMediaStore(path)
            return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "Direct file write failed for path $path: ${e.message}. Attempting MediaStore URI fallback.")
        }

        // Scoped Storage fallback: query MediaStore Content Uri for path and write via stream
        val contentUri = getUriFromPath(path)
        if (contentUri != null) {
            return@withContext write(contentUri, edit)
        }

        Log.e(TAG, "Failed to write tags for path: $path and no ContentUri fallback available.")
        false
    }

    /**
     * For SAF DocumentFile URIs or MediaStore Content URIs (Android 10+ scoped storage).
     * Streams to a temp file, edits, then streams back.
     */
    suspend fun write(uri: Uri, edit: TagEdit): Boolean = withContext(Dispatchers.IO) {
        val ext = resolveAudioExtension(uri)
        val tempFile = File(context.cacheDir, "tag_write_${System.currentTimeMillis()}.$ext")

        try {
            // 1. Copy original to temp
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return@withContext false
            }

            // 2. Edit temp file
            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault

            applyTagEdit(tag, edit)
            AudioFileIO.write(audioFile)

            // 3. Copy temp back to original URI
            val mode = if (uri.scheme == "content") "rwt" else "w"
            context.contentResolver.openOutputStream(uri, mode)?.use { output ->
                tempFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                Log.e(TAG, "Failed to open output stream for URI: $uri")
                return@withContext false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing tags to URI $uri", e)
            false
        } finally {
            tempFile.delete()
        }
    }

    private fun applyTagEdit(tag: Tag, edit: TagEdit) {
        edit.title?.let { tag.setOrDeleteField(FieldKey.TITLE, it) }
        edit.artist?.let { tag.setOrDeleteField(FieldKey.ARTIST, it) }
        edit.album?.let { tag.setOrDeleteField(FieldKey.ALBUM, it) }
        edit.albumArtist?.let { tag.setOrDeleteField(FieldKey.ALBUM_ARTIST, it) }
        edit.year?.let { tag.setOrDeleteField(FieldKey.YEAR, if (it > 0) it.toString() else null) }
        edit.genre?.let { tag.setOrDeleteField(FieldKey.GENRE, it) }
        edit.lyrics?.let { tag.setOrDeleteField(FieldKey.LYRICS, it) }
        edit.trackNumber?.let { tag.setOrDeleteField(FieldKey.TRACK, if (it > 0) it.toString() else null) }
    }

    private fun Tag.setOrDeleteField(key: FieldKey, value: String?) {
        if (value.isNullOrBlank()) {
            try {
                deleteField(key)
            } catch (_: Exception) {}
        } else {
            try {
                setField(key, value)
            } catch (_: Exception) {
                try {
                    val field = createField(key, value)
                    setField(field)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set tag field $key with value $value", e)
                }
            }
        }
    }

    private fun resolveAudioExtension(uri: Uri): String {
        // 1. Try DocumentFile name
        try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val ext = docFile?.name?.substringAfterLast('.', "")?.lowercase()
            if (!ext.isNullOrBlank() && ext in VALID_AUDIO_EXTENSIONS) {
                return ext
            }
        } catch (_: Exception) {}

        // 2. Try OpenableColumns.DISPLAY_NAME query
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val displayName = cursor.getString(nameIndex)
                        val ext = displayName?.substringAfterLast('.', "")?.lowercase()
                        if (!ext.isNullOrBlank() && ext in VALID_AUDIO_EXTENSIONS) {
                            return ext
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Check MIME type
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

        // 4. Try last path segment
        val lastPath = uri.lastPathSegment
        if (lastPath != null) {
            val ext = lastPath.substringAfterLast('.', "").lowercase()
            if (ext in VALID_AUDIO_EXTENSIONS) {
                return ext
            }
        }

        return "mp3"
    }

    private fun getUriFromPath(path: String): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DATA} = ?"
        val selectionArgs = arrayOf(path)
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    if (idColumn != -1) {
                        val id = cursor.getLong(idColumn)
                        android.content.ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve ContentUri for path: $path", e)
            null
        }
    }

    private fun refreshMediaStore(path: String) {
        val file = File(path)
        if (file.exists()) {
            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        }
    }
}