package io.github.x80070006.holowave.lyrics

data class LyricLine(val timeMs: Long, val text: String)

enum class LyricsSource { EMBEDDED, IMPORTED, LRCLIB }

sealed interface Lyrics {
    val source: LyricsSource

    data class Synced(val lines: List<LyricLine>, override val source: LyricsSource) : Lyrics {
        /** Index of the line being sung at [positionMs], or -1 before the first line. */
        fun indexAt(positionMs: Long): Int {
            var lo = 0
            var hi = lines.lastIndex
            var found = -1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (lines[mid].timeMs <= positionMs) {
                    found = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            return found
        }
    }

    data class Plain(val text: String, override val source: LyricsSource) : Lyrics

    data class Instrumental(override val source: LyricsSource) : Lyrics
}

/**
 * Parser for the LRC format (plain and "enhanced"), written defensively:
 * inputs come from audio tags, user-picked files and the network.
 */
object Lrc {
    const val MAX_INPUT_CHARS = 256 * 1024
    private const val MAX_LINES = 5_000
    private const val MAX_LINE_CHARS = 500

    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTag = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val offsetTag = Regex("""^\[offset:\s*([+-]?\d{1,6})\s*]""", RegexOption.IGNORE_CASE)

    fun looksSynced(text: String): Boolean =
        text.lineSequence().take(400).any { timeTag.matchAt(it.trimStart(), 0) != null }

    /** Returns synced lyrics when the text has timestamps, plain lyrics otherwise, null when empty. */
    fun parse(raw: String, source: LyricsSource): Lyrics? {
        val text = raw.take(MAX_INPUT_CHARS).replace("\u0000", "")
        if (text.isBlank()) return null
        if (!looksSynced(text)) {
            val plain = text.lineSequence()
                .map { it.trimEnd().take(MAX_LINE_CHARS) }
                .take(MAX_LINES)
                .joinToString("\n")
                .trim()
            return if (plain.isEmpty()) null else Lyrics.Plain(plain, source)
        }

        var offsetMs = 0L
        val lines = ArrayList<LyricLine>()
        for (rawLine in text.lineSequence()) {
            if (lines.size >= MAX_LINES) break
            val line = rawLine.trim()
            offsetTag.find(line)?.let { m ->
                offsetMs = m.groupValues[1].toLongOrNull() ?: 0L
                continue
            }
            var cursor = 0
            val stamps = ArrayList<Long>(2)
            while (true) {
                val m = timeTag.matchAt(line, cursor) ?: break
                stamps += toMillis(m.groupValues[1], m.groupValues[2], m.groupValues[3]) ?: break
                cursor = m.range.last + 1
            }
            if (stamps.isEmpty()) continue
            val lyric = line.substring(cursor).replace(wordTag, "").replace(Regex("\\s+"), " ").trim().take(MAX_LINE_CHARS)
            stamps.forEach { lines += LyricLine(it, lyric) }
        }
        if (lines.isEmpty()) return null
        // A positive LRC offset means lyrics should appear sooner.
        val shifted = lines
            .map { it.copy(timeMs = (it.timeMs - offsetMs).coerceAtLeast(0L)) }
            .sortedBy { it.timeMs }
            .dropWhile { it.text.isEmpty() }
        if (shifted.none { it.text.isNotEmpty() }) return null
        return Lyrics.Synced(shifted, source)
    }

    private fun toMillis(min: String, sec: String, frac: String): Long? {
        val m = min.toLongOrNull() ?: return null
        val s = sec.toLongOrNull()?.takeIf { it < 60 } ?: return null
        val f = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
        return m * 60_000L + s * 1_000L + f
    }

    /** Serializes synced lyrics back to LRC (used by the on-device cache). */
    fun format(lines: List<LyricLine>): String = buildString {
        lines.forEach { l ->
            val totalCs = l.timeMs / 10
            append("[%02d:%02d.%02d]".format(totalCs / 6000, (totalCs / 100) % 60, totalCs % 100))
            append(l.text)
            append('\n')
        }
    }
}
