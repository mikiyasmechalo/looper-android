package com.example.looper_android.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.looper_android.ui.widgets.sharedBoundsOrElement
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.composables.icons.lucide.EllipsisVertical
import com.example.looper_android.data.models.LyricsSyncMode

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
    val context = LocalContext.current
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
    val autoFallback by settingsViewModel.autoFallbackLyricsProviders.collectAsState()
    val enableInternet by settingsViewModel.enableInternet.collectAsState()
    val lyricsSyncStyle by settingsViewModel.lyricsSyncStyle.collectAsState()
    val lyricsProviderOrder by settingsViewModel.lyricsProviderOrder.collectAsState()
    val lyricsProvidersOff by settingsViewModel.lyricsProvidersOff.collectAsState()
    val effectiveDarkTheme = darkTheme && !dynamicTheming
    val dynamicColorActiveLyrics by settingsViewModel.dynamicColorActiveLyrics.collectAsState()
    val dynamicAccentColor by settingsViewModel.dynamicAccentColor.collectAsState()
    val enablePlayerGradient by settingsViewModel.enablePlayerGradient.collectAsState()
    val disableSquiggle by settingsViewModel.disableSquiggle.collectAsState()
    val customAccentColorInt by settingsViewModel.customAccentColor.collectAsState()

    val resolvedActiveColor = remember(state.accentColor, dynamicColorActiveLyrics, customAccentColorInt) {
        if (!dynamicColorActiveLyrics) {
            Color.White
        } else {
            val color = state.accentColor
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(
                android.graphics.Color.rgb(
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                ),
                hsl
            )
            // If the accent is monochromatic / grayish / near-white (saturation < 0.16),
            // it will be indistinguishable from inactive white/gray lyrics and background.
            // Fall back to the vibrant custom accent color!
            if (hsl[1] < 0.16f) {
                Color(customAccentColorInt)
            } else {
                color
            }
        }
    }

    val containerColor = when {
        !disableBlur -> {
            if (dynamicTheming || dynamicAccentColor) {
                state.accentColor.copy(alpha = 0.12f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        }
        effectiveDarkTheme -> Color(0xFF0A0A0A)
        enablePlayerGradient -> Color(0xFF262626)
        else -> Color(0xFF1E1E1E)
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = state.accentColor,
        animationSpec = tween(durationMillis = 600),
        label = "lyricsAccentAnim"
    )

    val lyricsHazeState = remember { HazeState() }
    var rawVerticalOffset by remember { mutableFloatStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var showLyricsMenuSheet by remember { mutableStateOf(false) }
    var showLyricsServicesSheet by remember { mutableStateOf(false) }

    val customLrcPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && state.currentSong != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!content.isNullOrBlank()) {
                    lyricsViewModel.applyCustomLyrics(state.currentSong!!, content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(lyricsState.userFeedbackMessage) {
        lyricsState.userFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            lyricsViewModel.clearUserFeedbackMessage()
        }
    }

    LaunchedEffect(state.currentSong?.id, state.currentSong?.path) {
        lyricsViewModel.loadLyricsForSong(state.currentSong)
    }

    LaunchedEffect(lyricsState.rawLyrics) {
        val lyrics = lyricsState.rawLyrics
        val song = state.currentSong
        if (lyrics != null && song != null && song.lyrics != lyrics) {
            viewModel.updateCurrentSongLyrics(song.id, lyrics)
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var hideTimerJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun resetHideTimer() {
        showControls = true
        hideTimerJob?.cancel()
        if (state.isPlaying) {
            hideTimerJob = coroutineScope.launch {
                delay(3500)
                if (state.isPlaying) {
                    showControls = false
                }
            }
        }
    }

    LaunchedEffect(state.isPlaying) {
        if (!state.isPlaying) {
            hideTimerJob?.cancel()
            showControls = true
        } else {
            resetHideTimer()
        }
    }

    val enableSlideGesture by settingsViewModel.enableSlideGesture.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.looper_android.ui.widgets.LocalHazeState provides lyricsHazeState,
        com.example.looper_android.ui.widgets.LocalIsBlurEnabled provides (!disableBlur)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (effectiveDarkTheme) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (state.isPlaying) {
                    showControls = !showControls
                    if (showControls) resetHideTimer() else hideTimerJob?.cancel()
                } else {
                    showControls = true
                }
            }
    ) {
        val rawArtPath = state.currentSong?.artPath
        var lastValidLyricsArtPath by remember { mutableStateOf(rawArtPath) }
        if (!rawArtPath.isNullOrEmpty()) {
            lastValidLyricsArtPath = rawArtPath
        }
        val artPath = rawArtPath ?: lastValidLyricsArtPath
        val baseColor = if (effectiveDarkTheme) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background
        
        Box(modifier = Modifier.fillMaxSize().haze(lyricsHazeState)) {
            if (dynamicLyrics && blurredArtworkForLyrics && !artPath.isNullOrEmpty()) {
                com.example.looper_android.ui.components.PersistentBlurredArtworkBackground(
                    artPath = artPath,
                    modifier = Modifier.fillMaxSize(),
                    blurRadius = 80.dp,
                    darknessAlpha = lyricsScreenDarkness
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(baseColor)
                                drawRect(
                                    Brush.radialGradient(
                                        0.0f to animatedAccentColor.copy(alpha = 0.45f),
                                        0.50f to animatedAccentColor.copy(alpha = 0.20f),
                                        0.80f to animatedAccentColor.copy(alpha = 0.05f),
                                        1.0f to Color.Transparent,
                                        center = Offset(size.width / 2f, size.height * 0.25f),
                                        radius = size.height * 0.85f
                                    )
                                )
                                drawRect(
                                    Brush.radialGradient(
                                        0.0f to animatedAccentColor.copy(alpha = 0.20f),
                                        0.60f to animatedAccentColor.copy(alpha = 0.08f),
                                        1.0f to Color.Transparent,
                                        center = Offset(size.width, size.height * 0.8f),
                                        radius = size.height * 0.70f
                                    )
                                )
                                drawRect(Color.Black.copy(alpha = lyricsScreenDarkness))
                            }
                        }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
            ) {
            if (!isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .then(
                            if (enableSlideGesture) {
                                Modifier.pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            if (!isDismissing) {
                                                rawVerticalOffset = 0f
                                            }
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            if (!isDismissing && (dragAmount > 0 || rawVerticalOffset > 0)) {
                                                change.consume()
                                                rawVerticalOffset = (rawVerticalOffset + dragAmount).coerceAtLeast(0f)
                                            }
                                        },
                                        onDragEnd = {
                                            if (!isDismissing) {
                                                if (rawVerticalOffset > 50.dp.toPx()) {
                                                    isDismissing = true
                                                    haptics.mediumImpact()
                                                    onBack()
                                                }
                                                rawVerticalOffset = 0f
                                            }
                                        },
                                        onDragCancel = {
                                            if (!isDismissing) {
                                                rawVerticalOffset = 0f
                                            }
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        ),
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
                            .size(54.dp)
                            .sharedBoundsOrElement("album_art", sharedTransitionScope, animatedVisibilityScope)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentSong?.title ?: "Unknown Title",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            maxLines = 1
                        )
                        val artistName = state.currentSong?.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                        Text(
                            text = artistName,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontFamily = DmSansFontFamily,
                            maxLines = 1
                        )
                    }

                    // Header Buttons (Half-circle touching flat sides, matching HomeTab Search/Settings style)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumSection(
                            width = 44.dp,
                            height = 44.dp,
                            backgroundColor = Color.White.copy(alpha = 0.04f),
                            showBorder = false,
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                            onClick = {
                                haptics.lightImpact()
                                showLyricsMenuSheet = true
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "Lyrics Options",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        PremiumSection(
                            width = 44.dp,
                            height = 44.dp,
                            backgroundColor = Color.White.copy(alpha = 0.04f),
                            showBorder = false,
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp),
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
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.15f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PremiumSection(
                            width = 38.dp,
                            height = 38.dp,
                            backgroundColor = Color.White.copy(alpha = 0.04f),
                            showBorder = false,
                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                            onClick = {
                                haptics.lightImpact()
                                showLyricsMenuSheet = true
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "Lyrics Options",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        PremiumSection(
                            width = 38.dp,
                            height = 38.dp,
                            backgroundColor = Color.White.copy(alpha = 0.04f),
                            showBorder = false,
                            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                            onClick = {
                                haptics.lightImpact()
                                onBack()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronDown,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (lyricsState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = state.accentColor,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (lyricsState.isSynced && lyricsState.parsedLines.isNotEmpty()) {
                    AdvancedLyricRenderer(
                        lines = lyricsState.parsedLines,
                        currentPositionMs = state.position,
                        timingOffsetMs = lyricsState.timingOffsetMs,
                        onAdjustOffset = { delta ->
                            state.currentSong?.let { lyricsViewModel.adjustTimingOffset(it, delta) }
                        },
                        showControls = showControls,
                        isLandscape = isLandscape,
                        onSeek = { pos ->
                            viewModel.seekTo(pos)
                            resetHideTimer()
                        },
                        disableFontScaling = disableLyricFontScaling,
                        alignment = lyricsAlignment,
                        activeColor = resolvedActiveColor,
                        isPlaying = state.isPlaying,
                        syncMode = LyricsSyncMode.fromSetting(lyricsSyncStyle),
                        modifier = Modifier.fillMaxSize(),
                        onUserScroll = { resetHideTimer() }
                    )
                } else if (!lyricsState.isSynced && lyricsState.plainLines.isNotEmpty()) {
                    PlainLyricsRenderer(
                        lines = lyricsState.plainLines,
                        disableFontScaling = disableLyricFontScaling,
                        alignment = lyricsAlignment,
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
            val controlHeight = if (isLandscape) 150.dp else 235.dp
            val btnHeight = if (isLandscape) 52.dp else 80.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(controlHeight + navBottomPadding)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.20f to Color.Black.copy(alpha = 0.25f),
                            0.70f to Color.Black.copy(alpha = 0.65f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = if (isLandscape) 6.dp else 10.dp,
                        bottom = navBottomPadding + if (isLandscape) 14.dp else 28.dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = if (isLandscape) Modifier.widthIn(max = 440.dp) else Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedBoundsOrElement(
                                "player_seek_bar",
                                sharedTransitionScope,
                                animatedVisibilityScope
                            )
                    ) {
                        if (disableSquiggle) {
                            com.example.looper_android.ui.widgets.FlatSlider(
                                positionMs = state.position,
                                durationMs = state.duration,
                                songId = state.currentSong?.id ?: 0L,
                                isPlaying = state.isPlaying,
                                onSeek = { pos ->
                                    viewModel.seekTo(pos)
                                    resetHideTimer()
                                },
                                activeColor = state.accentColor
                            )
                        } else {
                            ExpressiveSlider(
                                positionMs = state.position,
                                durationMs = state.duration,
                                songId = state.currentSong?.id ?: 0L,
                                isPlaying = state.isPlaying,
                                onSeek = { pos ->
                                    viewModel.seekTo(pos)
                                    resetHideTimer()
                                },
                                activeColor = state.accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
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
                            height = btnHeight,
                            enableBlur = false,
                            showBorder = false,
                            hazeState = lyricsHazeState,
                            shape = RoundedCornerShape(
                                topStart = if (isLandscape) 26.dp else 40.dp,
                                bottomStart = if (isLandscape) 26.dp else 40.dp,
                                topEnd = if (isLandscape) 10.dp else 12.dp,
                                bottomEnd = if (isLandscape) 10.dp else 12.dp
                            ),
                            backgroundColor = Color.White.copy(alpha = 0.04f),
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
                                modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        val playPauseIconTint = if (state.isPlaying) {
                            Color.White
                        } else {
                            val hsl = FloatArray(3)
                            androidx.core.graphics.ColorUtils.colorToHSL(
                                android.graphics.Color.rgb(
                                    (state.accentColor.red * 255).toInt(),
                                    (state.accentColor.green * 255).toInt(),
                                    (state.accentColor.blue * 255).toInt()
                                ),
                                hsl
                            )
                            hsl[2] = 0.15f
                            Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
                        }

                        PremiumSection(
                            modifier = Modifier
                                .weight(1f)
                                .sharedBoundsOrElement(
                                    "player_play_pause_btn",
                                    sharedTransitionScope,
                                    animatedVisibilityScope
                                ),
                            height = btnHeight,
                            shape = RoundedCornerShape(if (isLandscape) 10.dp else 12.dp),
                            enableBlur = false,
                            showBorder = false,
                            hazeState = lyricsHazeState,
                            backgroundColor = if (!state.isPlaying) state.accentColor else Color.White.copy(alpha = 0.04f),
                            onClick = {
                                haptics.mediumImpact()
                                viewModel.togglePlayPause()
                                resetHideTimer()
                            }
                        ) {
                            AnimatedPlayPauseIcon(
                                isPlaying = state.isPlaying,
                                tint = playPauseIconTint,
                                modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp)
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
                            height = btnHeight,
                            enableBlur = false,
                            showBorder = false,
                            hazeState = lyricsHazeState,
                            shape = RoundedCornerShape(
                                topStart = if (isLandscape) 10.dp else 12.dp,
                                bottomStart = if (isLandscape) 10.dp else 12.dp,
                                topEnd = if (isLandscape) 26.dp else 40.dp,
                                bottomEnd = if (isLandscape) 26.dp else 40.dp
                            ),
                            backgroundColor = Color.White.copy(alpha = 0.04f),
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
                                modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

        if (showLyricsMenuSheet && state.currentSong != null) {
            val hasWordTiming = lyricsState.parsedLines.any { it.hasWordTiming }
            LyricsOptionsMenuSheet(
                song = state.currentSong!!,
                lyricsSource = lyricsState.source,
                autoFallback = autoFallback,
                isInternetEnabled = enableInternet,
                isFetchingProvider = lyricsState.isFetchingProvider,
                accentColor = animatedAccentColor,
                syncStyle = lyricsSyncStyle,
                isWordSynced = hasWordTiming,
                isSynced = lyricsState.isSynced,
                hasLyrics = lyricsState.parsedLines.isNotEmpty() || !lyricsState.rawLyrics.isNullOrBlank(),
                onDismiss = { showLyricsMenuSheet = false },
                onSelectProvider = { provider ->
                    lyricsViewModel.refetchWithProvider(state.currentSong!!, provider)
                },
                onSelectSyncStyle = { style ->
                    settingsViewModel.setLyricsSyncStyle(style)
                },
                onOpenServicesSheetClick = {
                    showLyricsMenuSheet = false
                    showLyricsServicesSheet = true
                },
                onToggleAutoFallback = { enabled ->
                    settingsViewModel.setAutoFallbackLyricsProviders(enabled)
                },
                onEnableInternetClick = {
                    settingsViewModel.setEnableInternet(true)
                },
                onExportLrcClick = {
                    lyricsViewModel.exportLyricsToLrcFile(state.currentSong!!, context)
                },
                onEmbedLyricsClick = {
                    lyricsViewModel.embedLyricsToAudioFile(state.currentSong!!)
                },
                onPickCustomLrcClick = {
                    showLyricsMenuSheet = false
                    customLrcPicker.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                }
            )
        }

        if (showLyricsServicesSheet) {
            LyricsServicesSheet(
                providerOrder = lyricsProviderOrder,
                providersOff = lyricsProvidersOff,
                accentColor = animatedAccentColor,
                onDismiss = { showLyricsServicesSheet = false },
                onReorder = { newOrder ->
                    settingsViewModel.setLyricsProviderOrder(newOrder)
                },
                onToggleProvider = { providerId, enabled ->
                    settingsViewModel.toggleLyricsProvider(providerId, enabled)
                }
            )
        }

        if (lyricsState.showInternetPrompt) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { lyricsViewModel.dismissInternetPrompt() },
                title = {
                    Text(
                        text = "Internet Access Disabled",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = DmSansFontFamily
                    )
                },
                text = {
                    Text(
                        text = "Online lyrics require an internet connection, but 'Use Internet' is currently disabled in Settings. Would you like to enable it now?",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontFamily = DmSansFontFamily
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            settingsViewModel.setEnableInternet(true)
                            lyricsViewModel.dismissInternetPrompt()
                        }
                    ) {
                        Text(
                            text = "Enable",
                            color = animatedAccentColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { lyricsViewModel.dismissInternetPrompt() }
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = DmSansFontFamily
                        )
                    }
                },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
}
