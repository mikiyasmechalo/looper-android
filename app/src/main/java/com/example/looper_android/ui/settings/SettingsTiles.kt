package com.example.looper_android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun SettingsSubHeader(
    title: String,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        color = accentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = DmSansFontFamily,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsTileContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsSwitchTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    SettingsTileContainer(onClick = { onCheckedChange(!checked) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SettingsOptionTile(
    title: String,
    subtitle: String? = null,
    selectedValue: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    SettingsTileContainer(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedValue,
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DmSansFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsSliderTile(
    title: String,
    subtitle: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { "${it.toInt()}" },
    icon: ImageVector? = null,
    onValueChange: (Float) -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    SettingsTileContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = DmSansFontFamily
                        )
                        if (!subtitle.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = subtitle,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontFamily = DmSansFontFamily
                            )
                        }
                    }
                }
                Text(
                    text = valueFormatter(value),
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun SettingsActionTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    destructive: Boolean = false
) {
    SettingsTileContainer(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    val badgeBg = if (destructive) Color(0xFFFF5252).copy(alpha = 0.14f) else accentColor.copy(alpha = 0.14f)
                    val iconTint = if (destructive) Color(0xFFFF5252) else accentColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (destructive) Color(0xFFFF5252) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsColorPaletteTile(
    title: String,
    selectedColor: Color,
    colors: List<Color>,
    onColorSelected: (Color) -> Unit
) {
    SettingsTileContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DmSansFontFamily
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(colors) { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSegmentedTile(
    title: String,
    subtitle: String? = null,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    SettingsTileContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DmSansFontFamily
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontFamily = DmSansFontFamily
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (label, value) ->
                    val isSelected = value == selectedOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor else Color.Transparent)
                            .clickable {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onOptionSelected(value)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.background else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDropdownTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val selectedLabel = options.find { it.second == selectedOption }?.first ?: selectedOption

    SettingsTileContainer(onClick = { expanded = true }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd), contentAlignment = Alignment.TopEnd) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = selectedLabel,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(Color(0xFF222220), RoundedCornerShape(12.dp))
                        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                        .widthIn(min = 150.dp)
                ) {
                    options.forEach { (label, value) ->
                        val isSelected = value == selectedOption
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (isSelected) accentColor else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent),
                            onClick = {
                                onOptionSelected(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
