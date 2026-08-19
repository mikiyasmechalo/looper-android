package com.example.looper_android.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ArtworkExportHelper {

    suspend fun saveImageToDownloads(
        context: Context,
        imageSource: String?,
        collectionName: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageSource.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }

        // Handle JSON array of URLs (Artist collage)
        val targetUrl = if (imageSource.startsWith("[") && imageSource.endsWith("]")) {
            try {
                val json = JSONArray(imageSource)
                if (json.length() > 0) json.optString(0) else null
            } catch (_: Exception) {
                imageSource
            }
        } else {
            imageSource
        }

        if (targetUrl.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }

        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(targetUrl)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result !is SuccessResult) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image for export", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to decode image", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            val safeName = collectionName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
            val fileName = "${safeName}_artwork_${System.currentTimeMillis()}.jpg"

            var success = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Looper")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        success = true
                    }
                }
            } else {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Looper"
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val outFile = File(downloadsDir, fileName)
                FileOutputStream(outFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    success = true
                }
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Saved to Downloads/Looper/$fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
