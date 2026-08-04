package com.example.looper_android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        QueueEntity::class, 
        QueueItemEntity::class,
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        PlaylistEntity::class
    ], 
    version = 3, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun libraryDao(): LibraryDao
}
