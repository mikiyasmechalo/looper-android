package com.example.looper_android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.FileDown
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.WifiOff
import com.composables.icons.lucide.X
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.data.lyrics.providers.LyricsProviderInfo
import com.example.looper_android.data.repository.LyricsFetcher
import com.example.looper_android.ui.theme.DmSansFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsOptionsMenuSheet(
    song: SongEntity,
    lyricsSource: String,
    autoFallback: Boolean,
    isInternetEnabled: Boolean,
    isFetchingProvider: String?,
    accentColor: Color,
    syncStyle: String = "char",
    isWordSynced: Boolean = false,
    isSynced: Boolean = true,
    hasLyrics: Boolean = true,
    onDismiss: () -> Unit,
    onSelectProvider: (String) -> Unit,
    onSelectSyncStyle: (String) -> Unit = {},
    onOpenServicesSheetClick: () -> Unit = {},
    onToggleAutoFallback: (Boolean) -> Unit,
    onEnableInternetClick: () -> Unit,
    onExportLrcClick: () -> Unit,
    onEmbedLyricsClick: () -> Unit,
    onPickCustomLrcClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val durationText = song.duration?.let {
        val totalSecs = it / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%d:%02d", mins, secs)
    } ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // --- 1. Song Header (Art + Title/Artist + Close Button) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.artPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist ?: "Unknown Artist"}${if (durationText.isNotEmpty()) " • $durationText" else ""}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- 2. Source & Sync Type Badges (Slimmer, using normal dynamic accent color) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 9.dp, vertical = 2.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Disc,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Source: $lyricsSource",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }

                // Sync Type Badge Pill
                if (hasLyrics) {
                    val (syncIcon, syncText, syncColor) = when {
                        isWordSynced -> Triple(Lucide.Sparkles, "Word-synced", accentColor)
                        isSynced -> Triple(Lucide.List, "Line-synced", Color.White.copy(alpha = 0.85f))
                        else -> Triple(Lucide.FileText, "Plain text", Color.White.copy(alpha = 0.6f))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(syncColor.copy(alpha = 0.10f))
                            .border(0.8.dp, syncColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 9.dp, vertical = 2.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = null,
                                tint = syncColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = syncText,
                                color = syncColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = DmSansFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // --- Internet Disabled Notice Banner ---
            if (!isInternetEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Internet Access Disabled",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily
                            )
                            Text(
                                text = "Enable 'Use Internet' to fetch online lyrics.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontFamily = DmSansFontFamily
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFB74D))
                                .clickable { onEnableInternetClick() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Enable",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- 3. Slimmer Sync Highlight Style Selector ---
            Text(
                text = "Highlight Style",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DmSansFontFamily
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(17.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    "char" to "Character",
                    "word" to "Word",
                    "line" to "Line"
                )
                modes.forEach { (modeKey, title) ->
                    val isSelected = syncStyle.equals(modeKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.22f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) accentColor.copy(alpha = 0.55f) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectSyncStyle(modeKey)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // --- 4. Horizontal Scrolling Source Selector: Individual Pills ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lyrics Provider",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily
                )
                Text(
                    text = "${LyricsFetcher.ALL_PROVIDERS.size} sources",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontFamily = DmSansFontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(LyricsFetcher.ALL_PROVIDERS) { provider ->
                    val isCurrent = when (provider.uppercase()) {
                        "LOCAL", "LOCAL SIDECAR" -> lyricsSource.equals("Local Sidecar", ignoreCase = true) ||
                                lyricsSource.equals("Local", ignoreCase = true) ||
                                lyricsSource.equals("Synced LRC", ignoreCase = true) ||
                                lyricsSource.equals("Custom LRC", ignoreCase = true) ||
                                lyricsSource.equals("Plain Text", ignoreCase = true)

                        "EMBEDDED" -> lyricsSource.equals("Embedded", ignoreCase = true)
                        else -> lyricsSource.equals(provider, ignoreCase = true)
                    }
                    val isLoading = isFetchingProvider.equals(provider, ignoreCase = true)
                    val isClickable = !isCurrent && !isLoading
                    val providerInfo = LyricsProviderInfo.of(provider)
                    val providerLabel = providerInfo?.label ?: provider

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) accentColor.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                width = if (isCurrent) 1.dp else 0.8.dp,
                                color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .clickable(enabled = isClickable) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectProvider(provider)
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = providerLabel,
                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.70f),
                                fontSize = 11.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = DmSansFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LyricsActionTile(
                icon = Lucide.List,
                title = "Configure Lyrics Providers Order",
                subtitle = "Reorder and enable/disable services",
                onClick = {
                    onOpenServicesSheetClick()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- 5. Option 2: Auto Fallback Providers Toggle ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleAutoFallback(!autoFallback) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = null,
                        tint = if (autoFallback) accentColor else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Fallback Providers",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                    Text(
                        text = "Cycle all providers if the primary fails",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontFamily = DmSansFontFamily
                    )
                }

                Switch(
                    checked = autoFallback,
                    onCheckedChange = { onToggleAutoFallback(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(4.dp))

            // --- 6. Embed Lyrics into Audio File (if not already embedded) ---
            if (!lyricsSource.equals("Embedded", ignoreCase = true) && !lyricsSource.equals("None", ignoreCase = true)) {
                LyricsActionTile(
                    icon = Lucide.Disc,
                    title = "Embed Lyrics into Audio File",
                    subtitle = "Save current lyrics into audio metadata tags",
                    onClick = {
                        onEmbedLyricsClick()
                    }
                )
            }

            // --- 7. Export Lyrics into LRC File ---
            LyricsActionTile(
                icon = Lucide.FileDown,
                title = "Export Lyrics into LRC File",
                subtitle = "Save current lyrics to device storage",
                onClick = {
                    onExportLrcClick()
                }
            )

            // --- 8. Use Custom Lyrics (LRC File) ---
            LyricsActionTile(
                icon = Lucide.FileText,
                title = "Use Custom Lyrics (LRC File)",
                subtitle = "Import a local .lrc or .txt file for this song",
                onClick = {
                    onPickCustomLrcClick()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LyricsActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DmSansFontFamily
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontFamily = DmSansFontFamily
            )
        }
    }
}
