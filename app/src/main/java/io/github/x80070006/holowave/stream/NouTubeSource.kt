package io.github.x80070006.holowave.stream

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import io.github.x80070006.holowave.data.StreamText
import io.github.x80070006.holowave.playback.PlaybackSource
import io.github.x80070006.holowave.playback.PositionClock
import io.github.x80070006.holowave.playback.SourceKind
import io.github.x80070006.holowave.playback.SourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * YouTube Music, played by NouTube and driven from here through NouTube's own media session.
 * HOLOWAVE never loads YouTube: it sends play, pause, next, previous and seek to the session
 * and mirrors what the session reports. [attach] is called by [NouTubeListenerService], which
 * is the only component Android allows to obtain that session (notification access).
 */
class NouTubeSource : PlaybackSource {
    override val kind = SourceKind.NOUTUBE

    private val _state = MutableStateFlow(SourceState(SourceKind.NOUTUBE))
    override val state: StateFlow<SourceState> = _state.asStateFlow()

    @Volatile private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = attach(null)
    }

    /** Follows [session], or lets go of the previous one when null. Main thread. */
    fun attach(session: MediaController?) {
        if (session != null && session.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = session
        session?.registerCallback(callback)
        refresh()
    }

    private fun refresh() {
        val c = controller
        if (c == null) {
            _state.value = SourceState(SourceKind.NOUTUBE)
            return
        }
        val m = c.metadata
        val s = c.playbackState
        _state.value = stateOf(
            title = m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = m?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            durationMs = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            playbackState = s?.state ?: PlaybackState.STATE_NONE,
            artwork = m?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
        )
    }

    override fun position(): Long {
        val s = controller?.playbackState ?: return 0L
        val playing = s.state == PlaybackState.STATE_PLAYING
        return PositionClock.estimate(s.position, s.lastPositionUpdateTime, s.playbackSpeed, playing, SystemClock.elapsedRealtime(), _state.value.durationMs)
    }

    override fun play() { controller?.transportControls?.play() }
    override fun pause() { controller?.transportControls?.pause() }
    override fun next() { controller?.transportControls?.skipToNext() }
    override fun previous() { controller?.transportControls?.skipToPrevious() }
    override fun seekTo(ms: Long) { controller?.transportControls?.seekTo(ms.coerceAtLeast(0L)) }

    companion object {
        /** Pure mapping from what a media session reports to [SourceState]; kept apart for tests. */
        fun stateOf(
            title: String, artist: String, album: String, durationMs: Long,
            playbackState: Int, artwork: Bitmap?,
        ) = SourceState(
            kind = SourceKind.NOUTUBE,
            available = true,
            title = StreamText.title(title),
            artist = StreamText.artist(artist),
            album = album.take(300),
            durationMs = durationMs.coerceAtLeast(0L),
            isPlaying = playbackState == PlaybackState.STATE_PLAYING,
            isBuffering = playbackState == PlaybackState.STATE_BUFFERING || playbackState == PlaybackState.STATE_CONNECTING,
            artwork = artwork,
        )
    }
}
