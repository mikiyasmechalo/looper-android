package com.example.looper_android.data.repository

import com.example.looper_android.data.models.LyricLine

object LyricsParser {

    fun parse(lrcContent: String, durationMs: Long = 180000L): List<LyricLine> {
        val lines = lrcContent.split("\n")
        val rawLines = mutableListOf<LyricLine>()

        // Flexible regex: matches [mm:ss], [mm:ss.x], [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx], [mmm:ss.xx]
        val timeRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[:\.](\d{1,3}))?\](.*)""")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            if (trimmedLine.startsWith("[source:")) continue

            val matches = timeRegex.findAll(trimmedLine).toList()
            if (matches.isNotEmpty()) {
                val text = matches.last().groupValues[4].trim()
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues[3]

                    val milliseconds = when (fracStr.length) {
                        1 -> fracStr.toLong() * 100L
                        2 -> fracStr.toLong() * 10L
                        3 -> fracStr.toLong()
                        else -> 0L
                    }

                    val timeMs = minutes * 60 * 1000L + seconds * 1000L + milliseconds
                    rawLines.add(LyricLine(timeMs, timeMs, text))
                }
            }
        }

        // Fallback for plain text lyrics (no timestamps matched)
        if (rawLines.isEmpty()) {
            val plainLines = mutableListOf<String>()
            val metaRegex = Regex("""^\[[a-zA-Z]+:.*\]$""")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("[source:")) continue
                if (metaRegex.matches(trimmed)) continue
                if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.contains(" ")) continue
                plainLines.add(trimmed)
            }

            if (plainLines.isNotEmpty()) {
                val totalMs = if (durationMs > 0L) durationMs else 180000L
                val interval = totalMs / plainLines.size
                val plainResult = mutableListOf<LyricLine>()
                for (i in plainLines.indices) {
                    val startMs = interval * i
                    val endMs = interval * (i + 1)
                    plainResult.add(LyricLine(startMs, endMs, plainLines[i]))
                }
                return plainResult
            }
            return emptyList()
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

        return result
    }
}
