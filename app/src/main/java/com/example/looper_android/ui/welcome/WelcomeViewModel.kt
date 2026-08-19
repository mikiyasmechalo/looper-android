package com.example.looper_android.ui.welcome

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.looper_android.data.db.LibraryDao
import com.example.looper_android.data.repository.LibraryScanner
import com.example.looper_android.data.repository.SafPermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeUiState(
    val isAudioPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false,
    val isAllFilesPermissionGranted: Boolean = false,
    val grantedFolders: List<String> = emptyList(),
    val isScanning: Boolean = false,
    val scanProgressText: String = "",
    val scanStatusMessage: String = "Ready to scan...",
    val detectedSongsCount: Int = 0,
    val isWelcomeBypassed: Boolean = false
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safPermissionRepo: SafPermissionRepository,
    private val libraryScanner: LibraryScanner,
    private val libraryDao: LibraryDao
) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        checkPermissionsAndAutoScan()
        refreshGrantedFolders()
        val isBypassed = prefs.getBoolean("is_welcome_bypassed", false)
        _uiState.value = _uiState.value.copy(isWelcomeBypassed = isBypassed)
    }

    fun checkPermissionsAndAutoScan() {
        val audioGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        val notifGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val allFilesGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }

        _uiState.value = _uiState.value.copy(
            isAudioPermissionGranted = audioGranted,
            isNotificationPermissionGranted = notifGranted,
            isAllFilesPermissionGranted = allFilesGranted
        )

        // Automatically trigger live scan as soon as audio permission is granted!
        if ((audioGranted || allFilesGranted) && !_uiState.value.isScanning) {
            viewModelScope.launch {
                val count = libraryDao.getSongsCount()
                if (count == 0) {
                    triggerBackgroundScan()
                }
            }
        }
    }

    fun addSafFolder(treeUri: Uri) {
        val saved = safPermissionRepo.saveGrantedTreeUri(treeUri)
        if (saved) {
            refreshGrantedFolders()
        }
    }

    fun removeSafFolder(treeUriString: String) {
        runCatching { Uri.parse(treeUriString) }.getOrNull()?.let {
            safPermissionRepo.removeGrantedTreeUri(it)
            refreshGrantedFolders()
        }
    }

    private fun refreshGrantedFolders() {
        val uris = safPermissionRepo.getGrantedTreeUris()
        val folderDisplayNames = uris.map { uri ->
            safPermissionRepo.getPathFromTreeUri(uri) ?: uri.lastPathSegment ?: uri.toString()
        }
        _uiState.value = _uiState.value.copy(grantedFolders = folderDisplayNames)
    }

    fun triggerBackgroundScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanStatusMessage = "Scanning music files...",
                scanProgressText = ""
            )
            
            // Scan MediaStore only (MediaStore already indexes all local audio tracks)
            runCatching {
                libraryScanner.scanMediaStore { processed, total, phase ->
                    _uiState.value = _uiState.value.copy(
                        scanStatusMessage = phase,
                        scanProgressText = if (total > 0) "$processed/$total songs" else "$processed songs scanned"
                    )
                }
            }

            val count = libraryDao.getSongsCount()
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                detectedSongsCount = count,
                scanProgressText = "",
                scanStatusMessage = if (count > 0) "Scan Complete ($count songs)" else "No songs found"
            )
        }
    }

    fun bypassWelcome(onComplete: () -> Unit) {
        if (_uiState.value.isScanning) return // Do NOT allow entering while scanning!
        prefs.edit { putBoolean("is_welcome_bypassed", true) }
        _uiState.value = _uiState.value.copy(isWelcomeBypassed = true)
        onComplete()
    }
}
