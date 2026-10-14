package com.example.looper_android.data.repository

import com.example.looper_android.data.models.LyricLine
import com.example.looper_android.data.models.LyricWord

data class ParsedLyricsResult(
    val isSynced: Boolean,
    val timedLines: List<LyricLine> = emptyList(),
    val plainLines: List<String> = emptyList()
)

object LyricsParser {

    private val LINE_TAG = Regex("""\[(\d{1,3})\s*:\s*(\d{1,2})(?:[:\.](\d{1,3}))?\](.*)""")
    private val WORD_TAG = Regex("""<(\d{1,3})\s*:\s*(\d{1,2})(?:[:\.](\d{1,3}))?>""")
    private val BG_LINE = Regex("""^\[bg:(.*)\]$""", RegexOption.IGNORE_CASE)
    private val TIMING_LINE = Regex("""^\[w:([0-9,;]*)\]$""", RegexOption.IGNORE_CASE)
    private val VOICE_LINE = Regex("""^\[v:(\d+)\]$""", RegexOption.IGNORE_CASE)
    private val OFFSET_TAG = Regex("""\[offset\s*:\s*(-?\d+)\]""", RegexOption.IGNORE_CASE)

    private const val WORD_TIMED_INSTRUMENTAL_GAP_MS = 5000L
    private const val LINE_TIMED_INSTRUMENTAL_GAP_MS = 15000L
    private const val INTRO_GAP_MS = 5000L
    private const val MIN_LINE_GAP_MS = 10L

    private class RawWord(
        val startMs: Long,
        val endMs: Long?,
        val text: String
    )

    private class RawLine(
        var startMs: Long,
        val text: String,
        var words: List<RawWord> = emptyList(),
        var opposite: Boolean = false,
        var backgroundText: String = "",
        var backgroundWords: List<RawWord> = emptyList(),
        var timingTargetsBackground: Boolean = false
    )

    fun isTimed(lrcContent: String): Boolean {
        return LINE_TAG.containsMatchIn(lrcContent)
    }

    fun parseLyrics(
        lrcContent: String,
        durationMs: Long = 180000L,
        userShiftMs: Long = 0L
    ): ParsedLyricsResult {
        val cleanContent = lrcContent.removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val lines = cleanContent.split("\n")
        val rawLines = mutableListOf<RawLine>()
        var globalOffsetMs = userShiftMs

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // 1. Check for [offset:xxx] tag
            val offsetMatch = OFFSET_TAG.find(trimmed)
            if (offsetMatch != null) {
                globalOffsetMs = userShiftMs + (offsetMatch.groupValues[1].toLongOrNull() ?: 0L)
                continue
            }

            // 2. Background vocals line [bg:...]
            val bgMatch = BG_LINE.find(trimmed)
            if (bgMatch != null) {
                if (rawLines.isNotEmpty()) {
                    val raw = rawLines.last()
                    val content = bgMatch.groupValues[1].trim()
                    raw.backgroundWords = if (WORD_TAG.containsMatchIn(content)) {
                        parseWords(content, globalOffsetMs)
                    } else emptyList()

                    raw.backgroundText = if (raw.backgroundWords.isNotEmpty()) {
                        raw.backgroundWords.joinToString("") { it.text }.trim()
                    } else content

                    raw.timingTargetsBackground = true
                }
                continue
            }

            // 3. Syllable timing payload [w:offset,duration,length;...]
            val timingMatch = TIMING_LINE.find(trimmed)
            if (timingMatch != null) {
                if (rawLines.isNotEmpty()) {
                    val raw = rawLines.last()
                    val payload = timingMatch.groupValues[1]
                    if (raw.timingTargetsBackground) {
                        raw.backgroundWords = parseTimingPayload(payload, raw.backgroundText, raw.startMs)
                    } else {
                        raw.words = parseTimingPayload(payload, raw.text, raw.startMs)
                    }
                }
                continue
            }

            // 4. Voice marker [v:2] (duet opposite side)
            val voiceMatch = VOICE_LINE.find(trimmed)
            if (voiceMatch != null) {
                if (rawLines.isNotEmpty()) {
                    rawLines.last().opposite = voiceMatch.groupValues[1] != "1"
                }
                continue
            }

            // Skip custom source tags
            if (trimmed.startsWith("[source:", ignoreCase = true) ||
                trimmed.startsWith("[picked:", ignoreCase = true) ||
                trimmed.startsWith("[checked:", ignoreCase = true)
            ) {
                continue
            }

            // 5. Line timestamp tag [mm:ss.xx]
            val match = LINE_TAG.find(line)
            if (match != null) {
                val startMs = (toMs(
                    match.groupValues[1],
                    match.groupValues[2],
                    match.groupValues[3]
                ) + globalOffsetMs).coerceAtLeast(0L)

                val content = match.groupValues[4].trim()

                val words = if (WORD_TAG.containsMatchIn(content)) {
                    parseWords(content, globalOffsetMs)
                } else emptyList()

                val text = if (words.isNotEmpty()) {
                    words.joinToString("") { it.text }.trim()
                } else content

                rawLines.add(RawLine(startMs = startMs, text = text, words = words))
            }
        }

