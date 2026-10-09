package io.github.x80070006.holowave.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.x80070006.holowave.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val connected: Boolean = false,
    val currentId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val durationMs: Long = 0L,
    val queueIds: List<Long> = emptyList(),
    val queueIndex: Int = -1,
)

/** UI-side handle to [PlaybackService], exposing the player as observable state. */
class PlayerConnection(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    /** Playback position, refreshed ~10 times per second while playing (lyrics need the precision). */
    val position: StateFlow<Long> = _position.asStateFlow()

    private var controller: MediaController? = null
    private var ticker: Job? = null
    private val pending = ArrayList<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            publish(c)
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        ticker?.cancel()
        controller?.run {
            removeListener(listener)
            release()
        }
        controller = null
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else {
            pending += action
            connect()
        }
    }

    fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        withController { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(tracks.map(::toMediaItem), startIndex.coerceIn(0, tracks.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playNext(track: Track) = withController { c ->
        if (c.mediaItemCount == 0) play(listOf(track), 0)
        else c.addMediaItem((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), toMediaItem(track))
    }

    fun enqueue(track: Track) = withController { c ->
        if (c.mediaItemCount == 0) play(listOf(track), 0) else c.addMediaItem(toMediaItem(track))
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun skipTo(index: Int) = withController { c -> if (index in 0 until c.mediaItemCount) c.seekToDefaultPosition(index) }

    fun seekTo(ms: Long) = withController { c ->
        c.seekTo(ms.coerceAtLeast(0L))
        _position.value = ms
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun publish(p: Player) {
        val item = p.currentMediaItem
        val ids = (0 until p.mediaItemCount).mapNotNull { p.getMediaItemAt(it).mediaId.toLongOrNull() }
        _state.value = PlaybackState(
            connected = true,
            currentId = item?.mediaId?.toLongOrNull(),
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            isPlaying = p.isPlaying,
            isBuffering = p.playbackState == Player.STATE_BUFFERING,
            shuffle = p.shuffleModeEnabled,
            repeat = when (p.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
            durationMs = p.duration.takeIf { it > 0 } ?: item?.mediaMetadata?.durationMs ?: 0L,
            queueIds = ids,
            queueIndex = p.currentMediaItemIndex,
        )
        _position.value = p.currentPosition
        if (p.isPlaying) startTicker() else ticker?.cancel()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                controller?.let { _position.value = it.currentPosition }
                delay(100)
            }
        }
    }

    private fun toMediaItem(t: Track): MediaItem = MediaItem.Builder()
        .setMediaId(t.id.toString())
        .setUri(t.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t.title)
                .setArtist(t.artist)
                .setAlbumTitle(t.album)
                .setDurationMs(t.durationMs)
                .setArtworkUri(t.uri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(Bundle.EMPTY)
                .build(),
        )
        .build()
}
