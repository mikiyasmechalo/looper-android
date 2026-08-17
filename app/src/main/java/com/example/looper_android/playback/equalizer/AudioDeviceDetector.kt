package com.example.looper_android.playback.equalizer

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class AudioOutputDeviceType(val displayName: String) {
    SPEAKER("Phone Speaker"),
    WIRED_HEADSET("Wired Headphone / AUX"),
    BLUETOOTH("Bluetooth Audio"),
    USB_DAC("USB Audio / DAC"),
    OTHER("Other Audio Output")
}

data class AudioOutputDevice(
    val type: AudioOutputDeviceType,
    val deviceKey: String, // e.g. "SPEAKER", "WIRED_HEADSET", "BLUETOOTH", "USB_DAC"
    val displayName: String,
    val isConnected: Boolean = true
)

@Singleton
class AudioDeviceDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _currentOutputDevice = MutableStateFlow(detectCurrentDevice())
    val currentOutputDevice: StateFlow<AudioOutputDevice> = _currentOutputDevice.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refresh()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(receiver, filter)
    }

    fun refresh() {
        scope.launch {
            _currentOutputDevice.value = detectCurrentDevice()
        }
    }

    private fun detectCurrentDevice(): AudioOutputDevice {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            // 1. Check for Bluetooth Output
            for (d in devices) {
                if (d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    d.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && d.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && d.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                ) {
                    val rawName = d.productName.toString().trim()
                    val name = if (rawName.isNotBlank() && rawName.lowercase() != "bluetooth" && rawName.lowercase() != "bluetooth audio") rawName else "Bluetooth Audio"
                    val key = if (name != "Bluetooth Audio") "BT_${name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}" else "BT_DEFAULT"
                    return AudioOutputDevice(
                        type = AudioOutputDeviceType.BLUETOOTH,
                        deviceKey = key,
                        displayName = name
                    )
                }
            }

            // 2. Check for Wired Headset / Headphones
            for (d in devices) {
                if (d.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    d.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    d.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                    d.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                    d.type == AudioDeviceInfo.TYPE_AUX_LINE
                ) {
                    return AudioOutputDevice(
                        type = AudioOutputDeviceType.WIRED_HEADSET,
                        deviceKey = "WIRED_HEADSET",
                        displayName = "Wired Headset / AUX"
                    )
                }
            }

            // 3. Check for USB Audio / DAC
            for (d in devices) {
                if (d.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    d.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    d.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                ) {
                    val name = if (d.productName.isNotBlank()) d.productName.toString() else "USB Audio / DAC"
                    return AudioOutputDevice(
                        type = AudioOutputDeviceType.USB_DAC,
                        deviceKey = "USB_DAC",
                        displayName = name
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
                return AudioOutputDevice(
                    type = AudioOutputDeviceType.BLUETOOTH,
                    deviceKey = "BLUETOOTH",
                    displayName = "Bluetooth Audio"
                )
            }
            @Suppress("DEPRECATION")
            if (audioManager.isWiredHeadsetOn) {
                return AudioOutputDevice(
                    type = AudioOutputDeviceType.WIRED_HEADSET,
                    deviceKey = "WIRED_HEADSET",
                    displayName = "Wired Headset"
                )
            }
        }

        // Default: Built-in Speaker
        return AudioOutputDevice(
            type = AudioOutputDeviceType.SPEAKER,
            deviceKey = "SPEAKER",
            displayName = "Phone Speaker"
        )
    }
}
