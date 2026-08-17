package com.example.looper_android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowDownZA
import com.composables.icons.lucide.ArrowUpAZ
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Grid3x3
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

data class CategorySortOptionItem<T>(
    val option: T,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CategorySortBottomSheet(
    options: List<CategorySortOptionItem<T>>,
    selectedOption: T,
    isAscending: Boolean,
    onOptionSelected: (T) -> Unit,
    onToggleAscending: () -> Unit,
    viewMode: ViewMode? = null,
    onViewModeChange: ((ViewMode) -> Unit)? = null,
    onDismiss: () -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            // Optional Layout View Mode Selector
            if (viewMode != null && onViewModeChange != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layout View",
                        color = Color.White,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val viewModes = listOf(
                        Triple(ViewMode.GRID_2, "Grid (2)", Lucide.LayoutGrid),
                        Triple(ViewMode.GRID_3, "Grid (3)", Lucide.Grid3x3),
                        Triple(ViewMode.LIST, "List", Lucide.List)
                    )

                    viewModes.forEach { (mode, label, icon) ->
                        val isSelected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentColor else Color.Transparent)
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onViewModeChange(mode)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.70f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = DmSansFontFamily
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.10f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }

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
                        imageVector = if (isAscending) Lucide.ArrowUpAZ else Lucide.ArrowDownZA,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Options List Container
            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = null
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, item ->
                        val isSelected = (selectedOption == item.option)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onOptionSelected(item.option)
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = item.label,
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
                                        imageVector = Lucide.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (index < options.lastIndex) {
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
