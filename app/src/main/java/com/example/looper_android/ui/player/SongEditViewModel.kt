package com.example.looper_android.ui.player

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.AudioTagReader
import com.example.looper_android.data.repository.AudioTagWriter
import com.example.looper_android.data.repository.WriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PendingWritePermission(
    val intentSender: IntentSender,
    val pendingSong: SongEntity,
    val pendingEdit: AudioTagWriter.TagEdit
)

@HiltViewModel
class SongEditViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val tagWriter: AudioTagWriter,
    private val tagReader: AudioTagReader
) : ViewModel() {

    private val _pendingPermission = MutableStateFlow<PendingWritePermission?>(null)
    val pendingPermission: StateFlow<PendingWritePermission?> = _pendingPermission.asStateFlow()

    fun refreshSongFromDisk(song: SongEntity, onRefreshed: (SongEntity) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val freshTag = try {
                if (song.path.startsWith("content://")) {
                    tagReader.read(Uri.parse(song.path))
                } else {
                    tagReader.read(song.path)
                }
            } catch (_: Exception) { null }

            val refreshed = if (freshTag != null) {
                song.copy(
                    title = freshTag.title ?: song.title,
                    artist = freshTag.artist ?: song.artist,
                    album = freshTag.album ?: song.album,
                    albumArtist = freshTag.albumArtist ?: song.albumArtist,
                    year = freshTag.year ?: song.year,
                    genre = freshTag.genre ?: song.genre,
                    lyrics = freshTag.lyrics ?: song.lyrics
                )
            } else song

            if (refreshed != song) {
                libraryDao.updateSong(refreshed)
            }

            withContext(Dispatchers.Main) {
                onRefreshed(refreshed)
            }
        }
    }

    fun saveEdit(
        song: SongEntity,
        newTitle: String,
        newArtist: String? = null,
        newAlbum: String? = null,
        newYear: Int?,
        newGenre: String?,
        newLyrics: String?,
        newArtPath: String? = null,
        onComplete: ((success: Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTitle = newTitle.trim().ifBlank { song.title }
            val updatedArtist = newArtist?.trim()?.ifBlank { null } ?: song.artist
            val updatedAlbum = newAlbum?.trim()?.ifBlank { null } ?: song.album
            val updatedGenre = newGenre?.trim()?.ifBlank { null }
            val updatedLyrics = newLyrics?.trim()?.ifBlank { null }
            val updatedArtPath = when (newArtPath) {
                "" -> null
                null -> song.artPath
                else -> newArtPath
            }

            val updatedSong = song.copy(
                title = updatedTitle,
                artist = updatedArtist,
                album = updatedAlbum,
                year = newYear,
                genre = updatedGenre,
                lyrics = updatedLyrics,
                artPath = updatedArtPath
            )

            val edit = AudioTagWriter.TagEdit(
                title = updatedTitle,
                artist = updatedArtist,
                album = updatedAlbum,
                albumArtist = song.albumArtist,
                year = newYear,
                genre = updatedGenre,
                lyrics = updatedLyrics,
                trackNumber = song.trackNumber
            )

            // 1. Attempt physical disk write FIRST before mutating Room DB
            val writeResult = tagWriter.write(song.path, edit)

            withContext(Dispatchers.Main) {
                when (writeResult) {
                    is WriteResult.Success -> {
                        // Physical write succeeded -> update Room DB
                        viewModelScope.launch(Dispatchers.IO) {
                            libraryDao.updateSong(updatedSong)
                        }
                        onComplete?.invoke(true)
                    }
                    is WriteResult.RequiresPermission -> {
                        // Store proposed update in pending request; do NOT mutate Room DB yet
                        _pendingPermission.value = PendingWritePermission(
                            intentSender = writeResult.intentSender,
                            pendingSong = updatedSong,
                            pendingEdit = edit
                        )
                        onComplete?.invoke(false)
                    }
                    is WriteResult.Failure -> {
                        // Physical write failed -> do NOT mutate Room DB
                        onComplete?.invoke(false)
                    }
                }
            }
        }
    }

    fun onPermissionGranted() {
        val pending = _pendingPermission.value ?: return
        _pendingPermission.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = tagWriter.write(pending.pendingSong.path, pending.pendingEdit)
            withContext(Dispatchers.Main) {
                if (result is WriteResult.Success) {
                    // Update Room DB ONLY after single-file write permission was granted AND write succeeded
                    viewModelScope.launch(Dispatchers.IO) {
                        libraryDao.updateSong(pending.pendingSong)
                    }
                }
            }
        }
    }

    fun onPermissionDenied() {
        _pendingPermission.value = null
    }
}
