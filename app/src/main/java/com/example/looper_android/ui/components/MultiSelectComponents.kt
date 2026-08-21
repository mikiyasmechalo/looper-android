package com.example.looper_android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle

val LocalSelectionBottomBar = staticCompositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    error("No SelectionBottomBar provided")
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    accentColor: Color = Color.White,
    hasStatusBarsPadding: Boolean = false
) {
    val isAllSelected = selectedCount > 0 && selectedCount == totalCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close / Deselect button (matches Search/Back button size & shape 1-to-1)
        PremiumSection(
            width = 48.dp,
            height = 48.dp,
            backgroundColor = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
            onClick = onClearSelection
        ) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "Close Selection",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "$selectedCount Selected",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DmSansFontFamily,
            modifier = Modifier.weight(1f)
        )

        // Select All / Deselect All Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isAllSelected) accentColor else Color.White.copy(alpha = 0.10f))
                .clickable { onToggleSelectAll() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAllSelected) "Deselect All" else "Select All",
                color = if (isAllSelected) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )
        }
    }
}

@Composable
fun MultiSelectBottomBar(
    isVisible: Boolean,
    accentColor: Color,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    showFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    showDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
    enableBlur: Boolean = com.example.looper_android.ui.widgets.LocalIsBlurEnabled.current,
    hazeState: dev.chrisbanes.haze.HazeState? = com.example.looper_android.ui.widgets.LocalHazeState.current,
    darkTheme: Boolean = false
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            scrollState.scrollTo(0)
        }
    }

    val containerColor = if (darkTheme) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(if (bottomPadding > 0.dp) Modifier.padding(bottom = bottomPadding) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        PremiumSection(
            shape = RoundedCornerShape(28.dp),
            backgroundColor = containerColor,
            enableBlur = enableBlur,
            hazeState = hazeState
        ) {
            Row(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ ->
                            // Consume drag events so they do not leak to parent HorizontalPager
                        }
                    }
                    .horizontalScroll(scrollState)
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            MultiSelectActionButton(icon = Lucide.Play, label = "Play", onClick = onPlay, accentColor = accentColor)
            MultiSelectActionButton(icon = Lucide.Shuffle, label = "Shuffle", onClick = onShuffle, accentColor = accentColor)
            MultiSelectActionButton(icon = Lucide.ListPlus, label = "Play Next", onClick = onPlayNext, accentColor = accentColor)
            MultiSelectActionButton(icon = Lucide.ListMusic, label = "Queue", onClick = onAddToQueue, accentColor = accentColor)
            MultiSelectActionButton(icon = Lucide.Plus, label = "Playlist", onClick = onAddToPlaylist, accentColor = accentColor)

            if (showFavorite && onToggleFavorite != null) {
                MultiSelectActionButton(icon = Lucide.Star, label = "Favorite", onClick = onToggleFavorite, accentColor = Color(0xFFFFD54F))
            }

            if (showDelete && onDelete != null) {
                MultiSelectActionButton(icon = Lucide.Trash2, label = "Delete", onClick = onDelete, iconTint = Color(0xFFFF5252))
            }
        }
    }
}
}

@Composable
private fun MultiSelectActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    accentColor: Color = Color.White,
    iconTint: Color = Color.White
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            fontWeight = FontWeight.Medium,
            fontFamily = DmSansFontFamily
        )
    }
}
