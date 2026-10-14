package com.example.looper_android.data.lyrics.formats

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream

object KrcLyricsParser {

    private val LINE = Regex("""^\[(\d+),(\d+)\](.*)$""")
    private val KRC_WORD = Regex("""<(\d+),(\d+),\d+>([^<]*)""")
    private val YRC_WORD = Regex("""\((\d+),(\d+),\d+\)([^()]*)""")
    private val LATIN_END = Regex("""[A-Za-z0-9',.!?]$""")
    private val LATIN_START = Regex("""^[A-Za-z0-9]""")

    private val KRC_KEY = byteArrayOf(
        64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45,
        206.toByte(), 210.toByte(), 110, 105
    )

    fun decodeKrc(fileBytes: ByteArray): String? {
        if (fileBytes.size <= 4) return null
        val header = String(fileBytes.copyOfRange(0, 4), Charsets.US_ASCII)
        if (header != "krc1") return null

        val packed = ByteArray(fileBytes.size - 4)
        for (i in 4 until fileBytes.size) {
            val keyByte = KRC_KEY[(i - 4) % KRC_KEY.size].toInt()
            packed[i - 4] = (fileBytes[i].toInt() xor keyByte).toByte()
        }

        return try {
            val inflater = InflaterInputStream(ByteArrayInputStream(packed))
            val buffer = ByteArray(4096)
            val output = ByteArrayOutputStream()
            var read: Int
            while (inflater.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            output.toString(Charsets.UTF_8.name())
        } catch (_: Exception) {
            null
        }
    }

    fun parseKrc(krc: String): List<TimedLyricLine> =
        parseInternal(krc, KRC_WORD, relative = true)

    fun parseYrc(yrc: String): List<TimedLyricLine> =
        parseInternal(yrc, YRC_WORD, relative = false)

    private fun parseInternal(
        source: String,
        wordRegex: Regex,
        relative: Boolean
    ): List<TimedLyricLine> {
        val lines = mutableListOf<TimedLyricLine>()
        for (raw in source.split(Regex("""\r?\n"""))) {
            val match = LINE.find(raw.trim()) ?: continue
            val start = match.groupValues[1].toLongOrNull() ?: continue
            val duration = match.groupValues[2].toLongOrNull() ?: continue
            val end = start + duration
            val content = match.groupValues[3]

            val words = mutableListOf<TimedSyllable>()
            for (w in wordRegex.findAll(content)) {
                val text = w.groupValues[3]
                if (text.isEmpty()) continue

                // A space timed on its own belongs to the word before it
                if (text.trim().isEmpty()) {
                    if (words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                        val last = words.removeAt(words.size - 1)
                        words.add(last.copy(text = "${last.text} "))
                    }
                    continue
                }

                // NetEase sometimes drops space between Latin words ("how(..)to")
                if (!relative && words.isNotEmpty() && LATIN_END.containsMatchIn(words.last().text) && LATIN_START.containsMatchIn(text)) {
                    val last = words.removeAt(words.size - 1)
                    words.add(last.copy(text = "${last.text} "))
                }

                val offset = w.groupValues[1].toLongOrNull() ?: continue
                val wordDuration = w.groupValues[2].toLongOrNull() ?: continue
                val wordStart = offset + if (relative) start else 0L

                words.add(
                    TimedSyllable(
                        text = text,
                        startMs = wordStart,
                        endMs = wordStart + wordDuration
                    )
                )
            }

            val lineText = if (words.isEmpty()) content.trim() else words.joinToString("") { it.text }.trim()
            if (lineText.isEmpty()) continue
            lines.add(
                TimedLyricLine(
                    startMs = start,
                    endMs = end,
                    text = lineText,
                    words = words
                )
            )
        }

        return lines.sortedBy { it.startMs }
    }
}
