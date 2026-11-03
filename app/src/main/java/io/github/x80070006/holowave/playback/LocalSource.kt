package io.github.x80070006.holowave.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The files of the device, played by [PlaybackService]. */
class LocalSource(private val player: PlayerConnection, scope: CoroutineScope) : PlaybackSource {
    override val kind = SourceKind.LOCAL

    override val state: StateFlow<SourceState> = player.state
        .map {
            SourceState(
                kind = SourceKind.LOCAL, available = it.connected, title = it.title, artist = it.artist,
                durationMs = it.durationMs, isPlaying = it.isPlaying, isBuffering = it.isBuffering,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, SourceState(SourceKind.LOCAL))

    override fun position(): Long = player.position.value

    override fun play() { if (!state.value.isPlaying) player.togglePlay() }
    override fun pause() { if (state.value.isPlaying) player.togglePlay() }
    override fun next() = player.next()
    override fun previous() = player.previous()
    override fun seekTo(ms: Long) = player.seekTo(ms)
}
