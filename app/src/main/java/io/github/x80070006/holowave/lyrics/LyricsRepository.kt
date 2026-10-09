package io.github.x80070006.holowave.lyrics

import android.content.ContentResolver
import android.net.Uri
import android.util.AtomicFile
import android.util.Log
import io.github.x80070006.holowave.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

sealed interface LyricsLookup {
    data class Available(val lyrics: Lyrics) : LyricsLookup
    data object Missing : LyricsLookup
    /** Not on the device and online lookup is off or the network failed. */
    data object Offline : LyricsLookup
}

/**
 * Resolves lyrics, offline first:
 *   1. lyrics the user imported (.lrc) for this track
 *   2. lyrics embedded in the audio file tags
 *   3. lyrics previously downloaded from LRCLIB (kept forever on device)
 *   4. LRCLIB over HTTPS, if the user allows it, then cached for offline use
 */
class LyricsRepository(
    private val resolver: ContentResolver,
    filesDir: File,
    private val client: LrcLibClient,
) {
    private val dir = File(filesDir, "lyrics").apply { mkdirs() }
    private val memory = HashMap<String, LyricsLookup>()
    private val lock = Mutex()

    suspend fun lyricsFor(track: Track, allowNetwork: Boolean): LyricsLookup {
        val key = keyOf(track)
        lock.withLock { memory[key] }?.let { if (it !is LyricsLookup.Offline || !allowNetwork) return it }

        val result = withContext(Dispatchers.IO) { resolve(track, key, allowNetwork) }
        lock.withLock { memory[key] = result }
        return result
    }

    private suspend fun resolve(track: Track, key: String, allowNetwork: Boolean): LyricsLookup {
        readEntry(key, imported = true)?.let { return it }
        readEmbedded(track.uri)?.let { return LyricsLookup.Available(it) }
        readEntry(key, imported = false)?.let { return it }
        if (!allowNetwork) return LyricsLookup.Offline
        return fetch(track, key)
    }

    /** Downloads lyrics for tracks that have none on the device. Returns how many were found. */
    suspend fun prefetch(track: Track): Boolean = withContext(Dispatchers.IO) {
        val key = keyOf(track)
        if (entryFile(key, true).exists() || entryFile(key, false).exists()) return@withContext true
        if (readEmbedded(track.uri) != null) return@withContext true
        val result = fetch(track, key)
        lock.withLock { memory[key] = result }
        result is LyricsLookup.Available
    }

    suspend fun import(track: Track, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val text = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                val bytes = input.readNBytesCompat(Lrc.MAX_INPUT_CHARS * 2)
                String(bytes, Charsets.UTF_8).removePrefix("﻿")
            }
        }.getOrNull() ?: return@withContext false
        val lyrics = Lrc.parse(text, LyricsSource.IMPORTED) ?: return@withContext false
        val key = keyOf(track)
        writeEntry(key, imported = true, lyrics)
        lock.withLock { memory[key] = LyricsLookup.Available(lyrics) }
        true
    }

    suspend fun forget(track: Track) = withContext(Dispatchers.IO) {
        val key = keyOf(track)
        entryFile(key, true).delete()
        entryFile(key, false).delete()
        lock.withLock { memory.remove(key) }
    }

    suspend fun clearDownloaded() = withContext(Dispatchers.IO) {
        dir.listFiles()?.filter { it.name.startsWith("dl-") }?.forEach { it.delete() }
        lock.withLock { memory.clear() }
    }

    fun storedCount(): Int = dir.listFiles()?.count { it.name.endsWith(".json") } ?: 0

    private suspend fun fetch(track: Track, key: String): LyricsLookup =
        when (val r = client.lookup(track)) {
            is LrcLibClient.Result.Found -> {
                val lyrics = when {
                    r.synced != null -> Lrc.parse(r.synced, LyricsSource.LRCLIB)
                    r.plain != null -> Lrc.parse(r.plain, LyricsSource.LRCLIB)
                    else -> null
                } ?: if (r.instrumental) Lyrics.Instrumental(LyricsSource.LRCLIB) else null
                if (lyrics != null) {
                    writeEntry(key, imported = false, lyrics)
                    LyricsLookup.Available(lyrics)
                } else {
                    writeMissing(key)
                    LyricsLookup.Missing
                }
            }
            LrcLibClient.Result.NotFound -> {
                writeMissing(key)
                LyricsLookup.Missing
            }
            LrcLibClient.Result.Unavailable -> LyricsLookup.Offline
        }

    private fun readEmbedded(uri: Uri): Lyrics? = runCatching {
        resolver.openInputStream(uri)?.use { EmbeddedLyrics.read(it) }
    }.getOrNull()?.let { Lrc.parse(it, LyricsSource.EMBEDDED) }

    // ------------------------------------------------------------- storage

    private fun entryFile(key: String, imported: Boolean) = File(dir, (if (imported) "user-" else "dl-") + key + ".json")

    private fun readEntry(key: String, imported: Boolean): LyricsLookup? {
        val file = entryFile(key, imported)
        if (!file.exists()) return null
        return runCatching {
            val o = JSONObject(String(AtomicFile(file).readFully(), Charsets.UTF_8))
            val source = if (imported) LyricsSource.IMPORTED else LyricsSource.LRCLIB
            when (o.optString("kind")) {
                "none" -> {
                    // Re-ask LRCLIB once in a while: the database keeps growing.
                    if (System.currentTimeMillis() - o.optLong("at") > MISSING_TTL_MS) null else LyricsLookup.Missing
                }
                "instrumental" -> LyricsLookup.Available(Lyrics.Instrumental(source))
                else -> Lrc.parse(o.optString("text"), source)?.let { LyricsLookup.Available(it) }
            }
        }.getOrElse {
            Log.w(TAG, "Dropping unreadable lyrics entry")
            file.delete()
            null
        }
    }

    private fun writeEntry(key: String, imported: Boolean, lyrics: Lyrics) {
        val o = JSONObject().put("at", System.currentTimeMillis())
        when (lyrics) {
            is Lyrics.Synced -> o.put("kind", "synced").put("text", Lrc.format(lyrics.lines))
            is Lyrics.Plain -> o.put("kind", "plain").put("text", lyrics.text)
            is Lyrics.Instrumental -> o.put("kind", "instrumental")
        }
        write(entryFile(key, imported), o)
    }

    private fun writeMissing(key: String) =
        write(entryFile(key, false), JSONObject().put("kind", "none").put("at", System.currentTimeMillis()))

    private fun write(file: File, o: JSONObject) {
        val atomic = AtomicFile(file)
        val stream = runCatching { atomic.startWrite() }.getOrNull() ?: return
        try {
            stream.write(o.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            atomic.failWrite(stream)
        }
    }

    companion object {
        private const val TAG = "LyricsRepository"
        private const val MISSING_TTL_MS = 14L * 24 * 3600 * 1000

        /** Stable identity that survives a MediaStore re-index (ids can change, tags do not). */
        fun keyOf(track: Track): String {
            val identity = listOf(track.artist, track.title, track.album)
                .joinToString("\u001F") { it.trim().lowercase(Locale.ROOT) } + "\u001F" + (track.durationMs / 2000)
            val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            return digest.take(16).joinToString("") { "%02x".format(it) }
        }

        private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (out.size() < limit) {
                val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
    }
}
