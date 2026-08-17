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
    fun provideCustomEqualizerAudioProcessor(): com.example.looper_android.playback.equalizer.dsp.CustomEqualizerAudioProcessor {
        return com.example.looper_android.playback.equalizer.dsp.CustomEqualizerAudioProcessor()
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        equalizerAudioProcessor: com.example.looper_android.playback.equalizer.dsp.CustomEqualizerAudioProcessor
    ): Player {
        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(true)
                    .setAudioProcessors(arrayOf(equalizerAudioProcessor))
                    .build()
            }
        }
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 15_000,
                /* maxBufferMs */ 50_000,
                /* bufferForPlaybackMs */ 2_500,
                /* bufferForPlaybackAfterRebufferMs */ 5_000
            )
            .build()
        return ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
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
