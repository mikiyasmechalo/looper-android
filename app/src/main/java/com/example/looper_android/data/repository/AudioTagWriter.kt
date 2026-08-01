package com.example.looper_android.data.repository

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
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

sealed interface WriteResult {
    object Success : WriteResult
    data class RequiresPermission(
        val intentSender: IntentSender,
        val pendingPathOrUri: String,
        val pendingEdit: AudioTagWriter.TagEdit
    ) : WriteResult
    data class Failure(val error: Throwable) : WriteResult
}

@Singleton
class AudioTagWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safPermissionRepo: SafPermissionRepository
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
     * Writes tag edits physically to file.
     * Uses persistent SAF directory tree if available, or direct file write.
     * Falls back to MediaStore.createWriteRequest system permission prompt on API 30+.
     */
    suspend fun write(path: String, edit: TagEdit): WriteResult = withContext(Dispatchers.IO) {
        // PATH 1: SAF Document URI directly (content://)
        if (path.startsWith("content://")) {
            return@withContext writeViaUri(Uri.parse(path), path, edit)
        }

        // PATH 2: Check if file path is covered by any granted SAF tree folder
        val treeUri = safPermissionRepo.isPathOrUriCovered(path)
        if (treeUri != null) {
            val docUri = safPermissionRepo.buildDocumentUriFromPath(treeUri, path)
            if (docUri != null) {
                Log.d(TAG, "Writing via SAF tree document URI for path: $path")
                val safResult = writeViaUri(docUri, path, edit)
                if (safResult is WriteResult.Success) {
                    refreshMediaStore(path)
                    return@withContext safResult
                }
            }
        }

        // PATH 3: Direct file system write (pre-Android 10 or writable storage)
        val file = File(path)
        if (file.exists()) {
            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tagOrCreateAndSetDefault
                applyTagEdit(tag, edit)
                AudioFileIO.write(audioFile)
                refreshMediaStore(path)
                return@withContext WriteResult.Success
            } catch (e: Exception) {
                Log.w(TAG, "Direct write failed for $path (${e.javaClass.simpleName}): ${e.message}")
            }
        }

        // PATH 4: Resolve MediaStore content URI and write via stream (will trigger single-file dialog if out of scope)
        val contentUri = getUriFromPath(path)
        if (contentUri != null) {
            val res = writeViaUri(contentUri, path, edit)
            if (res !is WriteResult.Failure) return@withContext res
        }

        // Fallback: If MediaStore resolution or stream write failed on API 30+, build write request for content URI
        if (contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            buildCreateWriteRequest(listOf(contentUri), path, edit)?.let {
                return@withContext it
            }
        }

        Log.e(TAG, "No write path available for: $path")
        WriteResult.Failure(IllegalStateException("Cannot write tags: file not accessible"))
    }

    suspend fun write(uri: Uri, edit: TagEdit): WriteResult = withContext(Dispatchers.IO) {
        writeViaUri(uri, uri.toString(), edit)
    }

    private suspend fun writeViaUri(uri: Uri, pathForFallback: String, edit: TagEdit): WriteResult =
        withContext(Dispatchers.IO) {
            val ext = resolveAudioExtension(uri)
            val tempFile = File(context.cacheDir, "tag_write_${System.currentTimeMillis()}.$ext")
            try {
                // 1. Copy original → temp file
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: run {
                    Log.w(TAG, "Cannot open input stream for $uri. Requesting write permission.")
                    val contentUri = if (uri.scheme == "content" && uri.authority == "media") uri else getUriFromPath(pathForFallback)
                    if (contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        return@withContext buildCreateWriteRequest(listOf(contentUri), pathForFallback, edit)
                            ?: WriteResult.Failure(IllegalStateException("Cannot open input stream for $uri"))
                    }
                    return@withContext WriteResult.Failure(IllegalStateException("Cannot open input stream for $uri"))
                }

                // 2. Apply edits on temp file
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                applyTagEdit(tag, edit)
                AudioFileIO.write(audioFile)

                // 3. Write back via content resolver (mode "rwt" = read-write-truncate)
                context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                } ?: run {
                    Log.w(TAG, "Cannot open output stream for $uri. Requesting write permission.")
                    val contentUri = if (uri.scheme == "content" && uri.authority == "media") uri else getUriFromPath(pathForFallback)
                    if (contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        return@withContext buildCreateWriteRequest(listOf(contentUri), pathForFallback, edit)
                            ?: WriteResult.Failure(IllegalStateException("Cannot open output stream for $uri"))
                    }
                    return@withContext WriteResult.Failure(IllegalStateException("Cannot open output stream for $uri"))
                }

                WriteResult.Success

            } catch (e: RecoverableSecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WriteResult.RequiresPermission(
                        intentSender = e.userAction.actionIntent.intentSender,
                        pendingPathOrUri = pathForFallback,
                        pendingEdit = edit
                    )
                } else {
                    WriteResult.Failure(e)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception writing to $uri (${e.javaClass.simpleName}): ${e.message}")
                val contentUri = if (uri.scheme == "content" && uri.authority == "media") uri else getUriFromPath(pathForFallback)
                if (contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    buildCreateWriteRequest(listOf(contentUri), pathForFallback, edit)
                        ?: WriteResult.Failure(e)
                } else {
                    WriteResult.Failure(e)
                }
            } finally {
                tempFile.delete()
            }
        }

    private fun buildCreateWriteRequest(
        uris: List<Uri>,
        pathOrUri: String,
        edit: TagEdit
    ): WriteResult.RequiresPermission? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pi: PendingIntent = MediaStore.createWriteRequest(
                    context.contentResolver, uris
                )
                WriteResult.RequiresPermission(
                    intentSender = pi.intentSender,
                    pendingPathOrUri = pathOrUri,
                    pendingEdit = edit
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build createWriteRequest", e)
                null
            }
        } else null
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
        try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            val ext = docFile?.name?.substringAfterLast('.', "")?.lowercase()
            if (!ext.isNullOrBlank() && ext in VALID_AUDIO_EXTENSIONS) {
                return ext
            }
        } catch (_: Exception) {}

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

        return "mp3"
    }

    private fun getUriFromPath(path: String): Uri? {
        if (path.isBlank()) return null
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
                        ContentUris.withAppendedId(
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