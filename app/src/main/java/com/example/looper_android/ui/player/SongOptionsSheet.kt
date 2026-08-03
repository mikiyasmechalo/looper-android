package com.example.looper_android.ui.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CirclePlay
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.SquarePen
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.User
import com.composables.icons.lucide.X
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection
import java.io.File
import com.example.looper_android.ui.library.AllSongsViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Public API (matches Flutter's showSongOptionsBottomSheet)
// ─────────────────────────────────────────────────────────────────────────────

enum class ActiveSheet {
    OPTIONS, EDIT, DELETE, DETAILS, TECH_INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    accentColor: Color = Color(0xFF55DF69),
    useBlur: Boolean = false,
    isPureBlack: Boolean = false,
    playlist: com.example.looper_android.data.db.PlaylistEntity? = null,
    isCurrentlyPlaying: Boolean = false,
    showGoToArtist: Boolean = true,
    showGoToAlbum: Boolean = true,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((String) -> Unit)? = null,
) {
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val allSongsViewModel: AllSongsViewModel = hiltViewModel()
    val songEditViewModel: SongEditViewModel = hiltViewModel()

    val writePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            songEditViewModel.onPermissionGranted()
        } else {
            songEditViewModel.onPermissionDenied()
        }
    }

    val pendingWritePermission by songEditViewModel.pendingPermission.collectAsState()
    LaunchedEffect(pendingWritePermission) {
        pendingWritePermission?.let { pending ->
            val request = androidx.activity.result.IntentSenderRequest.Builder(pending.intentSender).build()
            writePermissionLauncher.launch(request)
        }
    }

    val playbackState by playbackViewModel.playbackState.collectAsState()
    val isFavorite = song.isFavorite
    val showSleepTimer = playbackState.isSleepTimerActive
    val sleepTimerLabel = playbackState.sleepTimerLabel

    var activeSheet by remember { mutableStateOf<ActiveSheet?>(ActiveSheet.OPTIONS) }

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    fun hapticLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun hapticMedium() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun hapticHeavy() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    val sheetBg = if (isPureBlack) {
        Color.Black
    } else {
        Color(0xFF1E1E1E)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (activeSheet) {
        ActiveSheet.OPTIONS -> {
            ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
//            isFocusable = true
        )
    ) {
        val content: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = sheetBg,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
            ) {
                // ── Header ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                    ) {
                        com.example.looper_android.ui.components.OptimizedImage(
                            url = song.artPath,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist ?: "Unknown Artist",
                            color = if (onArtistClick != null && !song.artist.isNullOrBlank()) accentColor else Color.White.copy(alpha = 0.54f),
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (onArtistClick != null && !song.artist.isNullOrBlank()) {
                                Modifier.clickable {
                                    hapticLight()
                                    onDismiss()
                                    onArtistClick(song.artist!!)
                                }
                            } else Modifier
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.10f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Options ──────────────────────────────────────────────
                data class OptionTileData(
                    val svgAsset: String?,
                    val vectorIcon: ImageVector?,
                    val label: String,
                    val onClick: () -> Unit
                )

                val options = buildList {
                    if (!isCurrentlyPlaying) {
                        add(OptionTileData(null, Lucide.CirclePlay, "Play Next") {
                            hapticLight()
                            playbackViewModel.addNext(song)
                            onDismiss()
                        })
                        add(OptionTileData(null, Lucide.ListPlus, "Add to Queue") {
                            hapticLight()
                            playbackViewModel.addToQueue(song)
                            onDismiss()
                        })
                    }
                    if (showGoToArtist && onArtistClick != null && !song.artist.isNullOrBlank()) {
                        add(OptionTileData(null, Lucide.User, "Go to Artist") {
                            hapticLight()
                            onDismiss()
                            onArtistClick(song.artist!!)
                        })
                    }
                    if (showGoToAlbum && onAlbumClick != null && !song.album.isNullOrBlank()) {
                        add(OptionTileData(null, Lucide.Disc, "Go to Album") {
                            hapticLight()
                            onDismiss()
                            onAlbumClick(song.album!!)
                        })
                    }
                    add(OptionTileData(null, Lucide.ListMusic, "Add to Playlists") {
                        hapticLight()
                        // TODO: Implement Add to Playlist
                        onDismiss()
                    })
                    if (showSleepTimer) {
                        add(OptionTileData(null, Lucide.Timer, if (showSleepTimer) sleepTimerLabel else "Sleep Timer") {
                            hapticLight()
                            // TODO: Implement Sleep Timer dialog
                            onDismiss()
                        })
                    }
                    add(OptionTileData(
                        null,
                        Lucide.Star,
                        if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                    ) {
                        hapticLight()
                        allSongsViewModel.updateSong(song.copy(isFavorite = !song.isFavorite))
                        onDismiss()
                    })

                    if (playlist != null) {
                        add(OptionTileData(null, Lucide.Trash2, "Remove from Playlist") {
                            hapticMedium()
                            // TODO: Implement Remove from Playlist
                            onDismiss()
                        })
                    }

                    add(OptionTileData(null, Lucide.SquarePen, "Edit Song Info") {
                        hapticLight()
                        activeSheet = ActiveSheet.EDIT
                    })
                    add(OptionTileData(null, Lucide.SlidersHorizontal, "Equalizer") {
                        hapticLight()
                        // TODO: Implement Equalizer
                        onDismiss()
                    })
                    add(OptionTileData(null, Lucide.Info, "Song Details") {
                        hapticLight()
                        activeSheet = ActiveSheet.DETAILS
                    })
                    add(OptionTileData(null, Lucide.Share2, "Share") {
                        hapticLight()
                        onDismiss()
                        try {
                            val uri = if (song.path.startsWith("content://")) {
                                Uri.parse(song.path)
                            } else {
                                val file = File(song.path)
                                try {
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                } catch (_: Exception) {
                                    Uri.fromFile(file)
                                }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, song.title)
                                putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist ?: "Unknown"}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Audio"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    })
                    add(OptionTileData(null, Lucide.Trash2, "Delete Song") {
                        hapticHeavy()
                        activeSheet = ActiveSheet.DELETE
                    })
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        PremiumSection(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color(0xFF1E1E1E),
                            onClick = null
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                options.forEachIndexed { index, option ->
                                    val (svgAsset, vectorIcon, label, onClick) = option
                                    val isDelete = label == "Delete Song"
                                    val isRemoveFromPlaylist = label == "Remove from Playlist"
                                    val isFav = (label == "Remove from Favorites" || label.contains("Favorite")) && isFavorite
                                    val iconColor = when {
                                        isDelete || isRemoveFromPlaylist || isFav -> Color(0xFFFF4D4D)
                                        else -> accentColor
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, radius = 300.dp),
                                                onClick = onClick
                                            )
                                            .padding(horizontal = 20.dp, vertical = 15.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val vectorIcon: ImageVector? = option.vectorIcon
                                        if (vectorIcon != null) {
                                            Icon(
                                                imageVector = vectorIcon,
                                                contentDescription = label,
                                                tint = iconColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontFamily = DmSansFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (index < options.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            thickness = 0.8.dp,
                                            color = Color.White.copy(alpha = 0.04f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Blur wrapper (matches Flutter BackdropFilter)
        if (useBlur && !isPureBlack && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(16f, 16f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
        ActiveSheet.EDIT -> {
            EditSongSheet(
                song = song,
                accentColor = accentColor,
                onDismiss = onDismiss,
                onSave = { title, artist, album, year, genre, lyrics, artPath ->
                    songEditViewModel.saveEdit(
                        song = song,
                        newTitle = title,
                        newArtist = artist,
                        newAlbum = album,
                        newYear = if (year > 0) year else null,
                        newGenre = genre,
                        newLyrics = lyrics,
                        newArtPath = artPath
                    )
                    onDismiss()
                }
            )
        }
        ActiveSheet.DELETE -> {
            DeleteSongDialog(
                songTitle = song.title,
                onDismiss = { activeSheet = ActiveSheet.OPTIONS },
                onConfirm = {
                    if (isCurrentlyPlaying) {
                        playbackViewModel.skipToNext()
                    }
                    allSongsViewModel.deleteSong(song)
                    onDismiss()
                }
            )
        }
        ActiveSheet.DETAILS -> {
            SongDetailsBottomSheet(
                song = song,
                onDismiss = onDismiss,
                accentColor = accentColor,
                showGoToArtist = showGoToArtist,
                showGoToAlbum = showGoToAlbum,
                onArtistClick = { artist ->
                    onDismiss()
                    onArtistClick?.invoke(artist)
                },
                onAlbumClick = { album ->
                    onDismiss()
                    onAlbumClick?.invoke(album)
                },
                onTechnicalInfoClick = {
                    activeSheet = ActiveSheet.TECH_INFO
                }
            )
        }
        ActiveSheet.TECH_INFO -> {
            TechnicalInfoBottomSheet(
                song = song,
                onDismiss = { activeSheet = ActiveSheet.DETAILS },
                accentColor = accentColor
            )
        }
        null -> {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Edit Song Sheet (matches Flutter EditSongSheet)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongSheet(
    song: SongEntity,
    accentColor: Color = Color(0xFF55DF69),
    useBlur: Boolean = false,
    isPureBlack: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        artist: String,
        album: String,
        year: Int,
        genre: String,
        lyrics: String,
        artPath: String? // null = unchanged, "" = cleared, else = new path
    ) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var title by remember(song) { mutableStateOf(song.title) }
    var artist by remember(song) { mutableStateOf(song.artist ?: "") }
    var album by remember(song) { mutableStateOf(song.album ?: "") }
    var year by remember(song) {
        mutableStateOf(
            if (song.year != null && song.year > 0) song.year.toString() else ""
        )
    }
    var genre by remember(song) { mutableStateOf(song.genre ?: "") }
    var lyrics by remember(song) { mutableStateOf(song.lyrics ?: "") }
    var pickedArtPath by remember(song) { mutableStateOf<String?>(null) } // null = unchanged
    var isSaving by remember { mutableStateOf(false) }

    val songEditViewModel: SongEditViewModel = hiltViewModel()
    LaunchedEffect(song) {
        songEditViewModel.refreshSongFromDisk(song) { fresh ->
            title = fresh.title
            artist = fresh.artist ?: ""
            album = fresh.album ?: ""
            year = if (fresh.year != null && fresh.year > 0) fresh.year.toString() else ""
            genre = fresh.genre ?: ""
            lyrics = fresh.lyrics ?: ""
        }
    }

    val currentArtPath = pickedArtPath ?: song.artPath

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedArtPath = it.toString()
        }
    }

    val sheetBg = Color(0xFF1E1E1E)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = { }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = sheetBg,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            if (useBlur && !isPureBlack && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(16f, 16f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.SquarePen,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Song Info",
                            color = Color.White,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = song.title,
                            color = Color.White.copy(alpha = 0.50f),
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                onSave(
                                    title.trim(),
                                    artist.trim(),
                                    album.trim(),
                                    year.toIntOrNull() ?: 0,
                                    genre.trim(),
                                    lyrics.trim(),
                                    pickedArtPath
                                )
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "Save",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val nestedScrollConnection = remember(scrollState) {
                    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                        override fun onPreScroll(
                            available: androidx.compose.ui.geometry.Offset,
                            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                        ): androidx.compose.ui.geometry.Offset {
                            val delta = available.y
                            return if (delta < 0 && scrollState.value < scrollState.maxValue) {
                                val consumed = -scrollState.dispatchRawDelta(-delta)
                                androidx.compose.ui.geometry.Offset(0f, -consumed)
                            } else {
                                androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                ) {
                    // Artwork Section
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val artToDisplay = when {
                            pickedArtPath == "" -> null
                            pickedArtPath != null -> pickedArtPath
                            else -> currentArtPath
                        }

                        if (!artToDisplay.isNullOrEmpty()) {
                            OptimizedImage(
                                url = artToDisplay,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Music,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Choose Art",
                                    color = Color.White.copy(alpha = 0.40f),
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Edit camera badge overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accentColor)
                                .border(
                                    width = 2.dp,
                                    color = sheetBg,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Camera,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!currentArtPath.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { pickedArtPath = "" },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remove artwork",
                                color = Color.Red.copy(alpha = 0.9f),
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fields
                    SectionLabel("TRACK INFO")
                    Spacer(modifier = Modifier.height(12.dp))

                    EditField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title *",
                        icon = Lucide.Music,
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EditField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = "Artist",
                        icon = Lucide.User,
                        accentColor = accentColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    EditField(
                        value = album,
                        onValueChange = { album = it },
                        label = "Album",
                        icon = Lucide.Disc,
                        accentColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                    SectionLabel("DETAILS")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row {
                        EditField(
                            value = year,
                            onValueChange = { if (it.length <= 4) year = it },
                            label = "Year",
                            icon = Lucide.Calendar,
                            accentColor = accentColor,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(2f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        EditField(
                            value = genre,
                            onValueChange = { genre = it },
                            label = "Genre",
                            icon = Lucide.Tag,
                            accentColor = accentColor,
                            modifier = Modifier.weight(3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    SectionLabel("LYRICS")
                    Spacer(modifier = Modifier.height(12.dp))

                    EditField(
                        value = lyrics,
                        onValueChange = { lyrics = it },
                        label = "Lyrics (Plain text or LRC)",
                        icon = Lucide.MicVocal,
                        accentColor = accentColor,
                        maxLines = 8,
                        keyboardType = KeyboardType.Text,
                        hint = "Enter plain lyrics or synchronized LRC lyrics format [00:00.00]..."
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // File path info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.AudioLines,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = song.path.substringAfterLast("/"),
                            color = Color.White.copy(alpha = 0.38f),
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save button
                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                onSave(
                                    title.trim(),
                                    artist.trim(),
                                    album.trim(),
                                    year.toIntOrNull() ?: 0,
                                    genre.trim(),
                                    lyrics.trim(),
                                    pickedArtPath
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = accentColor.copy(alpha = 0.40f)
                        ),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Changes",
                                color = Color.Black,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Delete Confirmation Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeleteSongDialog(
    songTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Delete Song",
                    color = Color.White,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$songTitle\"? This action cannot be undone.",
                color = Color.White.copy(alpha = 0.60f),
                fontFamily = DmSansFontFamily,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    isDeleting = true
                    onConfirm()
                },
                enabled = !isDeleting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.9f)
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.54f),
                    fontFamily = DmSansFontFamily
                )
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Sleep Timer Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    isTimerActive: Boolean = false,
    activeLabel: String = "",
    accentColor: Color = Color(0xFF55DF69),
    useBlur: Boolean = false,
    isPureBlack: Boolean = false,
    onDismiss: () -> Unit,
    onStartTimer: (minutes: Int) -> Unit,
    onStartSongs: (count: Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    var customMinutes by remember { mutableIntStateOf(15) }
    var customSongs by remember { mutableIntStateOf(3) }

    val view = LocalView.current
    fun hapticLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
    fun hapticMedium() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    val sheetBg = if (isPureBlack) {
        Color.Black
    } else if (useBlur) {
        Color.Black.copy(alpha = 0.60f)
    } else {
        Color(0xFF1E1E1E)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = { }
    ) {
        val content: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = sheetBg,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isPureBlack) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sleep Timer",
                        color = Color.White,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isTimerActive) "Active: $activeLabel" else "Select when to pause music playback",
                    color = if (isTimerActive) accentColor else Color.White.copy(alpha = 0.54f),
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stop by time
                SectionLabel("STOP BY TIME")
                Spacer(modifier = Modifier.height(12.dp))

                PremiumSection(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFF141414),
                    onClick = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    hapticLight()
                                    if (customMinutes > 1) customMinutes--
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$customMinutes ${if (customMinutes == 1) "Min" else "Mins"}",
                                color = Color.White,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = {
                                    hapticLight()
                                    customMinutes++
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                hapticMedium()
                                onStartTimer(customMinutes)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(
                                text = "Start",
                                color = Color.Black,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 5, 10, 30, 45).forEach { min ->
                        TimerChip(
                            label = "$min Min",
                            onClick = {
                                hapticMedium()
                                onStartTimer(min)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stop by song count
                SectionLabel("STOP BY SONG COUNT")
                Spacer(modifier = Modifier.height(12.dp))

                PremiumSection(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFF141414),
                    onClick = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    hapticLight()
                                    if (customSongs > 1) customSongs--
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$customSongs ${if (customSongs == 1) "Song" else "Songs"}",
                                color = Color.White,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = {
                                    hapticLight()
                                    customSongs++
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                hapticMedium()
                                onStartSongs(customSongs)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(
                                text = "Start",
                                color = Color.Black,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Song chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(1, 2, 3, 5, 10).forEach { count ->
                        TimerChip(
                            label = "$count ${if (count == 1) "Song" else "Songs"}",
                            onClick = {
                                hapticMedium()
                                onStartSongs(count)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isTimerActive) {
                    OutlinedButton(
                        onClick = {
                            hapticMedium()
                            onCancelTimer()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Red.copy(alpha = 0.20f)
                        ),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.90f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Color.Red.copy(alpha = 0.90f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancel Sleep Timer",
                            color = Color.Red.copy(alpha = 0.90f),
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (useBlur && !isPureBlack && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(16f, 16f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playlist Selector Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlaylistSelectorDialog(
    playlists: List<com.example.looper_android.data.db.PlaylistEntity>,
    accentColor: Color = Color(0xFF55DF69),
    onDismiss: () -> Unit,
    onPlaylistSelected: (com.example.looper_android.data.db.PlaylistEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Add to Playlists",
                color = Color.White,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists created yet.",
                    color = Color.White.copy(alpha = 0.54f),
                    fontFamily = DmSansFontFamily
                )
            } else {
                Column {
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                fontFamily = DmSansFontFamily,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.54f),
                    fontFamily = DmSansFontFamily
                )
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.38f),
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accentColor: Color,
    svgAsset: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontFamily = DmSansFontFamily,
            fontSize = 15.sp
        ),
        label = {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.45f),
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp
            )
        },
        placeholder = if (hint != null) {
            {
                Text(
                    text = hint,
                    color = Color.White.copy(alpha = 0.25f),
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp
                )
            }
        } else null,
        leadingIcon = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        singleLine = maxLines == 1,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
private fun TimerChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

