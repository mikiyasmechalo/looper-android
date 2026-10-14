package com.example.looper_android.data.lyrics.formats

import java.util.Locale

object TimedLrcWriter {

    fun write(lines: List<TimedLyricLine>): String? {
        val usable = lines.filter { l ->
            l.text.trim().isNotEmpty() || l.words.any { it.text.trim().isNotEmpty() }
        }
        if (usable.isEmpty()) return null

        val unsynced = usable.all { it.startMs == 0L && it.endMs == 0L }
        if (unsynced) {
            return usable.joinToString("\n") { clean(plainText(it)) }
        }

        val buffer = StringBuilder()
        for (line in usable) {
            val main = timedText(line.words, line.startMs)
            val lineHeader = formatTimestamp(line.startMs)
            val lineContent = main?.first ?: clean(line.text.trim())

            buffer.append("[$lineHeader]$lineContent\n")
            if (main != null) {
                buffer.append("[w:${main.second}]\n")
            }
            if (line.oppositeSide) {
                buffer.append("[v:2]\n")
            }

            val bg = timedText(line.background, line.startMs)
            if (bg != null) {
                buffer.append("[bg:${bg.first}]\n")
                buffer.append("[w:${bg.second}]\n")
            }
        }
        return buffer.toString().trimEnd()
    }

    private fun timedText(
        syllables: List<TimedSyllable>,
        lineStartMs: Long
    ): Pair<String, String>? {
        if (syllables.isEmpty()) return null
        val parts = syllables.filter { it.text.isNotEmpty() }.map { s ->
            Pair(clean(s.text), s)
        }.toMutableList()

        while (parts.isNotEmpty() && parts.first().first.trimStart().isEmpty()) {
            parts.removeAt(0)
        }
        while (parts.isNotEmpty() && parts.last().first.trimEnd().isEmpty()) {
            parts.removeAt(parts.size - 1)
        }
        if (parts.isEmpty()) return null

        parts[0] = Pair(parts[0].first.trimStart(), parts[0].second)
        val lastIdx = parts.size - 1
        parts[lastIdx] = Pair(parts[lastIdx].first.trimEnd(), parts[lastIdx].second)

        val timing = parts.joinToString(";") { p ->
            val offset = p.second.startMs - lineStartMs
            val duration = (p.second.endMs - p.second.startMs).coerceAtLeast(0L)
            "$offset,$duration,${p.first.length}"
        }
        val fullText = parts.joinToString("") { it.first }
        return Pair(fullText, timing)
    }

    private fun plainText(line: TimedLyricLine): String {
        return if (line.isWordSynced) {
            line.words.joinToString("") { it.text }.trim()
        } else {
            line.text.trim()
        }
    }

    private fun clean(text: String): String {
        return text
            .replace(Regex("""[\r\n]"""), " ")
            .replace('[', '(')
            .replace(']', ')')
    }

    fun formatTimestamp(ms: Long): String {
        val safe = if (ms < 0L) 0L else ms
        val minutes = safe / 60000L
        val seconds = (safe % 60000L) / 1000L
        val millis = safe % 1000L
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }
}
