package com.example.looper_android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.looper_android.playback.equalizer.DeviceEqualizerProfile
import com.example.looper_android.playback.equalizer.EqualizerPresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val AUTO_SWITCH_ENABLED = booleanPreferencesKey("eq_auto_switch_enabled")
        val LAST_ACTIVE_DEVICE_KEY = stringPreferencesKey("eq_last_active_device_key")
        val KNOWN_BT_DEVICES = stringSetPreferencesKey("eq_known_bluetooth_devices")
    }

    val isAutoSwitchEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_SWITCH_ENABLED] ?: true
    }

    val knownBluetoothDevices: Flow<List<com.example.looper_android.playback.equalizer.BluetoothDeviceProfileInfo>> = dataStore.data.map { prefs ->
        val rawSet = prefs[KNOWN_BT_DEVICES] ?: emptySet()
        val list = mutableListOf(
            com.example.looper_android.playback.equalizer.BluetoothDeviceProfileInfo(
                deviceKey = "BT_DEFAULT",
                displayName = "Default Bluetooth Profile"
            )
        )
        for (item in rawSet) {
            val parts = item.split("|")
            if (parts.size == 2 && parts[0] != "BT_DEFAULT") {
                list.add(
                    com.example.looper_android.playback.equalizer.BluetoothDeviceProfileInfo(
                        deviceKey = parts[0],
                        displayName = parts[1]
                    )
                )
            }
        }
        list
    }

    suspend fun registerBluetoothDevice(deviceKey: String, displayName: String) {
        if (deviceKey == "BT_DEFAULT" || deviceKey.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[KNOWN_BT_DEVICES]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$deviceKey|") }
            current.add("$deviceKey|$displayName")
            prefs[KNOWN_BT_DEVICES] = current
        }
    }

    suspend fun deleteBluetoothProfile(deviceKey: String) {
        if (deviceKey == "BT_DEFAULT") return
        dataStore.edit { prefs ->
            val current = prefs[KNOWN_BT_DEVICES]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$deviceKey|") }
            prefs[KNOWN_BT_DEVICES] = current

            prefs.remove(booleanPreferencesKey("eq_enabled_$deviceKey"))
            prefs.remove(stringPreferencesKey("eq_preset_$deviceKey"))
            prefs.remove(stringPreferencesKey("eq_bands_$deviceKey"))
            prefs.remove(intPreferencesKey("eq_bass_$deviceKey"))
            prefs.remove(intPreferencesKey("eq_virt_$deviceKey"))
            prefs.remove(floatPreferencesKey("eq_preamp_$deviceKey"))
        }
    }

    suspend fun setAutoSwitchEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_SWITCH_ENABLED] = enabled
        }
    }

    fun getProfile(deviceKey: String, deviceName: String): Flow<DeviceEqualizerProfile> {
        val enabledKey = booleanPreferencesKey("eq_enabled_$deviceKey")
        val presetKey = stringPreferencesKey("eq_preset_$deviceKey")
        val bandsKey = stringPreferencesKey("eq_bands_$deviceKey")
        val bassKey = intPreferencesKey("eq_bass_$deviceKey")
        val virtKey = intPreferencesKey("eq_virt_$deviceKey")
        val preampKey = floatPreferencesKey("eq_preamp_$deviceKey")

        return dataStore.data.map { prefs ->
            val default = EqualizerPresets.defaultProfile(deviceKey, deviceName)

            val isEnabled = prefs[enabledKey] ?: default.isEnabled
            val presetName = prefs[presetKey] ?: default.presetName
            val bandsStr = prefs[bandsKey]
            val bandLevels = if (!bandsStr.isNullOrBlank()) {
                val parsed = bandsStr.split(",").mapNotNull { it.trim().toFloatOrNull() }
                if (parsed.size == 10) parsed else default.bandLevels
            } else {
                default.bandLevels
            }
            val bassBoost = prefs[bassKey] ?: default.bassBoost
            val virtualizer = prefs[virtKey] ?: default.virtualizer
            val preampGainDb = prefs[preampKey] ?: default.preampGainDb

            DeviceEqualizerProfile(
                deviceKey = deviceKey,
                deviceName = deviceName,
                isEnabled = isEnabled,
                presetName = presetName,
                bandLevels = bandLevels,
                bassBoost = bassBoost,
                virtualizer = virtualizer,
                preampGainDb = preampGainDb
            )
        }
    }

    suspend fun getProfileSync(deviceKey: String, deviceName: String): DeviceEqualizerProfile {
        return getProfile(deviceKey, deviceName).first()
    }

    suspend fun saveProfile(profile: DeviceEqualizerProfile) {
        val enabledKey = booleanPreferencesKey("eq_enabled_${profile.deviceKey}")
        val presetKey = stringPreferencesKey("eq_preset_${profile.deviceKey}")
        val bandsKey = stringPreferencesKey("eq_bands_${profile.deviceKey}")
        val bassKey = intPreferencesKey("eq_bass_${profile.deviceKey}")
        val virtKey = intPreferencesKey("eq_virt_${profile.deviceKey}")
        val preampKey = floatPreferencesKey("eq_preamp_${profile.deviceKey}")

        dataStore.edit { prefs ->
            prefs[enabledKey] = profile.isEnabled
            prefs[presetKey] = profile.presetName
            prefs[bandsKey] = profile.bandLevels.joinToString(",") { "%.2f".format(it) }
            prefs[bassKey] = profile.bassBoost
            prefs[virtKey] = profile.virtualizer
            prefs[preampKey] = profile.preampGainDb
        }
    }
}
