package com.example.looper_android.data.lyrics.formats

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

object TtmlLyricsParser {

    fun parse(ttml: String): List<TimedLyricLine>? {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(ttml.toByteArray(Charsets.UTF_8)))

            val agentTypes = mutableMapOf<String, String>()
            val agentNodes = doc.getElementsByTagNameNS("*", "agent")
            for (i in 0 until agentNodes.length) {
                val agent = agentNodes.item(i) as? Element ?: continue
                val id = getAttr(agent, "id")
                val type = getAttr(agent, "type")
                if (id != null && type != null) agentTypes[id] = type
            }

            val lines = mutableListOf<TimedLyricLine>()
            val singers = mutableListOf<String?>()

            val pNodes = doc.getElementsByTagNameNS("*", "p")
            for (i in 0 until pNodes.length) {
                val p = pNodes.item(i) as? Element ?: continue
                val begin = parseTime(getAttr(p, "begin"))
                val end = parseTime(getAttr(p, "end")) ?: begin

                val main = mutableListOf<TimedSyllable>()
                val background = mutableListOf<TimedSyllable>()
                collectSpans(p, main, background, inBackground = false)

                val startMs: Long
                val endMs: Long
                if (main.isNotEmpty()) {
                    startMs = begin ?: main.first().startMs
                    val allEnds = listOfNotNull(end) + main.map { it.endMs } + background.map { it.endMs }
                    endMs = allEnds.maxOrNull() ?: startMs
                } else {
                    startMs = begin ?: 0L
                    endMs = end ?: startMs
                }

                val text = if (main.isNotEmpty()) {
                    main.joinToString("") { it.text }.trim()
                } else {
                    getOwnText(p).replace(Regex("""\s+"""), " ").trim()
                }
                if (text.isEmpty()) continue

                lines.add(
                    TimedLyricLine(
                        startMs = startMs,
                        endMs = endMs,
                        text = text,
                        words = main,
                        background = background
                    )
                )
                singers.add(getAttr(p, "agent"))
            }

            if (lines.isEmpty()) return null

            val sides = TimedLyricsUtils.assignDuetSides(singers, agentTypes)
            val withSides = lines.mapIndexed { idx, line ->
                if (idx < sides.size && sides[idx]) {
                    line.copy(oppositeSide = true)
                } else line
            }

            withSides.sortedBy { it.startMs }
        } catch (e: Exception) {
            null
        }
    }

    private fun collectSpans(
        parent: Element,
        main: MutableList<TimedSyllable>,
        background: MutableList<TimedSyllable>,
        inBackground: Boolean
    ) {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node !is Element || !node.localName.equals("span", ignoreCase = true)) continue

            val isBg = inBackground || getAttr(node, "role") == "x-bg"
            val begin = parseTime(getAttr(node, "begin"))

            var hasTimedChildren = false
            val spanChildren = node.childNodes
            for (j in 0 until spanChildren.length) {
                val child = spanChildren.item(j)
                if (child is Element && child.localName.equals("span", ignoreCase = true)) {
                    hasTimedChildren = true
                    break
                }
            }

            if (hasTimedChildren || begin == null) {
                collectSpans(node, main, background, isBg)
                continue
            }

            var text = node.textContent ?: ""
            if (text.trim().isEmpty()) continue

            val nextSibling = node.nextSibling
            if (nextSibling is Text && nextSibling.data.startsWith(" ") && !text.endsWith(" ")) {
                text = "$text "
            }

            val end = parseTime(getAttr(node, "end")) ?: begin
            val cleanText = if (isBg) stripBgParens(text) else text

            val syllable = TimedSyllable(
                text = cleanText,
                startMs = begin,
                endMs = if (end < begin) begin else end
            )

            if (isBg) background.add(syllable) else main.add(syllable)
        }
    }

    private fun stripBgParens(text: String): String {
        return text.replace(Regex("""^\(|\)(?=\s*$)"""), "")
    }

    private fun getOwnText(p: Element): String {
        val sb = StringBuilder()
        val children = p.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element) {
                if (getAttr(node, "role") == "x-bg") continue
                sb.append(node.textContent)
            } else if (node is Text) {
                sb.append(node.data)
            }
        }
        return sb.toString()
    }

    private fun getAttr(e: Element, localName: String): String? {
        val attrs = e.attributes ?: return null
        for (i in 0 until attrs.length) {
            val a = attrs.item(i)
            val name = a.localName ?: a.nodeName.substringAfter(":")
            if (name.equals(localName, ignoreCase = true)) return a.nodeValue
        }
        return null
    }

    fun parseTime(raw: String?): Long? {
        if (raw == null) return null
        val value = raw.trim().lowercase()
        if (value.isEmpty()) return null

        val unitRegex = Regex("""^(\d+(?:\.\d+)?)(ms|s|m|h)$""")
        val unitMatch = unitRegex.find(value)
        if (unitMatch != null) {
            val amount = unitMatch.groupValues[1].toDoubleOrNull() ?: return null
            val unit = unitMatch.groupValues[2]
            val factor = when (unit) {
                "ms" -> 1.0
                "s" -> 1000.0
                "m" -> 60000.0
                "h" -> 3600000.0
                else -> 1.0
            }
            return (amount * factor).toLong()
        }

        val parts = value.split(":")
        if (parts.size > 3) return null
        var seconds = 0.0
        for (part in parts) {
            val n = part.toDoubleOrNull() ?: return null
            seconds = seconds * 60.0 + n
        }
        return (seconds * 1000.0).toLong()
    }
}
