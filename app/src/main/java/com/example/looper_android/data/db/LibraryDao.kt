package com.example.looper_android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // --- Songs ---
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongsCount(): Int

    @Query("SELECT id, path, title, artist, albumArtist, album, genre, duration, trackNumber, year, artPath, dateAdded, playCount, lastPlayed, isFavorite, NULL as lyrics, hasCustomEqualizer, equalizerGains FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT path FROM songs")
    fun getAllSongPathsFlow(): Flow<List<String>>

    @Query("SELECT id, path, title, artist, albumArtist, album, genre, duration, trackNumber, year, artPath, dateAdded, playCount, lastPlayed, isFavorite, NULL as lyrics, hasCustomEqualizer, equalizerGains FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE path = :path LIMIT 1")
    suspend fun getSongByPath(path: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    fun getSongById(id: Long): Flow<SongEntity?>

    @Query("SELECT path, id, artPath, albumArtist, lyrics, genre, playCount, lastPlayed, isFavorite, hasCustomEqualizer, equalizerGains, year FROM songs")
    suspend fun getAllSongsForDiff(): List<SongDiffData>

    @Query("SELECT lyrics FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongLyricsById(id: Long): String?

    @Query("SELECT lyrics FROM songs WHERE path = :path LIMIT 1")
    suspend fun getSongLyricsByPath(path: String): String?

    @Query("SELECT * FROM songs WHERE artPath IS NULL OR artPath = ''")
    suspend fun getSongsWithMissingArt(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET lyrics = :lyrics WHERE id = :id")
    suspend fun updateSongLyrics(id: Long, lyrics: String)

    @Update
    suspend fun updateSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :lastPlayed WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, lastPlayed: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET lastPlayed = NULL WHERE lastPlayed IS NOT NULL")
    suspend fun clearHistory()

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>): Int

    @Query("DELETE FROM songs WHERE path LIKE :pathPrefix || '%'")
    suspend fun deleteSongsByPathPrefix(pathPrefix: String)

    @Query("DELETE FROM songs WHERE path LIKE 'content://%'")
    suspend fun deleteContentUriSongs(): Int

    @Query("DELETE FROM songs")
    suspend fun clearSongs()

    // --- Albums ---
    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE name = :albumName")
    suspend fun deleteAlbumByName(albumName: String)

    @Query("UPDATE albums SET name = :newAlbumName, artist = :newArtist, artPath = :newArtPath WHERE name = :oldAlbumName")
    suspend fun updateAlbumDetails(oldAlbumName: String, newAlbumName: String, newArtist: String?, newArtPath: String?)

    @Query("DELETE FROM albums")
    suspend fun clearAlbums()

    // --- Artists ---
    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists WHERE name = :artistName COLLATE NOCASE")
    suspend fun deleteArtistByName(artistName: String)

    @Query("DELETE FROM artists")
    suspend fun clearArtists()

    @Query("UPDATE artists SET artistImageUrl = :imageUrl WHERE name = :name COLLATE NOCASE")
    suspend fun updateArtistImageUrl(name: String, imageUrl: String?)

    @Query("UPDATE artists SET name = :newName WHERE name = :oldName COLLATE NOCASE")
    suspend fun updateArtistName(oldName: String, newName: String)

    @Query("UPDATE songs SET artist = :newName WHERE artist = :oldName COLLATE NOCASE")
    suspend fun updateSongArtistByArtistName(oldName: String, newName: String)

    @Query("SELECT * FROM artists WHERE artistImageUrl IS NULL OR artistImageUrl = ''")
    suspend fun getArtistsWithMissingImage(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE artistImageUrl IS NOT NULL AND artistImageUrl != ''")
    suspend fun getArtistsWithImages(): List<ArtistEntity>

    @Query("SELECT * FROM songs WHERE lyrics IS NULL OR lyrics = ''")
    suspend fun getSongsWithMissingLyrics(): List<SongEntity>

    // --- Playlists ---
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("UPDATE playlists SET name = :newName WHERE id = :id")
    suspend fun renamePlaylist(id: Long, newName: String)
}