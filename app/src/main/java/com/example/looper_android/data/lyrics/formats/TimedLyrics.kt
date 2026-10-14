package com.example.looper_android.data.lyrics.formats

data class TimedSyllable(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class TimedLyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<TimedSyllable> = emptyList(),
    val background: List<TimedSyllable> = emptyList(),
    val oppositeSide: Boolean = false
) {
    val isWordSynced: Boolean get() = words.isNotEmpty()
}

object TimedLyricsUtils {

    private val CREDIT_LINE = Regex("""^[^:：]{1,24}[:：]\s*\S""")

    fun withoutCreditLines(lines: List<TimedLyricLine>, title: String = ""): List<TimedLyricLine> {
        fun textOf(l: TimedLyricLine): String =
            if (l.isWordSynced) l.words.joinToString("") { it.text }.trim() else l.text.trim()

        fun squash(s: String): String =
            s.lowercase().replace(Regex("""[^\p{L}\p{M}\p{N}]"""), "")

        val titleKey = squash(title)

        fun isCredit(l: TimedLyricLine): Boolean = CREDIT_LINE.containsMatchIn(textOf(l))
        fun isHeading(l: TimedLyricLine): Boolean {
            val text = textOf(l)
            return titleKey.isNotEmpty() && text.contains(" - ") && squash(text).startsWith(titleKey)
        }

        var start = 0
        while (start < lines.size && (isCredit(lines[start]) || isHeading(lines[start]))) {
            start++
        }
        var end = lines.size
        while (end > start && isCredit(lines[end - 1])) {
            end--
        }

        return if (start >= end) lines else lines.subList(start, end)
    }

    fun fitsDuration(lines: List<TimedLyricLine>, durationSeconds: Int): Boolean {
        if (durationSeconds <= 0 || lines.isEmpty()) return true
        return lines.last().startMs <= (durationSeconds + 10) * 1000L
    }

    fun assignDuetSides(
        singers: List<String?>,
        agentTypes: Map<String, String>
    ): List<Boolean> {
        val order = mutableListOf<String>()
        val sides = mutableListOf<Boolean>()

        for (singer in singers) {
            if (singer.isNullOrEmpty()) {
                sides.add(false)
                continue
            }
            val type = agentTypes[singer] ?: "person"
            if (type.equals("group", ignoreCase = true)) {
                sides.add(false)
                continue
            }
            if (!order.contains(singer)) order.add(singer)
            sides.add(order.indexOf(singer) % 2 != 0)
        }

        val assigned = sides.size
        val oppositeCount = sides.count { it }
        if (assigned > 0 && oppositeCount.toFloat() / assigned >= 0.85f) {
            return sides.map { !it }
        }
        return sides
    }
}
