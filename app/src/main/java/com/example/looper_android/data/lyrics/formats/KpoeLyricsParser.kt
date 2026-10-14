package com.example.looper_android.data.lyrics.formats

import org.json.JSONArray
import org.json.JSONObject

object KpoeLyricsParser {

    fun parse(payload: Any?): List<TimedLyricLine>? {
        val root = when (payload) {
            is JSONObject -> payload
            is String -> try { JSONObject(payload) } catch (_: Exception) { return null }
            else -> return null
        }

        val rawLines = findLines(root) ?: return null
        if (rawLines.length() == 0) return null

        val type = root.optString("type", "").lowercase()
        val lineSyncedOnly = type == "line"

        val agentTypes = mutableMapOf<String, String>()
        val metadata = root.optJSONObject("metadata")
        val agents = metadata?.optJSONObject("agents")
        if (agents != null) {
            val keys = agents.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = agents.optJSONObject(key)
                if (obj != null && obj.has("type")) {
                    val alias = obj.optString("alias", key)
                    agentTypes[alias] = obj.optString("type")
                }
            }
        }

        val lines = mutableListOf<TimedLyricLine>()
        val singers = mutableListOf<String?>()

        for (i in 0 until rawLines.length()) {
            val entry = rawLines.optJSONObject(i) ?: continue
            val start = toMs(entry.opt("time"))
            val duration = toMs(entry.opt("duration"))
            val explicitEnd = toMs(entry.opt("endTime"))
            val end = if (explicitEnd > 0L) explicitEnd else start + duration
            val text = entry.optString("text", "")

            val main = mutableListOf<TimedSyllable>()
            val background = mutableListOf<TimedSyllable>()
            val syllabus = entry.optJSONArray("syllabus") ?: entry.optJSONArray("words")

            if (!lineSyncedOnly && syllabus != null) {
                val count = syllabus.length()
                for (j in 0 until count) {
                    val syl = syllabus.optJSONObject(j) ?: continue
                    val sylText = syl.optString("text", "")
                    if (sylText.isEmpty()) continue
                    val sylStart = if (syl.has("time")) toMs(syl.opt("time")) else start
                    val sylDuration = toMs(syl.opt("duration"))
                    val sylEnd = if (sylDuration == 0L && count == 1) end else sylStart + sylDuration

                    val item = TimedSyllable(text = sylText, startMs = sylStart, endMs = sylEnd)
                    if (syl.optBoolean("isBackground", false)) {
                        background.add(item)
                    } else {
                        main.add(item)
                    }
                }
            }

            val lineText = if (main.isNotEmpty()) {
                main.joinToString("") { it.text }.trim()
            } else text.trim()
            if (lineText.isEmpty()) continue

            lines.add(
                TimedLyricLine(
                    startMs = start,
                    endMs = end,
                    text = lineText,
                    words = if (main.size > 1) main else emptyList(),
                    background = background
                )
            )

            val element = entry.optJSONObject("element")
            singers.add(element?.optString("singer"))
        }

        if (lines.isEmpty()) return null

        val sides = TimedLyricsUtils.assignDuetSides(singers, agentTypes)
        return lines.mapIndexed { idx, line ->
            if (idx < sides.size && sides[idx]) {
                line.copy(oppositeSide = true)
            } else line
        }
    }

    private fun findLines(payload: JSONObject): JSONArray? {
        val lyrics = payload.optJSONArray("lyrics")
        if (lyrics != null) return lyrics
        val data = payload.opt("data")
        if (data is JSONArray) return data
        if (data is JSONObject) return data.optJSONArray("lyrics")
        return null
    }

    private fun toMs(value: Any?): Long {
        if (value == null) return 0L
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> {
                if (value != Math.floor(value)) (value * 1000.0).toLong() else value.toLong()
            }
            is Float -> {
                if (value != Math.floor(value.toDouble()).toFloat()) (value * 1000f).toLong() else value.toLong()
            }
            is Number -> value.toLong()
            is String -> {
                val d = value.toDoubleOrNull() ?: return 0L
                if (d != Math.floor(d)) (d * 1000.0).toLong() else d.toLong()
            }
            else -> 0L
        }
    }
}
