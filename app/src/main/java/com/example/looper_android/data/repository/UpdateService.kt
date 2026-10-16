package com.example.looper_android.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.looper_android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val tagName: String,
    val releaseName: String,
    val htmlUrl: String,
    val body: String,
    val apkDownloadUrl: String?,
    val isPrerelease: Boolean
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val updateInfo: UpdateInfo, val isSkipped: Boolean = false) : UpdateStatus()
    data class UpToDate(val currentVersion: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

@Singleton
class UpdateService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "UpdateService"
        const val GITHUB_OWNER = "mikiyasmechalo"
        const val GITHUB_REPO = "looper-android"
        const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
        const val RELEASES_HTML_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _updateAvailable = MutableStateFlow<UpdateInfo?>(null)
    val updateAvailable: StateFlow<UpdateInfo?> = _updateAvailable.asStateFlow()

    val skippedUpdateVersion: Flow<String?> = settingsRepository.skippedUpdateVersion

    private var hasCheckedOnLaunch = false

    suspend fun checkForUpdates(manual: Boolean = false): UpdateStatus = withContext(Dispatchers.IO) {
        if (!manual && hasCheckedOnLaunch) {
            return@withContext _updateStatus.value
        }
        hasCheckedOnLaunch = true

        val currentVersion = BuildConfig.VERSION_NAME

        // If automated check, respect snooze period (e.g. "show again after a week")
        if (!manual) {
            val remindAfter = settingsRepository.remindUpdateAfterTimestamp.firstOrNull() ?: 0L
            if (System.currentTimeMillis() < remindAfter) {
                Log.d(TAG, "Update check snoozed until timestamp $remindAfter")
                return@withContext UpdateStatus.Idle
            }
        }

        _updateStatus.value = UpdateStatus.Checking

        try {
            val request = Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Looper-Android-App/$currentVersion")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = if (response.code == 404) {
                    "No releases found yet on GitHub."
                } else {
                    "GitHub check failed (HTTP ${response.code})"
                }
                val status = if (response.code == 404) {
                    UpdateStatus.UpToDate(currentVersion)
                } else {
                    UpdateStatus.Error(errorMsg)
                }
                _updateStatus.value = status
                return@withContext status
            }

            val bodyStr = response.body?.string()
            if (bodyStr.isNullOrBlank()) {
                val status = UpdateStatus.UpToDate(currentVersion)
                _updateStatus.value = status
                return@withContext status
            }

            val releasesJson = JSONArray(bodyStr)
            if (releasesJson.length() == 0) {
                val status = UpdateStatus.UpToDate(currentVersion)
                _updateStatus.value = status
                return@withContext status
            }

            var targetUpdate: UpdateInfo? = null
            for (i in 0 until releasesJson.length()) {
                val releaseObj = releasesJson.getJSONObject(i)
                val isDraft = releaseObj.optBoolean("draft", false)
                if (isDraft) continue

                val tagName = releaseObj.optString("tag_name", "")
                val releaseName = releaseObj.optString("name", tagName)
                val htmlUrl = releaseObj.optString("html_url", RELEASES_HTML_URL)
                val body = releaseObj.optString("body", "")
                val isPrerelease = releaseObj.optBoolean("prerelease", false)

                var apkUrl: String? = null
                val assets = releaseObj.optJSONArray("assets")
                if (assets != null) {
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                            break
                        }
                    }
                }

                if (tagName.isNotBlank() && isUpdateAvailable(currentVersion, tagName)) {
                    targetUpdate = UpdateInfo(
                        tagName = tagName,
                        releaseName = releaseName,
                        htmlUrl = htmlUrl,
                        body = body,
                        apkDownloadUrl = apkUrl,
                        isPrerelease = isPrerelease
                    )
                    break
                }
            }

            val skippedVersion = settingsRepository.skippedUpdateVersion.firstOrNull()
            val isSkipped = targetUpdate != null && targetUpdate.tagName.equals(skippedVersion, ignoreCase = true)

            val status = if (targetUpdate != null) {
                if (!manual && isSkipped) {
                    Log.d(TAG, "Update ${targetUpdate.tagName} was skipped by user, suppressing prompt")
                    _updateAvailable.value = null
                    UpdateStatus.Idle
                } else {
                    if (!isSkipped) {
                        _updateAvailable.value = targetUpdate
                    }
                    UpdateStatus.Available(targetUpdate, isSkipped = isSkipped)
                }
            } else {
                UpdateStatus.UpToDate(currentVersion)
            }

            _updateStatus.value = status
            status
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            val status = UpdateStatus.Error(e.localizedMessage ?: "Network error checking for updates")
            _updateStatus.value = status
            status
        }
    }

    /**
     * Skip this version: stores version tag so auto-checks will never prompt for it again.
     */
    suspend fun skipUpdate(version: String) {
        settingsRepository.setSkippedUpdateVersion(version)
        _updateAvailable.value = null
        val currentStatus = _updateStatus.value
        if (currentStatus is UpdateStatus.Available) {
            _updateStatus.value = currentStatus.copy(isSkipped = true)
        }
    }

    /**
     * Snooze updates for 1 week (7 days).
     */
    suspend fun snoozeOneWeek() {
        val oneWeekMs = 7L * 24 * 60 * 60 * 1000L
        settingsRepository.setRemindUpdateAfterTimestamp(System.currentTimeMillis() + oneWeekMs)
        _updateAvailable.value = null
    }

    /**
     * Reset skipped version and re-check.
     */
    suspend fun resetSkippedUpdate() {
        settingsRepository.resetSkippedUpdate()
        checkForUpdates(manual = true)
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    fun openUrl(url: String) {
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
        }
    }

    /**
     * Semantic version comparison supporting semver and prerelease tags like v1.0.0-beta1
     */
    fun isUpdateAvailable(current: String, latest: String): Boolean {
        try {
            val (cCore, cIsPre, cPreNum) = parseVersion(current)
            val (lCore, lIsPre, lPreNum) = parseVersion(latest)

            for (i in 0 until 3) {
                if (lCore[i] > cCore[i]) return true
                if (lCore[i] < cCore[i]) return false
            }

            // Same core numbers (e.g. 1.0.0 vs 1.0.0):
            // If current is prerelease and latest is full release -> update is available
            if (cIsPre && !lIsPre) return true

            // If both are prereleases: compare prerelease revision number (beta2 > beta1)
            if (cIsPre && lIsPre) {
                return lPreNum > cPreNum
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compare versions: $current vs $latest", e)
            return false
        }
    }

    private data class ParsedVersion(
        val core: List<Int>,
        val isPrerelease: Boolean,
        val prereleaseNum: Int
    )

    private fun parseVersion(raw: String): ParsedVersion {
        val clean = raw.trim().removePrefix("v").removePrefix("V")
        val parts = clean.split("-", limit = 2)
        val coreStr = parts[0]
        val preStr = if (parts.size > 1) parts[1] else ""

        val numRegex = Regex("\\d+")
        val coreNums = numRegex.findAll(coreStr).map { it.value.toIntOrNull() ?: 0 }.toMutableList()
        while (coreNums.size < 3) {
            coreNums.add(0)
        }

        val preNums = numRegex.findAll(preStr).map { it.value.toIntOrNull() ?: 0 }.toList()
        val preNum = if (preNums.isNotEmpty()) preNums[0] else if (preStr.isNotEmpty()) 0 else 999999
        val isPre = preStr.isNotEmpty()

        return ParsedVersion(coreNums, isPre, preNum)
    }
}
