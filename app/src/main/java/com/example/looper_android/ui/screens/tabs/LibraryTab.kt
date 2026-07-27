package com.example.looper_android.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.looper_android.ui.theme.LucideIcons
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun LibraryTab() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212))
    ) {
        // Mock Library Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(
                width = 48.dp, 
                height = 48.dp, 
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp), 
                onClick = {}
            ) {
                androidx.compose.material3.Icon(LucideIcons.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Library",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            PremiumSection(
                width = 48.dp, 
                height = 48.dp, 
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 32.dp, bottomEnd = 32.dp), 
                onClick = {}
            ) {
                androidx.compose.material3.Icon(LucideIcons.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(horizontal = 16.dp).background(Color.White.copy(alpha = 0.15f))
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Library Categories...", color = Color.Gray)
        }
    }
}
