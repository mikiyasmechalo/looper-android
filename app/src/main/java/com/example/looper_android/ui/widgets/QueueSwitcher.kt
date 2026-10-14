package com.example.looper_android.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.looper_android.data.db.QueueEntity
import com.example.looper_android.ui.theme.DmSansFontFamily

@Composable
fun QueueSwitcher(
    queues: List<QueueEntity>,
    activeQueueId: Long?,
    activeQueueName: String,
    onQueueSelected: (Long) -> Unit,
    onDeleteQueue: ((Long) -> Unit)? = null,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var buttonWidthDp by remember { mutableStateOf(0.dp) }

    val label = activeQueueName

    Box(modifier = modifier.wrapContentWidth()) {
        Row(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 220.dp)
                .onGloballyPositioned { coordinates ->
                    buttonWidthDp = with(density) { coordinates.size.width.toDp() }
                }
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.28f), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { expanded = true }
                    )
                }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = accentColor,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            )
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = "Switch Queue",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 4.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFF1E1E22).copy(alpha = 0.96f),
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = (if (buttonWidthDp > 0.dp) Modifier.width(buttonWidthDp) else Modifier.widthIn(min = 120.dp, max = 220.dp))
                .padding(vertical = 2.dp, horizontal = 2.dp)
        ) {
            queues.forEach { queue ->
                val isActive = queue.id == activeQueueId || queue.name.equals(activeQueueName, ignoreCase = true)
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isActive) accentColor.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .then(
                            if (isActive) Modifier.border(0.75.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            else Modifier
                        ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = if (isActive) Lucide.Check else Lucide.ListMusic,
                            contentDescription = null,
                            tint = if (isActive) accentColor else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    text = {
                        Text(
                            text = queue.name,
                            color = if (isActive) accentColor else Color.White,
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = if (!queue.isPermanent && onDeleteQueue != null) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        onDeleteQueue(queue.id)
                                        expanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = "Delete Queue",
                                    tint = Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else null,
                    onClick = {
                        onQueueSelected(queue.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
