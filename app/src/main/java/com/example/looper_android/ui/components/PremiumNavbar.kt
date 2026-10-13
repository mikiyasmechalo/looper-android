package com.example.looper_android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.example.looper_android.ui.widgets.PremiumSection
import com.example.looper_android.ui.widgets.sharedBoundsOrElement

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PremiumNavbar(
    currentIndex: Int,
    onTap: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    enableBlur: Boolean = false,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    darkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selectedBg = if (darkTheme) Color(0xFF1A1A1A) else Color(0xFF2A2A2A)
    val unselectedBg = if (darkTheme) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (currentIndex) {
                0 -> {
                    // Home is selected (Pill 1)
                    PremiumSection(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                        backgroundColor = selectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = { onTap(0) }
                    ) {
                        NavItem(icon = Lucide.House, label = "Home", isSelected = true, accentColor = accentColor)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Combined unselected section for Songs & Library (No seam!)
                    PremiumSection(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                        backgroundColor = unselectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope)
                                    .clickable { onTap(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                NavItem(icon = Lucide.Music2, label = "Songs", isSelected = false, accentColor = accentColor)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope)
                                    .clickable { onTap(2) },
                                contentAlignment = Alignment.Center
                            ) {
                                NavItem(icon = Lucide.Library, label = "Library", isSelected = false, accentColor = accentColor)
                            }
                        }
                    }
                }
                1 -> {
                    // Songs is selected (Pill 2 in middle)
                    PremiumSection(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                        backgroundColor = unselectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = { onTap(0) }
                    ) {
                        NavItem(icon = Lucide.House, label = "Home", isSelected = false)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    PremiumSection(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = selectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = { onTap(1) }
                    ) {
                        NavItem(icon = Lucide.Music2, label = "Songs", isSelected = true, accentColor = accentColor)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    PremiumSection(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                        backgroundColor = unselectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = { onTap(2) }
                    ) {
                        NavItem(icon = Lucide.Library, label = "Library", isSelected = false, accentColor = accentColor)
                    }
                }
                else -> {
                    // Library is selected (Pill 3)
                    // Combined unselected section for Home & Songs (No seam!)
                    PremiumSection(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                        backgroundColor = unselectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope)
                                    .clickable { onTap(0) },
                                contentAlignment = Alignment.Center
                            ) {
                                NavItem(icon = Lucide.House, label = "Home", isSelected = false, accentColor = accentColor)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope)
                                    .clickable { onTap(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                NavItem(icon = Lucide.Music2, label = "Songs", isSelected = false, accentColor = accentColor)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    PremiumSection(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                        backgroundColor = selectedBg,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = { onTap(2) }
                    ) {
                        NavItem(icon = Lucide.Library, label = "Library", isSelected = true, accentColor = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current) {
    val scale = if (isSelected) 1.1f else 1f
    val color = if (isSelected) accentColor else Color.White.copy(alpha = 0.4f)
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(17.dp)
                .scale(scale),
            tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.05.sp,
            fontFamily = com.example.looper_android.ui.theme.DmSansFontFamily,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
