package com.example.looper_android.ui.equalizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.looper_android.playback.equalizer.AudioOutputDeviceType
import com.example.looper_android.playback.equalizer.BluetoothDeviceProfileInfo
import com.example.looper_android.playback.equalizer.EqualizerPresets
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.settings.SettingsViewModel
import com.example.looper_android.ui.theme.BrandAccent
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper
import com.example.looper_android.ui.widgets.PremiumSection
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Bluetooth
import com.composables.icons.lucide.Cable
import com.composables.icons.lucide.Radio
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Speaker
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Check
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val haptics = rememberHapticFeedbackHelper()
    val activeDevice by viewModel.activeOutputDevice.collectAsState()
    val selectedTab by viewModel.selectedDeviceTab.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isAutoSwitch by viewModel.isAutoSwitchEnabled.collectAsState()
    val knownBtDevices by viewModel.knownBluetoothDevices.collectAsState()
    val selectedBtKey by viewModel.selectedBluetoothDeviceKey.collectAsState()

    val darkTheme by settingsViewModel.darkTheme.collectAsState()
    val customAccentColor by settingsViewModel.customAccentColor.collectAsState()
    val dynamicAccentColor by settingsViewModel.dynamicAccentColor.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()

    // Resolve active dynamic/custom accent color
    val accentColor = remember(playbackState.accentColor, dynamicAccentColor, customAccentColor) {
        if (dynamicAccentColor && playbackState.accentColor != Color.Unspecified && playbackState.accentColor != BrandAccent) {
            playbackState.accentColor
        } else if (playbackState.accentColor != Color.Unspecified) {
            playbackState.accentColor
        } else {
            Color(customAccentColor)
        }
    }

    var showBtDeviceSheet by remember { mutableStateOf(false) }

    val isEditingActiveDevice = if (selectedTab == AudioOutputDeviceType.BLUETOOTH) {
        activeDevice.type == AudioOutputDeviceType.BLUETOOTH && (selectedBtKey == null || selectedBtKey == activeDevice.deviceKey || (selectedBtKey == "BT_DEFAULT" && activeDevice.deviceKey == "BT_DEFAULT"))
    } else {
        selectedTab == activeDevice.type
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to accentColor.copy(alpha = 0.12f),
                    0.20f to (if (darkTheme) Color(0xFF07070A) else Color(0xFF101015)),
                    1.0f to (if (darkTheme) Color(0xFF07070A) else Color(0xFF101015))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                EqualizerTopBar(
                    selectedTab = selectedTab,
                    isMasterEnabled = currentProfile.isEnabled,
                    accentColor = accentColor,
                    onBack = onBack,
                    onToggleMaster = {
                        haptics.lightImpact()
                        viewModel.setMasterEnabled(it)
                    },
                    onReset = {
                        haptics.lightImpact()
                        viewModel.resetCurrentProfile()
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ── 1. Output Device Tabs ──────────────────────────────────
                Text(
                    text = "OUTPUT DEVICE PROFILES",
                    color = accentColor.copy(alpha = 0.85f),
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                DeviceTabsRow(
                    selectedTab = selectedTab,
                    activeDeviceType = activeDevice.type,
                    accentColor = accentColor,
                    onSelectTab = {
                        haptics.selectionClick()
                        viewModel.selectDeviceTab(it)
                    }
                )

                // ── Bluetooth Device Selector Card (if Bluetooth tab) ────────
                if (selectedTab == AudioOutputDeviceType.BLUETOOTH) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val currentBtName = remember(selectedBtKey, knownBtDevices, activeDevice) {
                        val match = knownBtDevices.firstOrNull { it.deviceKey == selectedBtKey }
                        match?.displayName ?: if (activeDevice.type == AudioOutputDeviceType.BLUETOOTH) activeDevice.displayName else "Default Bluetooth Profile"
                    }
                    val isBtConnectedNow = activeDevice.type == AudioOutputDeviceType.BLUETOOTH && (selectedBtKey == activeDevice.deviceKey || (selectedBtKey == "BT_DEFAULT" && activeDevice.deviceKey == "BT_DEFAULT"))

                    BluetoothDeviceSelectorCard(
                        selectedDeviceName = currentBtName,
                        isConnectedNow = isBtConnectedNow,
                        accentColor = accentColor,
                        onOpenSelector = {
                            haptics.selectionClick()
                            showBtDeviceSheet = true
                        }
                    )
                }

                // ── Non-Active Device Notification Banner ────────────────────
                if (!isEditingActiveDevice) {
                    Spacer(modifier = Modifier.height(10.dp))
                    NonActiveDeviceBanner(
                        selectedTabName = selectedTab.displayName,
                        activeDeviceName = activeDevice.displayName,
                        accentColor = accentColor,
                        onSync = {
                            haptics.selectionClick()
                            viewModel.syncToActiveDevice()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 2. Preset Selector ─────────────────────────────────────
                Text(
                    text = "PRESETS",
                    color = accentColor.copy(alpha = 0.85f),
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                PresetChipsRow(
                    currentPreset = currentProfile.presetName,
                    accentColor = accentColor,
                    isEnabled = currentProfile.isEnabled,
                    onSelectPreset = {
                        haptics.selectionClick()
                        viewModel.selectPreset(it)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── 3. Frequency Response Curve & 10-Band Sliders ──────────
                Text(
                    text = "10-BAND GRAPHIC EQUALIZER",
                    color = accentColor.copy(alpha = 0.85f),
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                PremiumSection(
                    cornerRadius = 20.dp,
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        // Frequency response canvas
                        FrequencyCurveCanvas(
                            bandLevels = currentProfile.bandLevels,
                            accentColor = if (currentProfile.isEnabled) accentColor else Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 10 vertical sliders
                        TenBandSlidersRow(
                            bandLevels = currentProfile.bandLevels,
                            accentColor = accentColor,
                            isEnabled = currentProfile.isEnabled,
                            onLevelChange = { index, gainDb ->
                                viewModel.setBandLevel(index, gainDb)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 4. Sound Enhancements (Bass Boost, Virtualizer, Preamp) ──
                Text(
                    text = "SOUND ENHANCEMENTS",
                    color = accentColor.copy(alpha = 0.85f),
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                PremiumSection(
                    cornerRadius = 20.dp,
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Preamp Volume Gain (-15..+15 dB)
                        EffectSliderRow(
                            icon = Lucide.Volume2,
                            title = "Preamp Gain",
                            subtitle = "Adjust source output volume before equalizer stages",
                            valueText = "${if (currentProfile.preampGainDb > 0) "+" else ""}%.1f dB".format(currentProfile.preampGainDb),
                            progress = (currentProfile.preampGainDb + 15f) / 30f,
                            accentColor = accentColor,
                            isEnabled = currentProfile.isEnabled,
                            onProgressChange = {
                                val db = (it * 30f) - 15f
                                viewModel.setPreampGain(db)
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        // Bass Boost (0..100%)
                        EffectSliderRow(
                            icon = Lucide.Speaker,
                            title = "Bass Boost",
                            subtitle = "80Hz low-shelf resonant sub-bass enhancement",
                            valueText = "${currentProfile.bassBoost}%",
                            progress = currentProfile.bassBoost / 100f,
                            accentColor = accentColor,
                            isEnabled = currentProfile.isEnabled,
                            onProgressChange = {
                                viewModel.setBassBoost((it * 100f).roundToInt())
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        // 3D Virtualizer (0..100%)
                        EffectSliderRow(
                            icon = Lucide.Sparkles,
                            title = "3D Spatial Virtualizer",
                            subtitle = "Psychoacoustic Mid-Side stereo soundstage widening",
                            valueText = "${currentProfile.virtualizer}%",
                            progress = currentProfile.virtualizer / 100f,
                            accentColor = accentColor,
                            isEnabled = currentProfile.isEnabled,
                            onProgressChange = {
                                viewModel.setVirtualizer((it * 100f).roundToInt())
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── 5. Auto-Switching Settings Card ────────────────────────
                PremiumSection(
                    cornerRadius = 20.dp,
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.RefreshCw,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Switch Device Profiles",
                                color = Color.White,
                                fontFamily = JostFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Automatically apply profile when output route changes",
                                color = Color.White.copy(alpha = 0.50f),
                                fontFamily = JostFontFamily,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isAutoSwitch,
                            onCheckedChange = {
                                haptics.lightImpact()
                                viewModel.toggleAutoSwitch(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // ── Bluetooth Devices Bottom Sheet ─────────────────────────────────
        if (showBtDeviceSheet) {
            BluetoothDeviceSelectorBottomSheet(
                knownDevices = knownBtDevices,
                selectedKey = selectedBtKey ?: "BT_DEFAULT",
                activeBtKey = if (activeDevice.type == AudioOutputDeviceType.BLUETOOTH) activeDevice.deviceKey else null,
                accentColor = accentColor,
                onDismiss = { showBtDeviceSheet = false },
                onSelectDevice = { key, name ->
                    haptics.selectionClick()
                    viewModel.selectBluetoothDevice(key, name)
                    showBtDeviceSheet = false
                },
                onDeleteProfile = { key ->
                    haptics.lightImpact()
                    viewModel.deleteBluetoothProfile(key)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EqualizerTopBar(
    selectedTab: AudioOutputDeviceType,
    isMasterEnabled: Boolean,
    accentColor: Color,
    onBack: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PremiumSection(
            shape = RoundedCornerShape(32.dp),
            width = 44.dp,
            height = 44.dp,
            backgroundColor = Color.White.copy(alpha = 0.08f),
            onClick = onBack
        ) {
            Icon(
                imageVector = Lucide.ChevronLeft,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Equalizer",
                color = Color.White,
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = if (isMasterEnabled) "${selectedTab.displayName} (Active)" else "Off",
                color = if (isMasterEnabled) accentColor else Color.White.copy(alpha = 0.45f),
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }

        // Reset profile button
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Icon(
                imageVector = Lucide.RotateCcw,
                contentDescription = "Reset EQ",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Master toggle switch
        Switch(
            checked = isMasterEnabled,
            onCheckedChange = onToggleMaster,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEVICE TABS ROW (Clean Centered Layout with Top-Right Active Dot)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DeviceTabsRow(
    selectedTab: AudioOutputDeviceType,
    activeDeviceType: AudioOutputDeviceType,
    accentColor: Color,
    onSelectTab: (AudioOutputDeviceType) -> Unit
) {
    val tabs = listOf(
        AudioOutputDeviceType.SPEAKER to Lucide.Volume2,
        AudioOutputDeviceType.WIRED_HEADSET to Lucide.Headphones,
        AudioOutputDeviceType.BLUETOOTH to Lucide.Bluetooth,
        AudioOutputDeviceType.USB_DAC to Lucide.Cable
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (type, icon) ->
            val isSelected = selectedTab == type
            val isActiveNow = activeDeviceType == type

            val bgAlpha by animateFloatAsState(if (isSelected) 0.16f else 0.04f, tween(250), label = "bg")
            val borderAlpha by animateFloatAsState(if (isSelected) 0.70f else 0.08f, tween(250), label = "border")
            val borderColor = if (isSelected) accentColor.copy(alpha = borderAlpha) else Color.White.copy(alpha = borderAlpha)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = bgAlpha) else Color.White.copy(alpha = bgAlpha)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(type) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Centered Icon & Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.displayName,
                            tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (type) {
                                AudioOutputDeviceType.SPEAKER -> "Speaker"
                                AudioOutputDeviceType.WIRED_HEADSET -> "Headset"
                                AudioOutputDeviceType.BLUETOOTH -> "Bluetooth"
                                AudioOutputDeviceType.USB_DAC -> "USB DAC"
                                else -> "Other"
                            },
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f),
                            fontFamily = JostFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    // Sleek Top-Right Active Indicator Dot
                    if (isActiveNow) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 0.dp, end = 2.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BLUETOOTH DEVICE SELECTOR CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BluetoothDeviceSelectorCard(
    selectedDeviceName: String,
    isConnectedNow: Boolean,
    accentColor: Color,
    onOpenSelector: () -> Unit
) {
    PremiumSection(
        cornerRadius = 16.dp,
        backgroundColor = accentColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenSelector
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Bluetooth,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BLUETOOTH PROFILE",
                    color = accentColor,
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedDeviceName,
                    color = Color.White,
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isConnectedNow) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.20f))
                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CONNECTED",
                        color = accentColor,
                        fontFamily = JostFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = "Switch Bluetooth Profile",
                tint = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BLUETOOTH DEVICE SELECTOR BOTTOM SHEET
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothDeviceSelectorBottomSheet(
    knownDevices: List<BluetoothDeviceProfileInfo>,
    selectedKey: String,
    activeBtKey: String?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelectDevice: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141419),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Bluetooth Device Profiles",
                color = Color.White,
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Select a profile to customize or apply for your Bluetooth devices",
                color = Color.White.copy(alpha = 0.50f),
                fontFamily = JostFontFamily,
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(knownDevices) { item ->
                    val isSelected = item.deviceKey == selectedKey
                    val isConnected = item.deviceKey == activeBtKey

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDevice(item.deviceKey, item.displayName) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.Bluetooth,
                            contentDescription = null,
                            tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayName,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                fontFamily = JostFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.5.sp
                            )
                            if (isConnected) {
                                Text(
                                    text = "Currently Connected",
                                    color = accentColor,
                                    fontFamily = JostFontFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (item.deviceKey != "BT_DEFAULT") {
                            IconButton(
                                onClick = { onDeleteProfile(item.deviceKey) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = "Delete Profile",
                                    tint = Color.White.copy(alpha = 0.40f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "Selected",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NON-ACTIVE DEVICE NOTIFICATION BANNER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NonActiveDeviceBanner(
    selectedTabName: String,
    activeDeviceName: String,
    accentColor: Color,
    onSync: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Lucide.Radio,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Editing $selectedTabName profile. Currently playing via $activeDeviceName.",
            color = Color.White.copy(alpha = 0.85f),
            fontFamily = JostFontFamily,
            fontSize = 11.5.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Switch",
            color = accentColor,
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.clickable { onSync() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRESET CHIPS ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PresetChipsRow(
    currentPreset: String,
    accentColor: Color,
    isEnabled: Boolean,
    onSelectPreset: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(EqualizerPresets.PRESET_NAMES) { preset ->
            val isSelected = currentPreset == preset
            val chipBg = if (isSelected && isEnabled) accentColor else Color.White.copy(alpha = 0.06f)
            val chipText = if (isSelected && isEnabled) Color.Black else Color.White.copy(alpha = if (isSelected) 1.0f else 0.70f)
            val border = if (isSelected && !isEnabled) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .then(if (border != null) Modifier.border(border, RoundedCornerShape(20.dp)) else Modifier)
                    .clickable { onSelectPreset(preset) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = preset,
                    color = chipText,
                    fontFamily = JostFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FREQUENCY CURVE CANVAS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FrequencyCurveCanvas(
    bandLevels: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Draw 0dB center line
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        if (bandLevels.isEmpty()) return@Canvas

        val n = bandLevels.size
        val stepX = w / (n - 1).coerceAtLeast(1)
        val points = mutableListOf<Offset>()

        for (i in 0 until n) {
            val x = i * stepX
            val normalized = (bandLevels[i] / 15f).coerceIn(-1f, 1f)
            val y = midY - (normalized * (h * 0.42f))
            points.add(Offset(x, y))
        }

        // Build smooth Bézier spline path
        val curvePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cpx = (p0.x + p1.x) / 2f
                cubicTo(cpx, p0.y, cpx, p1.y, p1.x, p1.y)
            }
        }

        // Fill under curve
        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // Draw spline curve line
        drawPath(
            path = curvePath,
            color = accentColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw node circles
        points.forEach { pt ->
            drawCircle(
                color = Color(0xFF16161D),
                radius = 3.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = accentColor,
                radius = 2.2.dp.toPx(),
                center = pt
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10-BAND SLIDERS ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TenBandSlidersRow(
    bandLevels: List<Float>,
    accentColor: Color,
    isEnabled: Boolean,
    onLevelChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0 until 10) {
            val level = bandLevels.getOrElse(i) { 0f }
            val label = labels.getOrElse(i) { "" }
            VerticalBandSlider(
                bandIndex = i,
                levelDb = level,
                freqLabel = label,
                accentColor = accentColor,
                isEnabled = isEnabled,
                onLevelChanged = { onLevelChange(i, it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VERTICAL BAND SLIDER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VerticalBandSlider(
    bandIndex: Int,
    levelDb: Float,
    freqLabel: String,
    accentColor: Color,
    isEnabled: Boolean,
    onLevelChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedbackHelper()

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain readout (e.g. +6, 0, -3)
        val rounded = levelDb.roundToInt()
        val text = if (rounded > 0) "+$rounded" else "$rounded"
        Text(
            text = text,
            color = if (isEnabled) (if (rounded == 0) Color.White.copy(alpha = 0.5f) else accentColor) else Color.White.copy(alpha = 0.25f),
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .width(28.dp)
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val h = size.height
                        if (h > 0) {
                            val ratio = 1.0f - (change.position.y / h).coerceIn(0f, 1f)
                            val newDb = (ratio * 30f) - 15f
                            onLevelChanged(newDb)
                        }
                    }
                }
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures { offset ->
                        val h = size.height
                        if (h > 0) {
                            val ratio = 1.0f - (offset.y / h).coerceIn(0f, 1f)
                            val newDb = (ratio * 30f) - 15f
                            haptics.selectionClick()
                            onLevelChanged(newDb)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val totalHeight = maxHeight
            val thumbSize = 16.dp
            val usableHeight = (totalHeight - thumbSize).coerceAtLeast(0.dp)
            val normalized = ((levelDb + 15f) / 30f).coerceIn(0f, 1f)
            val thumbOffsetY = usableHeight * (1.0f - normalized)

            // Track line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )

            // Center 0dB notch
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.35f))
            )

            // Thumb
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = thumbOffsetY)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(if (isEnabled) accentColor else Color.Gray)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Hz frequency label
        Text(
            text = freqLabel,
            color = Color.White.copy(alpha = 0.60f),
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.5.sp,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUDIO EFFECT SLIDER ROW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EffectSliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    valueText: String,
    progress: Float,
    accentColor: Color,
    isEnabled: Boolean,
    onProgressChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) accentColor else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontFamily = JostFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.45f),
                            fontFamily = JostFontFamily,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
            Text(
                text = valueText,
                color = if (isEnabled) accentColor else Color.White.copy(alpha = 0.40f),
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = progress,
            onValueChange = onProgressChange,
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.Gray.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
