package com.example.looper_android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ArtworkService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val settingsRepository: SettingsRepository
) {
    private val iTunesSearchUrl = "https://itunes.apple.com/search"
    private val deezerSearchUrl = "https://api.deezer.com/search/artist"

    private val collaborationSplitRegex = Regex(
        """\s+(?:ft\.?|feat\.?|featuring|vs\.?|with|and|&|\+|\/|x)\s+|,\s*""",
        RegexOption.IGNORE_CASE
    )

    fun splitArtistNames(artistString: String): List<String> {
        val splits = artistString
            .split(collaborationSplitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("unknown artist", ignoreCase = true) && !it.equals("unknown", ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .take(4)
        return if (splits.isEmpty() && artistString.isNotBlank() && !artistString.equals("unknown artist", ignoreCase = true)) {
            listOf(artistString.trim())
        } else {
            splits
        }
    }

    /**
     * Placeholder / Generic Avatar Detection Algorithm.
     * Decodes image into 64x64 buffer and calculates unique colors and Manhattan distance transitions.
     * Threshold: uniqueColors.size < 400 && adjDiff < 8.0 -> Generic silhouette placeholder.
     */
    private fun isGenericImage(bytes: ByteArray): Boolean {
        return try {
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return true
            val scaled = Bitmap.createScaledBitmap(original, 64, 64, true)
            if (scaled != original) {
                original.recycle()
            }

            val width = scaled.width
            val height = scaled.height
            val totalPixels = width * height
            val pixels = IntArray(totalPixels)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            scaled.recycle()

            val uniqueColors = HashSet<Int>(4096)
            var totalDiff = 0L

            for (i in 0 until totalPixels) {
                val color = pixels[i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                uniqueColors.add((r shl 16) or (g shl 8) or b)

                if (i > 0) {
                    val prevColor = pixels[i - 1]
                    val prevR = (prevColor shr 16) and 0xFF
                    val prevG = (prevColor shr 8) and 0xFF
                    val prevB = prevColor and 0xFF
                    val diff = abs(r - prevR) + abs(g - prevG) + abs(b - prevB)
                    totalDiff += diff
                }
            }

            val adjDiff = if (totalPixels > 1) totalDiff.toDouble() / (totalPixels - 1) else 0.0
            val isGeneric = uniqueColors.size < 400 && adjDiff < 8.0
            Log.d("ArtworkService", "Entropy analysis: uniqueColors=${uniqueColors.size}, adjDiff=$adjDiff -> isGeneric=$isGeneric")
            isGeneric
        } catch (e: Exception) {
            Log.e("ArtworkService", "Error analyzing image entropy", e)
            false
        }
    }

    private fun downloadImageBytes(urlString: String): ByteArray? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LooperMusicPlayer/1.0 (Android)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode == 200) {
                val bytes = connection.inputStream.use { it.readBytes() }
                connection.disconnect()
                bytes
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e("ArtworkService", "Failed to download image bytes: ${e.message}")
            null
        }
    }

    private fun saveArtistImageToDisk(artistName: String, bytes: ByteArray): String? {
        return try {
            val artistDir = File(context.filesDir, "artist_images")
            if (!artistDir.exists()) artistDir.mkdirs()
            val safeName = artistName.replace(Regex("""[^\w\s]+"""), "").trim().replace("\\s+".toRegex(), "_")
            val file = File(artistDir, "$safeName.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ArtworkService", "Failed to save artist image to disk: ${e.message}")
            null
        }
    }

    private data class ArtistFetchResult(
        val remoteUrl: String?,
        val localPath: String?,
        val isGeneric: Boolean
    )

    private suspend fun fetchSingleSubArtist(name: String): ArtistFetchResult = withContext(Dispatchers.IO) {
        val safeName = name.replace(Regex("""[^\w\s]+"""), "").trim().replace("\\s+".toRegex(), "_")
        val cachedFile = File(File(context.filesDir, "artist_images"), "$safeName.jpg")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext ArtistFetchResult(
                remoteUrl = null,
                localPath = cachedFile.absolutePath,
                isGeneric = false
            )
        }

        try {
            val urlString = "$deezerSearchUrl?q=${URLEncoder.encode(name, "UTF-8")}&limit=1"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LooperMusicPlayer/1.0 (Android)")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val data = JSONObject(response)
                val results = data.optJSONArray("data")
                if (results != null && results.length() > 0) {
                    val result = results.getJSONObject(0)
                    val bigPic = result.optString("picture_big", "")
                    val medPic = result.optString("picture_medium", "")
                    val imageUrl = bigPic.ifBlank { medPic }.ifBlank { null }
                    connection.disconnect()
                    if (imageUrl != null) {
                        val bytes = downloadImageBytes(imageUrl)
                        if (bytes != null && bytes.isNotEmpty()) {
                            val generic = isGenericImage(bytes)
                            if (!generic) {
                                val savedPath = saveArtistImageToDisk(name, bytes) ?: imageUrl
                                return@withContext ArtistFetchResult(
                                    remoteUrl = imageUrl,
                                    localPath = savedPath,
                                    isGeneric = false
                                )
                            } else {
                                return@withContext ArtistFetchResult(
                                    remoteUrl = imageUrl,
                                    localPath = null,
                                    isGeneric = true
                                )
                            }
                        }
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("ArtworkService", "Failed single artist fetch for $name: ${e.message}")
        }
        ArtistFetchResult(remoteUrl = null, localPath = null, isGeneric = true)
    }

    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        val enableInternet = settingsRepository.enableInternet.first()
        if (!enableInternet) return@withContext null
        if (artistName.isBlank() || artistName.equals("unknown artist", ignoreCase = true) || artistName.equals("unknown", ignoreCase = true)) return@withContext null

        val subArtists = splitArtistNames(artistName)
        if (subArtists.isEmpty()) return@withContext null

        val realPhotos = mutableListOf<String>()
        var fallbackGenericUrl: String? = null

        for (i in subArtists.indices) {
            val currentEnabled = settingsRepository.enableInternet.first()
            if (!currentEnabled) break

            val subName = subArtists[i]
            val res = fetchSingleSubArtist(subName)
            if (!res.isGeneric && res.localPath != null) {
                realPhotos.add(res.localPath)
            } else if (res.remoteUrl != null && fallbackGenericUrl == null) {
                fallbackGenericUrl = res.remoteUrl
            }

            // Enforce 2.0-second delay between network queries as specified
            if (i < subArtists.size - 1) {
                kotlinx.coroutines.delay(2000)
            }
        }

        val resultValue: String? = when {
            realPhotos.isNotEmpty() -> {
                if (realPhotos.size == 1) {
                    realPhotos[0]
                } else {
                    val jsonArray = JSONArray(realPhotos)
                    jsonArray.toString()
                }
            }
            fallbackGenericUrl != null -> {
                fallbackGenericUrl
            }
            else -> null
        }

        if (resultValue != null) {
            Log.d("ArtworkService", "Saved artist images for '$artistName': $resultValue (count=${realPhotos.size})")
            libraryDao.updateArtistImageUrl(artistName, resultValue)
        }

        return@withContext resultValue
    }

    suspend fun fetchAndCacheArtistImage(artistName: String): String? {
        return getArtistImageUrl(artistName)
    }

    private val isDownloadingMissingArtists = java.util.concurrent.atomic.AtomicBoolean(false)

    suspend fun downloadAllMissingArtistImages() = withContext(Dispatchers.IO) {
        if (!isDownloadingMissingArtists.compareAndSet(false, true)) {
            Log.d("ArtworkService", "downloadAllMissingArtistImages is already running, skipping concurrent trigger")
            return@withContext
        }
        try {
            val enableInternet = settingsRepository.enableInternet.first()
            if (!enableInternet) return@withContext

            val missingArtists = libraryDao.getArtistsWithMissingImage()
            Log.d("ArtworkService", "Fetching artist images for ${missingArtists.size} missing artists")
            for (artist in missingArtists) {
                val currentEnabled = settingsRepository.enableInternet.first()
                if (!currentEnabled) break
                getArtistImageUrl(artist.name)
                kotlinx.coroutines.delay(2000) // Rate limit delay of 2.0s
            }
        } finally {
            isDownloadingMissingArtists.set(false)
        }
    }

    suspend fun downloadArtworkForSong(song: SongEntity) = withContext(Dispatchers.IO) {
        val enableInternet = settingsRepository.enableInternet.first()
        val downloadArtwork = settingsRepository.downloadArtwork.first()
        if (!enableInternet || !downloadArtwork) return@withContext
        if (song.title.isBlank() || song.title.equals("track", ignoreCase = true)) return@withContext

        // Smart Title Cleanup
        var cleanTitle = song.title.replace(Regex("\\s*[\\(\\[][^\\]\\)]*(video|audio|lyrics|official|hq|hd|remastered|feat\\.|ft\\.)[^\\]\\)]*[\\)\\]]", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.replace("_", " ").replace("-", " ")
        cleanTitle = cleanTitle.replace(Regex("\\s+"), " ").trim()

        val query = if (song.artist.isNullOrBlank() || song.artist.equals("unknown artist", ignoreCase = true) || song.artist.equals("unknown", ignoreCase = true)) {
            cleanTitle
        } else {
            "${song.artist} $cleanTitle"
        }

        try {
            val urlString = "$iTunesSearchUrl?term=${URLEncoder.encode(query, "UTF-8")}&limit=1&media=music"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val data = JSONObject(response)
                val results = data.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val result = results.getJSONObject(0)
                    var imageUrl: String? = result.optString("artworkUrl100", "").ifBlank { null }
                    if (imageUrl != null) {
                        imageUrl = imageUrl.replace("100x100bb", "600x600bb")
                        val updatedSong = song.copy(artPath = imageUrl)
                        libraryDao.updateSong(updatedSong)
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("ArtworkService", "Failed song artwork download for ${song.title}", e)
        }
    }

    suspend fun downloadAllMissingArtworks() = withContext(Dispatchers.IO) {
        val enableInternet = settingsRepository.enableInternet.first()
        val downloadArtwork = settingsRepository.downloadArtwork.first()
        if (!enableInternet || !downloadArtwork) return@withContext

        val missingArtSongs = libraryDao.getSongsWithMissingArt()
        for (song in missingArtSongs) {
            val currentEnabled = settingsRepository.enableInternet.first() && settingsRepository.downloadArtwork.first()
            if (!currentEnabled) break
            downloadArtworkForSong(song)
            kotlinx.coroutines.delay(350)
        }

        downloadAllMissingArtistImages()
    }
}
