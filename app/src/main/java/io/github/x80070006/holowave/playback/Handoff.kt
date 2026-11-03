package io.github.x80070006.holowave.playback

/**
 * Decides which source the app follows and which one must be silenced when two play at once.
 * Pure Kotlin: the rule is the whole point of the local to YouTube hand-over, so it is tested.
 */
object Handoff {
    class Decision(val active: SourceKind, val pause: SourceKind?)

    /** A source with nothing loaded cannot be the one the app shows: fall back to the other if it has a song. */
    fun withTrack(active: SourceKind, localHasTrack: Boolean, noutubeHasTrack: Boolean): SourceKind = when {
        active == SourceKind.LOCAL && !localHasTrack && noutubeHasTrack -> SourceKind.NOUTUBE
        active == SourceKind.NOUTUBE && !noutubeHasTrack && localHasTrack -> SourceKind.LOCAL
        else -> active
    }

    /**
     * The source that has just started playing takes over and the other one is paused.
     * Without a new start, the app keeps following [active], or the only source playing.
     */
    fun decide(
        active: SourceKind,
        localPlaying: Boolean, noutubePlaying: Boolean,
        wasLocalPlaying: Boolean, wasNoutubePlaying: Boolean,
    ): Decision {
        val localStarted = localPlaying && !wasLocalPlaying
        val noutubeStarted = noutubePlaying && !wasNoutubePlaying
        return when {
            localStarted && !noutubeStarted -> Decision(SourceKind.LOCAL, if (noutubePlaying) SourceKind.NOUTUBE else null)
            noutubeStarted && !localStarted -> Decision(SourceKind.NOUTUBE, if (localPlaying) SourceKind.LOCAL else null)
            // Both started in the same instant: keep the current one and silence the other.
            localStarted && noutubeStarted -> Decision(active, if (active == SourceKind.LOCAL) SourceKind.NOUTUBE else SourceKind.LOCAL)
            active == SourceKind.LOCAL && !localPlaying && noutubePlaying -> Decision(SourceKind.NOUTUBE, null)
            active == SourceKind.NOUTUBE && !noutubePlaying && localPlaying -> Decision(SourceKind.LOCAL, null)
            else -> Decision(active, null)
        }
    }
}
