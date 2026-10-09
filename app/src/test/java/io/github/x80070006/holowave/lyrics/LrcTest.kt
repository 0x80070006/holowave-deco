package io.github.x80070006.holowave.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcTest {

    private fun synced(text: String) = Lrc.parse(text, LyricsSource.IMPORTED) as Lyrics.Synced

    @Test fun parsesBasicTimestamps() {
        val l = synced("[ar:Someone]\n[00:01.50]Hello\n[00:03.2]World\n[01:02.345]Later")
        assertEquals(listOf(1500L, 3200L, 62345L), l.lines.map { it.timeMs })
        assertEquals(listOf("Hello", "World", "Later"), l.lines.map { it.text })
    }

    @Test fun expandsRepeatedTimestampsAndSorts() {
        val l = synced("[00:10.00][00:30.00]Chorus\n[00:20.00]Verse")
        assertEquals(listOf("Chorus" to 10_000L, "Verse" to 20_000L, "Chorus" to 30_000L), l.lines.map { it.text to it.timeMs })
    }

    @Test fun appliesOffsetAndStripsWordTags() {
        val l = synced("[offset:+500]\n[00:02.00]<00:02.00>Two <00:02.50>words")
        assertEquals(1500L, l.lines.single().timeMs)
        assertEquals("Two words", l.lines.single().text)
    }

    @Test fun plainTextFallsBackToPlainLyrics() {
        assertTrue(Lrc.parse("Just words\nno timestamps", LyricsSource.EMBEDDED) is Lyrics.Plain)
    }

    @Test fun rejectsEmptyAndGarbage() {
        assertNull(Lrc.parse("   \n  ", LyricsSource.LRCLIB))
        assertNull(Lrc.parse("[00:01.00]\n[00:02.00]", LyricsSource.LRCLIB))
        // Invalid seconds are not timestamps: no crash, no synced result.
        assertTrue(Lrc.parse("[00:99.00]nope", LyricsSource.LRCLIB) !is Lyrics.Synced)
    }

    @Test fun indexAtFindsCurrentLine() {
        val l = synced("[00:01.00]a\n[00:05.00]b\n[00:09.00]c")
        assertEquals(-1, l.indexAt(500))
        assertEquals(0, l.indexAt(1000))
        assertEquals(1, l.indexAt(8999))
        assertEquals(2, l.indexAt(60_000))
    }

    @Test fun formatRoundTrips() {
        val l = synced("[00:01.25]a\n[02:05.50]b")
        assertEquals(l.lines, synced(Lrc.format(l.lines)).lines)
    }

    @Test fun capsHugeInput() {
        val huge = buildString { repeat(100_000) { append("[00:01.00]line $it\n") } }
        assertTrue(synced(huge).lines.size <= 5_000)
    }
}
