package io.github.x80070006.holowave.playback

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.github.x80070006.holowave.HolowaveApp
import io.github.x80070006.holowave.MainActivity
import io.github.x80070006.holowave.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Background playback (screen off included) with the system media controls:
 * notification shade and lock screen, draggable progress, album-tinted artwork
 * and a favourite button. Only trusted controllers (this app, System UI,
 * Bluetooth, the platform) may connect.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val scope = MainScope()
    private val artExecutor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    private val favorites get() = (application as HolowaveApp).playlists

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            // Keeps the CPU awake while the screen is off; local files need no Wi-Fi lock.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.playback_channel)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_holowave) },
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setBitmapLoader(CacheBitmapLoader(DecoArtworkLoader()))
            .setCallback(SessionCallback())
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refreshButtons()
        })
        scope.launch { favorites.state.collect { refreshButtons() } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        artExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun currentTrackId(): Long? = session?.player?.currentMediaItem?.mediaId?.toLongOrNull()

    /** Heart button in the notification / lock screen, reflecting the favourites store. */
    private fun refreshButtons() {
        session?.setMediaButtonPreferences(buttons())
    }

    private fun buttons(): List<CommandButton> {
        val id = currentTrackId()
        val fav = id != null && id in favorites.state.value.favorites
        return listOf(
            CommandButton.Builder(if (fav) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
                .setDisplayName(getString(if (fav) R.string.unfavorite else R.string.favorite))
                .setSessionCommand(TOGGLE_FAVORITE)
                .build(),
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            if (!controller.isTrusted && controller.packageName != packageName) {
                return MediaSession.ConnectionResult.reject()
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(TOGGLE_FAVORITE).build(),
                )
                .setMediaButtonPreferences(buttons())
                .build()
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) = refreshButtons()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == TOGGLE_FAVORITE.customAction) {
                currentTrackId()?.let { favorites.toggleFavorite(it) }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    /** Builds the Art Deco artwork from covers decoded by MediaProvider, MediaStore URIs only. */
    private inner class DecoArtworkLoader : BitmapLoader {
        override fun supportsMimeType(mimeType: String) = true

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
            Futures.immediateFailedFuture(UnsupportedOperationException("Embedded artwork bytes are not used"))

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            val allowed = uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY
            if (!allowed) return Futures.immediateFailedFuture(SecurityException("Artwork outside MediaStore"))
            return artExecutor.submit<Bitmap> {
                val cover = runCatching { contentResolver.loadThumbnail(uri, Size(512, 512), null) }.getOrNull()
                NotificationArt.render(cover)
            }
        }
    }

    private companion object {
        val TOGGLE_FAVORITE = SessionCommand("io.github.x80070006.holowave.TOGGLE_FAVORITE", Bundle.EMPTY)
    }
}
