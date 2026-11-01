package io.github.x80070006.holowave.stream

import io.github.x80070006.holowave.data.StreamKey
import io.github.x80070006.holowave.history.EndReason
import io.github.x80070006.holowave.history.Listen
import io.github.x80070006.holowave.history.ListenTracker

/** What the media session of NouTube says is playing. [artwork] is a Bitmap on Android. */
class Heard(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artwork: Any? = null,
) {
    val key: String get() = StreamKey.of(artist, title)
}

/**
 * Turns media-session events of NouTube into finished listens. Pure Kotlin so the rules
 * (same song re-announced, skips, pauses) can be tested without a device.
 *
 * Time is the wall clock, not the session's reported position: web players reset that
 * position at the very moment they announce the next song, which would lose the listen.
 */
class StreamCapture(private val onFinished: (Heard, Listen) -> Unit) {

    private var current: Heard? = null
    private var startedAt = 0L
    private val tracker = ListenTracker { listen -> current?.let { onFinished(it, listen) } }

    private fun pos(now: Long) = (now - startedAt).coerceAtLeast(0L)

    /** The session announced [meta] at [now] (null when nothing is loaded). */
    fun onMetadata(meta: Heard?, now: Long, playing: Boolean) {
        val cur = current
        if (meta != null && meta.title.isNotBlank() && cur != null && cur.key == meta.key) {
            // Web players re-announce the same song as covers and durations arrive: only fill gaps.
            current = Heard(
                cur.title, cur.artist, cur.album.ifBlank { meta.album },
                if (cur.durationMs > 0) cur.durationMs else meta.durationMs,
                cur.artwork ?: meta.artwork,
            )
            return
        }
        finish(now, stopped = false)
        if (meta == null || meta.title.isBlank()) {
            current = null
            return
        }
        current = meta
        startedAt = now
        tracker.trackStarted(now, meta.durationMs, 0L, playing)
    }

    fun onPlayback(playing: Boolean, now: Long) = tracker.playingChanged(playing, pos(now))

    /** The session ended or capture was switched off. */
    fun stop(now: Long) {
        finish(now, stopped = true)
        current = null
    }

    private fun finish(now: Long, stopped: Boolean) {
        val c = current ?: return
        if (!tracker.isTracking) return
        val p = pos(now)
        val reason = when {
            stopped -> EndReason.STOPPED
            c.durationMs > 0 && p >= c.durationMs - 3_000 -> EndReason.NATURAL
            else -> EndReason.USER_SKIP
        }
        tracker.trackEnded(reason, p)
    }
}
