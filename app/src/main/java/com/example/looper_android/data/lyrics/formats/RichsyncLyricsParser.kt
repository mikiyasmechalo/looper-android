package com.example.looper_android.data.lyrics.formats

import org.json.JSONArray

object RichsyncLyricsParser {

    private data class Chunk(
        val text: String,
        val atMs: Long
    )

    fun parse(richsyncBody: String): List<TimedLyricLine>? {
        val root = try {
            JSONArray(richsyncBody)
        } catch (_: Exception) {
            return null
        }

        val lines = mutableListOf<TimedLyricLine>()
        for (i in 0 until root.length()) {
            val lineObj = root.optJSONObject(i) ?: continue
            val ts = lineObj.optDouble("ts", -1.0)
            if (ts < 0) continue
            val te = lineObj.optDouble("te", ts)

            val start = (ts * 1000.0).toLong()
            val end = (te * 1000.0).toLong()

            val lArray = lineObj.optJSONArray("l")
            val chunks = mutableListOf<Chunk>()
            if (lArray != null) {
                for (j in 0 until lArray.length()) {
                    val cObj = lArray.optJSONObject(j) ?: continue
                    val cText = cObj.optString("c", "")
                    if (cText.isEmpty()) continue
                    val o = cObj.optDouble("o", 0.0)
                    val atMs = start + (o * 1000.0).toLong()
                    chunks.add(Chunk(cText, atMs))
                }
            }

            val words = mutableListOf<TimedSyllable>()
            for (j in 0 until chunks.size) {
                val chunk = chunks[j]
                if (chunk.text.trim().isEmpty()) {
                    if (words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                        val last = words.removeAt(words.size - 1)
                        words.add(last.copy(text = "${last.text} "))
                    }
                    continue
                }

                val nextMs = if (j + 1 < chunks.size) chunks[j + 1].atMs else end
                words.add(
                    TimedSyllable(
                        text = chunk.text,
                        startMs = chunk.atMs,
                        endMs = if (nextMs > chunk.atMs) nextMs else chunk.atMs
                    )
                )
            }

            val lineText = if (words.isEmpty()) {
                lineObj.optString("x", "").trim()
            } else {
                words.joinToString("") { it.text }.trim()
            }
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

        return if (lines.isEmpty()) null else lines
    }
}
