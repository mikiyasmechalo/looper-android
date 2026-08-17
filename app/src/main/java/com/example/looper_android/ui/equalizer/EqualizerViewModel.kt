package com.example.looper_android.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.repository.EqualizerRepository
import com.example.looper_android.playback.equalizer.*
import com.example.looper_android.playback.equalizer.dsp.CustomEqualizerAudioProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audioDeviceDetector: AudioDeviceDetector,
    private val equalizerRepository: EqualizerRepository,
    private val equalizerAudioProcessor: CustomEqualizerAudioProcessor
) : ViewModel() {

    val activeOutputDevice: StateFlow<AudioOutputDevice> = audioDeviceDetector.currentOutputDevice

    private val _selectedDeviceTab = MutableStateFlow(AudioOutputDeviceType.SPEAKER)
    val selectedDeviceTab: StateFlow<AudioOutputDeviceType> = _selectedDeviceTab.asStateFlow()

    val isAutoSwitchEnabled: StateFlow<Boolean> = equalizerRepository.isAutoSwitchEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val knownBluetoothDevices: StateFlow<List<BluetoothDeviceProfileInfo>> = equalizerRepository.knownBluetoothDevices
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(
            BluetoothDeviceProfileInfo("BT_DEFAULT", "Default Bluetooth Profile")
        ))

    private val _selectedBluetoothDeviceKey = MutableStateFlow<String?>(null)
    val selectedBluetoothDeviceKey: StateFlow<String?> = _selectedBluetoothDeviceKey.asStateFlow()

    private val _currentProfile = MutableStateFlow(
        EqualizerPresets.defaultProfile("SPEAKER", "Phone Speaker")
    )
    val currentProfile: StateFlow<DeviceEqualizerProfile> = _currentProfile.asStateFlow()

    init {
        viewModelScope.launch {
            // Default selected tab follows active device initially
            val active = activeOutputDevice.value
            _selectedDeviceTab.value = active.type
            if (active.type == AudioOutputDeviceType.BLUETOOTH) {
                _selectedBluetoothDeviceKey.value = active.deviceKey
                equalizerRepository.registerBluetoothDevice(active.deviceKey, active.displayName)
            }
            loadProfileForCurrentSelection()
        }

        viewModelScope.launch {
            activeOutputDevice.collect { active ->
                if (active.type == AudioOutputDeviceType.BLUETOOTH) {
                    equalizerRepository.registerBluetoothDevice(active.deviceKey, active.displayName)
                }
                if (isAutoSwitchEnabled.value) {
                    _selectedDeviceTab.value = active.type
                    if (active.type == AudioOutputDeviceType.BLUETOOTH) {
                        _selectedBluetoothDeviceKey.value = active.deviceKey
                    }
                    loadProfileForCurrentSelection()
                }
            }
        }
    }

    fun selectDeviceTab(type: AudioOutputDeviceType) {
        _selectedDeviceTab.value = type
        if (type == AudioOutputDeviceType.BLUETOOTH && _selectedBluetoothDeviceKey.value == null) {
            val active = activeOutputDevice.value
            _selectedBluetoothDeviceKey.value = if (active.type == AudioOutputDeviceType.BLUETOOTH) active.deviceKey else "BT_DEFAULT"
        }
        viewModelScope.launch {
            loadProfileForCurrentSelection()
        }
    }

    fun selectBluetoothDevice(deviceKey: String, displayName: String) {
        _selectedBluetoothDeviceKey.value = deviceKey
        viewModelScope.launch {
            loadProfileForCurrentSelection()
        }
    }

    fun deleteBluetoothProfile(deviceKey: String) {
        viewModelScope.launch {
            equalizerRepository.deleteBluetoothProfile(deviceKey)
            if (_selectedBluetoothDeviceKey.value == deviceKey) {
                _selectedBluetoothDeviceKey.value = "BT_DEFAULT"
                loadProfileForCurrentSelection()
            }
        }
    }

    fun syncToActiveDevice() {
        val active = activeOutputDevice.value
        _selectedDeviceTab.value = active.type
        if (active.type == AudioOutputDeviceType.BLUETOOTH) {
            _selectedBluetoothDeviceKey.value = active.deviceKey
        }
        viewModelScope.launch {
            loadProfileForCurrentSelection()
        }
    }

    private suspend fun loadProfileForCurrentSelection() {
        val type = _selectedDeviceTab.value
        val (key, name) = resolveKeyAndName(type)
        val profile = equalizerRepository.getProfileSync(key, name)
        _currentProfile.value = profile
        syncToAudioProcessorIfActive(profile)
    }

    private fun resolveKeyAndName(type: AudioOutputDeviceType): Pair<String, String> {
        return if (type == AudioOutputDeviceType.BLUETOOTH) {
            val btKey = _selectedBluetoothDeviceKey.value ?: "BT_DEFAULT"
            val known = knownBluetoothDevices.value.firstOrNull { it.deviceKey == btKey }
            val name = known?.displayName ?: (if (btKey == "BT_DEFAULT") "Default Bluetooth Profile" else btKey.removePrefix("BT_").replace("_", " "))
            Pair(btKey, name)
        } else {
            Pair(deviceKeyForType(type), type.displayName)
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        val updated = _currentProfile.value.copy(isEnabled = enabled)
        updateAndSave(updated)
    }

    fun selectPreset(presetName: String) {
        val presetBands = EqualizerPresets.PRESET_MAP[presetName] ?: return
        val updated = _currentProfile.value.copy(
            presetName = presetName,
            bandLevels = presetBands
        )
        updateAndSave(updated)
    }

    fun setBandLevel(bandIndex: Int, gainDb: Float) {
        val currentLevels = _currentProfile.value.bandLevels.toMutableList()
        if (bandIndex in currentLevels.indices) {
            currentLevels[bandIndex] = gainDb.coerceIn(-15f, 15f)
            val updated = _currentProfile.value.copy(
                presetName = "Custom",
                bandLevels = currentLevels
            )
            updateAndSave(updated)
        }
    }

    fun setBassBoost(percent: Int) {
        val updated = _currentProfile.value.copy(bassBoost = percent.coerceIn(0, 100))
        updateAndSave(updated)
    }

    fun setVirtualizer(percent: Int) {
        val updated = _currentProfile.value.copy(virtualizer = percent.coerceIn(0, 100))
        updateAndSave(updated)
    }

    fun setPreampGain(gainDb: Float) {
        val updated = _currentProfile.value.copy(preampGainDb = gainDb.coerceIn(-15f, 15f))
        updateAndSave(updated)
    }

    fun resetCurrentProfile() {
        val type = _selectedDeviceTab.value
        val (key, name) = resolveKeyAndName(type)
        val default = EqualizerPresets.defaultProfile(key, name)
        updateAndSave(default)
    }

    fun toggleAutoSwitch(enabled: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setAutoSwitchEnabled(enabled)
        }
    }

    private fun updateAndSave(profile: DeviceEqualizerProfile) {
        _currentProfile.value = profile
        syncToAudioProcessorIfActive(profile)
        viewModelScope.launch {
            equalizerRepository.saveProfile(profile)
        }
    }

    private fun syncToAudioProcessorIfActive(profile: DeviceEqualizerProfile) {
        val active = activeOutputDevice.value
        val isActiveMatching = if (active.type == AudioOutputDeviceType.BLUETOOTH) {
            profile.deviceKey == active.deviceKey || (profile.deviceKey == "BT_DEFAULT" && active.deviceKey == "BT_DEFAULT")
        } else {
            profile.deviceKey == deviceKeyForType(active.type)
        }
        if (isActiveMatching) {
            equalizerAudioProcessor.updateParameters(
                enabled = profile.isEnabled,
                bandGains = profile.bandLevels,
                preampDb = profile.preampGainDb,
                bassBoostPercent = profile.bassBoost,
                virtualizerPercent = profile.virtualizer
            )
        }
    }

    private fun deviceKeyForType(type: AudioOutputDeviceType): String {
        return when (type) {
            AudioOutputDeviceType.SPEAKER -> "SPEAKER"
            AudioOutputDeviceType.WIRED_HEADSET -> "WIRED_HEADSET"
            AudioOutputDeviceType.BLUETOOTH -> "BLUETOOTH"
            AudioOutputDeviceType.USB_DAC -> "USB_DAC"
            AudioOutputDeviceType.OTHER -> "OTHER"
        }
    }
}
