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
        save(0L, artist, title, lrc)
    }

    fun save(songId: Long, artist: String?, title: String?, lrc: String) {
        if (lrc.isBlank()) return
        try {
            if (songId > 0L) {
                val idFile = File(cacheDir, "id_$songId.lrc")
                idFile.writeText(lrc)
            }
            val key = generateKey(artist, title)
            val file = File(cacheDir, "$key.lrc")
            file.writeText(lrc)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save lyrics cache for id=$songId, $artist - $title", e)
        }
    }

    fun get(artist: String?, title: String?): String? {
        return get(0L, artist, title)
    }

    fun get(songId: Long, artist: String?, title: String?): String? {
        try {
            if (songId > 0L) {
                val idFile = File(cacheDir, "id_$songId.lrc")
                if (idFile.exists() && idFile.length() > 0) {
                    return idFile.readText()
                }
            }
            val key = generateKey(artist, title)
            val file = File(cacheDir, "$key.lrc")
            if (file.exists() && file.length() > 0) {
                return file.readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read lyrics cache for id=$songId, $artist - $title", e)
        }
        return null
    }

    fun saveProvider(artist: String?, title: String?, provider: String, lrc: String) {
        saveProvider(0L, artist, title, provider, lrc)
    }

    fun saveProvider(songId: Long, artist: String?, title: String?, provider: String, lrc: String) {
        if (lrc.isBlank()) return
        try {
            val norm = provider.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            if (songId > 0L) {
                val idFile = File(byProviderDir, "id_$songId.$norm.lrc")
                idFile.writeText(lrc)
            }
            val key = generateKey(artist, title)
            val file = File(byProviderDir, "$key.$norm.lrc")
            file.writeText(lrc)

            // Also save as current active lyrics file if not blank
            save(songId, artist, title, lrc)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save provider lyrics cache for $provider (id=$songId)", e)
        }
    }

    fun getProvider(artist: String?, title: String?, provider: String): String? {
        return getProvider(0L, artist, title, provider)
    }

    fun getProvider(songId: Long, artist: String?, title: String?, provider: String): String? {
        return try {
            val norm = provider.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            if (songId > 0L) {
                val idFile = File(byProviderDir, "id_$songId.$norm.lrc")
                if (idFile.exists() && idFile.length() > 0) {
                    return idFile.readText()
                }
            }
            val key = generateKey(artist, title)
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
        return hasProvider(0L, artist, title, provider)
    }

    fun hasProvider(songId: Long, artist: String?, title: String?, provider: String): Boolean {
        return try {
            val norm = provider.lowercase().replace(Regex("""[^a-z0-9]"""), "_")
            if (songId > 0L) {
                val idFile = File(byProviderDir, "id_$songId.$norm.lrc")
                if (idFile.exists() && idFile.length() > 0) return true
            }
            val key = generateKey(artist, title)
            val file = File(byProviderDir, "$key.$norm.lrc")
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    fun getAllProviderLyrics(artist: String?, title: String?): List<String> {
        return getAllProviderLyrics(0L, artist, title)
    }

    fun getAllProviderLyrics(songId: Long, artist: String?, title: String?): List<String> {
        val list = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        try {
            if (songId > 0L) {
                val idMain = File(cacheDir, "id_$songId.lrc")
                if (idMain.exists() && idMain.length() > 0) {
                    val txt = idMain.readText()
                    if (seen.add(txt)) list.add(txt)
                }
                val idFiles = byProviderDir.listFiles { _, name -> name.startsWith("id_$songId.") && name.endsWith(".lrc") }
                if (idFiles != null) {
                    for (f in idFiles) {
                        if (f.exists() && f.length() > 0) {
                            val txt = f.readText()
                            if (seen.add(txt)) list.add(txt)
                        }
                    }
                }
            }

            val key = generateKey(artist, title)
            val mainFile = File(cacheDir, "$key.lrc")
            if (mainFile.exists() && mainFile.length() > 0) {
                val txt = mainFile.readText()
                if (seen.add(txt)) list.add(txt)
            }
            val files = byProviderDir.listFiles { _, name -> name.startsWith("$key.") && name.endsWith(".lrc") }
            if (files != null) {
                for (f in files) {
                    if (f.exists() && f.length() > 0) {
                        val txt = f.readText()
                        if (seen.add(txt)) list.add(txt)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read provider lyrics for id=$songId, $artist - $title", e)
        }
        return list
    }
}
