package com.example.looper_android.data.repository

import com.example.looper_android.data.models.LyricLine

data class ParsedLyricsResult(
    val isSynced: Boolean,
    val timedLines: List<LyricLine> = emptyList(),
    val plainLines: List<String> = emptyList()
)

object LyricsParser {

    fun parseLyrics(lrcContent: String, durationMs: Long = 180000L): ParsedLyricsResult {
        val cleanContent = lrcContent.removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val lines = cleanContent.split("\n")
        val rawLines = mutableListOf<LyricLine>()

        // Flexible regex matching [mm:ss], [m:ss], [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx], [mm:ss,xx]
        val tagRegex = Regex("""\[(\d{1,3})\s*:\s*(\d{1,2})(?:[\.:,](\d{1,3}))?\]""")
        val offsetRegex = Regex("""\[offset\s*:\s*(-?\d+)\]""", RegexOption.IGNORE_CASE)
        var globalOffsetMs = 0L

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            if (trimmedLine.startsWith("[source:", ignoreCase = true)) continue

            val offsetMatch = offsetRegex.find(trimmedLine)
            if (offsetMatch != null) {
                globalOffsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                continue
            }

            val tagMatches = tagRegex.findAll(trimmedLine).toList()
            if (tagMatches.isNotEmpty()) {
                // Extract text by removing all timestamp tags on this line (supports multiple tags per line)
                val text = trimmedLine.replace(tagRegex, "").trim()
                for (match in tagMatches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]

                    val milliseconds = when (fracStr.length) {
                        1 -> fracStr.toLong() * 100L
                        2 -> fracStr.toLong() * 10L
                        3 -> fracStr.toLong()
                        else -> 0L
                    }

                    val timeMs = (minutes * 60 * 1000L + seconds * 1000L + milliseconds + globalOffsetMs).coerceAtLeast(0L)
                    rawLines.add(LyricLine(timeMs, timeMs, text))
                }
            }
        }

        // If no timestamps were matched, treat as pure plain text lyrics (do NOT fake timestamps)
        if (rawLines.isEmpty()) {
            val plainLines = mutableListOf<String>()
            val metaRegex = Regex("""^\[[a-zA-Z]+:.*\]$""")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("[source:", ignoreCase = true)) continue
                if (metaRegex.matches(trimmed)) continue
                if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.contains(" ")) continue
                plainLines.add(trimmed)
            }

            return ParsedLyricsResult(
                isSynced = false,
                timedLines = emptyList(),
                plainLines = plainLines
            )
        }

        rawLines.sortBy { it.startTimeMs }

        val result = mutableListOf<LyricLine>()
        if (rawLines.isNotEmpty() && rawLines.first().startTimeMs > 0L) {
            result.add(
                LyricLine(
                    startTimeMs = 0L,
                    endTimeMs = rawLines.first().startTimeMs,
                    text = "..."
                )
            )
        }

        for (i in 0 until rawLines.size) {
            val current = rawLines[i]
            val nextStartTime = if (i < rawLines.size - 1) rawLines[i + 1].startTimeMs else current.startTimeMs + 10000L

            val gap = nextStartTime - current.startTimeMs
            if (i < rawLines.size - 1 && gap > 15000L && current.text.isNotEmpty()) {
                val singingDuration = 10000L
                val breakStart = current.startTimeMs + singingDuration
                result.add(current.copy(endTimeMs = breakStart))
                result.add(
                    LyricLine(
                        startTimeMs = breakStart,
                        endTimeMs = nextStartTime,
                        text = "..."
                    )
                )
            } else {
                result.add(current.copy(endTimeMs = nextStartTime))
            }
        }

        return ParsedLyricsResult(
            isSynced = true,
            timedLines = result,
            plainLines = result.map { it.text }
        )
    }

    @Deprecated("Use parseLyrics instead to distinguish synced vs plain lyrics")
    fun parse(lrcContent: String, durationMs: Long = 180000L): List<LyricLine> {
        val parsed = parseLyrics(lrcContent, durationMs)
        return parsed.timedLines
    }
}
