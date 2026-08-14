package com.example.looper_android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Play
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.User
import com.composables.icons.lucide.X
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.repository.GenreData
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.library.SongTile
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun SearchScreen(
    searchContext: String = "global",
    viewModel: SearchViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onGenreClick: (String) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()

    LaunchedEffect(searchContext) {
        viewModel.updateSearchContext(searchContext)
    }

    var textFieldValue by remember { 
        androidx.compose.runtime.mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(query, androidx.compose.ui.text.TextRange(query.length))
        ) 
    }
    
    var showAllSongs by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedSongForOptions by remember { androidx.compose.runtime.mutableStateOf<SongEntity?>(null) }
    val songEditViewModel: com.example.looper_android.ui.player.SongEditViewModel = hiltViewModel()

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = androidx.compose.ui.text.input.TextFieldValue(query, androidx.compose.ui.text.TextRange(query.length))
        }
        showAllSongs = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                
        ) {
        // Top Header
        val title = if (searchContext == "global" || searchContext == "songs") "Search songs" else "Search library"
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(
                width = 44.dp,
                height = 44.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )
        }
        
        // Search Bar
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = textFieldValue,
                onValueChange = { 
                    textFieldValue = it
                    viewModel.updateQuery(it.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = DmSansFontFamily
                ),
                cursorBrush = SolidColor(Color(0xFF55DF69)),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "Search library...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                    innerTextField()
                }
            )
            
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Clear",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { viewModel.updateQuery("") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Results
        if (query.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search your library",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        } else if (results.topResult == null && results.songs.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty() && results.genres.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "No results found",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 32.dp),
                    fontFamily = DmSansFontFamily
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
            ) {
                // Top Result
                results.topResult?.let { topResult ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = "Top Result",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = DmSansFontFamily,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            when (topResult) {
                                is SongEntity -> {
                                    TopSongCard(song = topResult, isPlaying = playbackState.currentSong?.id == topResult.id, onOptionsClick = { selectedSongForOptions = topResult }) {
                                        playbackViewModel.setPlaylist(listOf(topResult), queueName = "Search Results")
                                    }
                                }
                                is AlbumEntity -> {
                                    TopAlbumCard(album = topResult, onClick = { onAlbumClick(topResult.name) })
                                }
                                is ArtistEntity -> {
                                    TopArtistCard(artist = topResult, onClick = { onArtistClick(topResult.name) })
                                }
                                is GenreData -> {
                                    TopGenreCard(genre = topResult, onClick = { onGenreClick(topResult.name) })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Songs (if context is global, songs, or library)
                if ((searchContext == "global" || searchContext == "songs" || searchContext == "library") && results.songs.isNotEmpty()) {
                    val displaySongs = if (showAllSongs) results.songs else results.songs.take(5)
                    items(displaySongs, key = { it.id }) { song ->
                        SongTile(
                            song = song,
                            isCurrent = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                            onClick = { playbackViewModel.setPlaylist(listOf(song), queueName = "Search Results") },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                    if (!showAllSongs && results.songs.size > 5) {
                        item {
                            androidx.compose.material3.TextButton(
                                onClick = { showAllSongs = true },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                androidx.compose.material3.Text("Show more", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
                
                // Albums
                if (results.albums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = DmSansFontFamily,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(results.albums) { album ->
                                AlbumResultCard(album = album, onClick = { onAlbumClick(album.name) })
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // Songs (if context is albums, show songs after albums)
                if (searchContext == "albums" && results.songs.isNotEmpty()) {
                    val displaySongs = if (showAllSongs) results.songs else results.songs.take(5)
                    items(displaySongs, key = { it.id }) { song ->
                        SongTile(
                            song = song,
                            isCurrent = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                            onClick = { playbackViewModel.setPlaylist(listOf(song), queueName = "Search Results") },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                    if (!showAllSongs && results.songs.size > 5) {
                        item {
                            androidx.compose.material3.TextButton(
                                onClick = { showAllSongs = true },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                androidx.compose.material3.Text("Show more", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }

                // Artists
                if (results.artists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = DmSansFontFamily,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(results.artists) { artist ->
                                ArtistResultCard(artist = artist, onClick = { onArtistClick(artist.name) })
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Songs (if context is artists, show songs after artists)
                if (searchContext == "artists" && results.songs.isNotEmpty()) {
                    val displaySongs = if (showAllSongs) results.songs else results.songs.take(5)
                    items(displaySongs, key = { it.id }) { song ->
                        SongTile(
                            song = song,
                            isCurrent = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                            onClick = { playbackViewModel.setPlaylist(listOf(song), queueName = "Search Results") },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                    if (!showAllSongs && results.songs.size > 5) {
                        item {
                            androidx.compose.material3.TextButton(
                                onClick = { showAllSongs = true },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                androidx.compose.material3.Text("Show more", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }

                // Genres
                if (results.genres.isNotEmpty()) {
                    item {
                        Text(
                            text = "Genres",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = DmSansFontFamily,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(results.genres) { genre ->
                                GenreResultCard(genre = genre, onClick = { onGenreClick(genre.name) })
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Songs (if context is genres, show songs after genres)
                if (searchContext == "genres" && results.songs.isNotEmpty()) {
                    val displaySongs = if (showAllSongs) results.songs else results.songs.take(5)
                    items(displaySongs, key = { it.id }) { song ->
                        SongTile(
                            song = song,
                            isCurrent = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                            onClick = { playbackViewModel.setPlaylist(listOf(song), queueName = "Search Results") },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                    if (!showAllSongs && results.songs.size > 5) {
                        item {
                            androidx.compose.material3.TextButton(
                                onClick = { showAllSongs = true },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                androidx.compose.material3.Text("Show more", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
        }
        }
        }
        
        selectedSongForOptions?.let { song ->
            com.example.looper_android.ui.player.SongOptionsSheet(
                song = song,
                accentColor = playbackState.accentColor,
                onDismiss = { selectedSongForOptions = null },
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick
            )
        }
    }
}

@Composable
fun TopSongCard(song: SongEntity, isPlaying: Boolean, onOptionsClick: () -> Unit, onClick: () -> Unit) {
    val accentColor = com.example.looper_android.ui.theme.LocalAccentColor.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPlaying) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OptimizedImage(
                url = song.artPath,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.album ?: "Unknown Album",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.material3.IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TopAlbumCard(album: AlbumEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OptimizedImage(
                url = album.artPath,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = album.artist ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Album",
                    color = Color(0xFF55DF69),
                    fontSize = 12.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TopArtistCard(artist: ArtistEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (artist.artPath != null) {
                    OptimizedImage(
                        url = artist.artPath,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(Lucide.User, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Artist",
                    color = Color(0xFF55DF69),
                    fontSize = 12.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TopGenreCard(genre: GenreData, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF55DF69).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.Music, contentDescription = null, tint = Color(0xFF55DF69), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = genre.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Genre",
                    color = Color(0xFF55DF69),
                    fontSize = 12.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AlbumResultCard(album: AlbumEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        OptimizedImage(
            url = album.artPath,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = DmSansFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist ?: "Unknown Artist",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontFamily = DmSansFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistResultCard(artist: ArtistEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artist.artPath != null) {
                OptimizedImage(
                    url = artist.artPath,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(Lucide.User, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = DmSansFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GenreResultCard(genre: GenreData, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF55DF69).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Music, contentDescription = null, tint = Color(0xFF55DF69), modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = genre.name,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = DmSansFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
