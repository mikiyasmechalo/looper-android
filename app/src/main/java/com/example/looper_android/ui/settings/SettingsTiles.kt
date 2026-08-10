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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
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
                    checkedThumbColor = Color(0xFF121212),
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
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
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
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
                    val iconTint = if (destructive) Color(0xFFFF5252) else accentColor
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
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
