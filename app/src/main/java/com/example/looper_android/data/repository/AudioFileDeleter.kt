package com.example.looper_android.data.repository

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeleteResult {
    object Success : DeleteResult
    data class RequiresPermission(
        val intentSender: IntentSender,
        val pathOrUri: String
    ) : DeleteResult
    data class Failure(val error: Throwable) : DeleteResult
}

@Singleton
class AudioFileDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safPermissionRepo: SafPermissionRepository
) {
    companion object {
        private const val TAG = "AudioFileDeleter"
    }

    suspend fun delete(path: String): DeleteResult = withContext(Dispatchers.IO) {
        if (path.isBlank()) {
            return@withContext DeleteResult.Failure(IllegalArgumentException("Path is blank"))
        }

        // PATH 1: SAF Document URI directly (content://)
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            try {
                val docFile = DocumentFile.fromSingleUri(context, uri)
                if (docFile != null && docFile.exists() && docFile.delete()) {
                    Log.d(TAG, "Deleted via DocumentFile single URI: $path")
                    return@withContext DeleteResult.Success
                }
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    Log.d(TAG, "Deleted via ContentResolver: $path")
                    return@withContext DeleteResult.Success
                }
            } catch (e: RecoverableSecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return@withContext DeleteResult.RequiresPermission(
                        intentSender = e.userAction.actionIntent.intentSender,
                        pathOrUri = path
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting content URI: $path (${e.message})")
            }
        }

        // PATH 2: Check if file path is covered by any granted SAF tree folder
        val treeUri = safPermissionRepo.isPathOrUriCovered(path)
        if (treeUri != null) {
            val docUri = safPermissionRepo.buildDocumentUriFromPath(treeUri, path)
            if (docUri != null) {
                try {
                    val docFile = DocumentFile.fromSingleUri(context, docUri)
                    if (docFile != null && docFile.exists() && docFile.delete()) {
                        Log.d(TAG, "Deleted via SAF tree DocumentFile: $path")
                        refreshMediaStore(path)
                        return@withContext DeleteResult.Success
                    }
                    if (DocumentsContract.deleteDocument(context.contentResolver, docUri)) {
                        Log.d(TAG, "Deleted via DocumentsContract.deleteDocument: $path")
                        refreshMediaStore(path)
                        return@withContext DeleteResult.Success
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SAF tree deletion failed for $path: ${e.message}")
                }
            }
        }

        // PATH 3: Direct file system deletion (if app has write access / writable storage)
        val file = File(path)
        if (file.exists()) {
            try {
                if (file.delete()) {
                    Log.d(TAG, "Deleted directly via File.delete(): $path")
                    refreshMediaStore(path)
                    return@withContext DeleteResult.Success
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct file.delete() failed for $path: ${e.message}")
            }
        }

        // PATH 4: MediaStore Content URI deletion (Scoped Storage on Android 10 / 11+)
        val contentUri = getUriFromPath(path)
        if (contentUri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // On Android 11+, MediaStore.createDeleteRequest provides the system permission confirmation dialog
                buildCreateDeleteRequest(listOf(contentUri), path)?.let {
                    return@withContext it
                }
            } else {
                // Android 10 (API 29)
                try {
                    val rows = context.contentResolver.delete(contentUri, null, null)
                    if (rows > 0) {
                        return@withContext DeleteResult.Success
                    }
                } catch (e: RecoverableSecurityException) {
                    return@withContext DeleteResult.RequiresPermission(
                        intentSender = e.userAction.actionIntent.intentSender,
                        pathOrUri = path
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "ContentResolver delete failed for $contentUri: ${e.message}")
                }
            }
        }

        Log.e(TAG, "No physical deletion path succeeded for: $path")
        DeleteResult.Failure(IllegalStateException("Cannot delete file: permission denied or file inaccessible"))
    }

    private fun buildCreateDeleteRequest(
        uris: List<Uri>,
        pathOrUri: String
    ): DeleteResult.RequiresPermission? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pi: PendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver, uris
                )
                DeleteResult.RequiresPermission(
                    intentSender = pi.intentSender,
                    pathOrUri = pathOrUri
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build createDeleteRequest", e)
                null
            }
        } else null
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
        try {
            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        } catch (_: Exception) {}
    }
}
