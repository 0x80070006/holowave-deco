package io.github.x80070006.holowave.data

import java.security.MessageDigest
import java.util.Locale

/** Where a track comes from, shown as a small badge and usable as a filter. */
enum class TrackSource(val code: Int) {
    LOCAL(0), NOUTUBE(1), ONLINE(2);

    companion object {
        const val NOUTUBE_PACKAGE = "jp.nonbili.noutube"
        fun of(code: Int): TrackSource = entries.firstOrNull { it.code == code } ?: LOCAL

        /** NouTube downloads are recognised by the app Android recorded as their owner, not by a folder. */
        fun fromOwner(ownerPackage: String?): TrackSource =
            if (ownerPackage == NOUTUBE_PACKAGE) NOUTUBE else LOCAL
    }
}

/**
 * Identity of a track that survives a MediaStore re-index (ids can change, tags do not).
 * The same function keys cached lyrics and listening history, so it must never change:
 * changing it would orphan data users already have.
 */
object TrackKey {
    fun of(artist: String, title: String, album: String, durationMs: Long): String {
        val identity = listOf(artist, title, album).joinToString("\u001F") { it.trim().lowercase(Locale.ROOT) } +
            "\u001F" + (durationMs / 2000)
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    fun of(track: Track): String = of(track.artist, track.title, track.album, track.durationMs)
}
