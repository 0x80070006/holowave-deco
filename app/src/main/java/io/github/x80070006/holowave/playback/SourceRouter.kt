package io.github.x80070006.holowave.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The single place the UI asks what is playing and how to control it: it follows
 * whichever of the two sources was started last, and silences the other one.
 */
class SourceRouter(private val local: PlaybackSource, private val noutube: PlaybackSource) {
    private val _active = MutableStateFlow(SourceKind.LOCAL)
    val active: StateFlow<SourceKind> = _active.asStateFlow()

    private val _now = MutableStateFlow(local.state.value)
    /** State of the active source. */
    val now: StateFlow<SourceState> = _now.asStateFlow()

    val activeSource: PlaybackSource get() = source(_active.value)

    fun source(kind: SourceKind): PlaybackSource = if (kind == SourceKind.LOCAL) local else noutube

    fun start(scope: CoroutineScope) {
        var wasLocal = false
        var wasNoutube = false
        scope.launch {
            combine(local.state, noutube.state) { l, n -> l to n }.collect { (l, n) ->
                val d = Handoff.decide(_active.value, l.isPlaying, n.isPlaying, wasLocal, wasNoutube)
                wasLocal = l.isPlaying
                wasNoutube = n.isPlaying
                d.pause?.let { source(it).pause() }
                val shown = Handoff.withTrack(d.active, l.hasTrack, n.hasTrack)
                _active.value = shown
                _now.value = if (shown == SourceKind.LOCAL) l else n
            }
        }
    }
}
