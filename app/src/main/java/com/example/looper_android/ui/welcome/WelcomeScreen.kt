package com.example.looper_android.ui.welcome

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper

@Composable
fun WelcomeScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = rememberHapticFeedbackHelper()
    val context = LocalContext.current

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissionsAndAutoScan()
    }

    val safFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            haptics.mediumImpact()
            viewModel.addSafFolder(it)
        }
    }

    val isSufficientPermission = uiState.isAudioPermissionGranted || uiState.isAllFilesPermissionGranted
    val isGoStartEnabled = isSufficientPermission && !uiState.isScanning

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11110E))
    ) {
        // Glowing Background Orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x3355DF69), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-150).dp, y = 150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x2255DF69), Color.Transparent)
                    )
                )
        )

        val density = androidx.compose.ui.platform.LocalDensity.current
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        var savedTopPx by remember { mutableStateOf<Float?>(null) }
        val currentTopPx = with(density) { topInset.toPx() }
        val saved = savedTopPx
        if (saved == null && currentTopPx > 0f) { savedTopPx = currentTopPx }
        val stableTopDp = with(density) { (saved ?: currentTopPx).toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = stableTopDp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Logo & Typography
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1A55DF69))
                    .border(1.dp, Color(0x3355DF69), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = "Logo",
                    tint = com.example.looper_android.ui.theme.LocalAccentColor.current,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LOOPER",
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "LOOPER PLAYER",
                color = com.example.looper_android.ui.theme.LocalAccentColor.current.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Premium About Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = null,
                            tint = com.example.looper_android.ui.theme.LocalAccentColor.current,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ABOUT LOOPER PLAYER",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Looper Player is a next-generation Music-OS built for high-fidelity offline audio playback. Features real-time lyrics, advanced session management, and persistent folder write access for ID3 tag editing.",
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 12.sp,
                        fontFamily = DmSansFontFamily,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // System Permission Checklist & Live Scanning Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.ShieldCheck,
                            contentDescription = null,
                            tint = com.example.looper_android.ui.theme.LocalAccentColor.current,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MUSIC STORAGE PERMISSION",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Music & Audio Access Permission Row
                    PermissionCheckRow(
                        title = "MUSIC & AUDIO ACCESS",
                        description = "Required to discover and play standard offline audio tracks on your device.",
                        isGranted = uiState.isAudioPermissionGranted,
                        onGrant = {
                            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                android.Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            audioPermissionLauncher.launch(perm)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.06f)
                    )

                    // 2. Persistent SAF Music Folders Row (With ID3 tag edit hint banner)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.grantedFolders.isNotEmpty()) Color(0x2655DF69)
                                        else Color.White.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.grantedFolders.isNotEmpty()) Lucide.Check else Lucide.FolderPlus,
                                    contentDescription = null,
                                    tint = if (uiState.grantedFolders.isNotEmpty()) com.example.looper_android.ui.theme.LocalAccentColor.current else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MUSIC LIBRARY FOLDERS (SAF)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Grants persistent read & write access to scan songs and edit ID3 tags without system dialog prompts.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontFamily = DmSansFontFamily,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(com.example.looper_android.ui.theme.LocalAccentColor.current.copy(alpha = 0.15f))
                                    .clickable {
                                        haptics.lightImpact()
                                        safFolderPickerLauncher.launch(null)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+ ADD FOLDER",
                                    color = com.example.looper_android.ui.theme.LocalAccentColor.current,
                                    fontSize = 11.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Display list of added SAF folders if any
                        if (uiState.grantedFolders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            uiState.grantedFolders.forEach { folderPath ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Lucide.FolderCheck,
                                        contentDescription = null,
                                        tint = com.example.looper_android.ui.theme.LocalAccentColor.current,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = folderPath,
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 11.sp,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Scanning Progress Status Banner (With numeric count e.g., 42/150 songs)
            if (uiState.isScanning || isSufficientPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (uiState.isScanning) Color.White.copy(alpha = 0.04f)
                            else com.example.looper_android.ui.theme.LocalAccentColor.current.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (uiState.isScanning) Color.White.copy(alpha = 0.08f)
                            else com.example.looper_android.ui.theme.LocalAccentColor.current.copy(alpha = 0.18f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                color = com.example.looper_android.ui.theme.LocalAccentColor.current,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.CircleCheck,
                                contentDescription = null,
                                tint = com.example.looper_android.ui.theme.LocalAccentColor.current,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isScanning) "SCANNING LIBRARY..." else "SCAN COMPLETE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (uiState.isScanning && uiState.scanProgressText.isNotEmpty()) {
                                    "${uiState.scanStatusMessage} • ${uiState.scanProgressText}"
                                } else {
                                    uiState.scanStatusMessage
                                },
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontFamily = DmSansFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "GO START" Action Button (Disabled while scanning is in progress!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (isGoStartEnabled) com.example.looper_android.ui.theme.LocalAccentColor.current else Color.White.copy(alpha = 0.10f)
                    )
                    .clickable(enabled = isGoStartEnabled) {
                        haptics.mediumImpact()
                        viewModel.bypassWelcome {
                            onNavigateToDashboard()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.CirclePlay,
                        contentDescription = null,
                        tint = if (isGoStartEnabled) Color.Black else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.isScanning) "SCANNING..." else "GO START",
                        color = if (isGoStartEnabled) Color.Black else Color.White.copy(alpha = 0.3f),
                        fontSize = 15.sp,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionCheckRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val haptics = rememberHapticFeedbackHelper()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) Color(0x2655DF69) else Color.White.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Lucide.Check else Lucide.ShieldAlert,
                contentDescription = null,
                tint = if (isGranted) com.example.looper_android.ui.theme.LocalAccentColor.current else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = DmSansFontFamily,
                lineHeight = 16.sp
            )
        }
        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                        haptics.lightImpact()
                        onGrant()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "GRANT",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
