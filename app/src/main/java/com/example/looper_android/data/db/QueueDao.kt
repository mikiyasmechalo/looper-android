package com.example.looper_android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queues ORDER BY lastPlayedAt DESC")
    fun getAllQueues(): Flow<List<QueueEntity>>

    @Query("SELECT * FROM queues WHERE name = :name LIMIT 1")
    suspend fun getQueueByName(name: String): QueueEntity?

    @Query("SELECT COUNT(*) FROM queues WHERE isPermanent = 0")
    suspend fun countUserQueues(): Int

    @Query("SELECT * FROM queues WHERE isPermanent = 0 ORDER BY lastPlayedAt ASC LIMIT 1")
    suspend fun getOldestUserQueue(): QueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: QueueEntity): Long

    @Query("DELETE FROM queues WHERE id = :queueId")
    suspend fun deleteQueue(queueId: Long)

    @Query("DELETE FROM queue_items WHERE queueId = :queueId")
    suspend fun deleteQueueItems(queueId: Long)

    @Transaction
    suspend fun deleteQueueWithItems(queueId: Long) {
        deleteQueueItems(queueId)
        deleteQueue(queueId)
    }

    @Query("SELECT * FROM queue_items WHERE queueId = :queueId ORDER BY positionInQueue ASC")
    fun getItemsForQueue(queueId: Long): Flow<List<QueueItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)
    
    @Query("UPDATE queues SET currentItemIndex = :index, currentPlaybackPositionMs = :position, shuffleState = :shuffleState, lastPlayedAt = :lastPlayedAt WHERE id = :queueId")
    suspend fun updateQueueState(queueId: Long, index: Int, position: Long, shuffleState: Boolean, lastPlayedAt: Long)
    
    @Query("UPDATE queues SET lastPlayedAt = :lastPlayedAt WHERE id = :queueId")
    suspend fun updateLastPlayed(queueId: Long, lastPlayedAt: Long)
}
