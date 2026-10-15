package com.example.looper_android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lightweight projection of SongEntity used for fast diffing during MediaStore scan.
 * Only includes fields needed to detect changes and preserve user data.
 */
data class SongDiffData(
    val path: String,
    val id: Long,
    val artPath: String?,
    val albumArtist: String?,
    val lyrics: String?,
    val genre: String?,
    val playCount: Int,
    val lastPlayed: Long?,
    val isFavorite: Boolean,
    val hasCustomEqualizer: Boolean,
    val equalizerGains: String?,
    val year: Int?
)

@Entity(tableName = "queues")
data class QueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val currentItemIndex: Int = 0,
    val currentPlaybackPositionMs: Long = 0,
    val shuffleState: Boolean = false,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val isPermanent: Boolean = false
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val queueId: Long,
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String? = null,
    val positionInQueue: Int
)

@Entity(
    tableName = "songs",
    indices = [
        androidx.room.Index(value = ["path"], unique = true),
        androidx.room.Index(value = ["title"]),
        androidx.room.Index(value = ["artist"]),
        androidx.room.Index(value = ["albumArtist"]),
        androidx.room.Index(value = ["album"]),
        androidx.room.Index(value = ["dateAdded"]),
        androidx.room.Index(value = ["lastPlayed"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val title: String,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val genre: String?,
    val duration: Int?, // milliseconds
    val trackNumber: Int?,
    val year: Int?,
    val artPath: String?,
    val dateAdded: Long, // timestamp
    val playCount: Int = 0,
    val lastPlayed: Long? = null,
    val isFavorite: Boolean = false,
    val lyrics: String? = null,
    val hasCustomEqualizer: Boolean = false,
    val equalizerGains: String? = null // Stored as JSON string or comma-separated
)

@Entity(
    tableName = "albums",
    indices = [androidx.room.Index(value = ["name"], unique = true)]
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val artist: String?,
    val artPath: String?,
    val year: Int?,
    val dateAdded: Long
)

@Entity(
    tableName = "artists",
    indices = [androidx.room.Index(value = ["name"], unique = true)]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val artPath: String?,
    val artistImageUrl: String?
)

@Entity(
    tableName = "playlists",
    indices = [androidx.room.Index(value = ["name"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val songPaths: String, // Stored as JSON string or comma-separated
    val dateCreated: Long,
    val dateModified: Long
)

