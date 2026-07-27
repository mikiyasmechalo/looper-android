package com.example.looper_android.domain

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.example.looper_android.data.db.QueueDao
import com.example.looper_android.data.db.QueueEntity
import com.example.looper_android.data.db.QueueItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton
import com.example.looper_android.data.db.SongEntity

@Singleton
class QueueManager @Inject constructor(
    private val queueDao: QueueDao,
    private val player: Player
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun createAndPlayQueue(queueName: String, songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        // 1. INSTANT PLAY (Main thread, < 1ms): Start selected song immediately
        val targetSong = songs[startIndex]
        val targetMediaItem = MediaItem.Builder()
            .setMediaId(targetSong.id.toString())
            .setUri(targetSong.path)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(targetSong.title)
                    .setArtist(targetSong.artist ?: "Unknown Artist")
                    .setAlbumTitle(targetSong.album ?: "Unknown Album")
                    .setArtworkUri(targetSong.artPath?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()

        player.setMediaItem(targetMediaItem)
        player.prepare()
        player.play()

        // 2. BACKGROUND QUEUE POPULATION: Append surrounding playlist without re-preparing active track or blocking UI
        scope.launch(Dispatchers.Default) {
            val itemsBefore = if (startIndex > 0) {
                songs.subList(0, startIndex).map { song ->
                    MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist ?: "Unknown Artist")
                                .setAlbumTitle(song.album ?: "Unknown Album")
                                .build()
                        )
                        .build()
                }
            } else emptyList()

            val itemsAfter = if (startIndex < songs.size - 1) {
                songs.subList(startIndex + 1, songs.size).map { song ->
                    MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.path)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist ?: "Unknown Artist")
                                .setAlbumTitle(song.album ?: "Unknown Album")
                                .build()
                        )
                        .build()
                }
            } else emptyList()

            scope.launch(Dispatchers.Main) {
                val chunkSize = 50
                if (itemsAfter.isNotEmpty()) {
                    for (chunk in itemsAfter.chunked(chunkSize)) {
                        player.addMediaItems(chunk)
                        yield()
                    }
                }
                if (itemsBefore.isNotEmpty()) {
                    var insertPos = 0
                    for (chunk in itemsBefore.chunked(chunkSize)) {
                        player.addMediaItems(insertPos, chunk)
                        insertPos += chunk.size
                        yield()
                    }
                }
            }

            // 3. Persist queue state in background DB
            val queueId = queueDao.insertQueue(QueueEntity(name = queueName, isActive = true))
            val itemsWithId = songs.mapIndexed { index, song -> 
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
            queueDao.insertQueueItems(itemsWithId)
            queueDao.setActiveQueue(queueId)
        }
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
                .setUri(item.uri)
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
