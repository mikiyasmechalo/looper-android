package com.example.looper_android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun PlayerScreen(
    title: String,
    artist: String,
    album: String,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit
) {
    // Simulated dynamic background gradient similar to the Flutter app
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2B2B2B), Color(0xFF121212))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PremiumSection(width = 48.dp, height = 48.dp, cornerRadius = 16.dp, onClick = {}) {
                Text("v", color = Color.White, fontSize = 20.sp) // Mock Down Arrow
            }
            PremiumSection(width = 48.dp, height = 48.dp, cornerRadius = 16.dp, onClick = {}) {
                Text("...", color = Color.White) // Mock Options
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Large Album Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            // Image would go here
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Audio Quality Badge (Mocking the logic from Flutter)
        PremiumSection(
            cornerRadius = 12.dp,
            backgroundColor = Color.White.copy(alpha = 0.05f)
        ) {
            Text(
                text = "High Quality • FLAC • 44.1 kHz",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Artist and Album (Fixing the clickable bounds requested!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = artist,
                color = Color(0xFF55DF69), // Brand Green
                fontSize = 18.sp,
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable { onArtistClick() }
            )
            Text(
                text = " • ",
                color = Color.Gray,
                fontSize = 18.sp
            )
            Text(
                text = album,
                color = Color(0xFF55DF69),
                fontSize = 18.sp,
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable { onAlbumClick() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(width = 64.dp, height = 64.dp, cornerRadius = 32.dp, onClick = {}) {
                Text("|<", color = Color.White) // Mock Prev
            }
            PremiumSection(
                width = 80.dp, 
                height = 80.dp, 
                cornerRadius = 40.dp, 
                backgroundColor = Color(0xFF55DF69), // Green Play Button
                onClick = {}
            ) {
                Text("||", color = Color.Black, fontWeight = FontWeight.Bold) // Mock Pause
            }
            PremiumSection(width = 64.dp, height = 64.dp, cornerRadius = 32.dp, onClick = {}) {
                Text(">|", color = Color.White) // Mock Next
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
