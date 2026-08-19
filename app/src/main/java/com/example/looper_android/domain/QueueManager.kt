package com.example.looper_android.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.QueueDao
import com.example.looper_android.data.db.QueueEntity
import com.example.looper_android.data.db.QueueItemEntity
import com.example.looper_android.data.db.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

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

    private val _activeQueueSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val activeQueueSongs: StateFlow<List<SongEntity>> = _activeQueueSongs.asStateFlow()

    var workingSongs: List<SongEntity>
        get() = _activeQueueSongs.value
        private set(value) {
            _activeQueueSongs.value = value
        }

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    val allQueues: Flow<List<QueueEntity>> = queueDao.getAllQueues()

    private val _activeQueueId = MutableStateFlow<Long?>(getActiveQueueIdFromPrefs())
    val activeQueueId: StateFlow<Long?> = _activeQueueId.asStateFlow()

    private val _activeQueueName = MutableStateFlow("Select Queue")
    val activeQueueName: StateFlow<String> = _activeQueueName.asStateFlow()

    private var queueJob: Job? = null

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
        forceShuffleOriginal: Boolean = false
    ) {
        if (songs.isEmpty()) return
        queueJob?.cancel()

        val validSongs = songs
        val safeStartIndex = startIndex.coerceIn(0, validSongs.lastIndex)

        val actualOriginalSongs = if (forceShuffleOriginal) {
            val startSong = validSongs[safeStartIndex]
            val others = validSongs.filterIndexed { index, _ -> index != safeStartIndex }.shuffled()
            listOf(startSong) + others
        } else {
            validSongs
        }

        val actualStartIndex = if (forceShuffleOriginal) 0 else safeStartIndex
        val startSong = actualOriginalSongs[actualStartIndex]

        originalSongs = actualOriginalSongs
        _activeQueueName.value = queueName
        
        val isShuffleModeOn = _isShuffleEnabled.value

        val currentWorkingSongs = if (isShuffleModeOn) {
            val others = actualOriginalSongs.filterIndexed { index, _ -> index != actualStartIndex }
            val shuffledOthers = others.shuffled()
            listOf(startSong) + shuffledOthers
        } else {
            actualOriginalSongs
        }
        val currentWorkingIndex = if (isShuffleModeOn) 0 else actualStartIndex
        
        // INSTANT UI UPDATE
        workingSongs = currentWorkingSongs

        // STEP 1: INSTANT PLAY
        val targetMediaItem = buildMediaItem(startSong)
        player.setMediaItem(targetMediaItem)
        player.shuffleModeEnabled = false
        player.prepare()
        player.play()

        // STEP 2: DEFERRED QUEUE ATTACHMENT
        queueJob = scope.launch(Dispatchers.Default) {
            delay(300)

            val afterItems = mutableListOf<androidx.media3.common.MediaItem>()
            for (i in currentWorkingIndex + 1 until currentWorkingSongs.size) {
                kotlinx.coroutines.yield()
                afterItems.add(buildMediaItem(currentWorkingSongs[i]))
            }
            
            val beforeItems = mutableListOf<androidx.media3.common.MediaItem>()
            for (i in 0 until currentWorkingIndex) {
                kotlinx.coroutines.yield()
                beforeItems.add(buildMediaItem(currentWorkingSongs[i]))
            }

            withContext(Dispatchers.Main) {
                val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                if (exoPlayer != null) {
                    if (afterItems.isNotEmpty()) {
                        exoPlayer.addMediaItems(afterItems)
                    }
                    if (beforeItems.isNotEmpty()) {
                        exoPlayer.addMediaItems(0, beforeItems)
                    }
                }
            }

            persistFullQueue(queueName, currentWorkingSongs, isShuffleModeOn, currentWorkingIndex)
        }
    }

    fun toggleShuffle() {
        if (workingSongs.isEmpty() || originalSongs.isEmpty()) return
        
        queueJob?.cancel()

        val currentMediaItem = player.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull()
        if (songId == null) return
        
        val currentSong = originalSongs.find { it.id == songId } ?: return
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
        
        queueJob = scope.launch(Dispatchers.Default) {
            val afterMediaItems = mutableListOf<androidx.media3.common.MediaItem>()
            if (newShuffle) {
                newWorkingSongs.subList(1, newWorkingSongs.size).forEach { song ->
                    kotlinx.coroutines.yield()
                    afterMediaItems.add(buildMediaItem(song))
                }
            } else {
                val origIndex = newIndex
                newWorkingSongs.subList(origIndex + 1, newWorkingSongs.size).forEach { song ->
                    kotlinx.coroutines.yield()
                    afterMediaItems.add(buildMediaItem(song))
                }
            }
            
            val beforeMediaItems = mutableListOf<androidx.media3.common.MediaItem>()
            if (!newShuffle) {
                val origIndex = newIndex
                newWorkingSongs.subList(0, origIndex).forEach { song ->
                    kotlinx.coroutines.yield()
                    beforeMediaItems.add(buildMediaItem(song))
                }
            }
            
            withContext(Dispatchers.Main) {
                val itemsCount = player.mediaItemCount
                val currentExoIndex = player.currentMediaItemIndex
                
                // 1. Immediately remove everything BEFORE the current song
                if (currentExoIndex > 0) {
                    player.removeMediaItems(0, currentExoIndex)
                }
                
                // Now the current song is shifted to index 0.
                
                // 2. Immediately remove everything AFTER the current song
                val newItemsCount = player.mediaItemCount
                if (newItemsCount > 1) {
                    player.removeMediaItems(1, newItemsCount)
                }
                
                // 3. Now it is completely safe to add items directly (fast execution, no chunking)
                if (afterMediaItems.isNotEmpty()) {
                    player.addMediaItems(afterMediaItems)
                }
                if (beforeMediaItems.isNotEmpty()) {
                    player.addMediaItems(0, beforeMediaItems)
                }
            }
            
            val activeName = activeQueueName.value
            if (activeName != "Unknown Queue") {
                persistFullQueue(activeName, newWorkingSongs, newShuffle, newIndex)
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

        val currentPos = try { player.currentPosition.coerceAtLeast(0L) } catch (_: Exception) { 0L }
        val queueId = if (existingQueue != null) {
            queueDao.deleteQueueItems(existingQueue.id)
            queueDao.updateQueueState(
                existingQueue.id, 
                startIndex, 
                currentPos, 
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

        val items = mutableListOf<com.example.looper_android.data.db.QueueItemEntity>()
        songs.forEachIndexed { index, song ->
            kotlinx.coroutines.yield()
            items.add(
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

    fun addNext(song: SongEntity) {
        addNext(listOf(song))
    }

    fun addNext(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        scope.launch(Dispatchers.Main) {
            if (workingSongs.isEmpty() || player.mediaItemCount == 0) {
                loadAndPlayQueue("All Songs", songs, 0, false)
                return@launch
            }

            val songIds = songs.map { it.id }.toSet()
            val filteredWorking = workingSongs.filter { it.id !in songIds }
            val currentMediaItem = player.currentMediaItem
            val currentSongId = currentMediaItem?.mediaId?.toLongOrNull()
            val currentIndex = if (currentSongId != null) {
                filteredWorking.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
            } else {
                player.currentMediaItemIndex.coerceIn(0, (filteredWorking.size - 1).coerceAtLeast(0))
            }

            val insertIndex = (currentIndex + 1).coerceAtMost(filteredWorking.size)
            val newWorking = filteredWorking.toMutableList().apply {
                addAll(insertIndex, songs)
            }
            workingSongs = newWorking

            val newOriginal = originalSongs.filter { it.id !in songIds }.toMutableList().apply {
                addAll(songs)
            }
            originalSongs = newOriginal

            // Remove duplicates from ExoPlayer
            for (i in player.mediaItemCount - 1 downTo 0) {
                val mId = player.getMediaItemAt(i).mediaId.toLongOrNull()
                if (mId != null && mId in songIds) {
                    player.removeMediaItem(i)
                }
            }
            val exoInsertIndex = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
            val mediaItems = songs.map { buildMediaItem(it) }
            player.addMediaItems(exoInsertIndex, mediaItems)

            val activeQName = _activeQueueName.value
            persistFullQueue(activeQName, newWorking, _isShuffleEnabled.value, player.currentMediaItemIndex)
        }
    }

    fun addToQueue(song: SongEntity) {
        addToQueue(listOf(song))
    }

    fun addToQueue(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        scope.launch(Dispatchers.Main) {
            if (workingSongs.isEmpty() || player.mediaItemCount == 0) {
                loadAndPlayQueue("All Songs", songs, 0, false)
                return@launch
            }

            val songIds = songs.map { it.id }.toSet()
            val filteredWorking = workingSongs.filter { it.id !in songIds }.toMutableList().apply {
                addAll(songs)
            }
            workingSongs = filteredWorking

            val newOriginal = originalSongs.filter { it.id !in songIds }.toMutableList().apply {
                addAll(songs)
            }
            originalSongs = newOriginal

            for (i in player.mediaItemCount - 1 downTo 0) {
                val mId = player.getMediaItemAt(i).mediaId.toLongOrNull()
                if (mId != null && mId in songIds) {
                    player.removeMediaItem(i)
                }
            }
            val mediaItems = songs.map { buildMediaItem(it) }
            player.addMediaItems(mediaItems)

            val activeQName = _activeQueueName.value
            persistFullQueue(activeQName, filteredWorking, _isShuffleEnabled.value, player.currentMediaItemIndex)
        }
    }

    fun removeSong(songId: Long) {
        scope.launch(Dispatchers.Main) {
            val currentWorking = workingSongs.filter { it.id != songId }
            workingSongs = currentWorking
            originalSongs = originalSongs.filter { it.id != songId }
            cachedSongs = cachedSongs.filter { it.id != songId }

            val currentMediaItem = player.currentMediaItem
            val currentPlayingId = currentMediaItem?.mediaId?.toLongOrNull()

            if (currentPlayingId == songId) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else if (currentWorking.isNotEmpty()) {
                    player.seekToDefaultPosition(0)
                } else {
                    player.stop()
                    player.clearMediaItems()
                }
            }

            for (i in 0 until player.mediaItemCount) {
                if (player.getMediaItemAt(i).mediaId == songId.toString()) {
                    player.removeMediaItem(i)
                    break
                }
            }

            val activeQName = _activeQueueName.value
            persistFullQueue(activeQName, currentWorking, _isShuffleEnabled.value, player.currentMediaItemIndex)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        scope.launch(Dispatchers.Main) {
            if (fromIndex !in workingSongs.indices || toIndex !in workingSongs.indices || fromIndex == toIndex) return@launch
            val list = workingSongs.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            workingSongs = list
            originalSongs = list

            if (fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount) {
                player.moveMediaItem(fromIndex, toIndex)
            }

            val activeQName = _activeQueueName.value
            persistFullQueue(activeQName, list, _isShuffleEnabled.value, player.currentMediaItemIndex)
        }
    }

    fun setQueueOrder(newOrder: List<SongEntity>, fromIndex: Int, toIndex: Int) {
        scope.launch(Dispatchers.Main) {
            workingSongs = newOrder
            originalSongs = newOrder

            if (fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount) {
                player.moveMediaItem(fromIndex, toIndex)
            }

            val activeQName = _activeQueueName.value
            persistFullQueue(activeQName, newOrder, _isShuffleEnabled.value, player.currentMediaItemIndex)
        }
    }

    fun removeQueueItem(index: Int) {
        scope.launch(Dispatchers.Main) {
            if (index !in workingSongs.indices) return@launch
            val songToRemove = workingSongs[index]
            val currentWorking = workingSongs.toMutableList().apply { removeAt(index) }
            workingSongs = currentWorking
            originalSongs = originalSongs.filter { it.id != songToRemove.id }

            val currentMediaItem = player.currentMediaItem
            val currentPlayingId = currentMediaItem?.mediaId?.toLongOrNull()

            if (currentPlayingId == songToRemove.id) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else if (currentWorking.isNotEmpty()) {
                    player.seekToDefaultPosition(0)
                } else {
                    player.stop()
                    player.clearMediaItems()
                }
            }

            if (index in 0 until player.mediaItemCount) {
                player.removeMediaItem(index)
            }

            val activeQName = _activeQueueName.value
            persistFullQueue(activeQName, currentWorking, _isShuffleEnabled.value, player.currentMediaItemIndex)
        }
    }

    fun switchToQueue(queueId: Long) {
        queueJob?.cancel()
        scope.launch(Dispatchers.Main) {
            val autoPlay = player.isPlaying
            
            withContext(Dispatchers.IO) {
                saveCurrentStateSync()
                
                val queue = queueDao.getAllQueues().firstOrNull()?.find { it.id == queueId } ?: return@withContext
                val items = queueDao.getItemsForQueue(queueId).firstOrNull() ?: return@withContext
                
                queueDao.updateLastPlayed(queueId, System.currentTimeMillis())
                setActiveQueueIdInPrefs(queueId)
                _activeQueueName.value = queue.name
                
                val songMap = if (cachedSongs.isNotEmpty()) cachedSongs.associateBy { it.id } else emptyMap()
                val restoredSongs = items.map { item ->
                    val sId = item.mediaId.toLongOrNull()
                    (if (sId != null) songMap[sId] else null) ?: SongEntity(
                        id = sId ?: 0L,
                        path = item.uri,
                        title = item.title,
                        artist = item.artist,
                        albumArtist = item.artist,
                        album = item.album,
                        genre = null,
                        duration = item.durationMs.toInt(),
                        trackNumber = null,
                        year = null,
                        artPath = item.albumArtUri,
                        dateAdded = 0L,
                        isFavorite = false
                    )
                }
                
                if (restoredSongs.isNotEmpty()) {
                    val startIdx = queue.currentItemIndex.coerceIn(0, (restoredSongs.size - 1).coerceAtLeast(0))
                    val startSong = restoredSongs[startIdx]
                    val targetMediaItem = buildMediaItem(startSong)
                    
                    withContext(Dispatchers.Main) {
                        workingSongs = restoredSongs
                        originalSongs = restoredSongs
                        _isShuffleEnabled.value = queue.shuffleState
                        
                        val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                        if (exoPlayer != null) {
                            exoPlayer.setMediaItem(targetMediaItem)
                            exoPlayer.seekTo(0, queue.currentPlaybackPositionMs)
                            exoPlayer.prepare()
                            if (autoPlay) {
                                exoPlayer.play()
                            }
                        }
                    }

                    queueJob = scope.launch(Dispatchers.Default) {
                        delay(250)
                        val afterItems = mutableListOf<androidx.media3.common.MediaItem>()
                        for (i in startIdx + 1 until restoredSongs.size) {
                            kotlinx.coroutines.yield()
                            afterItems.add(buildMediaItem(restoredSongs[i]))
                        }
                        val beforeItems = mutableListOf<androidx.media3.common.MediaItem>()
                        for (i in 0 until startIdx) {
                            kotlinx.coroutines.yield()
                            beforeItems.add(buildMediaItem(restoredSongs[i]))
                        }
                        withContext(Dispatchers.Main) {
                            val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                            if (exoPlayer != null) {
                                if (afterItems.isNotEmpty()) {
                                    exoPlayer.addMediaItems(afterItems)
                                }
                                if (beforeItems.isNotEmpty()) {
                                    exoPlayer.addMediaItems(0, beforeItems)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun deleteUserQueue(queueId: Long) {
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
    
    fun deleteQueue(queueId: Long) {
        scope.launch(Dispatchers.IO) {
            queueDao.deleteQueueWithItems(queueId)
            if (_activeQueueId.value == queueId) {
                setActiveQueueIdInPrefs(-1L)
            }
        }
    }

    fun clearQueue() {
        queueJob?.cancel()
        workingSongs = emptyList()
        originalSongs = emptyList()
        _activeQueueId.value = null
        _activeQueueName.value = "Select Queue"
        
        player.stop()
        player.clearMediaItems()
    }

    fun restoreLastQueue() {
        queueJob?.cancel()
        scope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                val lastQueueId = getActiveQueueIdFromPrefs()
                if (lastQueueId == null || lastQueueId == -1L) return@withContext
                
                val queues = queueDao.getAllQueues().firstOrNull()
                if (queues.isNullOrEmpty()) return@withContext
                
                val queue = queues.find { it.id == lastQueueId }
                if (queue == null) {
                    setActiveQueueIdInPrefs(-1L)
                    return@withContext
                }
                
                _activeQueueName.value = queue.name

                val items = queueDao.getItemsForQueue(lastQueueId).firstOrNull()
                if (items.isNullOrEmpty()) return@withContext
                
                val songMap = if (cachedSongs.isNotEmpty()) cachedSongs.associateBy { it.id } else emptyMap()
                val restoredSongs = items.map { item ->
                    val sId = item.mediaId.toLongOrNull()
                    (if (sId != null) songMap[sId] else null) ?: SongEntity(
                        id = sId ?: 0L,
                        path = item.uri,
                        title = item.title,
                        artist = item.artist,
                        albumArtist = item.artist,
                        album = item.album,
                        genre = null,
                        duration = item.durationMs.toInt(),
                        trackNumber = null,
                        year = null,
                        artPath = item.albumArtUri,
                        dateAdded = 0L,
                        isFavorite = false
                    )
                }
                
                if (restoredSongs.isNotEmpty()) {
                    val startIdx = queue.currentItemIndex.coerceIn(0, (restoredSongs.size - 1).coerceAtLeast(0))
                    val startSong = restoredSongs[startIdx]
                    val targetMediaItem = buildMediaItem(startSong)
                    
                    withContext(Dispatchers.Main) {
                        workingSongs = restoredSongs
                        originalSongs = restoredSongs
                        _isShuffleEnabled.value = queue.shuffleState
                        
                        val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                        if (exoPlayer != null) {
                            exoPlayer.setMediaItem(targetMediaItem)
                            exoPlayer.seekTo(0, queue.currentPlaybackPositionMs)
                            exoPlayer.prepare()
                        }
                    }

                    queueJob = scope.launch(Dispatchers.Default) {
                        delay(250)
                        val afterItems = mutableListOf<androidx.media3.common.MediaItem>()
                        for (i in startIdx + 1 until restoredSongs.size) {
                            kotlinx.coroutines.yield()
                            afterItems.add(buildMediaItem(restoredSongs[i]))
                        }
                        val beforeItems = mutableListOf<androidx.media3.common.MediaItem>()
                        for (i in 0 until startIdx) {
                            kotlinx.coroutines.yield()
                            beforeItems.add(buildMediaItem(restoredSongs[i]))
                        }
                        withContext(Dispatchers.Main) {
                            val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                            if (exoPlayer != null) {
                                if (afterItems.isNotEmpty()) {
                                    exoPlayer.addMediaItems(afterItems)
                                }
                                if (beforeItems.isNotEmpty()) {
                                    exoPlayer.addMediaItems(0, beforeItems)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun saveCurrentState() {
        val activeId = activeQueueId.value
        if (activeId != null && activeId != -1L) {
            val (currentIndex, position) = withContext(Dispatchers.Main) {
                Pair(
                    player.currentMediaItemIndex.coerceAtLeast(0),
                    player.currentPosition.coerceAtLeast(0L)
                )
            }
            val isShuffle = _isShuffleEnabled.value
            queueDao.updateQueueState(
                queueId = activeId,
                index = currentIndex,
                position = position,
                shuffleState = isShuffle,
                lastPlayedAt = System.currentTimeMillis()
            )
        }
    }
    
    fun saveCurrentStateSync() {
        val activeId = activeQueueId.value
        if (activeId != null && activeId != -1L) {
            val currentIndex = player.currentMediaItemIndex.coerceAtLeast(0)
            val position = player.currentPosition.coerceAtLeast(0L)
            val isShuffle = _isShuffleEnabled.value
            kotlinx.coroutines.runBlocking {
                queueDao.updateQueueState(
                    queueId = activeId,
                    index = currentIndex,
                    position = position,
                    shuffleState = isShuffle,
                    lastPlayedAt = System.currentTimeMillis()
                )
            }
        }
    }
}
