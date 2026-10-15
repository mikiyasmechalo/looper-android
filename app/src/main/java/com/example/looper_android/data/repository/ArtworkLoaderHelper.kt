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
        val opts = BitmapFactory.Options().apply { inSampleSize = 1 }

        // 1. Try content resolver stream or local file
        if (!artPath.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(artPath)
                if (uri.scheme == "content") {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream, null, opts)
                        if (bmp != null) return bmp
                    }
                } else {
                    val path = uri.path ?: artPath
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
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
                    val bmp = context.contentResolver.loadThumbnail(uri, Size(800, 800), null)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Embedded ID3 Picture from audio file via MMR (supports content:// and file://)
        if (!audioPath.isNullOrEmpty()) {
            try {
                val mmr = MediaMetadataRetriever()
                val audioUri = Uri.parse(audioPath)
                if (audioUri.scheme == "content") {
                    mmr.setDataSource(context, audioUri)
                } else {
                    val cleanPath = audioUri.path ?: audioPath
                    mmr.setDataSource(cleanPath)
                }
                val picBytes = mmr.embeddedPicture
                mmr.release()
                if (picBytes != null && picBytes.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(picBytes, 0, picBytes.size, opts)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun loadArtworkBytes(context: Context, artPath: String?, audioPath: String? = null): ByteArray? {
        val bitmap = loadArtworkBitmap(context, artPath, audioPath) ?: return null
        val stream = ByteArrayOutputStream()
        val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
            Bitmap.createScaledBitmap(bitmap, 512, 512, true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}
