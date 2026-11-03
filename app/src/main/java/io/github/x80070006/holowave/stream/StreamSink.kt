package io.github.x80070006.holowave.stream

import android.graphics.Bitmap
import io.github.x80070006.holowave.HolowaveApp
import io.github.x80070006.holowave.data.StreamKey
import io.github.x80070006.holowave.data.TrackSource
import io.github.x80070006.holowave.history.Listen
import io.github.x80070006.holowave.history.ListenSubject
import io.github.x80070006.holowave.history.Outcome
import io.github.x80070006.holowave.history.StreamedSighting

/**
 * Where a finished listen of a streamed song goes: the list of songs heard in NouTube and the
 * listening history. [embedded] listens (played inside HOLOWAVE) are always kept in the list;
 * the ones seen from the NouTube app need the user's opt-in (notification access).
 */
class StreamSink(private val app: HolowaveApp, private val embedded: Boolean) {
    fun finished(heard: Heard, listen: Listen) {
        val settings = app.settings.state.value
        if (!embedded && !settings.captureStreaming) return
        val now = System.currentTimeMillis()
        // A song only enters the list once it was really listened to, not on a skip.
        if (listen.listenedMs >= MIN_LISTEN_MS || listen.outcome == Outcome.COMPLETED) {
            app.streamed.record(
                StreamedSighting(heard.title, heard.artist, heard.album, heard.durationMs, heard.videoId, heard.artwork as? Bitmap),
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

    private companion object { const val MIN_LISTEN_MS = 20_000L }
}
