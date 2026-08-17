package com.example.looper_android.playback.equalizer

data class DeviceEqualizerProfile(
    val deviceKey: String, // "SPEAKER", "WIRED_HEADSET", "BLUETOOTH", "USB_DAC" or "BT_<name>"
    val deviceName: String,
    val isEnabled: Boolean = false,
    val presetName: String = "Flat",
    val bandLevels: List<Float> = List(10) { 0f }, // 10 ISO bands in dB (-15.0..+15.0)
    val bassBoost: Int = 0,                        // 0..100%
    val virtualizer: Int = 0,                      // 0..100%
    val preampGainDb: Float = 0f                   // -15.0..+15.0 dB
)

data class BluetoothDeviceProfileInfo(
    val deviceKey: String,
    val displayName: String,
    val isConnected: Boolean = false
)

object EqualizerPresets {
    val PRESET_MAP: Map<String, List<Float>> = linkedMapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Bass Booster" to listOf(6f, 5.5f, 4.5f, 3f, 1.5f, 0f, 0f, 0f, 0f, 0f),
        "Bass Reducer" to listOf(-6f, -5.5f, -4f, -2.5f, -1f, 0f, 0f, 0f, 0f, 0f),
        "Treble Booster" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 2.5f, 4f, 5.5f, 6f),
        "Vocal Booster" to listOf(-2f, -1.5f, 0f, 2f, 4f, 4f, 3f, 1.5f, 0f, -1f),
        "Rock" to listOf(4.5f, 3.5f, 2f, 0f, -1f, 0f, 1.5f, 3f, 4f, 4.5f),
        "Pop" to listOf(-1f, 1f, 3f, 3.5f, 2f, 0f, 1f, 2.5f, 3.5f, 2f),
        "Electronic" to listOf(5f, 4.5f, 2f, 0f, -1.5f, 1.5f, 0f, 2f, 4f, 4.5f),
        "Jazz" to listOf(3f, 2f, 1f, 1.5f, -1f, -1f, 0f, 1.5f, 2.5f, 3f),
        "Acoustic" to listOf(3.5f, 2.5f, 1.5f, 1f, 1f, 1.5f, 2.5f, 3.5f, 3f, 2f),
        "Classical" to listOf(4f, 3f, 2f, 1.5f, -1f, -1f, 0f, 2f, 3f, 3.5f),
        "Hip-Hop" to listOf(6f, 5f, 2.5f, 0f, -1f, 0f, 1f, 2f, 3f, 3.5f),
        "Custom" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )

    val PRESET_NAMES = PRESET_MAP.keys.toList()

    fun defaultProfile(deviceKey: String, deviceName: String): DeviceEqualizerProfile {
        return when (deviceKey) {
            "SPEAKER" -> DeviceEqualizerProfile(
                deviceKey = "SPEAKER",
                deviceName = "Phone Speaker",
                isEnabled = false,
                presetName = "Flat",
                bandLevels = PRESET_MAP["Flat"]!!,
                bassBoost = 0,
                virtualizer = 0,
                preampGainDb = 0f
            )
            "WIRED_HEADSET" -> DeviceEqualizerProfile(
                deviceKey = "WIRED_HEADSET",
                deviceName = "Wired Headphone / AUX",
                isEnabled = false,
                presetName = "Flat",
                bandLevels = PRESET_MAP["Flat"]!!,
                bassBoost = 15,
                virtualizer = 10,
                preampGainDb = 0f
            )
            "BLUETOOTH" -> DeviceEqualizerProfile(
                deviceKey = "BLUETOOTH",
                deviceName = "Bluetooth Audio",
                isEnabled = false,
                presetName = "Flat",
                bandLevels = PRESET_MAP["Flat"]!!,
                bassBoost = 15,
                virtualizer = 15,
                preampGainDb = 0f
            )
            "USB_DAC" -> DeviceEqualizerProfile(
                deviceKey = "USB_DAC",
                deviceName = "USB Audio / DAC",
                isEnabled = false,
                presetName = "Flat",
                bandLevels = PRESET_MAP["Flat"]!!,
                bassBoost = 0,
                virtualizer = 0,
                preampGainDb = 0f
            )
            else -> DeviceEqualizerProfile(
                deviceKey = deviceKey,
                deviceName = deviceName,
                isEnabled = false,
                presetName = "Flat",
                bandLevels = PRESET_MAP["Flat"]!!,
                bassBoost = 0,
                virtualizer = 0,
                preampGainDb = 0f
            )
        }
    }
}
