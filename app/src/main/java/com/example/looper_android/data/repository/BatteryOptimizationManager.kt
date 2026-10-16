package com.example.looper_android.data.repository

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationManager {
    private const val TAG = "BatteryOptManager"
    private const val PREFS_NAME = "playback_kill_tracker"

    private const val KEY_WAS_PLAYING = "was_playing_active"
    private const val KEY_CLEAN_EXIT = "clean_exit_recorded"
    private const val KEY_LAST_HEARTBEAT = "last_heartbeat_timestamp"
    private const val KEY_DONT_SHOW_AGAIN = "battery_dialog_dont_show_again"
    private const val KEY_LAST_PROMPT_TIME = "last_battery_prompt_time"

    /**
     * Checks if the app is already exempt from battery optimization (running unrestricted).
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }
        return true
    }

    /**
     * Launches the system prompt or settings page to request battery optimization exemption.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Direct ignore battery prompt failed, opening system settings", e)
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {
                    try {
                        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(appDetailsIntent)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Called when music starts or resumes playback.
     */
    fun onPlaybackStarted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_WAS_PLAYING, true)
            .putBoolean(KEY_CLEAN_EXIT, false)
            .putLong(KEY_LAST_HEARTBEAT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Periodic heartbeat while music is actively playing.
     */
    fun onPlaybackHeartbeat(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_WAS_PLAYING, false)) {
            prefs.edit()
                .putLong(KEY_LAST_HEARTBEAT, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * Called when playback is explicitly paused, stopped, or ends naturally.
     */
    fun onPlaybackStopped(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_WAS_PLAYING, false)
            .putBoolean(KEY_CLEAN_EXIT, true)
            .apply()
    }

    /**
     * Called during graceful service shutdown (e.g. notification close action or user task removal).
     */
    fun onServiceGracefulShutdown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_WAS_PLAYING, false)
            .putBoolean(KEY_CLEAN_EXIT, true)
            .commit()
    }

    /**
     * Checks if a REAL unexpected background force-close occurred while music was playing.
     * Guaranteed NOT to prompt if:
     * - Battery optimizations are already disabled (app is unrestricted).
     * - The user requested "Don't show again".
     * - The exit was caused by a standard app crash (stack trace captured by CrashHandler).
     * - Prompted within the last 24 hours.
     */
    fun checkUnexpectedKill(context: Context): Boolean {
        // 1. If already unrestricted, battery optimization is not restricting Looper
        if (isIgnoringBatteryOptimizations(context)) {
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 2. If user selected "Don't ask again"
        if (prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false)) {
            return false
        }

        // 3. Cooldown check (max once per 24 hours)
        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
        if (now - lastPrompt < 24 * 60 * 60 * 1000L) {
            return false
        }

        val wasPlaying = prefs.getBoolean(KEY_WAS_PLAYING, false)
        val cleanExit = prefs.getBoolean(KEY_CLEAN_EXIT, true)
        val lastHeartbeat = prefs.getLong(KEY_LAST_HEARTBEAT, 0L)

        // Must have been actively playing within the last 48 hours without clean exit
        if (!wasPlaying || cleanExit || (now - lastHeartbeat) > 48 * 60 * 60 * 1000L) {
            return false
        }

        // 4. On Android 11+ (API 30+), inspect OS exit reasons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val exitInfos = am?.getHistoricalProcessExitReasons(context.packageName, 0, 1)
            val lastExit = exitInfos?.firstOrNull()
            if (lastExit != null) {
                // If it was an unhandled exception crash, CrashHandler handles it
                if (lastExit.reason == ApplicationExitInfo.REASON_CRASH ||
                    lastExit.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Consumes the unexpected kill event immediately so it never prompts again for this occurrence.
     */
    fun consumeKillEvent(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_WAS_PLAYING, false)
            .putBoolean(KEY_CLEAN_EXIT, true)
            .putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
            .apply()
    }

    fun setDontShowAgain(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DONT_SHOW_AGAIN, value)
            .apply()
    }

    fun isDontShowAgain(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false)
    }
}
