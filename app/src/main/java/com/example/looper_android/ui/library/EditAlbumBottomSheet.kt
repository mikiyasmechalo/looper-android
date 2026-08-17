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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlbumBottomSheet(
    albumName: String,
    songs: List<SongEntity>,
    isSaving: Boolean = false,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    onDismiss: () -> Unit,
    onSave: (
        newAlbumName: String,
        newAlbumArtist: String?,
        newYear: Int?,
        newGenre: String?,
        pickedArtUri: Uri?,
        removeArtwork: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val firstSong = songs.firstOrNull()
    var name by remember(albumName) { mutableStateOf(albumName) }
    var artist by remember(firstSong) {
        mutableStateOf(firstSong?.albumArtist ?: firstSong?.artist ?: "")
    }
    var yearStr by remember(firstSong) {
        mutableStateOf(firstSong?.year?.takeIf { it > 0 }?.toString() ?: "")
    }
    var genre by remember(firstSong) {
        mutableStateOf(firstSong?.genre ?: "")
    }

    var currentArtPath by remember(songs) {
        mutableStateOf(songs.firstOrNull { !it.artPath.isNullOrEmpty() }?.artPath)
    }
    var pickedArtUri by remember { mutableStateOf<Uri?>(null) }
    var removeArtwork by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedArtUri = uri
            removeArtwork = false
        }
    }

    val sheetBg = MaterialTheme.colorScheme.surfaceVariant
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumSection(
                        width = 40.dp,
                        height = 40.dp,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        onClick = onDismiss
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = "Edit Album Tags",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                        Text(
                            text = "Applies to all ${songs.size} tracks",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 12.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }

                PremiumSection(
                    width = 40.dp,
                    height = 40.dp,
                    backgroundColor = accentColor,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isSaving && name.isNotBlank()) {
                            val parsedYear = yearStr.trim().toIntOrNull()
                            onSave(
                                name.trim(),
                                artist.trim().ifBlank { null },
                                parsedYear,
                                genre.trim().ifBlank { null },
                                pickedArtUri,
                                removeArtwork
                            )
                        }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Lucide.Check, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Artwork Section
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val hasArt = !removeArtwork && (pickedArtUri != null || !currentArtPath.isNullOrEmpty())
                    if (hasArt) {
                        OptimizedImage(
                            url = pickedArtUri?.toString() ?: currentArtPath,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Lucide.Disc, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Choose Cover", color = Color.White.copy(alpha = 0.40f), fontFamily = DmSansFontFamily, fontSize = 11.sp)
                        }
                    }

                    // Camera Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Camera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }

                if (!removeArtwork && (pickedArtUri != null || !currentArtPath.isNullOrEmpty())) {
                    TextButton(
                        onClick = {
                            removeArtwork = true
                            pickedArtUri = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Lucide.X, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove album cover", color = Color(0xFFFF5252), fontSize = 13.sp, fontFamily = DmSansFontFamily)
                    }
                }

                // 2. Common Tag Fields
                AlbumTagField(
                    label = "Album Title",
                    value = name,
                    onValueChange = { name = it },
                    accentColor = accentColor,
                    isSingleLine = true
                )

                AlbumTagField(
                    label = "Album Artist",
                    value = artist,
                    onValueChange = { artist = it },
                    accentColor = accentColor,
                    isSingleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AlbumTagField(
                        label = "Year",
                        value = yearStr,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) yearStr = it },
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        isSingleLine = true
                    )

                    AlbumTagField(
                        label = "Genre",
                        value = genre,
                        onValueChange = { genre = it },
                        accentColor = accentColor,
                        modifier = Modifier.weight(1.5f),
                        isSingleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AlbumTagField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSingleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DmSansFontFamily,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = isSingleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = accentColor
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = KeyboardCapitalization.Words
            )
        )
    }
}
