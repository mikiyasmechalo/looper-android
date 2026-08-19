package com.example.looper_android.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.icons.lucide.X
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.ArtistCollage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditArtistBottomSheet(
    artistName: String,
    currentImageUrl: String?,
    songs: List<SongEntity>,
    isSaving: Boolean = false,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    onDismiss: () -> Unit,
    onSave: (
        newArtistName: String,
        pickedImageUri: Uri?,
        removeImage: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(artistName) { mutableStateOf(artistName) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var removeImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri
            removeImage = false
        }
    }

    val displayArt = when {
        removeImage -> null
        pickedImageUri != null -> pickedImageUri.toString()
        else -> currentImageUrl
    }

    val sheetBg = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Artist",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                    Text(
                        text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"} will be updated",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Lucide.X, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Artist Image Circle Container
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                ArtistCollage(
                    artistImageUrl = displayArt,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                )

                // Edit badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(2.dp, sheetBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Camera,
                        contentDescription = "Pick Image",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!displayArt.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        pickedImageUri = null
                        removeImage = true
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = null,
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remove image",
                        color = Color.Red.copy(alpha = 0.8f),
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Artist Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Artist Name", fontFamily = DmSansFontFamily) },
                leadingIcon = {
                    Icon(Lucide.User, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = accentColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Action Button
            PremiumSection(
                height = 52.dp,
                backgroundColor = accentColor,
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    if (!isSaving && name.isNotBlank()) {
                        onSave(name.trim(), pickedImageUri, removeImage)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Lucide.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Changes",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
