package io.github.x80070006.holowave.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EmbeddedLyricsTest {

    private fun synchsafe(n: Int) =
        byteArrayOf(((n shr 21) and 0x7F).toByte(), ((n shr 14) and 0x7F).toByte(), ((n shr 7) and 0x7F).toByte(), (n and 0x7F).toByte())

    private fun be32(n: Int) = byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte())

    private fun id3(major: Int, frames: List<Pair<String, ByteArray>>): ByteArray {
        val body = ByteArrayOutputStream()
        frames.forEach { (id, data) ->
            body.write(id.toByteArray(Charsets.ISO_8859_1))
            body.write(if (major == 4) synchsafe(data.size) else be32(data.size))
            body.write(byteArrayOf(0, 0))
            body.write(data)
        }
        body.write(ByteArray(32)) // padding
        val tag = body.toByteArray()
        return "ID3".toByteArray() + byteArrayOf(major.toByte(), 0, 0) + synchsafe(tag.size) + tag + ByteArray(64) { 0x55 }
    }

    private fun uslt(text: String, encoding: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(encoding)
        out.write("eng".toByteArray())
        when (encoding) {
            1 -> {
                out.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0, 0)) // empty descriptor with BOM
                out.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
                out.write(text.toByteArray(Charsets.UTF_16LE))
            }
            3 -> { out.write(0); out.write(text.toByteArray(Charsets.UTF_8)) }
            else -> { out.write(0); out.write(text.toByteArray(Charsets.ISO_8859_1)) }
        }
        return out.toByteArray()
    }

    private fun read(bytes: ByteArray) = EmbeddedLyrics.read(ByteArrayInputStream(bytes))

    @Test fun readsUtf8UsltFromId3v24() {
        val lrc = "[00:01.00]Été à Paris\n[00:04.00]Encore"
        assertEquals(lrc, read(id3(4, listOf("TIT2" to byteArrayOf(3, 65), "USLT" to uslt(lrc, 3)))))
    }

    @Test fun readsUtf16UsltFromId3v23() {
        assertEquals("Hello\nthere", read(id3(3, listOf("USLT" to uslt("Hello\nthere", 1)))))
    }

    @Test fun readsTxxxLyrics() {
        val data = byteArrayOf(3) + "LYRICS".toByteArray() + byteArrayOf(0) + "[00:02.00]Hi".toByteArray()
        assertEquals("[00:02.00]Hi", read(id3(4, listOf("TXXX" to data))))
    }

    @Test fun prefersSyncedOverPlain() {
        val frames = listOf("USLT" to uslt("plain words", 3), "USLT" to uslt("[00:01.00]timed", 3))
        assertEquals("[00:01.00]timed", read(id3(4, frames)))
    }

    @Test fun readsFlacVorbisComment() {
        fun le32(n: Int) = byteArrayOf(n.toByte(), (n shr 8).toByte(), (n shr 16).toByte(), (n shr 24).toByte())
        val vendor = "test".toByteArray()
        val comments = listOf("TITLE=x", "LYRICS=[00:03.00]flac line").map { it.toByteArray() }
        val block = ByteArrayOutputStream().apply {
            write(le32(vendor.size)); write(vendor); write(le32(comments.size))
            comments.forEach { write(le32(it.size)); write(it) }
        }.toByteArray()
        val file = "fLaC".toByteArray() +
            byteArrayOf(0, 0, 0, 34) + ByteArray(34) +
            byteArrayOf(0x84.toByte(), (block.size shr 16).toByte(), (block.size shr 8).toByte(), block.size.toByte()) + block
        assertEquals("[00:03.00]flac line", read(file))
    }

    @Test fun survivesTruncatedAndHostileInput() {
        assertNull(read(byteArrayOf()))
        assertNull(read("ID3".toByteArray()))
        assertNull(read("RIFF....WAVE".toByteArray()))
        // Declared tag size far larger than the data available.
        assertNull(read("ID3".toByteArray() + byteArrayOf(4, 0, 0) + synchsafe(9_000_000) + ByteArray(20)))
        // Frame claiming to be larger than its tag.
        val bad = "ID3".toByteArray() + byteArrayOf(4, 0, 0) + synchsafe(20) + "USLT".toByteArray() + synchsafe(10_000) + byteArrayOf(0, 0) + ByteArray(6)
        assertNull(read(bad))
        // Unknown text encoding byte.
        assertNull(read(id3(4, listOf("USLT" to (byteArrayOf(9) + "eng".toByteArray() + byteArrayOf(0) + "x".toByteArray())))))
    }
}
