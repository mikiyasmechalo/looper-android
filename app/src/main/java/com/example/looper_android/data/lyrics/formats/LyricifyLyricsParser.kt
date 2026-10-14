package com.example.looper_android.data.lyrics.formats

object LyricifyLyricsParser {

    private val LINE = Regex("""^\[(\d+),(\d+)\](.*)$""")
    private val WORD = Regex("""([^(]+)\((\d+),(\d+)\)""")

    fun parse(text: String, offsetMs: Long = 0L): List<TimedLyricLine> {
        val syllables = text.contains("[type:LyricifySyllable]")
        val lines = mutableListOf<TimedLyricLine>()

        for (raw in text.split(Regex("""\r?\n"""))) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("[type:")) continue
            val parsed = if (syllables) {
                parseSyllableLine(line, offsetMs)
            } else {
                parsePlainLine(line, offsetMs)
            }
            if (parsed != null) lines.add(parsed)
        }

        return lines.sortedBy { it.startMs }
    }

    private fun parsePlainLine(line: String, offsetMs: Long): TimedLyricLine? {
        val match = LINE.find(line) ?: return null
        val text = match.groupValues[3].trim()
        if (text.isEmpty()) return null

        val start = (match.groupValues[1].toLongOrNull() ?: 0L) + offsetMs
        val end = (match.groupValues[2].toLongOrNull() ?: 0L) + offsetMs

        return TimedLyricLine(
            startMs = start.coerceAtLeast(0L),
            endMs = end.coerceAtLeast(0L),
            text = text
        )
    }

    private fun parseSyllableLine(line: String, offsetMs: Long): TimedLyricLine? {
        var body = line
        if (body.startsWith("[")) {
            val close = body.indexOf(']')
            if (close > 0) body = body.substring(close + 1)
        }

        val words = mutableListOf<TimedSyllable>()
        for (w in WORD.findAll(body)) {
            val sylText = w.groupValues[1]
            val start = (w.groupValues[2].toLongOrNull() ?: 0L) + offsetMs
            val duration = w.groupValues[3].toLongOrNull() ?: 0L

            words.add(
                TimedSyllable(
                    text = sylText,
                    startMs = start.coerceAtLeast(0L),
                    endMs = (start + duration).coerceAtLeast(0L)
                )
            )
        }

        if (words.isEmpty()) return null

        return TimedLyricLine(
            startMs = words.first().startMs,
            endMs = words.last().endMs,
            text = words.joinToString("") { it.text }.trim(),
            words = words
        )
    }
}
