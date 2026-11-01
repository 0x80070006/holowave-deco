package io.github.x80070006.holowave.stream

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.x80070006.holowave.HolowaveApp
import io.github.x80070006.holowave.data.StreamKey
import io.github.x80070006.holowave.data.StreamText
import io.github.x80070006.holowave.data.TrackSource
import io.github.x80070006.holowave.history.ListenSubject
import io.github.x80070006.holowave.history.Outcome
import io.github.x80070006.holowave.history.StreamedSighting

/**
 * Listens to the media session of NouTube to learn which songs the user streams.
 *
 * Android only hands out media sessions to apps holding "notification access", which is
 * why this is a notification-listener service. It deliberately ignores every notification:
 * the only thing read is the title, artist, album, duration and cover of NouTube's player.
 * It does nothing at all unless the user switched the capture on in HOLOWAVE's settings.
 */
class NouTubeListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private val app get() = application as HolowaveApp
    private var sessions: MediaSessionManager? = null
    private var controller: MediaController? = null

    private val capture = StreamCapture { heard, listen ->
        val settings = app.settings.state.value
        if (!settings.captureStreaming) return@StreamCapture
        val now = System.currentTimeMillis()
        // A song only enters the NouTube category once it was really listened to, not on a skip.
        if (listen.listenedMs >= MIN_LISTEN_MS || listen.outcome == Outcome.COMPLETED) {
            app.streamed.record(
                StreamedSighting(heard.title, heard.artist, heard.album, heard.durationMs, null, heard.artwork as? Bitmap),
                now, countPlay = true,
            )
        }
        if (settings.recordHistory) {
            app.history.record(
                ListenSubject(StreamKey.of(heard.artist, heard.title), heard.title, heard.artist, heard.album, TrackSource.ONLINE),
                listen,
            )
        }
    }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list -> attach(list) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = announce(metadata)
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (!enabled()) return
            capture.onPlayback(state?.state == PlaybackState.STATE_PLAYING, System.currentTimeMillis())
        }
        override fun onSessionDestroyed() {
            capture.stop(System.currentTimeMillis())
            controller = null
        }
    }

    override fun onListenerConnected() {
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        sessions = manager
        val me = ComponentName(this, NouTubeListenerService::class.java)
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsChanged, me, handler)
            attach(manager.getActiveSessions(me))
        }.onFailure { Log.w(TAG, "Cannot watch media sessions", it) }
    }

    override fun onListenerDisconnected() = detach()

    override fun onDestroy() {
        detach()
        super.onDestroy()
    }

    // Notifications are never read.
    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    private fun enabled() = app.settings.state.value.captureStreaming

    private fun attach(list: List<MediaController>?) {
        val noutube = list?.firstOrNull { it.packageName == NouTubeBridge.PACKAGE }
        if (noutube?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = noutube
        noutube?.let {
            it.registerCallback(callback, handler)
            announce(it.metadata)
            callback.onPlaybackStateChanged(it.playbackState)
        }
    }

    private fun detach() {
        capture.stop(System.currentTimeMillis())
        controller?.unregisterCallback(callback)
        controller = null
        runCatching { sessions?.removeOnActiveSessionsChangedListener(sessionsChanged) }
    }

    private fun announce(m: MediaMetadata?) {
        if (!enabled()) {
            capture.stop(System.currentTimeMillis())
            return
        }
        val c = controller
        val state = c?.playbackState
        val title = m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val heard = if (m == null || title.isBlank()) null else Heard(
            title = StreamText.title(title),
            artist = StreamText.artist(m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()),
            album = m.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty().take(300),
            durationMs = m.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
            artwork = m.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
        )
        capture.onMetadata(heard, System.currentTimeMillis(), state?.state == PlaybackState.STATE_PLAYING)
    }

    private companion object {
        const val TAG = "NouTubeListener"
        const val MIN_LISTEN_MS = 20_000L
    }
}
