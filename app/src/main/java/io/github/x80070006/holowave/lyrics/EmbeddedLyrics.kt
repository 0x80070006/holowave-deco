package io.github.x80070006.holowave.lyrics

import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Extracts lyrics stored inside audio files, without any third-party tag library:
 *  - ID3v2.3 / v2.4 (MP3): USLT frames and TXXX frames named LYRICS / SYNCEDLYRICS
 *  - FLAC: Vorbis comments LYRICS / SYNCEDLYRICS / UNSYNCEDLYRICS
 *
 * Every length read from the file is bounds-checked before allocating, so a
 * malformed or hostile file cannot make the app allocate unbounded memory.
 */
object EmbeddedLyrics {
    private const val MAX_TAG_BYTES = 16 * 1024 * 1024
    private const val MAX_FLAC_BLOCK = 8 * 1024 * 1024
    private val lyricKeys = setOf("LYRICS", "SYNCEDLYRICS", "UNSYNCEDLYRICS", "LYRICS-ENG")

    fun read(input: InputStream): String? = try {
        val data = DataInputStream(input.buffered())
        val magic = ByteArray(4)
        data.readFully(magic)
        when {
            magic[0] == 'I'.code.toByte() && magic[1] == 'D'.code.toByte() && magic[2] == '3'.code.toByte() ->
                readId3(data, magic[3].toInt() and 0xFF)
            magic.contentEquals("fLaC".toByteArray(Charsets.US_ASCII)) -> readFlac(data)
            else -> null
        }
    } catch (_: EOFException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    // ---------------------------------------------------------------- ID3v2

    private fun readId3(data: DataInputStream, major: Int): String? {
        if (major != 3 && major != 4) return null
        data.readUnsignedByte() // revision
        val flags = data.readUnsignedByte()
        val size = synchsafe(data.readInt())
        if (size <= 0 || size > MAX_TAG_BYTES) return null
        var tag = ByteArray(size)
        data.readFully(tag)
        if (major == 3 && flags and 0x80 != 0) tag = unsynchronise(tag)

        var pos = 0
        if (flags and 0x40 != 0 && tag.size >= 4) { // extended header
            val ext = if (major == 4) synchsafe(tag.int(0)) else tag.int(0) + 4
            if (ext < 0 || ext > tag.size) return null
            pos = ext
        }

        var best: String? = null
        while (pos + 10 <= tag.size) {
            val id = String(tag, pos, 4, Charsets.ISO_8859_1)
            if (id[0] == '\u0000') break // padding
            val rawSize = tag.int(pos + 4)
            val frameSize = if (major == 4) synchsafe(rawSize) else rawSize
            val frameFlags = ((tag[pos + 8].toInt() and 0xFF) shl 8) or (tag[pos + 9].toInt() and 0xFF)
            val start = pos + 10
            if (frameSize <= 0 || frameSize > tag.size - start) break
            pos = start + frameSize
            if (id != "USLT" && id != "TXXX") continue

            var body = tag.copyOfRange(start, start + frameSize)
            if (major == 4) {
                if (frameFlags and 0x000C != 0) continue // compressed or encrypted: skip
                if (frameFlags and 0x0001 != 0) { // data length indicator
                    if (body.size < 4) continue
                    body = body.copyOfRange(4, body.size)
                }
                if (frameFlags and 0x0002 != 0) body = unsynchronise(body)
            } else if (frameFlags and 0x00C0 != 0) {
                continue // v2.3 compression / encryption
            }
            val text = when (id) {
                "USLT" -> parseUslt(body)
                else -> parseTxxx(body)
            } ?: continue
            // Prefer timestamped text when a file carries both flavours.
            if (best == null || (!Lrc.looksSynced(best) && Lrc.looksSynced(text))) best = text
        }
        return best
    }

    private fun parseUslt(body: ByteArray): String? {
        if (body.size < 5) return null
        val charset = id3Charset(body[0].toInt()) ?: return null
        val descEnd = terminator(body, 4, body[0].toInt())
        if (descEnd < 0) return null
        return decode(body, descEnd + terminatorLength(body[0].toInt()), body.size, charset)
    }

    private fun parseTxxx(body: ByteArray): String? {
        if (body.size < 2) return null
        val encoding = body[0].toInt()
        val charset = id3Charset(encoding) ?: return null
        val descEnd = terminator(body, 1, encoding)
        if (descEnd < 0) return null
        val desc = decode(body, 1, descEnd, charset)?.trim()?.uppercase() ?: return null
        if (desc !in lyricKeys) return null
        return decode(body, descEnd + terminatorLength(encoding), body.size, charset)
    }

    private fun id3Charset(encoding: Int): Charset? = when (encoding) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> null
    }

