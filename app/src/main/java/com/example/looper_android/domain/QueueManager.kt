package com.example.looper_android.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.QueueDao
import com.example.looper_android.data.db.QueueEntity
import com.example.looper_android.data.db.QueueItemEntity
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
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
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mediaSourceFactory by lazy { DefaultMediaSourceFactory(context) }
    private val prefs: SharedPreferences = context.getSharedPreferences("looper_queues", Context.MODE_PRIVATE)

    private val _cachedSongsFlow = MutableStateFlow<List<SongEntity>>(emptyList())
    private var cachedSongs: List<SongEntity>
        get() = _cachedSongsFlow.value
        set(value) { _cachedSongsFlow.value = value }
    @Volatile private var cachedMediaItems: List<MediaItem> = emptyList()
    private val cacheReady = AtomicBoolean(false)

    val allQueues: Flow<List<QueueEntity>> = queueDao.getAllQueues()

    private val _activeQueueId = MutableStateFlow<Long?>(getActiveQueueIdFromPrefs())
    val activeQueueId: StateFlow<Long?> = _activeQueueId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeQueueSongs: Flow<List<SongEntity>> = kotlinx.coroutines.flow.combine(
        _activeQueueId.flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else queueDao.getItemsForQueue(id)
        },
        _cachedSongsFlow
    ) { items, allSongs ->
        val songMap = allSongs.associateBy { it.id }
        items.mapNotNull { item ->
            val songId = item.mediaId.toLongOrNull()
            songId?.let { songMap[it] }
        }
    }.flowOn(Dispatchers.Default)

    private fun getActiveQueueIdFromPrefs(): Long? {
        val id = prefs.getLong("active_queue_id", -1L)
        return if (id == -1L) null else id
    }

    private fun setActiveQueueIdInPrefs(id: Long) {
        prefs.edit().putLong("active_queue_id", id).apply()
        _activeQueueId.value = id
    }

    init {
        scope.launch(Dispatchers.IO) {
            libraryDao.getAllSongs().collectLatest { songs ->
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

    fun loadAndPlayQueue(
        queueName: String,
        songs: List<SongEntity>,
        startIndex: Int = 0,
        shuffled: Boolean = false
    ) {
        if (songs.isEmpty()) return

        // STEP 1: INSTANT PLAY
        val targetSong = songs[startIndex]
        val targetMediaItem = buildMediaItem(targetSong)
        player.setMediaItem(targetMediaItem)
        if (!shuffled) {
            player.shuffleModeEnabled = false
        } else {
            player.shuffleModeEnabled = true
        }
        player.prepare()
        player.play()

        // STEP 2: DEFERRED QUEUE ATTACHMENT
        scope.launch(Dispatchers.Default) {
            val queueMediaItems = songs.map { buildMediaItem(it) }

            if (cacheReady.get() && queueMediaItems.isNotEmpty()) {
                val prepared = if (shuffled) {
                    val others = queueMediaItems.indices.filter { it != startIndex }.shuffled()
                    val idx = listOf(startIndex) + others
                    PreparedQueueData(
                        items = idx.map { queueMediaItems[it] },
                        songs = idx.map { songs[it] },
                        startIndex = 0
                    )
                } else {
                    PreparedQueueData(
                        items = queueMediaItems,
                        songs = songs,
                        startIndex = startIndex
                    )
                }

                delay(250)



                withContext(Dispatchers.Main) {
                    val exoPlayer = player as? ExoPlayer
                    if (exoPlayer != null) {
                        // ExoPlayer's addMediaSources can block the main thread if adding thousands of items.
                        // For large queues, we just use setMediaItems which handles bulk addition natively.
                        if (queueMediaItems.isNotEmpty()) {
                            val pos = player.currentPosition
                            player.setMediaItems(prepared.items, prepared.startIndex, pos)
                        }
                    }
                }

                persistFullQueue(queueName, prepared.songs, shuffled, prepared.startIndex)
            } else {
                while (!cacheReady.get()) delay(50)
                val pos = withContext(Dispatchers.Main) { player.currentPosition }
                if (queueMediaItems.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(queueMediaItems, startIndex, pos)
                    }
                }
                persistFullQueue(queueName, songs, shuffled, startIndex)
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

    private suspend fun persistFullQueue(queueName: String, songs: List<SongEntity>, shuffled: Boolean, startIndex: Int) {
        saveCurrentState() // Save previous queue before overwriting/switching

        var existingQueue = queueDao.getQueueByName(queueName)
        val isAllSongs = queueName == "All Songs"

        if (existingQueue == null && !isAllSongs) {
            val userCount = queueDao.countUserQueues()
            if (userCount >= 10) {
                val oldest = queueDao.getOldestUserQueue()
                if (oldest != null) {
                    queueDao.deleteQueueWithItems(oldest.id)
                }
            }
        }

        val queueId = if (existingQueue != null) {
            queueDao.deleteQueueItems(existingQueue.id)
            queueDao.updateQueueState(
                existingQueue.id, 
                startIndex, 
                0L, 
                shuffled, 
                System.currentTimeMillis()
            )
            existingQueue.id
        } else {
            queueDao.insertQueue(
                QueueEntity(
                    name = queueName,
                    currentItemIndex = startIndex,
                    shuffleState = shuffled,
                    lastPlayedAt = System.currentTimeMillis(),
                    isPermanent = isAllSongs
                )
            )
        }

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
        setActiveQueueIdInPrefs(queueId)
    }

    fun updateSongInQueue(updatedSong: SongEntity) {
        val currentSongs = _cachedSongsFlow.value.toMutableList()
        val index = currentSongs.indexOfFirst { it.id == updatedSong.id }
        if (index != -1) {
            currentSongs[index] = updatedSong
            _cachedSongsFlow.value = currentSongs
        }
    }

    fun switchToQueue(queueId: Long) {
        scope.launch(Dispatchers.Main) {
            val autoPlay = player.isPlaying
            
            withContext(Dispatchers.IO) {
                saveCurrentStateSync() // synchronous wait
                
                val queue = queueDao.getAllQueues().firstOrNull()?.find { it.id == queueId } ?: return@withContext
                val items = queueDao.getItemsForQueue(queueId).firstOrNull() ?: return@withContext
                
                queueDao.updateLastPlayed(queueId, System.currentTimeMillis())
                setActiveQueueIdInPrefs(queueId)
                
                withContext(Dispatchers.Main) {
                    player.shuffleModeEnabled = queue.shuffleState
                    playQueueItems(items, queue.currentItemIndex, queue.currentPlaybackPositionMs, autoPlay)
                }
            }
        }
    }

    fun deleteUserQueue(queueId: Long) {
        scope.launch(Dispatchers.IO) {
            val queue = queueDao.getAllQueues().firstOrNull()?.find { it.id == queueId }
            if (queue != null && !queue.isPermanent) {
                queueDao.deleteQueueWithItems(queueId)
                if (activeQueueId.value == queueId) {
                    // Fallback to "All Songs" if active queue deleted
                    val allSongs = queueDao.getQueueByName("All Songs")
                    if (allSongs != null) {
                        switchToQueue(allSongs.id)
                    }
                }
            }
        }
    }

    private fun playQueueItems(items: List<QueueItemEntity>, startIndex: Int, startPositionMs: Long, autoPlay: Boolean) {
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
    
    private suspend fun saveCurrentStateSync() {
        val activeId = activeQueueId.value ?: return
        
        val currentIdx = withContext(Dispatchers.Main) { player.currentMediaItemIndex }
        val currentPos = withContext(Dispatchers.Main) { player.currentPosition }
        val isShuffled = withContext(Dispatchers.Main) { player.shuffleModeEnabled }
        val lastPlayed = System.currentTimeMillis()
        
        queueDao.updateQueueState(activeId, currentIdx, currentPos, isShuffled, lastPlayed)
    }

    fun saveCurrentState() {
        val activeId = activeQueueId.value ?: return
        scope.launch(Dispatchers.Main) {
            val currentIdx = player.currentMediaItemIndex
            val currentPos = player.currentPosition
            val isShuffled = player.shuffleModeEnabled
            val lastPlayed = System.currentTimeMillis()
            
            scope.launch(Dispatchers.IO) {
                queueDao.updateQueueState(
                    queueId = activeId,
                    index = currentIdx,
                    position = currentPos,
                    shuffleState = isShuffled,
                    lastPlayedAt = lastPlayed
                )
            }
        }
    }
    
    fun deleteQueue(queueId: Long) {
        scope.launch(Dispatchers.IO) {
            val q = queueDao.getAllQueues().firstOrNull()?.find { it.id == queueId }
            if (q != null && !q.isPermanent) {
                queueDao.deleteQueueWithItems(queueId)
                if (queueId == activeQueueId.value) {
                    val all = queueDao.getAllQueues().firstOrNull() ?: emptyList()
                    val allSongsQ = all.find { it.isPermanent }
                    if (allSongsQ != null) {
                        setActiveQueueIdInPrefs(allSongsQ.id)
                    }
                }
            }
        }
    }

    fun restoreLastQueue() {
        scope.launch(Dispatchers.IO) {
            val activeId = getActiveQueueIdFromPrefs() ?: return@launch
            val queue = queueDao.getAllQueues().firstOrNull()?.find { it.id == activeId } ?: return@launch
            val items = queueDao.getItemsForQueue(activeId).firstOrNull() ?: return@launch
            
            if (items.isNotEmpty()) {
                scope.launch(Dispatchers.Main) {
                    player.shuffleModeEnabled = queue.shuffleState
                    playQueueItems(items, queue.currentItemIndex, queue.currentPlaybackPositionMs, autoPlay = false)
                }
            }
        }
    }
}
