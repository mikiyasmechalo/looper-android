package com.example.looper_android.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafPermissionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("saf_permissions_prefs", Context.MODE_PRIVATE)

    private val _grantedTreeUris = kotlinx.coroutines.flow.MutableStateFlow(getGrantedTreeUris())
    val grantedTreeUrisFlow: kotlinx.coroutines.flow.StateFlow<List<Uri>> = _grantedTreeUris

    /**
     * Takes persistable read & write URI permission for a selected SAF tree Uri
     * and saves it to local persistence.
     */
    fun saveGrantedTreeUri(uri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)

            val currentSet = getGrantedTreeUriStrings().toMutableSet()
            currentSet.add(uri.toString())
            prefs.edit { putStringSet("granted_tree_uris", currentSet) }
            _grantedTreeUris.value = getGrantedTreeUris()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Releases persistable URI permission and removes it from storage.
     */
    fun removeGrantedTreeUri(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val currentSet = getGrantedTreeUriStrings().toMutableSet()
        currentSet.remove(uri.toString())
        prefs.edit { putStringSet("granted_tree_uris", currentSet) }
        _grantedTreeUris.value = getGrantedTreeUris()
    }

    fun getGrantedTreeUris(): List<Uri> {
        val persisted = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission || it.isWritePermission }
            .map { it.uri.toString() }
            .toSet()

        val saved = getGrantedTreeUriStrings()
        val valid = saved.filter { persisted.contains(it) || it.isNotEmpty() }
        return valid.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
    }

    private fun getGrantedTreeUriStrings(): Set<String> {
        return prefs.getStringSet("granted_tree_uris", emptySet()) ?: emptySet()
    }

    /**
     * Checks if a file path (or URI) falls under any granted SAF tree directory.
     * Returns the matching tree Uri if covered, or null.
     */
    fun isPathOrUriCovered(pathOrUri: String): Uri? {
        val grantedUris = getGrantedTreeUris()
        if (grantedUris.isEmpty()) return null

        for (treeUri in grantedUris) {
            val folderPath = getPathFromTreeUri(treeUri)
            if (folderPath != null && pathOrUri.startsWith(folderPath, ignoreCase = true)) {
                return treeUri
            }
            if (pathOrUri.startsWith(treeUri.toString(), ignoreCase = true)) {
                return treeUri
            }
        }
        return null
    }

    /**
     * Converts a file path under a granted SAF tree URI to a SAF Document URI.
     */
    fun buildDocumentUriFromPath(treeUri: Uri, path: String): Uri? {
        return try {
            if (path.startsWith("content://")) return Uri.parse(path)

            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val folderPath = getPathFromTreeUri(treeUri) ?: return null
            if (!path.startsWith(folderPath, ignoreCase = true)) return null

            val relativePath = path.substring(folderPath.length).trimStart('/')
            val parts = rootDocId.split(":")
            val storageType = parts[0]
            val rootRelative = parts.getOrElse(1) { "" }

            val docId = if (relativePath.isEmpty()) {
                rootDocId
            } else if (rootRelative.isEmpty()) {
                "$storageType:$relativePath"
            } else {
                "$storageType:$rootRelative/$relativePath"
            }
            DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts a SAF tree URI (e.g., content://com.android.externalstorage.documents/tree/primary%3AMusic)
     * to a filesystem path representation (e.g. /storage/emulated/0/Music).
     */
    fun getPathFromTreeUri(treeUri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parts = docId.split(":")
            if (parts.size >= 2) {
                val type = parts[0]
                val relativePath = parts[1]
                if ("primary".equals(type, ignoreCase = true)) {
                    "/storage/emulated/0/$relativePath"
                } else {
                    "/storage/$type/$relativePath"
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
