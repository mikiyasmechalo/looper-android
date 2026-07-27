package com.example.looper_android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queues")
    fun getAllQueues(): Flow<List<QueueEntity>>

    @Query("SELECT * FROM queues WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveQueue(): QueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: QueueEntity): Long

    @Query("UPDATE queues SET isActive = 0")
    suspend fun deactivateAllQueues()

    @Transaction
    suspend fun setActiveQueue(queueId: Long) {
        deactivateAllQueues()
        setActiveStatus(queueId, true)
    }

    @Query("UPDATE queues SET isActive = :isActive WHERE id = :queueId")
    suspend fun setActiveStatus(queueId: Long, isActive: Boolean)

    @Query("SELECT * FROM queue_items WHERE queueId = :queueId ORDER BY positionInQueue ASC")
    fun getItemsForQueue(queueId: Long): Flow<List<QueueItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)
    
    @Query("UPDATE queues SET currentItemIndex = :index, currentPlaybackPositionMs = :position WHERE id = :queueId")
    suspend fun updateQueueState(queueId: Long, index: Int, position: Long)
}
