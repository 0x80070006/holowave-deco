package io.github.x80070006.holowave.tube

import android.graphics.Bitmap
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.x80070006.holowave.playback.PositionClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * The embedded YouTube Music page seen as a Media3 [Player], so that [io.github.x80070006.holowave.playback.PlaybackService]
 * can give it a media session: the notification, the lock screen and Bluetooth buttons then work for it
 * exactly as for local files, and the system keeps the service in the foreground while it plays.
 */
@OptIn(UnstableApi::class)
class TubePlayer(private val host: TubeHost, scope: CoroutineScope) : SimpleBasePlayer(Looper.getMainLooper()) {

    private var coverFor: Bitmap? = null
    private var coverBytes: ByteArray? = null

    init {
        scope.launch { host.state.collect { invalidateState() } }
    }

    private fun coverData(b: Bitmap?): ByteArray? {
        if (b == null) return null
        if (b !== coverFor) {
            coverFor = b
            coverBytes = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        }
        return coverBytes
    }

    override fun getState(): State {
        val s = host.state.value
        val commands = Player.Commands.Builder().addAll(
            Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_METADATA, Player.COMMAND_GET_TIMELINE,
        ).build()
        if (!s.hasTrack) return State.Builder().setAvailableCommands(commands).setPlaybackState(Player.STATE_IDLE).build()

        val metadata = MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).setIsPlayable(true).setIsBrowsable(false).apply {
            coverData(s.artwork)?.let { setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
        }.build()
        val item = MediaItemData.Builder(s.videoId ?: "tube")
            .setMediaItem(MediaItem.Builder().setMediaId(s.videoId ?: "tube").setMediaMetadata(metadata).build())
            .setMediaMetadata(metadata)
            .setDurationUs(if (s.durationMs > 0) s.durationMs * 1000 else C.TIME_UNSET)
            .setIsSeekable(s.durationMs > 0)
            .build()
        val position = PositionClock.estimate(s.positionMs, s.positionAt, 1f, s.playing, SystemClock.elapsedRealtime(), s.durationMs)
        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(listOf(item))
            .setCurrentMediaItemIndex(0)
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(s.playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setContentPositionMs(if (s.playing) PositionSupplier.getExtrapolating(position, 1f) else PositionSupplier.getConstant(position))
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) host.play() else host.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        host.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> host.next()
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> host.previous()
            else -> host.seekTo(positionMs)
        }
        return Futures.immediateVoidFuture()
    }
}
