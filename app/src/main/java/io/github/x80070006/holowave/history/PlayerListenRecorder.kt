package io.github.x80070006.holowave.history

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.x80070006.holowave.data.TrackKey
import io.github.x80070006.holowave.data.TrackSource

/**
 * Feeds a [ListenTracker] from a Media3 player, so listens are recorded even when the
 * screen is off and the app UI is closed (the player lives in the playback service).
 *
 * Media3 order of events: for a track change, `onPositionDiscontinuity` (the track being
 * left, with the position it was left at) comes before `onMediaItemTransition` (the new one).
 */
class PlayerListenRecorder(
    private val player: Player,
    private val log: ListenLog,
    private val enabled: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) : Player.Listener {

    private var subject: ListenSubject? = null
    private val tracker = ListenTracker { listen ->
        val s = subject
        if (s != null && enabled()) log.record(s, listen)
    }

    fun attach() {
        player.addListener(this)
        // The player may already be mid-track when the service restarts.
        player.currentMediaItem?.let { begin(it) }
    }

    /** Flushes the session in progress (service stopping). */
    fun detach() {
        player.removeListener(this)
        finish(EndReason.STOPPED, player.currentPosition)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        when {
            mediaItem == null -> finish(EndReason.STOPPED, 0)
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT && tracker.isTracking ->
                tracker.repeated(durationOf(mediaItem))
            else -> begin(mediaItem)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (!tracker.isTracking || oldPosition.mediaItem == null) return
        val sameItem = oldPosition.mediaItemIndex == newPosition.mediaItemIndex && oldPosition.mediaItem == newPosition.mediaItem
        when (reason) {
            // Looping the same item is reported as a repeat transition right after.
            Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> if (!sameItem) finish(EndReason.NATURAL, oldPosition.positionMs)
            Player.DISCONTINUITY_REASON_SEEK, Player.DISCONTINUITY_REASON_SKIP ->
                if (sameItem) tracker.seeked(oldPosition.positionMs, newPosition.positionMs)
                else finish(EndReason.USER_SKIP, oldPosition.positionMs)
            Player.DISCONTINUITY_REASON_REMOVE -> finish(EndReason.REPLACED, oldPosition.positionMs)
            else -> Unit
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        tracker.playingChanged(isPlaying, player.currentPosition)
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (state == Player.STATE_ENDED) finish(EndReason.NATURAL, durationOf(player.currentMediaItem))
    }

    private fun begin(item: MediaItem) {
        finish(EndReason.REPLACED, player.currentPosition)
        val m = item.mediaMetadata
        val title = m.title?.toString().orEmpty()
        val artist = m.artist?.toString().orEmpty()
        val album = m.albumTitle?.toString().orEmpty()
        val duration = durationOf(item)
        subject = ListenSubject(
            trackKey = TrackKey.of(artist, title, album, duration),
            title = title,
            artist = artist,
            album = album,
            source = TrackSource.of(m.extras?.getInt(EXTRA_SOURCE, 0) ?: 0),
        )
        tracker.trackStarted(now(), duration, player.currentPosition, player.isPlaying)
    }

    private fun finish(reason: EndReason, positionMs: Long) {
        if (tracker.isTracking) tracker.trackEnded(reason, positionMs)
    }

    private fun durationOf(item: MediaItem?): Long =
        item?.mediaMetadata?.durationMs?.takeIf { it > 0 }
            ?: player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: 0L

    companion object {
        const val EXTRA_SOURCE = "holowave.source"
    }
}
