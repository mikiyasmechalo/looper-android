package com.example.looper_android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.looper_android.ui.widgets.PremiumSection

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.looper_android.ui.widgets.sharedBoundsOrElement

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PremiumNavbar(
    currentIndex: Int,
    onTap: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val gap1 by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (currentIndex == 0 || currentIndex == 1) 6.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300)
    )
    val gap2 by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (currentIndex == 1 || currentIndex == 2) 6.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300)
    )

    val homeRadiusEnd by androidx.compose.animation.core.animateDpAsState(if (currentIndex == 2) 0.dp else 12.dp, androidx.compose.animation.core.tween(300))
    val songsRadiusStart by androidx.compose.animation.core.animateDpAsState(if (currentIndex == 2) 0.dp else 12.dp, androidx.compose.animation.core.tween(300))
    val songsRadiusEnd by androidx.compose.animation.core.animateDpAsState(if (currentIndex == 0) 0.dp else 12.dp, androidx.compose.animation.core.tween(300))
    val libraryRadiusStart by androidx.compose.animation.core.animateDpAsState(if (currentIndex == 0) 0.dp else 12.dp, androidx.compose.animation.core.tween(300))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center
        ) {
            // HOME (Morphs to Shuffle button in PlayerScreen)
            PremiumSection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .sharedBoundsOrElement("nav_morph_1", sharedTransitionScope, animatedVisibilityScope),
                shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = homeRadiusEnd, bottomEnd = homeRadiusEnd),
                backgroundColor = if (currentIndex == 0) Color(0xFF2A2A2A) else Color(0xFF1E1E1E),
                showRightBorder = currentIndex == 0 || currentIndex == 1,
                onClick = { onTap(0) }
            ) {
                NavItem(iconPath = "file:///android_asset/android_icons/home_tab_icon.svg", label = "Home", isSelected = currentIndex == 0)
            }

            Spacer(modifier = Modifier.width(gap1))

            // SONGS (Morphs to Repeat button in PlayerScreen)
            PremiumSection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .sharedBoundsOrElement("nav_morph_2", sharedTransitionScope, animatedVisibilityScope),
                shape = RoundedCornerShape(topStart = songsRadiusStart, bottomStart = songsRadiusStart, topEnd = songsRadiusEnd, bottomEnd = songsRadiusEnd),
                backgroundColor = if (currentIndex == 1) Color(0xFF2A2A2A) else Color(0xFF1E1E1E),
                showLeftBorder = currentIndex == 0 || currentIndex == 1,
                showRightBorder = currentIndex == 1 || currentIndex == 2,
                onClick = { onTap(1) }
            ) {
                NavItem(iconPath = "file:///android_asset/android_icons/songs_tab_icon.svg", label = "Songs", isSelected = currentIndex == 1)
            }

            Spacer(modifier = Modifier.width(gap2))

            // LIBRARY (Morphs to Lyrics button in PlayerScreen)
            PremiumSection(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .sharedBoundsOrElement("nav_morph_3", sharedTransitionScope, animatedVisibilityScope),
                shape = RoundedCornerShape(topStart = libraryRadiusStart, bottomStart = libraryRadiusStart, topEnd = 36.dp, bottomEnd = 36.dp),
                backgroundColor = if (currentIndex == 2) Color(0xFF2A2A2A) else Color(0xFF1E1E1E),
                showLeftBorder = currentIndex == 1 || currentIndex == 2,
                onClick = { onTap(2) }
            ) {
                NavItem(iconPath = "file:///android_asset/android_icons/library_tab_icon.svg", label = "Library", isSelected = currentIndex == 2)
            }
        }
    }
}

@Composable
fun NavItem(iconPath: String, label: String, isSelected: Boolean) {
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, label = "scale")
    val color = if (isSelected) Color(0xFF55DF69) else Color.White.copy(alpha = 0.4f)
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(iconPath)
                .build(),
            contentDescription = label,
            modifier = Modifier
                .size(17.dp)
                .scale(scale),
            colorFilter = ColorFilter.tint(color)
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
