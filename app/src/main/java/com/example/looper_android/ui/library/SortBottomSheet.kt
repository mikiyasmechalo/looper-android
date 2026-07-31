package com.example.looper_android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.models.SortOption
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    isAscending: Boolean,
    onSortSelected: (SortOption) -> Unit,
    onToggleAscending: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = Color(0xFF55DF69)
) {
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row: "Sort Order" + Ascending/Descending Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort Order",
                    color = Color.White,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onToggleAscending()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isAscending) "Ascending" else "Descending",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAscending) "Ascending" else "Descending",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.10f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Sort Options List
            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sortItems = SortOption.entries

                    sortItems.forEachIndexed { index, option ->
                        val isSelected = (currentSort == option)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSortSelected(option)
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    SortOption.TITLE -> Icons.Default.SortByAlpha
                                    SortOption.ARTIST -> Icons.Default.Person
                                    SortOption.ALBUM -> Icons.Default.Album
                                    SortOption.DATE_ADDED -> Icons.Default.AccessTime
                                    SortOption.GENRE -> Icons.Default.Category
                                    SortOption.DURATION -> Icons.Default.Timer
                                    SortOption.YEAR -> Icons.Default.CalendarToday
                                    SortOption.MOST_PLAYED -> Icons.Default.Equalizer
                                    SortOption.RECENTLY_PLAYED -> Icons.Default.History
                                },
                                contentDescription = option.label,
                                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = option.label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                                fontFamily = DmSansFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (index < sortItems.lastIndex) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.04f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
