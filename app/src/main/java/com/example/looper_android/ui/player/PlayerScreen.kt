package com.example.looper_android.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.AnimatedPlayPauseIcon
import com.example.looper_android.ui.components.AssetSvgIcon
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper
import com.example.looper_android.ui.widgets.*
import kotlin.math.roundToInt

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlaybackViewModel,
    onBack: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val state by viewModel.playbackState.collectAsState()
    val song = state.currentSong
    val haptics = rememberHapticFeedbackHelper()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    var rawVerticalOffset by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    if (song == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF11110E)),
            contentAlignment = Alignment.Center
        ) {
            Text("No song playing", color = Color.White, fontFamily = DmSansFontFamily)
        }
        return
    }

    val subtitleText = remember(song.artist, song.album) {
        val artist = song.artist ?: "Unknown Artist"
        if (!song.album.isNullOrEmpty() && song.album != "Unknown Album") {
            "$artist • ${song.album}"
        } else {
            artist
        }
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = state.accentColor,
        animationSpec = tween(durationMillis = 600),
        label = "accentColorAnim"
    )

    var isDismissing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11110E))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        rawVerticalOffset = 0f
                        isDismissing = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        rawVerticalOffset += dragAmount
                        if (rawVerticalOffset > 60.dp.toPx() && !isDismissing) {
                            isDismissing = true
                            haptics.mediumImpact()
                            onBack()
                        }
                    },
                    onDragEnd = {
                        rawVerticalOffset = 0f
                        isDismissing = false
                    },
                    onDragCancel = {
                        rawVerticalOffset = 0f
                        isDismissing = false
                    }
                )
            }
    ) {
        // Dynamic Accent Radial Gradient Layer (Uses single main accent color from artwork, animated 600ms)
        val maxDimensionPx = maxOf(screenWidthPx, screenHeightPx)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedAccentColor.copy(alpha = 0.42f),
                                    animatedAccentColor.copy(alpha = 0.18f),
                                    Color(0xFF11110E)
                                ),
                                center = Offset(screenWidthPx, 0f),
                                radius = maxDimensionPx * 1.35f
                            )
                        )
                    }
                }
        )

        SafeAreaColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button (48x48, radius 32dp, icon size 8dp)
                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    cornerRadius = 32.dp,
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    onClick = {
                        haptics.lightImpact()
                        onBack()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "down_arrow_icon.svg",
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }

                // Title & Quality badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Now Playing",
                        color = Color.White,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = state.audioQualityText,
                            color = Color.White.copy(alpha = 0.70f),
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            lineHeight = 10.sp,
                            modifier = Modifier.wrapContentHeight(unbounded = true) 
                        )
                    }
                }

                // Options / Sleep Timer button (48x48, radius 32dp, icon size 14dp)
                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    cornerRadius = 32.dp,
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    onClick = {
                        haptics.mediumImpact()
                        showOptionsSheet = true
                    }
                ) {
                    if (state.isSleepTimerActive) {
                        SleepTimerClock(
                            progress = 0.75f,
                            label = state.sleepTimerLabel
                        )
                    } else {
                        AssetSvgIcon(
                            assetName = "menu_button_icon.svg",
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2. Large Album Art with Gestures (Morphs from Mini Player album artwork)
            Padding(
                modifier = Modifier.padding(horizontal = 30.dp)
            ) {
                GestureArtwork(
                    song = song,
                    nextSong = state.queue.getOrNull(1),
                    prevSong = null,
                    onTap = {
                        haptics.lightImpact()
                        onNavigateToLyrics()
                    },
                    onSeekRelative = { offsetMs ->
                        viewModel.seekRelative(offsetMs)
                    },
                    onSkipNext = {
                        haptics.mediumImpact()
                        viewModel.skipToNext()
                    },
                    onSkipPrevious = {
                        haptics.mediumImpact()
                        viewModel.skipToPrevious()
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. Song Details & Favorite Button (Artist • Album Subtitle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedBoundsOrElement(
                            key = "song_title",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitleText,
                        color = Color.White.copy(alpha = 0.60f),
                        fontFamily = DmSansFontFamily,
                        fontSize = 16.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable {
                                song.artist?.let { onArtistClick(it) }
                            }
                            .sharedBoundsOrElement(
                                key = "song_artist",
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Favorite Button (56x56, radius 32dp, icon size 24dp)
                PremiumSection(
                    width = 56.dp,
                    height = 56.dp,
                    cornerRadius = 32.dp,
                    backgroundColor = if (state.isFavorite) Color(0x26FFC107) else Color.Transparent,
                    onClick = {
                        haptics.selectionClick()
                        viewModel.toggleFavorite()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "liked_icon.svg",
                        contentDescription = "Favorite",
                        tint = if (state.isFavorite) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Seek Bar
            Box(
                modifier = Modifier.padding(horizontal = 28.dp)
            ) {
                ExpressiveSlider(
                    positionMs = state.position,
                    durationMs = state.duration,
                    isPlaying = state.isPlaying,
                    onSeek = { targetMs ->
                        viewModel.seekTo(targetMs)
                    },
                    activeColor = state.accentColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Main Playback Control Row (Height 80dp, Prev/Next 16dp, Play/Pause 22dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Prev (16dp)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                    backgroundColor = Color(0xFF1E1E1E),
                    onClick = {
                        haptics.lightImpact()
                        viewModel.skipToPrevious()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "previous_button_icon.svg",
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Play / Pause (22dp) (Morphs from Mini Player play button)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .sharedBoundsOrElement("player_play_pause_btn", sharedTransitionScope, animatedVisibilityScope),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = if (!state.isPlaying) state.accentColor else Color(0xFF1E1E1E),
                    onClick = {
                        haptics.mediumImpact()
                        viewModel.togglePlayPause()
                    }
                ) {
                    AnimatedPlayPauseIcon(
                        isPlaying = state.isPlaying,
                        modifier = Modifier.size(22.dp),
                        tint = if (state.isPlaying) Color.White else Color(0xFF121212)
                    )
                }

                // Next (16dp)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 40.dp, bottomEnd = 40.dp),
                    backgroundColor = Color(0xFF1E1E1E),
                    onClick = {
                        haptics.lightImpact()
                        viewModel.skipToNext()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "next_button_icon.svg",
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Utility Controls Row (Height 64dp, Shuffle/Repeat/Lyrics 15dp, Queue 12dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Shuffle (15dp) (Morphs from Navbar Home tab)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope),
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                    backgroundColor = if (state.isShuffle) state.accentColor else Color(0xFF1E1E1E),
                    onClick = {
                        haptics.selectionClick()
                        viewModel.toggleShuffle()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "shuffle_button_icon.svg",
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffle) Color(0xFF121212) else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Repeat (15dp) (Morphs from Navbar Songs tab)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = if (state.repeatMode != RepeatMode.OFF) state.accentColor else Color(0xFF1E1E1E),
                    onClick = {
                        haptics.selectionClick()
                        viewModel.nextRepeatMode()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = if (state.repeatMode == RepeatMode.ONE) "repeat_1.svg" else "repeat_mode_icon.svg",
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) Color(0xFF121212) else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Lyrics (15dp) (Morphs from Navbar Library tab)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope),
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color(0xFF1E1E1E),
                    onClick = {
                        haptics.lightImpact()
                        onNavigateToLyrics()
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "lyrics_button_icon.svg",
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Queue (12dp)
                PremiumSection(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 32.dp, bottomEnd = 32.dp),
                    backgroundColor = Color(0xFF1E1E1E),
                    onClick = {
                        haptics.mediumImpact()
                        showQueueSheet = true
                    }
                ) {
                    AssetSvgIcon(
                        assetName = "queue_icon.svg",
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal Bottom Sheets
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = state.queue,
            currentSong = song,
            onDismiss = { showQueueSheet = false },
            onSongSelect = { selectedSong ->
                showQueueSheet = false
            }
        )
    }

    var showEditSheet by remember { mutableStateOf(false) }
    val songEditViewModel: SongEditViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    if (showOptionsSheet) {
        SongOptionsSheet(
            song = song,
            onDismiss = { showOptionsSheet = false },
            accentColor = state.accentColor,
            showSleepTimer = true,
            isSleepTimerActive = state.isSleepTimerActive,
            sleepTimerLabel = state.sleepTimerLabel,
            onPlayNextClick = { viewModel.addNext(song) },
            onAddToQueueClick = { viewModel.addToQueue(song) },
            onFavoriteClick = { viewModel.toggleFavorite(song) },
            onArtistClick = onArtistClick,
            onAlbumClick = onAlbumClick,
            onEqualizerClick = onNavigateToEqualizer,
            onEditSongClick = { showEditSheet = true }
        )
    }

    if (showEditSheet) {
        EditSongSheet(
            song = song,
            onDismiss = { showEditSheet = false },
            accentColor = state.accentColor,
            onSave = { newTitle, newArtist, newAlbum, newYear, newGenre, newLyrics, newArtPath ->
                songEditViewModel.saveEdit(
                    song = song,
                    newTitle = newTitle,
                    newArtist = newArtist,
                    newAlbum = newAlbum,
                    newYear = if (newYear > 0) newYear else null,
                    newGenre = newGenre,
                    newLyrics = newLyrics,
                    newArtPath = newArtPath
                )
                showEditSheet = false
            }
        )
    }
}

@Composable
private fun Padding(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

@Composable
private fun SafeAreaColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
