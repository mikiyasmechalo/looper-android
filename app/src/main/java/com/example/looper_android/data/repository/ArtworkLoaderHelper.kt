package com.example.looper_android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import java.io.ByteArrayOutputStream
import java.io.File

object ArtworkLoaderHelper {

    fun loadArtworkBitmap(context: Context, artPath: String?, audioPath: String? = null): Bitmap? {
        // 1. Try content resolver stream or local file
        if (!artPath.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(artPath)
                if (uri.scheme == "content") {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) return bmp
                    }
                } else if (uri.scheme == "file" || uri.scheme == null) {
                    val file = File(uri.path ?: artPath)
                    if (file.exists() && file.canRead()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) return bmp
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Android 10+ ContentResolver.loadThumbnail
        if (!artPath.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val uri = Uri.parse(artPath)
                if (uri.scheme == "content") {
                    val bmp = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Embedded ID3 Picture from audio file via MMR
        if (!audioPath.isNullOrEmpty()) {
            try {
                val cleanPath = when {
                    audioPath.startsWith("file://") -> Uri.parse(audioPath).path ?: audioPath
                    else -> audioPath
                }
                val file = File(cleanPath)
                if (file.exists() && file.canRead()) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(cleanPath)
                    val picBytes = mmr.embeddedPicture
                    mmr.release()
                    if (picBytes != null) {
                        val bmp = BitmapFactory.decodeByteArray(picBytes, 0, picBytes.size)
                        if (bmp != null) return bmp
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun loadArtworkBytes(context: Context, artPath: String?, audioPath: String? = null): ByteArray? {
        val bitmap = loadArtworkBitmap(context, artPath, audioPath) ?: return null
        val stream = ByteArrayOutputStream()
        // Scale down if massive to keep IPC parcel safe and snappy
        val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
            Bitmap.createScaledBitmap(bitmap, 512, 512, true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}
