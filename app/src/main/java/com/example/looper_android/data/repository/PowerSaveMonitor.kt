package com.example.looper_android.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PowerSaveMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _batteryPercentage = MutableStateFlow(100)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _isPowerSaveActive = MutableStateFlow(false)
    val isPowerSaveActive: StateFlow<Boolean> = _isPowerSaveActive.asStateFlow()

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent == null) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                if (level >= 0 && scale > 0) {
                    _batteryPercentage.value = (level * 100) / scale
                }
                _isCharging.value = charging
            }
        }
        context.registerReceiver(receiver, filter)

        // Combine settings with battery state
        combine(
            settingsRepository.autoPowerSaveEnabled,
            settingsRepository.autoPowerSaveThreshold,
            _batteryPercentage,
            _isCharging
        ) { enabled: Boolean, threshold: Int, battery: Int, charging: Boolean ->
            if (enabled && !charging && battery <= threshold) {
                _isPowerSaveActive.value = true
            } else {
                _isPowerSaveActive.value = false
            }
        }.launchIn(scope)
    }
}
