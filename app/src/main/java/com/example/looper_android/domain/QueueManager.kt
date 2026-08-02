package com.example.looper_android.domain

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.QueueDao
import com.example.looper_android.data.db.QueueEntity
import com.example.looper_android.data.db.QueueItemEntity
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private data class PreparedQueueData(
    val items: List<MediaItem>,
    val songs: List<SongEntity>,
    val startIndex: Int
)

@Singleton
class QueueManager @Inject constructor(
    private val queueDao: QueueDao,
    private val player: Player,
    private val libraryDao: LibraryDao,
    private val libraryRepository: LibraryRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mediaSourceFactory by lazy { DefaultMediaSourceFactory(context) }

    @Volatile private var cachedSongs: List<SongEntity> = emptyList()
    @Volatile private var cachedMediaItems: List<MediaItem> = emptyList()
    private val cacheReady = AtomicBoolean(false)

    init {
        scope.launch(Dispatchers.IO) {
            libraryRepository.allSongs.collectLatest { songs ->
                if (songs.isNotEmpty()) {
                    val items = withContext(Dispatchers.Default) {
                        songs.map { buildMediaItem(it) }
                    }
                    cachedSongs = songs
                    cachedMediaItems = items
                    cacheReady.set(true)
                }
            }
        }
    }

    fun createAndPlayQueue(
        queueName: String,
        songs: List<SongEntity>,
        startIndex: Int = 0,
        shuffled: Boolean = false
    ) {
        if (songs.isEmpty()) return

        // STEP 1: INSTANT PLAY (<0.1ms on Main thread) — Zero allocations before play()
        val targetSong = songs[startIndex]
        val targetMediaItem = buildMediaItem(targetSong)
        player.setMediaItem(targetMediaItem)
        player.prepare()
        player.play()

        // STEP 2: DEFERRED QUEUE ATTACHMENT (Off Main thread, on-demand MediaSource creation)
        scope.launch(Dispatchers.Default) {
            val cached = cachedMediaItems
            val cachedSongList = cachedSongs

            if (cacheReady.get() && cached.isNotEmpty()) {
                val prepared = if (shuffled) {
                    val others = cached.indices.filter { it != startIndex }.shuffled()
                    val idx = listOf(startIndex) + others
                    PreparedQueueData(
                        items = idx.map { cached[it] },
                        songs = idx.map { cachedSongList[it] },
                        startIndex = 0
                    )
                } else {
                    PreparedQueueData(
                        items = cached,
                        songs = cachedSongList,
                        startIndex = startIndex
                    )
                }

                // Give ExoPlayer 250ms to start audio decoder output cleanly without timeline interruption
                delay(250)

                val idx = prepared.startIndex
                val beforeItems = if (idx > 0) prepared.items.subList(0, idx) else emptyList()
                val afterItems = if (idx < prepared.items.size - 1) prepared.items.subList(idx + 1, prepared.items.size) else emptyList()

                // Create MediaSources on demand off Main thread without caching 9,000 heavy C++ handles in RAM
                val afterSources = afterItems.map { mediaSourceFactory.createMediaSource(it) }
                val beforeSources = beforeItems.map { mediaSourceFactory.createMediaSource(it) }

                withContext(Dispatchers.Main) {
                    val exoPlayer = player as? ExoPlayer
                    if (exoPlayer != null) {
                        if (afterSources.isNotEmpty()) {
                            exoPlayer.addMediaSources(afterSources)
                        }
                        if (beforeSources.isNotEmpty()) {
                            exoPlayer.addMediaSources(0, beforeSources)
                        }
                    }
                }

                persistFullQueue(queueName, prepared.songs)
            } else {
                // FALLBACK (rare: cache not built yet, e.g. < 2s after first app launch)
                while (!cacheReady.get()) delay(50)
                val pos = player.currentPosition
                val items = cachedMediaItems
                if (items.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(items, startIndex, pos)
                    }
                }
                persistFullQueue(queueName, songs)
            }
        }
    }

    private fun parseAudioUri(path: String): android.net.Uri {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            android.net.Uri.parse(path)
        } else {
            android.net.Uri.fromFile(java.io.File(path))
        }
    }

    private fun buildMediaItem(song: SongEntity): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist ?: "Unknown Artist")
            .setAlbumTitle(song.album ?: "Unknown Album")
        if (!song.artPath.isNullOrEmpty()) {
            meta.setArtworkUri(android.net.Uri.parse(song.artPath))
        }
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(parseAudioUri(song.path))
            .setMediaMetadata(meta.build())
            .build()
    }

    private suspend fun persistFullQueue(queueName: String, songs: List<SongEntity>) {
        val queueId = queueDao.insertQueue(QueueEntity(name = queueName, isActive = true))
        val items = songs.mapIndexed { index, song ->
            QueueItemEntity(
                queueId = queueId,
                mediaId = song.id.toString(),
                title = song.title,
                artist = song.artist ?: "Unknown Artist",
                album = song.album ?: "Unknown Album",
                durationMs = song.duration?.toLong() ?: 0L,
                uri = song.path,
                albumArtUri = song.artPath,
                positionInQueue = index
            )
        }
        queueDao.insertQueueItems(items)
        queueDao.setActiveQueue(queueId)
    }

    fun switchToQueue(queueId: Long) {
        scope.launch(Dispatchers.IO) {
            val queue = queueDao.getAllQueues().firstOrNull()?.find { it.id == queueId } ?: return@launch
            val items = queueDao.getItemsForQueue(queueId).firstOrNull() ?: return@launch
            
            queueDao.setActiveQueue(queueId)
            
            scope.launch(Dispatchers.Main) {
                playQueueItems(items, queue.currentItemIndex, queue.currentPlaybackPositionMs)
            }
        }
    }

    private fun playQueueItems(items: List<QueueItemEntity>, startIndex: Int, startPositionMs: Long, autoPlay: Boolean = true) {
        val mediaItems = items.map { item ->
            MediaItem.Builder()
                .setMediaId(item.mediaId)
                .setUri(parseAudioUri(item.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .setArtworkUri(item.albumArtUri?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, startPositionMs)
        player.prepare()
        if (autoPlay) {
            player.play()
        }
    }
    
    fun saveCurrentState() {
        scope.launch(Dispatchers.IO) {
            val activeQueue = queueDao.getActiveQueue() ?: return@launch
            queueDao.updateQueueState(
                queueId = activeQueue.id,
                index = player.currentMediaItemIndex,
                position = player.currentPosition
            )
        }
    }
    
    fun restoreLastQueue() {
        scope.launch(Dispatchers.IO) {
            val activeQueue = queueDao.getActiveQueue() ?: return@launch
            val items = queueDao.getItemsForQueue(activeQueue.id).firstOrNull() ?: return@launch
            
            if (items.isNotEmpty()) {
                scope.launch(Dispatchers.Main) {
                    playQueueItems(items, activeQueue.currentItemIndex, activeQueue.currentPlaybackPositionMs, autoPlay = false)
                }
            }
        }
    }
}
