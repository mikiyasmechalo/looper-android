package com.example.looper_android.ui.widgets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun FolderPickerLauncher(
    onFolderPicked: (Uri) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onFolderPicked(uri)
        }
    }
    
    return { launcher.launch(null) }
}
