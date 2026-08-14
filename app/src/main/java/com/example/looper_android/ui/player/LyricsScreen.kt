package com.example.looper_android.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.example.looper_android.ui.components.AnimatedPlayPauseIcon
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper
import com.example.looper_android.ui.widgets.ExpressiveSlider
import com.example.looper_android.ui.widgets.PremiumSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.looper_android.ui.widgets.sharedBoundsOrElement

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LyricsScreen(
    viewModel: PlaybackViewModel,
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val state by viewModel.playbackState.collectAsState()
    val lyricsState by lyricsViewModel.uiState.collectAsState()
    val haptics = rememberHapticFeedbackHelper()

    val darkTheme by settingsViewModel.darkTheme.collectAsState()
    val lyricsScreenDarkness by settingsViewModel.lyricsScreenDarkness.collectAsState()
    val disableLyricFontScaling by settingsViewModel.disableLyricFontScaling.collectAsState()
    val lyricsAlignment by settingsViewModel.lyricsAlignment.collectAsState()
    val dynamicTheming by settingsViewModel.dynamicTheming.collectAsState()
    val disableBlur by settingsViewModel.disableBlur.collectAsState()
    val dynamicLyrics by settingsViewModel.dynamicLyrics.collectAsState()
    val blurredArtworkForLyrics by settingsViewModel.blurredArtworkForLyrics.collectAsState()
    val effectiveDarkTheme = darkTheme && !dynamicTheming
    val dynamicColorActiveLyrics by settingsViewModel.dynamicColorActiveLyrics.collectAsState()
    val animatedAccentColor by animateColorAsState(
        targetValue = state.accentColor,
        animationSpec = tween(durationMillis = 600),
        label = "lyricsAccentAnim"
    )

    val lyricsHazeState = remember { HazeState() }
    var rawVerticalOffset by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentSong) {
        lyricsViewModel.loadLyricsForSong(state.currentSong)
    }

    var showControls by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun resetHideTimer() {
        showControls = true
        coroutineScope.launch {
            delay(3500)
            showControls = false
        }
    }

    LaunchedEffect(Unit) {
        resetHideTimer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (effectiveDarkTheme) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
                if (showControls) resetHideTimer()
            }
    ) {
        val artPath = state.currentSong?.artPath
        val baseColor = if (effectiveDarkTheme) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background
        
        Box(modifier = Modifier.fillMaxSize().haze(lyricsHazeState)) {
            if (dynamicLyrics && blurredArtworkForLyrics && !artPath.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = lyricsScreenDarkness))
                )
            } else if (dynamicLyrics) {
                // Gradient from music screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        0.0f to animatedAccentColor.copy(alpha = 0.55f),
                                        0.35f to animatedAccentColor.copy(alpha = 0.40f),
                                        0.60f to animatedAccentColor.copy(alpha = 0.22f),
                                        0.80f to animatedAccentColor.copy(alpha = 0.06f),
                                        1.0f to Color.Black,
                                        center = Offset(size.width, 0f),
                                        radius = size.height * 0.88f
                                    )
                                )
                            }
                        }
                )
            } else {
                // Grayish color or pure black
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                rawVerticalOffset = 0f
                                isDismissing = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0 || rawVerticalOffset > 0) {
                                    change.consume()
                                    rawVerticalOffset = (rawVerticalOffset + dragAmount).coerceAtLeast(0f)
                                }
                            },
                            onDragEnd = {
                                if (rawVerticalOffset > 50.dp.toPx() && !isDismissing) {
                                    isDismissing = true
                                    haptics.mediumImpact()
                                    onBack()
                                }
                                rawVerticalOffset = 0f
                            },
                            onDragCancel = {
                                rawVerticalOffset = 0f
                                isDismissing = false
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.currentSong?.artPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .sharedBoundsOrElement("album_art", sharedTransitionScope, animatedVisibilityScope)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentSong?.title ?: "Unknown Title",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1
                    )
                    val artistName = state.currentSong?.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                    val albumName = state.currentSong?.album?.trim()?.ifBlank { null }
                    val subtitleText = if (!albumName.isNullOrEmpty() && albumName != "Unknown Album") {
                        "$artistName • $albumName"
                    } else {
                        artistName
                    }
                    Text(
                        text = subtitleText,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1
                    )
                }

                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    cornerRadius = 32.dp,
                    backgroundColor = Color.Transparent,
                    onClick = {
                        haptics.lightImpact()
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (lyricsState.isLoading) {
                } else if (lyricsState.parsedLines.isNotEmpty()) {
                    AdvancedLyricRenderer(
                        lines = lyricsState.parsedLines,
                        currentPositionMs = state.position,
                        onSeek = { pos ->
                            viewModel.seekTo(pos)
                            resetHideTimer()
                        },
                        disableFontScaling = disableLyricFontScaling,
                        alignment = lyricsAlignment,
                        activeColor = if (dynamicColorActiveLyrics) state.accentColor else Color.White,
                        modifier = Modifier.fillMaxSize(),
                        onUserScroll = { resetHideTimer() }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lyrics not available.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 18.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }
        }

        val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp + navBottomPadding)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.2f to Color.Black.copy(alpha = 0.8f),
                            0.75f to Color.Black.copy(alpha = 0.95f),
                            1.0f to Color.Black
                        )
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = navBottomPadding + 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column {
                    Box(
                        modifier = Modifier.sharedBoundsOrElement(
                            "player_seek_bar",
                            sharedTransitionScope,
                            animatedVisibilityScope
                        )
                    ) {
                        ExpressiveSlider(
                            positionMs = state.position,
                            durationMs = state.duration,
                            isPlaying = state.isPlaying,
                            onSeek = { pos ->
                                viewModel.seekTo(pos)
                                resetHideTimer()
                            },
                            activeColor = state.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumSection(
                            modifier = Modifier
                                .weight(1f)
                                .sharedBoundsOrElement(
                                    "player_prev_btn",
                                    sharedTransitionScope,
                                    animatedVisibilityScope
                                ),
                            height = 80.dp,
                            enableBlur = !disableBlur,
                            hazeState = lyricsHazeState,
                            shape = RoundedCornerShape(
                                topStart = 40.dp,
                                bottomStart = 40.dp,
                                topEnd = 12.dp,
                                bottomEnd = 12.dp
                            ),
                            onClick = {
                                haptics.lightImpact()
                                viewModel.skipToPrevious()
                                resetHideTimer()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.SkipBack,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        PremiumSection(
                            modifier = Modifier
                                .weight(1f)
                                .sharedBoundsOrElement(
                                    "player_play_pause_btn",
                                    sharedTransitionScope,
                                    animatedVisibilityScope
                                ),
                            height = 80.dp,
                            cornerRadius = 12.dp,
                            enableBlur = !disableBlur,
                            hazeState = lyricsHazeState,
                            backgroundColor = if (state.isPlaying) androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant else state.accentColor,
                            onClick = {
                                haptics.mediumImpact()
                                viewModel.togglePlayPause()
                                resetHideTimer()
                            }
                        ) {
                            AnimatedPlayPauseIcon(
                                isPlaying = state.isPlaying,
                                tint = if (state.isPlaying) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        PremiumSection(
                            modifier = Modifier
                                .weight(1f)
                                .sharedBoundsOrElement(
                                    "player_next_btn",
                                    sharedTransitionScope,
                                    animatedVisibilityScope
                                ),
                            height = 80.dp,
                            enableBlur = !disableBlur,
                            hazeState = lyricsHazeState,
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                bottomStart = 12.dp,
                                topEnd = 40.dp,
                                bottomEnd = 40.dp
                            ),
                            onClick = {
                                haptics.lightImpact()
                                viewModel.skipToNext()
                                resetHideTimer()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.SkipForward,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
