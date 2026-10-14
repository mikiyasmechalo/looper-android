package com.example.looper_android.data.lyrics.providers

data class LyricsProviderInfo(
    val id: String,
    val label: String,
    val url: String,
    val wordCapable: Boolean = false,
    val letterCapable: Boolean = false,
    val plainOnly: Boolean = false
) {
    val bestLevel: Int
        get() = when {
            letterCapable -> 4
            wordCapable -> 3
            plainOnly -> 1
            else -> 2
        }

    companion object {
        val ALL = listOf(
            LyricsProviderInfo(
                id = "LRC.RED",
                label = "lrc.red",
                url = "https://lrc.red",
                wordCapable = true,
                letterCapable = true
            ),
            LyricsProviderInfo(
                id = "LYRICSPLUS",
                label = "LyricsPlus",
                url = "https://lyricsplus.binimum.org",
                wordCapable = true,
                letterCapable = true
            ),
            LyricsProviderInfo(
                id = "AMLL",
                label = "AMLL",
                url = "https://amll.dev",
                wordCapable = true,
                letterCapable = true
            ),
            LyricsProviderInfo(
                id = "NETEASE",
                label = "NetEase",
                url = "https://music.163.com",
                wordCapable = true
            ),
            LyricsProviderInfo(
                id = "KUGOU",
                label = "KuGou",
                url = "https://www.kugou.com",
                wordCapable = true
            ),
            LyricsProviderInfo(
                id = "MUSIXMATCH",
                label = "Musixmatch",
                url = "https://www.musixmatch.com",
                wordCapable = true
            ),
            LyricsProviderInfo(
                id = "LYRICIFY",
                label = "Lyricify",
                url = "https://lyricify.app",
                wordCapable = true,
                letterCapable = true
            ),
            LyricsProviderInfo(
                id = "LRCLIB",
                label = "LRCLIB",
                url = "https://lrclib.net"
            ),
            LyricsProviderInfo(
                id = "GENIUS",
                label = "Genius",
                url = "https://genius.com",
                plainOnly = true
            ),
            LyricsProviderInfo(
                id = "AZLYRICS",
                label = "AZLyrics",
                url = "https://www.azlyrics.com",
                plainOnly = true
            ),
            LyricsProviderInfo(
                id = "LYRICSMINT",
                label = "LyricsMINT",
                url = "https://www.lyricsmint.com",
                plainOnly = true
            ),
            LyricsProviderInfo(
                id = "LYRICFIND",
                label = "LyricFind",
                url = "https://www.lyricfind.com",
                plainOnly = true
            )
        )

        fun of(name: String?): LyricsProviderInfo? {
            if (name == null) return null
            val upper = name.uppercase()
            return ALL.firstOrNull { it.id == upper || it.label.uppercase() == upper }
        }

        fun order(stored: List<String>, legacyPrimary: String? = null): List<String> {
            val known = ALL.map { it.id }
            val result = stored.map { it.uppercase() }.filter { known.contains(it) }.toMutableList()

            if (result.isEmpty()) {
                val primary = legacyPrimary?.uppercase()
                return if (primary != null && known.contains(primary)) {
                    listOf(primary) + known.filter { it != primary }
                } else {
                    known
                }
            }

            for (i in known.indices) {
                val candidate = known[i]
                if (result.contains(candidate)) continue
                var at = 0
                for (j in i - 1 downTo 0) {
                    val before = result.indexOf(known[j])
                    if (before != -1) {
                        at = before + 1
                        break
                    }
                }
                result.add(at, candidate)
            }

            return result.distinct()
        }
    }
}
