package com.example.looper_android.data.lyrics.providers

import kotlin.math.abs

object LyricsQuery {

    private val NOISE_BRACKETS = Regex(
        """\s*[\(\[\{][^\)\]\}]*\b(from|feat|ft|featuring|with|prod|official|video|audio|lyrics?|visuali[sz]er|remaster(ed)?)\b[^\)\]\}]*[\)\]\}]""",
        RegexOption.IGNORE_CASE
    )

    private val NOISE_TAIL = Regex(
        """\s+[-–—|]\s+(from|feat|ft|official|remaster(ed)?|lyrics?)\b.*$""",
        RegexOption.IGNORE_CASE
    )

    private val FEAT_TAIL = Regex(
        """\s+(feat\.?|ft\.?|featuring)\s+.*$""",
        RegexOption.IGNORE_CASE
    )

    private val ARTIST_SEPARATORS = Regex(
        """\s*(?:,|;|/|\\|&|\+|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+|\s+featuring\s+|\s+and\s+)\s*""",
        RegexOption.IGNORE_CASE
    )

    fun cleanTitle(title: String): String {
        val cleaned = title
            .replace(NOISE_BRACKETS, "")
            .replace(NOISE_TAIL, "")
            .replace(FEAT_TAIL, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return if (cleaned.isEmpty()) title.trim() else cleaned
    }

    fun artistVariants(artist: String, max: Int = 3): List<String> {
        val full = artist.replace(Regex("""\s+"""), " ").trim()
        val variants = mutableListOf<String>()
        if (full.isNotEmpty()) variants.add(full)

        for (part in full.split(ARTIST_SEPARATORS)) {
            val name = part.trim()
            if (name.isNotEmpty() && !variants.any { it.equals(name, ignoreCase = true) }) {
                variants.add(name)
            }
        }
        return variants.take(max)
    }

    fun sameSong(
        wantTitle: String,
        wantArtist: String,
        title: String,
        artists: Iterable<String>
    ): Boolean {
        if (key(title) != key(wantTitle)) return false
        if (wantArtist.trim().isEmpty() || wantArtist.trim().equals("unknown artist", ignoreCase = true)) {
            return true
        }

        val credited = artists.map { key(it) }.filter { it.isNotEmpty() }
        val all = credited.joinToString("")

        return artistVariants(wantArtist, max = 6).map { key(it) }.any { want ->
            want.isNotEmpty() && (all.contains(want) || credited.any { it.contains(want) })
        }
    }

    fun sameLength(seconds: Double?, wantSeconds: Int, drift: Int = 4): Boolean {
        if (wantSeconds <= 0 || seconds == null || seconds <= 0.0) return true
        return abs(seconds - wantSeconds) <= drift
    }

    fun key(s: String): String {
        return cleanTitle(s)
            .lowercase()
            .replace(Regex("""[^\p{L}\p{M}\p{N}]"""), "")
    }
}
