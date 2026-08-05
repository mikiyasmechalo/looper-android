import re

with open('app/src/main/java/com/example/looper_android/ui/search/SearchScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix TopSongCard invocation
content = content.replace(
    'TopSongCard(song = topResult, isPlaying = playbackState.currentSong?.id == topResult.id) {',
    'TopSongCard(song = topResult, isPlaying = playbackState.currentSong?.id == topResult.id, onOptionsClick = { selectedSongForOptions = topResult }) {'
)

# Fix onOptionsClick inside SongTile for all contexts
content = content.replace(
    'onOptionsClick = { /* Show options */ }',
    'onOptionsClick = { selectedSongForOptions = song }'
)
content = content.replace(
    'onOptionsClick = {}',
    'onOptionsClick = { selectedSongForOptions = song }'
)

# Apply showAllSongs limit logic to the 4 lists
def replace_list(match):
    prefix = match.group(1)
    return prefix + '''
                    val displaySongs = if (showAllSongs) results.songs else results.songs.take(5)
                    items(displaySongs, key = { it.id }) { song ->'''

# The regex matches the items call and captures everything before it
content = re.sub(
    r'(if \(.*?&& results\.songs\.isNotEmpty\(\)\) \{)\s*items\(results\.songs,\s*key\s*=\s*\{\s*it\.id\s*\}\)\s*\{\s*song\s*->',
    replace_list,
    content,
    flags=re.DOTALL
)

# Now inject the show more button right after the items closure
def replace_button(match):
    return match.group(1) + '''
                    if (!showAllSongs && results.songs.size > 5) {
                        item {
                            androidx.compose.material3.TextButton(
                                onClick = { showAllSongs = true },
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                androidx.compose.material3.Text("Show more", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }'''

# Match the closing brace of the items block by searching for 'onOptionsClick = { selectedSongForOptions = song }' and the next two closing braces
content = re.sub(
    r'(onOptionsClick = \{ selectedSongForOptions = song \}\s*\n\s*\)\s*\n\s*\})',
    replace_button,
    content
)

with open('app/src/main/java/com/example/looper_android/ui/search/SearchScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
