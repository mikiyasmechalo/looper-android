package com.example.looper_android.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.repository.AudioTagWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongEditViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val tagWriter: AudioTagWriter
) : ViewModel() {

    fun saveEdit(
        song: SongEntity,
        newTitle: String,
        newArtist: String? = null,
        newAlbum: String? = null,
        newYear: Int?,
        newGenre: String?,
        newLyrics: String?,
        newArtPath: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
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

            // 1. Update Room immediately for responsive UI
            val updated = song.copy(
                title = updatedTitle,
                artist = updatedArtist,
                album = updatedAlbum,
                year = newYear,
                genre = updatedGenre,
                lyrics = updatedLyrics,
                artPath = updatedArtPath
            )
            libraryDao.updateSong(updated)

            // 2. Write back to the actual file on disk via Jaudiotagger
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

            val success = if (song.path.startsWith("content://")) {
                tagWriter.write(Uri.parse(song.path), edit)
            } else {
                tagWriter.write(song.path, edit)
            }

            onComplete?.invoke(success)
        }
    }
}