        // Plain text fallback (no timestamps matched)
        if (rawLines.isEmpty()) {
            val plainLines = mutableListOf<String>()
            val metaRegex = Regex("""^\[[a-zA-Z]+:.*\]$""")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                if (metaRegex.matches(trimmed)) continue
                if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.contains(" ")) continue
                plainLines.add(trimmed)
            }

            return ParsedLyricsResult(
                isSynced = false,
                timedLines = plainLines.map { text ->
                    LyricLine(startTimeMs = 0L, endTimeMs = 0L, text = text)
                },
                plainLines = plainLines
            )
        }

        // Sort lines stably by start time
        rawLines.sortBy { it.startMs }

        // Enforce minimum gap (10ms) between consecutive lines
        for (i in 1 until rawLines.size) {
            val prevStart = rawLines[i - 1].startMs
            val start = rawLines[i].startMs
            if (start - prevStart < MIN_LINE_GAP_MS) {
                rawLines[i].startMs = prevStart + MIN_LINE_GAP_MS
            }
        }

        val result = mutableListOf<LyricLine>()
        for (i in rawLines.indices) {
            val raw = rawLines[i]
            val startTime = raw.startMs
            val text = raw.text
            val isLast = i == rawLines.size - 1

            var nextStartTime = if (isLast) durationMs.coerceAtLeast(startTime + 4000L) else rawLines[i + 1].startMs
            val singingEnd = getSingingEnd(raw)
            if (singingEnd != null && nextStartTime < singingEnd && isLast) {
                nextStartTime = singingEnd
            }

            fun buildLine(end: Long): LyricLine {
                return LyricLine(
                    startTimeMs = startTime,
                    endTimeMs = end,
                    text = text,
                    words = finishWords(raw.words, end),
                    backgroundWords = finishWords(raw.backgroundWords, end),
                    isOppositeSide = raw.opposite,
                    backgroundText = raw.backgroundText
                )
            }

            // Word-timed instrumental break
            if (singingEnd != null && text.isNotEmpty() && nextStartTime - singingEnd > WORD_TIMED_INSTRUMENTAL_GAP_MS) {
                val breakStart = singingEnd + 600L
                result.add(buildLine(breakStart))
                result.add(
                    LyricLine(
                        startTimeMs = breakStart,
                        endTimeMs = nextStartTime,
                        text = "" // Empty string triggers instrumental break icon
                    )
                )
                continue
            }

            // Line-timed instrumental break (>15s gap)
            if (singingEnd == null && (nextStartTime - startTime) > LINE_TIMED_INSTRUMENTAL_GAP_MS && text.isNotEmpty()) {
                val singingDuration = 10000L
                val breakStart = startTime + singingDuration
                result.add(buildLine(breakStart))
                result.add(
                    LyricLine(
                        startTimeMs = breakStart,
                        endTimeMs = nextStartTime,
                        text = ""
                    )
                )
            } else {
                result.add(buildLine(nextStartTime))
            }
        }

        // Intro gap: if music plays for >= 5s before first lyric, show instrumental note
        if (result.isNotEmpty() && result.first().text.isNotEmpty() && result.first().startTimeMs >= INTRO_GAP_MS) {
            result.add(
                0,
                LyricLine(
                    startTimeMs = 0L,
                    endTimeMs = result.first().startTimeMs,
                    text = ""
                )
            )
        }

        return ParsedLyricsResult(
            isSynced = true,
            timedLines = result,
            plainLines = result.map { it.text }
        )
    }

    private fun getSingingEnd(raw: RawLine): Long? {
        var latest: Long? = null
        for (w in raw.words + raw.backgroundWords) {
            val end = w.endMs ?: w.startMs
            if (latest == null || end > latest) latest = end
        }
        return latest
    }

    private fun finishWords(rawWords: List<RawWord>, lineEnd: Long): List<LyricWord> {
        if (rawWords.isEmpty()) return emptyList()
        return rawWords.map { w ->
            LyricWord(
                startTimeMs = w.startMs,
                endTimeMs = w.endMs ?: if (lineEnd > w.startMs) lineEnd else w.startMs,
                text = w.text
            )
        }
    }

    private fun parseWords(content: String, offsetMs: Long): List<RawWord> {
        val tags = WORD_TAG.findAll(content).toList()
        if (tags.isEmpty()) return emptyList()

        val words = mutableListOf<RawWord>()
        val leading = content.substring(0, tags.first().range.first)

        for (i in tags.indices) {
            val tag = tags[i]
            val textEnd = if (i + 1 < tags.size) tags[i + 1].range.first else content.length
            var text = content.substring(tag.range.last + 1, textEnd)
            if (i == 0) text = leading + text
            if (text.isEmpty()) continue

            val startMs = (toMs(
                tag.groupValues[1],
                tag.groupValues[2],
                tag.groupValues[3]
            ) + offsetMs).coerceAtLeast(0L)

            val endMs = if (i + 1 < tags.size) {
                val nextTag = tags[i + 1]
                (toMs(
                    nextTag.groupValues[1],
                    nextTag.groupValues[2],
                    nextTag.groupValues[3]
                ) + offsetMs).coerceAtLeast(0L)
            } else null

            words.add(RawWord(startMs, endMs, text))
        }
        return words
    }

    private fun parseTimingPayload(payload: String, text: String, lineStartMs: Long): List<RawWord> {
        if (payload.isEmpty() || text.isEmpty()) return emptyList()
        val words = mutableListOf<RawWord>()
        var cursor = 0

        for (entry in payload.split(";")) {
            val fields = entry.split(",")
            if (fields.size != 3) return emptyList()
            val offset = fields[0].toLongOrNull() ?: return emptyList()
            val duration = fields[1].toLongOrNull() ?: return emptyList()
            val length = fields[2].toIntOrNull() ?: return emptyList()

            if (length <= 0 || cursor + length > text.length) return emptyList()
            val start = lineStartMs + offset
            val wordText = text.substring(cursor, cursor + length)
            words.add(RawWord(start, start + duration, wordText))
            cursor += length
        }

        return if (cursor == text.length) words else emptyList()
    }

    private fun toMs(minutesStr: String, secondsStr: String, fracStr: String?): Long {
        val minutes = minutesStr.toLongOrNull() ?: 0L
        val seconds = secondsStr.toLongOrNull() ?: 0L
        val milliseconds = when (fracStr?.length ?: 0) {
            1 -> (fracStr?.toLongOrNull() ?: 0L) * 100L
            2 -> (fracStr?.toLongOrNull() ?: 0L) * 10L
            3 -> (fracStr?.toLongOrNull() ?: 0L)
            else -> 0L
        }
        return minutes * 60000L + seconds * 1000L + milliseconds
    }

    @Deprecated("Use parseLyrics instead")
    fun parse(lrcContent: String, durationMs: Long = 180000L): List<LyricLine> {
        return parseLyrics(lrcContent, durationMs).timedLines
    }
}
