package com.example.looper_android.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val TAG = "CrashHandler"
    private const val CRASH_FILE_NAME = "crash_log.txt"
    private const val PREFS_NAME = "looper_crash_prefs"
    private const val KEY_LAST_CRASH_TIME = "last_crash_timestamp"
    private const val KEY_LAST_CRASH_SUMMARY = "last_crash_summary"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (currentHandler != this) {
            defaultHandler = currentHandler
            Thread.setDefaultUncaughtExceptionHandler(this)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            appContext?.let { ctx ->
                saveCrashLog(ctx, thread, throwable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val dateString = dateFormat.format(Date(now))

        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        }.getOrNull()

        val versionName = packageInfo?.versionName ?: "1.0.0-beta1"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 1L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 1L
        }

        val runtime = Runtime.getRuntime()
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMb = totalMemoryMb - freeMemoryMb

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val report = buildString {
            appendLine("==================================================")
            appendLine("LOOPER CRASH REPORT")
            appendLine("==================================================")
            appendLine("Timestamp:     $dateString")
            appendLine("App Version:   $versionName (code $versionCode)")
            appendLine("Package:       ${context.packageName}")
            appendLine("Device:        ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
            appendLine("Android OS:    Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Architecture:  ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Memory:        Used: ${usedMemoryMb}MB / Total: ${totalMemoryMb}MB (Max: ${maxMemoryMb}MB)")
            @Suppress("DEPRECATION")
            val threadId = thread.id
            appendLine("Thread:        ${thread.name} (ID: $threadId, Priority: ${thread.priority})")
            appendLine("Exception:     ${throwable.javaClass.name}: ${throwable.localizedMessage}")
            appendLine("--------------------------------------------------")
            appendLine("STACK TRACE:")
            appendLine(stackTrace)
            appendLine("==================================================")
        }

        val crashFile = File(context.filesDir, CRASH_FILE_NAME)
        FileOutputStream(crashFile, false).use { fos ->
            fos.write(report.toByteArray(Charsets.UTF_8))
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_CRASH_TIME, now)
            .putString(KEY_LAST_CRASH_SUMMARY, "${throwable.javaClass.simpleName}: ${throwable.message ?: "Unknown error"}")
            .commit()
    }

    fun hasCrashLog(context: Context): Boolean {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return file.exists() && file.length() > 0
    }

    fun getLastCrashLog(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists() && file.length() > 0) {
            runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
        } else null
    }

    fun getLastCrashDate(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_LAST_CRASH_TIME, 0L)
        if (timestamp <= 0L) return null
        val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun clearCrashLog(context: Context) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }
}
