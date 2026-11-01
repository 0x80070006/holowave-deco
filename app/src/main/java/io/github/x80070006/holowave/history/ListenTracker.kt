package io.github.x80070006.holowave.history

/** How a listening session of one track ended. */
enum class Outcome(val code: Int) {
    /** Played to the end, or at least [ListenTracker.COMPLETE_RATIO] of it. */
    COMPLETED(0),
    /** Skipped by the user before [ListenTracker.EARLY_SKIP_MS]: a strong negative signal. */
    EARLY_SKIP(1),
    /** Left (skip, queue replaced, app closed) after a real listen, without reaching the end. */
    PARTIAL(2);

    companion object {
        fun of(code: Int): Outcome = entries.firstOrNull { it.code == code } ?: PARTIAL
    }
}

/** One finished listening session, before it is stored. */
data class Listen(
    val startedAt: Long,
    val listenedMs: Long,
    val durationMs: Long,
    val outcome: Outcome,
    /** Position where the user left the track, for skips. */
    val leftAtMs: Long,
    /** Times the track looped back to its start within the session (repeat-one). */
    val repeats: Int,
    val seeks: Int,
)

/** Why the player left a track. */
enum class EndReason { NATURAL, USER_SKIP, REPLACED, STOPPED }

/**
 * Turns player callbacks into [Listen] records. Pure Kotlin, driven by explicit
 * timestamps and positions so the rules are unit-testable without a device.
 *
 * Listened time only counts positions advancing while playing: pauses, seeks forward
 * and seeks back never inflate it.
 */
class ListenTracker(private val onListen: (Listen) -> Unit) {

    private var active = false
    private var startedAt = 0L
    private var durationMs = 0L
    private var listenedMs = 0L
    private var playing = false
    private var lastPosition = 0L
    private var repeats = 0
    private var seeks = 0

    val isTracking: Boolean get() = active

    /** A new track begins (or the same one restarts after being finished). */
    fun trackStarted(now: Long, durationMs: Long, positionMs: Long = 0L, isPlaying: Boolean) {
        check(!active) { "finish the previous listen first" }
        active = true
        startedAt = now
        this.durationMs = durationMs.coerceAtLeast(0L)
        listenedMs = 0L
        repeats = 0
        seeks = 0
        playing = isPlaying
        lastPosition = positionMs
    }

    /** Playback started or paused at [positionMs]. */
    fun playingChanged(isPlaying: Boolean, positionMs: Long) {
        if (!active) return
        if (playing) accumulate(positionMs)
        playing = isPlaying
        lastPosition = positionMs
    }

    /** The user moved inside the same track. */
    fun seeked(from: Long, to: Long) {
        if (!active) return
        if (playing) accumulate(from)
        seeks++
        lastPosition = to
    }

    /** The track looped back to its start (repeat-one): counts as a replay, not a new listen. */
    fun repeated(endPositionMs: Long) {
        if (!active) return
        if (playing) accumulate(endPositionMs.coerceAtMost(durationMs))
        repeats++
        lastPosition = 0L
    }

    /** The player left the track at [positionMs]. Emits the [Listen]. */
    fun trackEnded(reason: EndReason, positionMs: Long) {
        if (!active) return
        if (playing) accumulate(positionMs)
        active = false
        playing = false

        val endPosition = if (reason == EndReason.NATURAL) maxOf(positionMs, durationMs) else positionMs
        val total = listenedMs
        val reachedEnd = reason == EndReason.NATURAL ||
            (durationMs > 0 && listenedMs >= durationMs * COMPLETE_RATIO) || repeats > 0
        val outcome = when {
            reachedEnd -> Outcome.COMPLETED
            reason == EndReason.USER_SKIP && listenedMs < EARLY_SKIP_MS -> Outcome.EARLY_SKIP
            else -> Outcome.PARTIAL
        }
        // A track that barely played (opened and left within a blink) says nothing about taste.
        if (total < MIN_RECORDED_MS) return
        onListen(Listen(startedAt, total, durationMs, outcome, endPosition, repeats, seeks))
    }

    private fun accumulate(toPosition: Long) {
        val delta = toPosition - lastPosition
        // Backwards or implausible jumps are seeks, never listening time.
        if (delta in 1..MAX_STEP_MS) listenedMs += delta
        lastPosition = toPosition
    }

    companion object {
        const val EARLY_SKIP_MS = 30_000L
        const val COMPLETE_RATIO = 0.9
        const val MIN_RECORDED_MS = 1_000L
        private const val MAX_STEP_MS = 6 * 60 * 60 * 1000L
    }
}
