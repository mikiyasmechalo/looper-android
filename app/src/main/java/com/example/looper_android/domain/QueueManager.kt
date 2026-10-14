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
import com.example.looper_android.ui.widgets.WidgetStateRepository
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
    private val settingsRepository: com.example.looper_android.data.repository.SettingsRepository,
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
            try {
                queueDao.deduplicateQueueItems()
            } catch (_: Exception) {}
            libraryDao.getAllSongs().collectLatest { songs ->
                if (songs.isNotEmpty()) {
                    val items = songs.map { buildMediaItem(it, loadArtworkBytes = false) }
                    cachedSongs = songs
                    cachedMediaItems = items
                    cacheReady.set(true)
                }
            }
        }

        // Real-time Queue State Tracker (Saves current track index & position immediately on transition / pause)
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val activeId = _activeQueueId.value
                val newIndex = player.currentMediaItemIndex
                if (activeId != null && activeId != -1L && newIndex >= 0) {
                    prefs.edit()
                        .putInt("queue_${activeId}_index", newIndex)
                        .putLong("queue_${activeId}_position", 0L)
                        .apply()
                    scope.launch(Dispatchers.IO) {
                        queueDao.updateQueueState(
                            queueId = activeId,
                            index = newIndex,
                            position = 0L,
                            shuffleState = _isShuffleEnabled.value,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    saveCurrentState()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    saveCurrentState()
                }
            }
        })
    }

    fun loadAndPlayQueue(
        queueName: String,
        songs: List<SongEntity>,
        startIndex: Int = 0,
        forceShuffleOriginal: Boolean = false
    ) {
        if (songs.isEmpty()) return
        queueJob?.cancel()

        val previousActiveId = _activeQueueId.value
        if (previousActiveId != null && previousActiveId != -1L) {
            val currentIdx = try { player.currentMediaItemIndex.coerceAtLeast(0) } catch (_: Exception) { 0 }
            val currentPos = try { player.currentPosition.coerceAtLeast(0L) } catch (_: Exception) { 0L }
            val currentShuffle = _isShuffleEnabled.value
            prefs.edit()
                .putInt("queue_${previousActiveId}_index", currentIdx)
                .putLong("queue_${previousActiveId}_position", currentPos)
                .apply()
            scope.launch(Dispatchers.IO) {
                queueDao.updateQueueState(
                    queueId = previousActiveId,
                    index = currentIdx,
                    position = currentPos,
                    shuffleState = currentShuffle,
                    lastPlayedAt = System.currentTimeMillis()
                )
            }
        }
        _activeQueueId.value = null

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

        val mediaItems = currentWorkingSongs.mapIndexed { idx, s ->
            buildMediaItem(s, loadArtworkBytes = (idx == currentWorkingIndex))
        }
        val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
        if (exoPlayer != null) {
            exoPlayer.shuffleModeEnabled = false
            exoPlayer.setMediaItems(mediaItems, currentWorkingIndex, 0L)
            exoPlayer.prepare()
            com.example.looper_android.playback.PlaybackService.start(context)
            exoPlayer.play()
        }

        queueJob = scope.launch(Dispatchers.IO) {
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

    private fun buildMediaItem(song: SongEntity, loadArtworkBytes: Boolean = false): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist ?: "Unknown Artist")
            .setAlbumTitle(song.album ?: "Unknown Album")
        
        if (!song.artPath.isNullOrEmpty()) {
            val artUri = if (song.artPath.startsWith("content://") || song.artPath.startsWith("file://") || song.artPath.startsWith("http://") || song.artPath.startsWith("https://")) {
                android.net.Uri.parse(song.artPath)
            } else {
                android.net.Uri.fromFile(java.io.File(song.artPath))
            }
            meta.setArtworkUri(artUri)
        }

        // Only load expensive bytes for the active song to avoid OOM / memory thrashing on large queues
        if (loadArtworkBytes) {
            try {
                val bytes = com.example.looper_android.data.repository.ArtworkLoaderHelper.loadArtworkBytes(context, song.artPath, song.path)
                if (bytes != null && bytes.isNotEmpty()) {
                    meta.setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            } catch (_: Exception) {}
        }

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(parseAudioUri(song.path))
            .setMediaMetadata(meta.build())
            .build()
    }

    private suspend fun persistFullQueue(queueName: String, songs: List<SongEntity>, shuffled: Boolean, startIndex: Int) {
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
        queueDao.replaceQueueItems(queueId, items)
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

    fun switchToNextSavedQueue(forcePlayFromBeginning: Boolean = true) {
        queueJob?.cancel()
        scope.launch(Dispatchers.Main) {
            val currentIdx = player.currentMediaItemIndex.coerceAtLeast(0)
            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val currentShuffle = _isShuffleEnabled.value
            val previousActiveId = _activeQueueId.value
            withContext(Dispatchers.IO) {
                if (previousActiveId != null && previousActiveId != -1L) {
                    queueDao.updateQueueState(
                        queueId = previousActiveId,
                        index = currentIdx,
                        position = currentPos,
                        shuffleState = currentShuffle,
                        lastPlayedAt = System.currentTimeMillis()
                    )
                }

                val queues = queueDao.getAllQueues().firstOrNull() ?: return@withContext
                if (queues.isEmpty()) return@withContext

                val currentIndex = if (previousActiveId != null) {
                    queues.indexOfFirst { it.id == previousActiveId }
                } else -1

                val nextQueue = if (currentIndex != -1 && currentIndex + 1 < queues.size) {
                    queues[currentIndex + 1]
                } else {
                    queues.first()
                }

                val nextQueueId = nextQueue.id
                val items = queueDao.getItemsForQueue(nextQueueId).firstOrNull() ?: return@withContext

                queueDao.updateLastPlayed(nextQueueId, System.currentTimeMillis())
                setActiveQueueIdInPrefs(nextQueueId)
                _activeQueueName.value = nextQueue.name

                val songMap = if (cachedSongs.isNotEmpty()) cachedSongs.associateBy { it.id } else emptyMap()
                val restoredSongs = items.distinctBy { it.positionInQueue }.map { item ->
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
                    val startIdx = if (forcePlayFromBeginning) 0 else nextQueue.currentItemIndex.coerceIn(0, (restoredSongs.size - 1).coerceAtLeast(0))
                    val startPos = if (forcePlayFromBeginning) 0L else nextQueue.currentPlaybackPositionMs
                    val startSong = restoredSongs[startIdx]
                    val targetMediaItem = buildMediaItem(startSong, loadArtworkBytes = true)

                    val afterItems = mutableListOf<androidx.media3.common.MediaItem>()
                    for (i in startIdx + 1 until restoredSongs.size) {
                        afterItems.add(buildMediaItem(restoredSongs[i], loadArtworkBytes = false))
                    }
                    val beforeItems = mutableListOf<androidx.media3.common.MediaItem>()
                    for (i in 0 until startIdx) {
                        beforeItems.add(buildMediaItem(restoredSongs[i], loadArtworkBytes = false))
                    }

                    withContext(Dispatchers.Main) {
                        workingSongs = restoredSongs
                        originalSongs = restoredSongs
                        _isShuffleEnabled.value = nextQueue.shuffleState

                        val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                        if (exoPlayer != null) {
                            exoPlayer.setMediaItem(targetMediaItem)
                            if (afterItems.isNotEmpty()) {
                                exoPlayer.addMediaItems(afterItems)
                            }
                            if (beforeItems.isNotEmpty()) {
                                exoPlayer.addMediaItems(0, beforeItems)
                            }
                            exoPlayer.seekTo(startIdx, startPos)
                            exoPlayer.prepare()
                            com.example.looper_android.playback.PlaybackService.start(context)
                            exoPlayer.play()
                        }
                    }
                }
            }
        }
    }

    fun switchToQueue(queueId: Long) {
        queueJob?.cancel()
        scope.launch(Dispatchers.Main) {
            val autoPlay = player.isPlaying
            val currentIdx = player.currentMediaItemIndex.coerceAtLeast(0)
            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val currentShuffle = _isShuffleEnabled.value
            val previousActiveId = _activeQueueId.value

            withContext(Dispatchers.IO) {
                try {
                    if (previousActiveId != null && previousActiveId != -1L) {
                        queueDao.updateQueueState(
                            queueId = previousActiveId,
                            index = currentIdx,
                            position = currentPos,
                            shuffleState = currentShuffle,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                    }

                    val queues = queueDao.getAllQueues().firstOrNull() ?: return@withContext
                    val queue = queues.find { it.id == queueId } ?: return@withContext
                    val items = queueDao.getItemsForQueue(queueId).firstOrNull() ?: return@withContext

                    queueDao.updateLastPlayed(queueId, System.currentTimeMillis())
                    setActiveQueueIdInPrefs(queueId)
                    _activeQueueName.value = queue.name

                    val songMap = if (cachedSongs.isNotEmpty()) cachedSongs.associateBy { it.id } else emptyMap()
                    val restoredSongs = items.distinctBy { it.positionInQueue }.map { item ->
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
                        val targetMediaItem = buildMediaItem(startSong, loadArtworkBytes = true)

                        val afterItems = mutableListOf<androidx.media3.common.MediaItem>()
                        for (i in startIdx + 1 until restoredSongs.size) {
                            afterItems.add(buildMediaItem(restoredSongs[i], loadArtworkBytes = false))
                        }
                        val beforeItems = mutableListOf<androidx.media3.common.MediaItem>()
                        for (i in 0 until startIdx) {
                            beforeItems.add(buildMediaItem(restoredSongs[i], loadArtworkBytes = false))
                        }

                        withContext(Dispatchers.Main) {
                            workingSongs = restoredSongs
                            originalSongs = restoredSongs
                            _isShuffleEnabled.value = queue.shuffleState

                            val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                            if (exoPlayer != null) {
                                exoPlayer.setMediaItem(targetMediaItem)
                                if (afterItems.isNotEmpty()) {
                                    exoPlayer.addMediaItems(afterItems)
                                }
                                if (beforeItems.isNotEmpty()) {
                                    exoPlayer.addMediaItems(0, beforeItems)
                                }
                                exoPlayer.seekTo(startIdx, queue.currentPlaybackPositionMs)
                                exoPlayer.prepare()
                                if (autoPlay) {
                                    com.example.looper_android.playback.PlaybackService.start(context)
                                    exoPlayer.play()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun deleteUserQueue(queueId: Long) {
        scope.launch(Dispatchers.IO) {
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun deleteQueue(queueId: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                queueDao.deleteQueueWithItems(queueId)
                if (_activeQueueId.value == queueId) {
                    setActiveQueueIdInPrefs(-1L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearQueue() {
        queueJob?.cancel()
        workingSongs = emptyList()
        originalSongs = emptyList()
        _activeQueueId.value = null
        _activeQueueName.value = "Select Queue"
        
        try {
            player.stop()
            player.clearMediaItems()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreLastQueueAwait(): Boolean = withContext(Dispatchers.IO) {
        try {
            val isAlreadyRestored = withContext(Dispatchers.Main) {
                workingSongs.isNotEmpty() && player.mediaItemCount > 0
            }
            if (isAlreadyRestored) {
                return@withContext true
            }

            var targetQueueId = getActiveQueueIdFromPrefs()
            var queue = if (targetQueueId != null && targetQueueId != -1L) {
                queueDao.getQueueById(targetQueueId)
            } else null

            if (queue == null) {
                val allQueues = queueDao.getAllQueues().firstOrNull() ?: emptyList()
                queue = allQueues.maxByOrNull { it.lastPlayedAt } ?: allQueues.firstOrNull()
                targetQueueId = queue?.id
                if (targetQueueId != null) {
                    setActiveQueueIdInPrefs(targetQueueId)
                }
            }

            if (queue == null || targetQueueId == null) return@withContext false

            var items = queueDao.getQueueItemsList(targetQueueId)
            if (items.isEmpty()) {
                val allQueues = queueDao.getAllQueues().firstOrNull() ?: emptyList()
                val fallback = allQueues.firstOrNull { it.id != targetQueueId && queueDao.getQueueItemsList(it.id).isNotEmpty() }
                if (fallback != null) {
                    queue = fallback
                    targetQueueId = fallback.id
                    setActiveQueueIdInPrefs(targetQueueId)
                    items = queueDao.getQueueItemsList(targetQueueId)
                }
            }
            if (items.isEmpty()) return@withContext false

            _activeQueueName.value = queue.name

            val uniqueItems = items.distinctBy { it.positionInQueue }
            val cachedSongs = libraryDao.getAllSongsList()
            val songMap = if (cachedSongs.isNotEmpty()) cachedSongs.associateBy { it.id } else emptyMap()
            val restoredSongs = uniqueItems.map { item ->
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
                val savedPrefIndex = prefs.getInt("queue_${targetQueueId}_index", queue.currentItemIndex)
                val savedPrefPosition = prefs.getLong("queue_${targetQueueId}_position", queue.currentPlaybackPositionMs)

                val startIdx = savedPrefIndex.coerceIn(0, (restoredSongs.size - 1).coerceAtLeast(0))
                val startPos = savedPrefPosition.coerceAtLeast(0L)

                val mediaItems = restoredSongs.mapIndexed { idx, s ->
                    buildMediaItem(s, loadArtworkBytes = (idx == startIdx))
                }

                val shouldResume = try {
                    settingsRepository.resumeOnStart.firstOrNull() ?: false
                } catch (_: Exception) { false }

                withContext(Dispatchers.Main) {
                    workingSongs = restoredSongs
                    originalSongs = restoredSongs
                    _activeQueueId.value = targetQueueId
                    _activeQueueName.value = queue.name
                    _isShuffleEnabled.value = queue.shuffleState

                    val exoPlayer = player as? androidx.media3.exoplayer.ExoPlayer
                    if (exoPlayer != null) {
                        exoPlayer.playWhenReady = shouldResume
                        exoPlayer.setMediaItems(mediaItems, startIdx, startPos)
                        exoPlayer.prepare()
                        if (shouldResume) {
                            exoPlayer.play()
                        }
                    }

                    val startSong = restoredSongs[startIdx]
                    WidgetStateRepository.updateState(
                        context = context,
                        song = startSong,
                        isPlaying = shouldResume,
                        positionMs = startPos,
                        durationMs = startSong.duration?.toLong() ?: 0L
                    )
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    fun restoreLastQueue() {
        queueJob?.cancel()
        scope.launch(Dispatchers.Main) {
            restoreLastQueueAwait()
        }
    }

    fun saveCurrentState() {
        val activeId = _activeQueueId.value
        if (activeId != null && activeId != -1L) {
            try {
                val (currentIndex, position) = if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                    Pair(
                        player.currentMediaItemIndex.coerceAtLeast(0),
                        player.currentPosition.coerceAtLeast(0L)
                    )
                } else {
                    kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                        Pair(
                            player.currentMediaItemIndex.coerceAtLeast(0),
                            player.currentPosition.coerceAtLeast(0L)
                        )
                    }
                }
                val isShuffle = _isShuffleEnabled.value

                prefs.edit()
                    .putInt("queue_${activeId}_index", currentIndex)
                    .putLong("queue_${activeId}_position", position)
                    .commit()

                scope.launch(Dispatchers.IO) {
                    queueDao.updateQueueState(
                        queueId = activeId,
                        index = currentIndex,
                        position = position,
                        shuffleState = isShuffle,
                        lastPlayedAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun saveCurrentStateSync() {
        saveCurrentState()
    }

    fun removeSongsByPathPrefix(pathPrefix: String) {
        purgeExcludedSongs(setOf(pathPrefix))
    }

    fun purgeExcludedSongs(excludedFolders: Set<String>) {
        if (excludedFolders.isEmpty()) return
        scope.launch(Dispatchers.Main) {
            val cleanSet = excludedFolders.map { it.trimEnd('/') }.filter { it.isNotBlank() }.toSet()
            if (cleanSet.isEmpty()) return@launch

            val isExcluded: (String) -> Boolean = { path ->
                cleanSet.any { clean -> path.startsWith(clean) || path.startsWith("file://$clean") }
            }

            val filteredWorking = workingSongs.filter { !isExcluded(it.path) }
            val hadRemoval = filteredWorking.size != workingSongs.size

            if (hadRemoval) {
                workingSongs = filteredWorking
                originalSongs = originalSongs.filter { !isExcluded(it.path) }
                cachedSongs = cachedSongs.filter { !isExcluded(it.path) }

                val currentMediaItem = player.currentMediaItem
                val currentSongId = currentMediaItem?.mediaId?.toLongOrNull()
                val currentSongIsRemoved = currentSongId != null && workingSongs.none { it.id == currentSongId }

                if (currentSongIsRemoved) {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    } else if (filteredWorking.isNotEmpty()) {
                        player.seekToDefaultPosition(0)
                    } else {
                        player.stop()
                        player.clearMediaItems()
                    }
                }

                for (i in player.mediaItemCount - 1 downTo 0) {
                    val mediaItem = player.getMediaItemAt(i)
                    val mId = mediaItem.mediaId.toLongOrNull()
                    val uriStr = mediaItem.localConfiguration?.uri?.toString() ?: ""
                    val isMatch = (mId != null && originalSongs.none { it.id == mId } && workingSongs.none { it.id == mId })
                        || cleanSet.any { uriStr.contains(it) }
                    if (isMatch) {
                        player.removeMediaItem(i)
                    }
                }

                val activeQName = _activeQueueName.value
                if (filteredWorking.isNotEmpty()) {
                    persistFullQueue(activeQName, filteredWorking, _isShuffleEnabled.value, player.currentMediaItemIndex.coerceAtLeast(0))
                } else {
                    clearQueue()
                }
            }

            withContext(Dispatchers.IO) {
                for (clean in cleanSet) {
                    queueDao.deleteQueueItemsByPathPrefix(clean)
                }
                queueDao.deleteEmptyUserQueues()

                val allQueues = queueDao.getAllQueues().firstOrNull() ?: emptyList()
                for (q in allQueues) {
                    val items = queueDao.getQueueItemsList(q.id)
                    if (items.isEmpty() && q.isPermanent) {
                        queueDao.updateQueueState(q.id, 0, 0L, q.shuffleState, q.lastPlayedAt)
                    } else if (items.isNotEmpty() && q.currentItemIndex >= items.size) {
                        val safeIdx = items.lastIndex
                        queueDao.updateQueueState(q.id, safeIdx, 0L, q.shuffleState, q.lastPlayedAt)
                    }
                }
            }
        }
    }
}
