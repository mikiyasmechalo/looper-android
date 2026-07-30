package com.example.looper_android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.example.looper_android.ui.theme.LucideIcons
import com.example.looper_android.ui.widgets.PremiumSection
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SortByAlpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = Color(0xFF55DF69)
) {
    val haptics = LocalHapticFeedback.current
    var isAscending by remember { mutableStateOf(currentSort != SortOption.TITLE_DESC && currentSort != SortOption.YEAR_NEWEST) }

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
                        isAscending = !isAscending
                        val toggledSort = when (currentSort) {
                            SortOption.TITLE_ASC -> SortOption.TITLE_DESC
                            SortOption.TITLE_DESC -> SortOption.TITLE_ASC
                            SortOption.YEAR_NEWEST -> SortOption.YEAR_OLDEST
                            SortOption.YEAR_OLDEST -> SortOption.YEAR_NEWEST
                            else -> currentSort
                        }
                        onSortSelected(toggledSort)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isAscending) LucideIcons.Search else LucideIcons.Settings, // placeholder or arrow icon
                        contentDescription = null,
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

            // Inner PremiumSection Box for Sort Options List (Matches Flutter 1-to-1)
            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sortItems = listOf(
                        SortOption.DEFAULT,
                        SortOption.TITLE_ASC,
                        SortOption.ARTIST_ASC,
                        SortOption.ALBUM_ASC,
                        SortOption.DURATION,
                        SortOption.YEAR_NEWEST
                    )

                    sortItems.forEachIndexed { index, option ->
                        val isSelected = (currentSort == option) || 
                            (option == SortOption.TITLE_ASC && currentSort == SortOption.TITLE_DESC) ||
                            (option == SortOption.YEAR_NEWEST && currentSort == SortOption.YEAR_OLDEST)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    val finalOption = if (!isAscending) {
                                        when (option) {
                                            SortOption.TITLE_ASC -> SortOption.TITLE_DESC
                                            SortOption.YEAR_NEWEST -> SortOption.YEAR_OLDEST
                                            else -> option
                                        }
                                    } else option
                                    onSortSelected(finalOption)
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    SortOption.DEFAULT -> LucideIcons.Library
                                    SortOption.TITLE_ASC -> Icons.Default.SortByAlpha
                                    SortOption.ARTIST_ASC -> Icons.Default.Person
                                    SortOption.ALBUM_ASC -> Icons.Default.Album
                                    SortOption.DURATION -> Icons.Default.AccessTime
                                    else -> Icons.Default.CalendarToday
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