    private fun terminatorLength(encoding: Int) = if (encoding == 1 || encoding == 2) 2 else 1

    /** Index of the string terminator starting at [from], or -1. UTF-16 terminators are 2-byte aligned. */
    private fun terminator(b: ByteArray, from: Int, encoding: Int): Int {
        if (encoding == 1 || encoding == 2) {
            var i = from
            while (i + 1 < b.size) {
                if (b[i] == 0.toByte() && b[i + 1] == 0.toByte()) return i
                i += 2
            }
            return -1
        }
        for (i in from until b.size) if (b[i] == 0.toByte()) return i
        return -1
    }

    private fun decode(b: ByteArray, from: Int, to: Int, charset: Charset): String? {
        if (from > to || from < 0 || to > b.size) return null
        val s = String(b, from, to - from, charset).trimEnd('\u0000').replace("\r\n", "\n").replace('\r', '\n')
        return s.takeIf { it.isNotBlank() }
    }

    private fun unsynchronise(src: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream(src.size)
        var i = 0
        while (i < src.size) {
            out.write(src[i].toInt())
            if (src[i] == 0xFF.toByte() && i + 1 < src.size && src[i + 1] == 0.toByte()) i++
            i++
        }
        return out.toByteArray()
    }

    private fun synchsafe(v: Int): Int {
        if (v and 0x80808080.toInt() != 0) return -1
        return ((v shr 24) and 0x7F shl 21) or ((v shr 16) and 0x7F shl 14) or ((v shr 8) and 0x7F shl 7) or (v and 0x7F)
    }

    private fun ByteArray.int(at: Int): Int =
        ((this[at].toInt() and 0xFF) shl 24) or ((this[at + 1].toInt() and 0xFF) shl 16) or
            ((this[at + 2].toInt() and 0xFF) shl 8) or (this[at + 3].toInt() and 0xFF)

    // ----------------------------------------------------------------- FLAC

    private fun readFlac(data: DataInputStream): String? {
        repeat(64) {
            val header = data.readUnsignedByte()
            val last = header and 0x80 != 0
            val type = header and 0x7F
            val length = (data.readUnsignedByte() shl 16) or (data.readUnsignedByte() shl 8) or data.readUnsignedByte()
            if (type == 4) {
                if (length > MAX_FLAC_BLOCK) return null
                val block = ByteArray(length)
                data.readFully(block)
                return vorbisLyrics(block)
            }
            skipFully(data, length.toLong())
            if (last) return null
        }
        return null
    }

    private fun vorbisLyrics(block: ByteArray): String? {
        var pos = 0
        fun le32(): Int {
            if (pos + 4 > block.size) throw IllegalArgumentException("truncated")
            val v = (block[pos].toInt() and 0xFF) or ((block[pos + 1].toInt() and 0xFF) shl 8) or
                ((block[pos + 2].toInt() and 0xFF) shl 16) or ((block[pos + 3].toInt() and 0xFF) shl 24)
            pos += 4
            return v
        }
        val vendor = le32()
        if (vendor < 0 || vendor > block.size - pos) return null
        pos += vendor
        val count = le32()
        if (count < 0) return null
        var best: String? = null
        repeat(minOf(count, 10_000)) {
            val len = le32()
            if (len < 0 || len > block.size - pos) return best
            val entry = String(block, pos, len, Charsets.UTF_8)
            pos += len
            val eq = entry.indexOf('=')
            if (eq > 0 && entry.substring(0, eq).uppercase() in lyricKeys) {
                val value = entry.substring(eq + 1).replace("\r\n", "\n")
                if (value.isNotBlank() && (best == null || (!Lrc.looksSynced(best!!) && Lrc.looksSynced(value)))) best = value
            }
        }
        return best
    }

    private fun skipFully(input: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() < 0) throw EOFException()
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }
}
