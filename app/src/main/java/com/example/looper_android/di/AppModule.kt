package com.example.looper_android.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.media3.common.Player

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ): Player {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): com.example.looper_android.data.db.AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.example.looper_android.data.db.AppDatabase::class.java,
            "looper_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideQueueDao(database: com.example.looper_android.data.db.AppDatabase): com.example.looper_android.data.db.QueueDao {
        return database.queueDao()
    }

    @Provides
    @Singleton
    fun provideLibraryDao(database: com.example.looper_android.data.db.AppDatabase): com.example.looper_android.data.db.LibraryDao {
        return database.libraryDao()
    }
}
