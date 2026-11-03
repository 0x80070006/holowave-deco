package io.github.x80070006.holowave.tube

import android.os.SystemClock
import io.github.x80070006.holowave.playback.PlaybackSource
import io.github.x80070006.holowave.playback.PositionClock
import io.github.x80070006.holowave.playback.SourceKind
import io.github.x80070006.holowave.playback.SourceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** YouTube Music played by the embedded page. */
class TubeSource(private val host: TubeHost, scope: CoroutineScope) : PlaybackSource {
    override val kind = SourceKind.NOUTUBE

    override val state: StateFlow<SourceState> = host.state
        .map {
            SourceState(
                kind = SourceKind.NOUTUBE, available = true, title = it.title, artist = it.artist,
                durationMs = it.durationMs, isPlaying = it.playing, artwork = it.artwork,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, SourceState(SourceKind.NOUTUBE, available = true))

    override fun position(): Long {
        val s = host.state.value
        return PositionClock.estimate(s.positionMs, s.positionAt, 1f, s.playing, SystemClock.elapsedRealtime(), s.durationMs)
    }

    override fun play() = host.play()
    override fun pause() = host.pause()
    override fun next() = host.next()
    override fun previous() = host.previous()
    override fun seekTo(ms: Long) = host.seekTo(ms)
}

/**
 * The YouTube side the app follows: the embedded page when it has a song, otherwise the NouTube
 * app (seen through notification access) when that one has one.
 */
class YoutubeSource(
    private val embedded: PlaybackSource,
    private val external: PlaybackSource,
    scope: CoroutineScope,
) : PlaybackSource {
    override val kind = SourceKind.NOUTUBE

    override val state: StateFlow<SourceState> = combine(embedded.state, external.state) { e, x ->
        if (e.hasTrack || !x.hasTrack) e else x
    }.stateIn(scope, SharingStarted.Eagerly, embedded.state.value)

    private fun current(): PlaybackSource = if (embedded.state.value.hasTrack || !external.state.value.hasTrack) embedded else external

    override fun position() = current().position()
    override fun play() = current().play()
    override fun pause() = current().pause()
    override fun next() = current().next()
    override fun previous() = current().previous()
    override fun seekTo(ms: Long) = current().seekTo(ms)
}
