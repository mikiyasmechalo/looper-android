package com.example.looper_android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Lucide
import com.example.looper_android.data.lyrics.providers.LyricsProviderInfo
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsServicesSheet(
    providerOrder: List<String>,
    providersOff: Set<String>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onReorder: (List<String>) -> Unit,
    onToggleProvider: (String, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = rememberHapticFeedbackHelper()

    var localOrder by remember(providerOrder) { mutableStateOf(providerOrder) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragVisualOffset by remember { mutableFloatStateOf(0f) }
    var initialDragIndex by remember { mutableIntStateOf(-1) }

    val density = LocalDensity.current
    val itemHeightPx = with(density) { 56.dp.toPx() }

    val enabledChain = localOrder.filter { !providersOff.contains(it) }
        .map { LyricsProviderInfo.of(it)?.label ?: it }
        .joinToString(" > ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Lyrics Services",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Drag to set the order lookups ask them in, switch one off to skip. Changes are saved as they're made.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = DmSansFontFamily,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (enabledChain.isNotEmpty()) {
                Text(
                    text = enabledChain,
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(localOrder, key = { _, id -> id }) { index, id ->
                    val isBeingDragged = draggedId == id
                    val isEnabled = !providersOff.contains(id)
                    val info = LyricsProviderInfo.of(id)
                    val label = info?.label ?: id

                    val badgeText = when {
                        info?.wordCapable == true && info.letterCapable -> "Word & Syllable"
                        info?.wordCapable == true -> "Word Sync"
                        info?.plainOnly == true -> "Plain Text"
                        else -> "Line Sync"
                    }

                    val badgeColor = when {
                        info?.wordCapable == true && info.letterCapable -> Color(0xFF00E5FF)
                        info?.wordCapable == true -> Color(0xFFBA68C8)
                        info?.plainOnly == true -> Color.White.copy(alpha = 0.45f)
                        else -> Color(0xFF66BB6A)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isBeingDragged) 10f else 1f)
                            .graphicsLayer {
                                if (isBeingDragged) {
                                    translationY = dragVisualOffset
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                    shadowElevation = 8f
                                }
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isBeingDragged) Color.White.copy(alpha = 0.12f)
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isBeingDragged) accentColor.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedId = id
                                            initialDragIndex = localOrder.indexOf(id)
                                            dragVisualOffset = 0f
                                            haptics.lightImpact()
                                        },
                                        onDragEnd = {
                                            val start = initialDragIndex
                                            val end = localOrder.indexOf(id)
                                            if (start != -1 && end != -1 && start != end) {
                                                onReorder(localOrder)
                                            }
                                            draggedId = null
                                            initialDragIndex = -1
                                            dragVisualOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedId = null
                                            initialDragIndex = -1
                                            dragVisualOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragVisualOffset += dragAmount.y
                                            val currentPos = localOrder.indexOf(id)
                                            val threshold = itemHeightPx * 0.5f

                                            while (dragVisualOffset > threshold && currentPos < localOrder.size - 1) {
                                                val target = currentPos + 1
                                                val list = localOrder.toMutableList()
                                                list.removeAt(currentPos)
                                                list.add(target, id)
                                                localOrder = list
                                                dragVisualOffset -= itemHeightPx
                                                haptics.lightImpact()
                                            }

                                            while (dragVisualOffset < -threshold && currentPos > 0) {
                                                val target = currentPos - 1
                                                val list = localOrder.toMutableList()
                                                list.removeAt(currentPos)
                                                list.add(target, id)
                                                localOrder = list
                                                dragVisualOffset += itemHeightPx
                                                haptics.lightImpact()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.GripVertical,
                                contentDescription = "Drag to reorder",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.38f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily
                            )
                            Text(
                                text = badgeText,
                                color = if (isEnabled) badgeColor else Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp,
                                fontFamily = DmSansFontFamily
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                haptics.selectionClick()
                                onToggleProvider(id, checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Done",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
