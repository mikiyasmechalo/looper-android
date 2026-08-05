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

    var originalSongs: List<SongEntity> = emptyList()
        private set
    var workingSongs: List<SongEntity> = emptyList()
        private set
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()


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

        originalSongs = songs
        _isShuffleEnabled.value = shuffled

        val startSong = songs[startIndex]
        workingSongs = if (shuffled) {
            val others = songs.filterIndexed { index, _ -> index != startIndex }.shuffled()
            listOf(startSong) + others
        } else {
            songs
        }
        val currentWorkingIndex = if (shuffled) 0 else startIndex

        // STEP 1: INSTANT PLAY
        val targetMediaItem = buildMediaItem(startSong)
        player.setMediaItem(targetMediaItem)
        player.shuffleModeEnabled = false
        player.prepare()
        player.play()

        // STEP 2: DEFERRED QUEUE ATTACHMENT
        scope.launch(Dispatchers.Default) {
            val queueMediaItems = workingSongs.map { buildMediaItem(it) }

            if (cacheReady.get() && queueMediaItems.isNotEmpty()) {
                delay(250)

                withContext(Dispatchers.Main) {
                    val exoPlayer = player as? ExoPlayer
                    if (exoPlayer != null) {
                        if (queueMediaItems.isNotEmpty()) {
                            val pos = player.currentPosition
                            player.setMediaItems(queueMediaItems, currentWorkingIndex, pos)
                        }
                    }
                }

                persistFullQueue(queueName, workingSongs, shuffled, currentWorkingIndex)
            } else {
                while (!cacheReady.get()) delay(50)
                val pos = withContext(Dispatchers.Main) { player.currentPosition }
                if (queueMediaItems.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(queueMediaItems, currentWorkingIndex, pos)
                    }
                }
                persistFullQueue(queueName, workingSongs, shuffled, currentWorkingIndex)
            }
        }
    }

    fun toggleShuffle() {
        if (workingSongs.isEmpty() || originalSongs.isEmpty()) return
        
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex < 0 || currentIndex >= workingSongs.size) return
        
        val currentSong = workingSongs[currentIndex]
        val wasShuffled = _isShuffleEnabled.value
        val newShuffle = !wasShuffled
        
        val newWorkingSongs = if (newShuffle) {
            val others = originalSongs.filter { it.id != currentSong.id }.shuffled()
            listOf(currentSong) + others
        } else {
            originalSongs
        }
        
        val newIndex = if (newShuffle) {
            0
        } else {
            newWorkingSongs.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
        }
        
        workingSongs = newWorkingSongs
        _isShuffleEnabled.value = newShuffle
        
        // Update ExoPlayer incrementally to prevent playback interruption or skipping
        if (newShuffle) {
            val itemsCount = player.mediaItemCount
            if (itemsCount > currentIndex + 1) {
                player.removeMediaItems(currentIndex + 1, itemsCount)
            }
            val others = newWorkingSongs.subList(1, newWorkingSongs.size).map { buildMediaItem(it) }
            if (others.isNotEmpty()) {
                player.addMediaItems(others)
            }
            if (currentIndex > 0) {
                player.removeMediaItems(0, currentIndex)
            }
        } else {
            val origIndex = newIndex
            val itemsCount = player.mediaItemCount
            if (itemsCount > currentIndex + 1) {
                player.removeMediaItems(currentIndex + 1, itemsCount)
            }
            val afterItems = newWorkingSongs.subList(origIndex + 1, newWorkingSongs.size).map { buildMediaItem(it) }
            if (afterItems.isNotEmpty()) {
                player.addMediaItems(afterItems)
            }
            if (currentIndex > 0) {
                player.removeMediaItems(0, currentIndex)
            }
            val beforeItems = newWorkingSongs.subList(0, origIndex).map { buildMediaItem(it) }
            if (beforeItems.isNotEmpty()) {
                player.addMediaItems(0, beforeItems)
            }
        }
        
        // Persist the new queue state
        scope.launch(Dispatchers.IO) {
            val activeId = activeQueueId.value ?: return@launch
            val queues = queueDao.getAllQueues().firstOrNull() ?: emptyList()
            val activeQueue = queues.find { it.id == activeId }
            val queueName = activeQueue?.name ?: "Current Queue"
            persistFullQueue(queueName, workingSongs, newShuffle, newIndex)
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
                
                while (!cacheReady.get()) delay(50)
                
                val songMap = cachedSongs.associateBy { it.id }
                val restoredSongs = items.mapNotNull { item ->
                    val songId = item.mediaId.toLongOrNull()
                    songId?.let { songMap[it] }
                }
                
                withContext(Dispatchers.Main) {
                    originalSongs = restoredSongs
                    workingSongs = restoredSongs
                    _isShuffleEnabled.value = queue.shuffleState
                    player.shuffleModeEnabled = false
                    val mediaItems = restoredSongs.map { buildMediaItem(it) }
                    player.setMediaItems(mediaItems, queue.currentItemIndex, queue.currentPlaybackPositionMs)
                    player.prepare()
                    if (autoPlay) player.play()
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


    
    private suspend fun saveCurrentStateSync() {
        val activeId = activeQueueId.value ?: return
        
        val currentIdx = withContext(Dispatchers.Main) { player.currentMediaItemIndex }
        val currentPos = withContext(Dispatchers.Main) { player.currentPosition }
        val isShuffled = _isShuffleEnabled.value
        val lastPlayed = System.currentTimeMillis()
        
        queueDao.updateQueueState(activeId, currentIdx, currentPos, isShuffled, lastPlayed)
    }

    fun saveCurrentState() {
        val activeId = activeQueueId.value ?: return
        scope.launch(Dispatchers.Main) {
            val currentIdx = player.currentMediaItemIndex
            val currentPos = player.currentPosition
            val isShuffled = _isShuffleEnabled.value
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
            
            while (!cacheReady.get()) delay(50)
            
            if (items.isNotEmpty()) {
                val songMap = cachedSongs.associateBy { it.id }
                val restoredSongs = items.mapNotNull { item ->
                    val songId = item.mediaId.toLongOrNull()
                    songId?.let { songMap[it] }
                }
                
                scope.launch(Dispatchers.Main) {
                    originalSongs = restoredSongs
                    workingSongs = restoredSongs
                    _isShuffleEnabled.value = queue.shuffleState
                    player.shuffleModeEnabled = false // Always false
                    val mediaItems = restoredSongs.map { buildMediaItem(it) }
                    player.setMediaItems(mediaItems, queue.currentItemIndex, queue.currentPlaybackPositionMs)
                    player.prepare()
                }
            }
        }
    }
}
