package com.example.looper_android.ui.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.domain.QueueManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun player(): androidx.media3.common.Player
    fun queueManager(): QueueManager
    fun libraryDao(): LibraryDao
}

class PlayPauseActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val player = entryPoint.player()
        val queueManager = entryPoint.queueManager()

        val isEmpty = withContext(Dispatchers.Main) { player.mediaItemCount == 0 }
        if (isEmpty) {
            queueManager.restoreLastQueueAwait()
        }

        withContext(Dispatchers.Main) {
            try {
                if (player.mediaItemCount > 0) {
                    val willPlay = !player.isPlaying
                    WidgetStateRepository.updatePlayPauseOptimistic(context, willPlay)
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class NextActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val player = entryPoint.player()
        val queueManager = entryPoint.queueManager()

        val isEmpty = withContext(Dispatchers.Main) { player.mediaItemCount == 0 }
        if (isEmpty) {
            queueManager.restoreLastQueueAwait()
        }

        withContext(Dispatchers.Main) {
            try {
                if (player.mediaItemCount > 0) {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    } else {
                        player.seekTo(0, 0L)
                    }
                    player.play()
                    WidgetStateRepository.updatePlayPauseOptimistic(context, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class PreviousActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val player = entryPoint.player()
        val queueManager = entryPoint.queueManager()

        val isEmpty = withContext(Dispatchers.Main) { player.mediaItemCount == 0 }
        if (isEmpty) {
            queueManager.restoreLastQueueAwait()
        }

        withContext(Dispatchers.Main) {
            try {
                if (player.mediaItemCount > 0) {
                    if (player.currentPosition > 3000L) {
                        player.seekTo(0L)
                    } else if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    } else {
                        player.seekTo(0L)
                    }
                    player.play()
                    WidgetStateRepository.updatePlayPauseOptimistic(context, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ToggleFavoriteActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val player = entryPoint.player()
        val libraryDao = entryPoint.libraryDao()

        val currentMediaId = withContext(Dispatchers.Main) {
            player.currentMediaItem?.mediaId?.toLongOrNull()
        } ?: WidgetStateRepository.state.value.songId ?: return

        withContext(Dispatchers.IO) {
            try {
                val song = libraryDao.getSongByIdSync(currentMediaId) ?: return@withContext
                val newFav = !song.isFavorite
                libraryDao.updateFavorite(currentMediaId, newFav)
                WidgetStateRepository.updateFavoriteOptimistic(context, newFav)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
