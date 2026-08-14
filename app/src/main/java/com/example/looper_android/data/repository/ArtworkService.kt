package com.example.looper_android.data.repository

import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkService @Inject constructor(
    private val libraryDao: LibraryDao,
    private val settingsRepository: SettingsRepository
) {
    private val iTunesSearchUrl = "https://itunes.apple.com/search"
    private val deezerSearchUrl = "https://api.deezer.com/search/artist"

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
            val urlString = "$iTunesSearchUrl?term=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=1&media=music"
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
                    var imageUrl = result.optString("artworkUrl100", null)
                    if (imageUrl != null) {
                        imageUrl = imageUrl.replace("100x100bb", "600x600bb")
                        
                        // Instead of downloading manually, we update the DB with the URL.
                        // Coil will handle the downloading and caching automatically in the UI.
                        val updatedSong = song.copy(artPath = imageUrl)
                        libraryDao.updateSong(updatedSong)
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getArtistImageUrl(artistName: String): String? = withContext(Dispatchers.IO) {
        val enableInternet = settingsRepository.enableInternet.first()
        if (!enableInternet) return@withContext null
        if (artistName.isBlank() || artistName.equals("unknown artist", ignoreCase = true)) return@withContext null

        try {
            val urlString = "$deezerSearchUrl?q=${java.net.URLEncoder.encode(artistName, "UTF-8")}&limit=1"
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
                    val imageUrl = result.optString("picture_big", result.optString("picture_medium", null))
                    connection.disconnect()
                    if (imageUrl != null) {
                        android.util.Log.d("ArtworkService", "Found Deezer artist image for $artistName: $imageUrl")
                        libraryDao.updateArtistImageUrl(artistName, imageUrl)
                    }
                    return@withContext imageUrl
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            android.util.Log.e("ArtworkService", "Failed to fetch artist image for $artistName: ${e.message}")
        }
        return@withContext null
    }

    suspend fun fetchAndCacheArtistImage(artistName: String): String? {
        return getArtistImageUrl(artistName)
    }

    suspend fun downloadAllMissingArtistImages() = withContext(Dispatchers.IO) {
        val enableInternet = settingsRepository.enableInternet.first()
        if (!enableInternet) return@withContext

        val missingArtists = libraryDao.getArtistsWithMissingImage()
        android.util.Log.d("ArtworkService", "Fetching artist images for ${missingArtists.size} missing artists")
        for (artist in missingArtists) {
            val currentEnabled = settingsRepository.enableInternet.first()
            if (!currentEnabled) break
            getArtistImageUrl(artist.name)
            kotlinx.coroutines.delay(350)
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
            kotlinx.coroutines.delay(350) // Politeness delay to avoid rate limits
        }

        downloadAllMissingArtistImages()
    }
}
