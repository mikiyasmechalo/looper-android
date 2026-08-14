package com.example.looper_android.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
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

    val label = activeQueueName

    Box(modifier = modifier.width(220.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = "Switch Queue",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 6.dp),
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            queues.forEach { queue ->
                val isActive = queue.id == activeQueueId
                DropdownMenuItem(
                    text = {
                        Text(
                            text = queue.name,
                            color = if (isActive) accentColor else Color.White,
                            fontFamily = DmSansFontFamily,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    trailingIcon = if (!queue.isPermanent && onDeleteQueue != null) {
                        {
                            IconButton(
                                onClick = {
                                    onDeleteQueue(queue.id)
                                    expanded = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Delete Queue",
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
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
