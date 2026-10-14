package com.example.looper_android.data.models

data class LyricLine(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val backgroundWords: List<LyricWord> = emptyList(),
    val isOppositeSide: Boolean = false,
    val backgroundText: String = ""
) {
    val effectiveBackgroundText: String = if (backgroundText.isNotEmpty()) {
        backgroundText
    } else {
        backgroundWords.joinToString(separator = "") { it.text }.trim()
    }

    val hasWordTiming: Boolean get() = words.isNotEmpty()

    fun getProgress(positionMs: Long): Float {
        if (positionMs <= startTimeMs) return 0f
        if (positionMs >= endTimeMs) return 1f
        val duration = endTimeMs - startTimeMs
        if (duration <= 0L) return 1f
        return ((positionMs - startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
    }

    companion object {
        private const val PUNCTUATION = """,.!?;:…、，。！？；："""
        private val WORD_TOKEN = Regex("""[^\s$PUNCTUATION]+[$PUNCTUATION]*\s*|[$PUNCTUATION]+\s*""")
        private val ENDS_WITH_STOP = Regex("""[.!?…。！？]\s*$""")
        private val ENDS_WITH_COMMA = Regex("""[,;:、，；：]\s*$""")

        fun endsWord(text: String): Boolean {
            return text.endsWith(" ") ||
                    text.endsWith("\u3000") ||
                    ENDS_WITH_STOP.containsMatchIn(text) ||
                    ENDS_WITH_COMMA.containsMatchIn(text)
        }

        fun splitAtPunctuation(words: List<LyricWord>): List<LyricWord> {
            val out = mutableListOf<LyricWord>()
            for (w in words) {
                val matches = WORD_TOKEN.findAll(w.text).map { it.value }.toList()
                if (matches.size < 2 || matches.joinToString("") != w.text) {
                    out.add(w)
                    continue
                }
                val totalLength = matches.sumOf { it.trim().length }
                val span = w.endTimeMs - w.startTimeMs
                var cursor = w.startTimeMs
                for (j in matches.indices) {
                    val part = matches[j]
                    val partLen = part.trim().length
                    val end = if (j == matches.size - 1) {
                        w.endTimeMs
                    } else {
                        val fraction = if (totalLength == 0) 0.0 else partLen.toDouble() / totalLength
                        cursor + (span * fraction).toLong()
                    }
                    out.add(LyricWord(startTimeMs = cursor, endTimeMs = end, text = part))
                    cursor = end
                }
            }
            return out
        }

        fun sungFraction(words: List<LyricWord>, positionMs: Long): Float {
            if (words.isEmpty()) return 0f
            val total = words.sumOf { it.text.length }
            if (total == 0) return 0f

            var before = 0
            for (w in words) {
                if (positionMs < w.startTimeMs) break
                val length = w.text.length
                if (positionMs >= w.endTimeMs) {
                    before += length
                    continue
                }
                val span = (w.endTimeMs - w.startTimeMs).coerceAtLeast(1L)
                val into = ((positionMs - w.startTimeMs).toFloat() / span).coerceIn(0f, 1f)
                return ((before + length * into) / total).coerceIn(0f, 1f)
            }
            return (before.toFloat() / total).coerceIn(0f, 1f)
        }
    }
}
