package com.example.looper_android.ui.player
import com.example.looper_android.ui.player.RepeatMode
import com.composables.icons.lucide.ListEnd

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeState
import androidx.compose.material.icons.Icons
import com.example.looper_android.ui.icons.CustomIcons
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Repeat1
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Star
import com.example.looper_android.ui.components.AnimatedPlayPauseIcon
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper
import com.example.looper_android.ui.widgets.ExpressiveSlider
import com.example.looper_android.ui.widgets.GestureArtwork
import com.example.looper_android.ui.widgets.PremiumSection
import com.example.looper_android.ui.widgets.SleepTimerClock
import com.example.looper_android.ui.widgets.sharedBoundsOrElement

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlaybackViewModel,
    settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = hiltViewModel(),
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

    val darkTheme by settingsViewModel.darkTheme.collectAsState()
    val dynamicTheming by settingsViewModel.dynamicTheming.collectAsState()
    val disableBlur by settingsViewModel.disableBlur.collectAsState()
    val disableSquiggle by settingsViewModel.disableSquiggle.collectAsState()
    val disableAnimatedDuration by settingsViewModel.disableAnimatedDuration.collectAsState()
    val showQualityBadge by settingsViewModel.showQualityBadge.collectAsState()
    val enablePlayerGradient by settingsViewModel.enablePlayerGradient.collectAsState()
    val enableSlideGesture by settingsViewModel.enableSlideGesture.collectAsState()

    val effectiveDarkTheme = darkTheme && !dynamicTheming
    val containerColor = when {
        dynamicTheming -> {
            if (disableBlur) {
                Color(0xFF1C1C1C).copy(alpha = 0.65f) // Smoked dark acrylic when blur is off
            } else {
                Color.White.copy(alpha = 0.08f) // Frosted light glassmorphism when blur is on
            }
        }
        effectiveDarkTheme -> Color(0xFF0A0A0A)
        enablePlayerGradient -> Color(0xFF262626) // Noticeable gray tint to distinguish from gradient background
        else -> Color(0xFF1E1E1E)
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    val playerHazeState = remember { dev.chrisbanes.haze.HazeState() }

    var rawVerticalOffset by remember { mutableFloatStateOf(0f) }

    var rememberedQualityText by remember {
        mutableStateOf(state.audioQualityText)
    }
    if (state.audioQualityText.isNotBlank()) {
        rememberedQualityText = state.audioQualityText
    }

    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    LaunchedEffect(song, state.isSongReady) {
        if (song == null && state.isSongReady) {
            onBack()
        }
    }

    if (song == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!state.isSongReady) {
                    CircularProgressIndicator(
                        color = state.accentColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading player...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = DmSansFontFamily
                    )
                } else {
                    Text(
                        text = "No song playing",
                        color = Color.White,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
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

    val favoriteScale = remember { Animatable(1f) }
    LaunchedEffect(state.isFavorite) {
        if (state.isFavorite) {
            favoriteScale.snapTo(0.6f)
            favoriteScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            favoriteScale.snapTo(1f)
        }
    }

    val isBlurEnabled = dynamicTheming && !disableBlur

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.looper_android.ui.widgets.LocalHazeState provides playerHazeState,
        com.example.looper_android.ui.widgets.LocalIsBlurEnabled provides isBlurEnabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enableSlideGesture && !showQueueSheet && !showOptionsSheet) {
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
                                    if (rawVerticalOffset > 80.dp.toPx()) {
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
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(if (effectiveDarkTheme) Color.Black else androidx.compose.material3.MaterialTheme.colorScheme.background)
                .haze(playerHazeState)
        ) {
            // 1. Prioritize Dynamic Theming Blurred Artwork Background
            val initialArtPath = remember { settingsViewModel.getInitialSongArtPath() }
            val currentArtPath = song.artPath ?: (if (dynamicTheming) initialArtPath else null)
            var lastValidArtPath by remember { mutableStateOf(currentArtPath) }
            if (!currentArtPath.isNullOrEmpty()) {
                lastValidArtPath = currentArtPath
            }
            val effectiveArtPath = currentArtPath ?: lastValidArtPath
            val shouldShowBlurredArt = !effectiveDarkTheme && dynamicTheming && !effectiveArtPath.isNullOrEmpty()

            if (shouldShowBlurredArt) {
                com.example.looper_android.ui.components.PersistentBlurredArtworkBackground(
                    artPath = effectiveArtPath,
                    modifier = Modifier.fillMaxSize(),
                    blurRadius = 24.dp,
                    darknessAlpha = 0.65f
                )
            } else if (enablePlayerGradient) {
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
            }

        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ─────────────────────────────────────────────────────────────
                // LEFT PANE: Expanded Square Album Artwork with Floating Top Bar
                // ─────────────────────────────────────────────────────────────
                val currentIdx = remember(state.queue, song.id) {
                    val idx = state.queue.indexOfFirst { it.id == song.id }
                    if (idx >= 0) idx else 0
                }

                Box(
                    modifier = Modifier
                        .weight(0.53f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Maximum size Square Album Artwork (takes 92% of screen height)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.92f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GestureArtwork(
                            song = song,
                            queue = state.queue.ifEmpty { listOf(song) },
                            currentSongIndex = currentIdx,
                            onSkipToIndex = { index ->
                                haptics.mediumImpact()
                                viewModel.skipToQueueItem(index)
                            },
                            onTap = {
                                haptics.lightImpact()
                                onNavigateToLyrics()
                            },
                            onSeekRelative = { offsetMs ->
                                viewModel.seekRelative(offsetMs)
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }

                    // Floating Top Controls Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.lightImpact()
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.mediumImpact()
                                    showOptionsSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isSleepTimerActive) {
                                SleepTimerClock(
                                    progress = 0.75f,
                                    label = state.sleepTimerLabel
                                )
                            } else {
                                Icon(
                                    imageVector = Lucide.EllipsisVertical,
                                    contentDescription = "More Options",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = 90f }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ─────────────────────────────────────────────────────────────
                // RIGHT PANE: Slimmer, tightly-proportioned controls
                // ─────────────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(0.47f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Song Details & Favorite Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = DmSansFontFamily,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (state.isPlaying) {
                                                Modifier.basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    velocity = 30.dp,
                                                    initialDelayMillis = 1200
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val artistName = song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"

                                Text(
                                    text = artistName,
                                    color = Color.White.copy(alpha = 0.60f),
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.2.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clickable {
                                            song.artist?.let {
                                                onBack()
                                                onArtistClick(it)
                                            }
                                        }
                                )

                                val displayQuality = if (state.audioQualityText.isNotBlank()) state.audioQualityText else rememberedQualityText
                                if (showQualityBadge && displayQuality.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(21.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(
                                                width = 0.5.dp,
                                                color = Color.White.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayQuality,
                                            color = Color.White.copy(alpha = 0.80f),
                                            fontFamily = JostFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.3.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            style = TextStyle(
                                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                                                    includeFontPadding = false
                                                ),
                                                lineHeight = 12.sp,
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim = LineHeightStyle.Trim.Both
                                                )
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.isFavorite) state.accentColor.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        haptics.selectionClick()
                                        viewModel.toggleFavorite()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CustomIcons.LikedIcon,
                                    contentDescription = "Favorite",
                                    tint = if (state.isFavorite) state.accentColor else Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = favoriteScale.value
                                            scaleY = favoriteScale.value
                                        }
                                )
                            }
                        }

                        // 2. Seek Bar
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
                                    songId = song.id,
                                    isPlaying = state.isPlaying,
                                    onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                                    activeColor = state.accentColor,
                                    disableAnimatedDuration = disableAnimatedDuration
                                )
                            } else if (disableAnimatedDuration) {
                                Column {
                                    ExpressiveSlider(
                                        positionMs = state.position,
                                        durationMs = state.duration,
                                        songId = song.id,
                                        isPlaying = state.isPlaying,
                                        onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                                        activeColor = state.accentColor,
                                        showTimestamps = false
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        com.example.looper_android.ui.widgets.FlatDurationText(
                                            durationMs = state.position,
                                            disableAnimatedDuration = true
                                        )
                                        com.example.looper_android.ui.widgets.FlatDurationText(
                                            durationMs = state.duration,
                                            disableAnimatedDuration = true
                                        )
                                    }
                                }
                            } else {
                                ExpressiveSlider(
                                    positionMs = state.position,
                                    durationMs = state.duration,
                                    songId = song.id,
                                    isPlaying = state.isPlaying,
                                    onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                                    activeColor = state.accentColor
                                )
                            }
                        }

                        // 3. Playback Controls Row (Prev, Play/Pause, Next)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Prev
                            PremiumSection(
                                enableBlur = !disableBlur,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .sharedBoundsOrElement("player_prev_btn", sharedTransitionScope, animatedVisibilityScope),
                                shape = RoundedCornerShape(
                                    topStart = 26.dp,
                                    bottomStart = 26.dp,
                                    topEnd = 10.dp,
                                    bottomEnd = 10.dp
                                ),
                                backgroundColor = containerColor,
                                onClick = {
                                    haptics.lightImpact()
                                    viewModel.skipToPrevious()
                                }
                            ) {
                                Icon(
                                    imageVector = Lucide.SkipBack,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Play / Pause
                            PremiumSection(
                                enableBlur = if (state.isPlaying) !disableBlur else false,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .sharedBoundsOrElement("player_play_pause_btn", sharedTransitionScope, animatedVisibilityScope),
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (!state.isPlaying) state.accentColor else containerColor,
                                onClick = {
                                    haptics.mediumImpact()
                                    viewModel.togglePlayPause()
                                }
                            ) {
                                val iconTint = if (state.isPlaying) {
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

                                AnimatedPlayPauseIcon(
                                    isPlaying = state.isPlaying,
                                    modifier = Modifier.size(20.dp),
                                    tint = iconTint
                                )
                            }

                            // Next
                            PremiumSection(
                                enableBlur = !disableBlur,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .sharedBoundsOrElement("player_next_btn", sharedTransitionScope, animatedVisibilityScope),
                                shape = RoundedCornerShape(
                                    topStart = 10.dp,
                                    bottomStart = 10.dp,
                                    topEnd = 26.dp,
                                    bottomEnd = 26.dp
                                ),
                                backgroundColor = containerColor,
                                onClick = {
                                    haptics.lightImpact()
                                    viewModel.skipToNext()
                                }
                            ) {
                                Icon(
                                    imageVector = Lucide.SkipForward,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 4. Utility Controls Row (Shuffle, Repeat, Lyrics, Queue)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Shuffle
                            PremiumSection(
                                enableBlur = !disableBlur,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope),
                                shape = RoundedCornerShape(
                                    topStart = 22.dp,
                                    bottomStart = 22.dp,
                                    topEnd = 10.dp,
                                    bottomEnd = 10.dp
                                ),
                                backgroundColor = if (state.isShuffle) state.accentColor else containerColor,
                                onClick = {
                                    haptics.selectionClick()
                                    viewModel.toggleShuffle()
                                }
                            ) {
                                Icon(
                                    imageVector = Lucide.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (state.isShuffle) androidx.compose.material3.MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Repeat
                            PremiumSection(
                                enableBlur = !disableBlur,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope),
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (state.repeatMode != RepeatMode.OFF) state.accentColor else containerColor,
                                onClick = {
                                    haptics.selectionClick()
                                    viewModel.nextRepeatMode()
                                }
                            ) {
                                Icon(
                                    imageVector = when (state.repeatMode) { RepeatMode.ONE -> Lucide.Repeat1; RepeatMode.NEXT_QUEUE -> Lucide.ListEnd; else -> Lucide.Repeat },
                                    contentDescription = "Repeat",
                                    tint = if (state.repeatMode != RepeatMode.OFF) androidx.compose.material3.MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Lyrics
                            PremiumSection(
                                enableBlur = !disableBlur,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope),
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = containerColor,
                                onClick = {
                                    haptics.lightImpact()
                                    onNavigateToLyrics()
                                }
                            ) {
                                Icon(
                                    imageVector = Lucide.MicVocal,
                                    contentDescription = "Lyrics",
                                    tint = Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Queue
                            PremiumSection(
                                enableBlur = !disableBlur,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(
                                    topStart = 10.dp,
                                    bottomStart = 10.dp,
                                    topEnd = 22.dp,
                                    bottomEnd = 22.dp
                                ),
                                backgroundColor = containerColor,
                                onClick = {
                                    haptics.mediumImpact()
                                    showQueueSheet = true
                                }
                            ) {
                                Icon(
                                    imageVector = Lucide.ListMusic,
                                    contentDescription = "Queue",
                                    tint = Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
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
                    // Close button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptics.lightImpact()
                                onBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Title or Quality badge (mutually exclusive per user request, vertically centered)
                    val displayQuality = if (state.audioQualityText.isNotBlank()) state.audioQualityText else rememberedQualityText
                    if (showQualityBadge && displayQuality.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .height(21.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayQuality,
                                color = Color.White.copy(alpha = 0.80f),
                                fontFamily = JostFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.3.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(
                                        includeFontPadding = false
                                    ),
                                    lineHeight = 12.sp,
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    )
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Now Playing",
                            color = Color.White,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Options / Sleep Timer button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptics.mediumImpact()
                                showOptionsSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isSleepTimerActive) {
                            SleepTimerClock(
                                progress = 0.75f,
                                label = state.sleepTimerLabel
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = 90f }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.6f))

                // 2. Large Album Art with Gestures (Morphs from Mini Player album artwork)
                val currentIdx = remember(state.queue, song.id) {
                    val idx = state.queue.indexOfFirst { it.id == song.id }
                    if (idx >= 0) idx else 0
                }

                Padding(
                    modifier = Modifier.padding(horizontal = 30.dp)
                ) {
                    GestureArtwork(
                        song = song,
                        queue = state.queue.ifEmpty { listOf(song) },
                        currentSongIndex = currentIdx,
                        onSkipToIndex = { index ->
                            haptics.mediumImpact()
                            viewModel.skipToQueueItem(index)
                        },
                        onTap = {
                            haptics.lightImpact()
                            onNavigateToLyrics()
                        },
                        onSeekRelative = { offsetMs ->
                            viewModel.seekRelative(offsetMs)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (state.isPlaying) {
                                        Modifier.basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            velocity = 30.dp,
                                            initialDelayMillis = 1200
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val artistName = song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"

                        Text(
                            text = artistName,
                            color = Color.White.copy(alpha = 0.60f),
                            fontFamily = DmSansFontFamily,
                            fontSize = 16.sp,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                song.artist?.let {
                                    onBack()
                                    onArtistClick(it)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Favorite Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isFavorite) state.accentColor.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable {
                                haptics.selectionClick()
                                viewModel.toggleFavorite()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CustomIcons.LikedIcon,
                            contentDescription = "Favorite",
                            tint = if (state.isFavorite) state.accentColor else Color.White.copy(alpha = 0.20f),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = favoriteScale.value
                                    scaleY = favoriteScale.value
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.85f))

                // 4. Seek Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
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
                            songId = song.id,
                            isPlaying = state.isPlaying,
                            onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                            activeColor = state.accentColor,
                            disableAnimatedDuration = disableAnimatedDuration
                        )
                    } else if (disableAnimatedDuration) {
                        Column {
                            ExpressiveSlider(
                                positionMs = state.position,
                                durationMs = state.duration,
                                songId = song.id,
                                isPlaying = state.isPlaying,
                                onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                                activeColor = state.accentColor,
                                showTimestamps = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.looper_android.ui.widgets.FlatDurationText(
                                    durationMs = state.position,
                                    disableAnimatedDuration = true
                                )
                                com.example.looper_android.ui.widgets.FlatDurationText(
                                    durationMs = state.duration,
                                    disableAnimatedDuration = true
                                )
                            }
                        }
                    } else {
                        ExpressiveSlider(
                            positionMs = state.position,
                            durationMs = state.duration,
                            songId = song.id,
                            isPlaying = state.isPlaying,
                            onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                            activeColor = state.accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Main Playback Control Row (Height 80dp, Prev/Next 16dp, Play/Pause 22dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Prev (16dp)
                    PremiumSection(
                        enableBlur = !disableBlur,
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .sharedBoundsOrElement("player_prev_btn", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(
                            topStart = 40.dp,
                            bottomStart = 40.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        backgroundColor = containerColor,
                        onClick = {
                            haptics.lightImpact()
                            viewModel.skipToPrevious()
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.SkipBack,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause (22dp) (Morphs from Mini Player play button)
                    PremiumSection(
                        enableBlur = if (state.isPlaying) !disableBlur else false,
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .sharedBoundsOrElement("player_play_pause_btn", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (!state.isPlaying) state.accentColor else containerColor,
                        onClick = {
                            haptics.mediumImpact()
                            viewModel.togglePlayPause()
                        }
                    ) {
                        val iconTint = if (state.isPlaying) {
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

                        AnimatedPlayPauseIcon(
                            isPlaying = state.isPlaying,
                            modifier = Modifier.size(22.dp),
                            tint = iconTint
                        )
                    }

                    // Next (16dp)
                    PremiumSection(
                        enableBlur = !disableBlur,
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .sharedBoundsOrElement("player_next_btn", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            bottomStart = 12.dp,
                            topEnd = 40.dp,
                            bottomEnd = 40.dp
                        ),
                        backgroundColor = containerColor,
                        onClick = {
                            haptics.lightImpact()
                            viewModel.skipToNext()
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
                        enableBlur = !disableBlur,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(
                            topStart = 32.dp,
                            bottomStart = 32.dp,
                            topEnd = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        backgroundColor = if (state.isShuffle) state.accentColor else containerColor,
                        onClick = {
                            haptics.selectionClick()
                            viewModel.toggleShuffle()
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.isShuffle) androidx.compose.material3.MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Repeat (15dp) (Morphs from Navbar Songs tab)
                    PremiumSection(
                        enableBlur = !disableBlur,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (state.repeatMode != RepeatMode.OFF) state.accentColor else containerColor,
                        onClick = {
                            haptics.selectionClick()
                            viewModel.nextRepeatMode()
                        }
                    ) {
                        Icon(
                            imageVector = when (state.repeatMode) { RepeatMode.ONE -> Lucide.Repeat1; RepeatMode.NEXT_QUEUE -> Lucide.ListEnd; else -> Lucide.Repeat },
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) androidx.compose.material3.MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Lyrics (15dp) (Morphs from Navbar Library tab)
                    PremiumSection(
                        enableBlur = !disableBlur,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = containerColor,
                        onClick = {
                            haptics.lightImpact()
                            onNavigateToLyrics()
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.MicVocal,
                            contentDescription = "Lyrics",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Queue (12dp)
                    PremiumSection(
                        enableBlur = !disableBlur,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            bottomStart = 12.dp,
                            topEnd = 32.dp,
                            bottomEnd = 32.dp
                        ),
                        backgroundColor = containerColor,
                        onClick = {
                            haptics.mediumImpact()
                            showQueueSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.ListMusic,
                            contentDescription = "Queue",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

        QueueBottomSheet(
            visible = showQueueSheet,
            queue = state.queue,
            currentSong = song,
            playbackViewModel = viewModel,
            hazeState = playerHazeState,
            onDismiss = { showQueueSheet = false },
            onSongSelect = { selectedSong ->
            },
            onArtistClick = { artist ->
                showQueueSheet = false
                onArtistClick(artist)
            },
            onAlbumClick = { album ->
                showQueueSheet = false
                onAlbumClick(album)
            }
        )

        if (showOptionsSheet) {
            SongOptionsSheet(
                song = song,
                onDismiss = { showOptionsSheet = false },
                accentColor = state.accentColor,
                isCurrentlyPlaying = true,
                showSleepTimer = true,
                showEqualizer = true,
                onArtistClick = { artist ->
                    onArtistClick(artist)
                },
                onAlbumClick = { album ->
                    onAlbumClick(album)
                },
                onNavigateToEqualizer = {
                    showOptionsSheet = false
                    onNavigateToEqualizer()
                }
            )
        }
    }
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val topInset: androidx.compose.ui.unit.Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var savedTopPx by remember { mutableStateOf<Float?>(null) }

    val currentTopPx: Float = with(density) { topInset.toPx() }
    val saved: Float? = savedTopPx
    if (saved == null && currentTopPx > 0f) {
        savedTopPx = currentTopPx
    }
    val targetPx: Float = saved ?: currentTopPx
    val stableTopDp = with(density) { targetPx.toDp() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = stableTopDp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
