package com.example.looper_android.ui.player

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Timer
import com.composables.icons.lucide.X
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    fun hapticLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun hapticMedium() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    var customMinutes by remember { mutableIntStateOf(15) }
    var customSongs by remember { mutableIntStateOf(3) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sleep Timer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = if (playbackState.isSleepTimerActive) {
                if (playbackState.sleepTimerDurationRemainingMs != null) {
                    "Active: Stopping in ${playbackState.sleepTimerLabel}"
                } else {
                    "Active: Stopping after ${playbackState.sleepTimerLabel}"
                }
            } else {
                "Select when to pause music playback"
            }

            Text(
                text = statusText,
                color = if (playbackState.isSleepTimerActive) accentColor else Color.White.copy(alpha = 0.54f),
                fontSize = 14.sp,
                fontFamily = DmSansFontFamily
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Section 1: STOP BY TIME ────────────────────────────────────
            Text(
                text = "STOP BY TIME",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = DmSansFontFamily
            )

            Spacer(modifier = Modifier.height(12.dp))

            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.05f),
                onClick = null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                hapticLight()
                                if (customMinutes > 1) customMinutes--
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                        ) {
                            Icon(Lucide.Minus, contentDescription = "Decrease", tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "$customMinutes ${if (customMinutes == 1) "Min" else "Mins"}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                hapticLight()
                                customMinutes++
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                        ) {
                            Icon(Lucide.Plus, contentDescription = "Increase", tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = {
                            hapticMedium()
                            playbackViewModel.startSleepTimer(durationMinutes = customMinutes)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10, 30, 45).forEach { mins ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                hapticMedium()
                                playbackViewModel.startSleepTimer(durationMinutes = mins)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "$mins Min",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Section 2: STOP BY SONG COUNT ──────────────────────────────
            Text(
                text = "STOP BY SONG COUNT",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = DmSansFontFamily
            )

            Spacer(modifier = Modifier.height(12.dp))

            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.05f),
                onClick = null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                hapticLight()
                                if (customSongs > 1) customSongs--
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                        ) {
                            Icon(Lucide.Minus, contentDescription = "Decrease", tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "$customSongs ${if (customSongs == 1) "Song" else "Songs"}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                hapticLight()
                                customSongs++
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                        ) {
                            Icon(Lucide.Plus, contentDescription = "Increase", tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                        }
                    }

                    Button(
                        onClick = {
                            hapticMedium()
                            playbackViewModel.startSleepTimer(songCount = customSongs)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Song Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3, 5, 10).forEach { songs ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                hapticMedium()
                                playbackViewModel.startSleepTimer(songCount = songs)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "$songs ${if (songs == 1) "Song" else "Songs"}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            // Cancel Sleep Timer Button
            if (playbackState.isSleepTimerActive) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        hapticMedium()
                        playbackViewModel.stopSleepTimer()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF4D4D),
                        containerColor = Color(0xFFFF4D4D).copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4D)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Lucide.X, contentDescription = null, tint = Color(0xFFFF4D4D), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel Sleep Timer",
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
