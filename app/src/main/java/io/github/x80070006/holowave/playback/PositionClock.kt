package io.github.x80070006.holowave.playback

/**
 * Media sessions report a position and the moment it was true; between reports the position
 * is extrapolated from the playback speed. Pure so it can be tested without a device.
 */
object PositionClock {
    fun estimate(positionMs: Long, updatedAtMs: Long, speed: Float, playing: Boolean, nowMs: Long, durationMs: Long): Long {
        // Web players report -1 (PLAYBACK_POSITION_UNKNOWN) before the first position is known.
        if (positionMs < 0) return 0L
        val elapsed = if (playing) ((nowMs - updatedAtMs).coerceAtLeast(0L) * speed).toLong() else 0L
        val at = positionMs + elapsed
        return if (durationMs > 0) at.coerceIn(0L, durationMs) else at.coerceAtLeast(0L)
    }
}
