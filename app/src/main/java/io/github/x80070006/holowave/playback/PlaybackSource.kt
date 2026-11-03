package io.github.x80070006.holowave.playback

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

enum class SourceKind { LOCAL, NOUTUBE }

/** What one source is playing right now, in the same shape whatever the source. */
data class SourceState(
    val kind: SourceKind,
    /** False when the source cannot be driven (NouTube absent, closed, or notification access missing). */
    val available: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val artwork: Bitmap? = null,
) {
    val hasTrack: Boolean get() = title.isNotBlank()
}

/** One thing that can play music and be told what to do. The UI never talks to a player directly. */
interface PlaybackSource {
    val kind: SourceKind
    val state: StateFlow<SourceState>

    /** Current position in ms, extrapolated between updates; cheap enough to call every frame. */
    fun position(): Long

    fun play()
    fun pause()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
}
