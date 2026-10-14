package com.example.looper_android.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class LyricsCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LyricsCache"
    }

    private val cacheDir: File
        get() = File(context.filesDir, "lyrics_cache").apply {
            if (!exists()) mkdirs()
        }

    private val byProviderDir: File
        get() = File(cacheDir, "by_provider").apply {
            if (!exists()) mkdirs()
        }

    fun generateKey(artist: String?, title: String?): String {
        val cleanArtist = (artist ?: "unknown").trim().lowercase()
        val cleanTitle = (title ?: "unknown").trim().lowercase()
        val input = "${cleanArtist}_${cleanTitle}"
        val safeName = input.replace(Regex("""[^a-z0-9_]"""), "_")
        return "${safeName}_${input.hashCode().absoluteValue}"
    }

    fun save(artist: String?, title: String?, lrc: String) {
        if (lrc.isBlank()) return
        try {
            val key = generateKey(artist, title)
            val file = File(cacheDir, "$key.lrc")
            file.writeText(lrc)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save lyrics cache for $artist - $title", e)
        }
    }

    fun get(artist: String?, title: String?): String? {
        return try {
            val key = generateKey(artist, title)
            val file = File(cacheDir, "$key.lrc")
            if (file.exists() && file.length() > 0) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read lyrics cache for $artist - $title", e)
            null
        }
    }

    fun saveProvider(artist: String?, title: String?, provider: String, lrc: String) {
        if (lrc.isBlank()) return
        try {
            val key = generateKey(artist, title)
            val norm = provider.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            val file = File(byProviderDir, "$key.$norm.lrc")
            file.writeText(lrc)

            // Also save as current active lyrics file if not blank
            save(artist, title, lrc)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save provider lyrics cache for $provider", e)
        }
    }

    fun getProvider(artist: String?, title: String?, provider: String): String? {
        return try {
            val key = generateKey(artist, title)
            val norm = provider.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            val file = File(byProviderDir, "$key.$norm.lrc")
            if (file.exists() && file.length() > 0) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasProvider(artist: String?, title: String?, provider: String): Boolean {
        return try {
            val key = generateKey(artist, title)
            val norm = provider.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            val file = File(byProviderDir, "$key.$norm.lrc")
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }
}
