package io.github.x80070006.holowave.history

import android.content.Context
import android.graphics.Bitmap
import io.github.x80070006.holowave.data.StreamKey
import io.github.x80070006.holowave.data.StreamedTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** What the capture knows about a song at the moment it was heard. */
class StreamedSighting(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val videoId: String? = null,
    val artwork: Bitmap? = null,
)

/**
 * Songs listened to in NouTube, kept in the app's private database. Covers are small JPEGs
 * in private storage; nothing here leaves the device.
 */
class StreamedStore(context: Context, private val db: HolowaveDb, private val scope: CoroutineScope) {

    private val artDir = File(context.filesDir, "streamed_art")

    val all: Flow<List<StreamedTrack>> = db.streamed().all().map { rows ->
        rows.map { e ->
            StreamedTrack(
                key = e.trackKey, title = e.title, artist = e.artist, album = e.album, durationMs = e.durationMs,
                videoId = e.videoId, artworkPath = e.artwork?.let { File(artDir, it).takeIf { f -> f.exists() }?.absolutePath },
                firstSeen = e.firstSeen, lastSeen = e.lastSeen, plays = e.plays,
            )
        }
    }

    val count: Flow<Int> = db.streamed().count()

    /** Stores or updates a song; [countPlay] adds one to its play counter. */
    fun record(sighting: StreamedSighting, now: Long, countPlay: Boolean) {
        val key = StreamKey.of(sighting.artist, sighting.title)
        scope.launch(Dispatchers.IO) {
            val artFile = sighting.artwork?.let { saveArtwork(key, it) }
            db.streamed().record(
                StreamedEntity(
                    trackKey = key, title = sighting.title.take(MAX_TEXT), artist = sighting.artist.take(MAX_TEXT),
                    album = sighting.album.take(MAX_TEXT), durationMs = sighting.durationMs, videoId = sighting.videoId,
                    artwork = artFile, firstSeen = now, lastSeen = now, plays = 0,
                ),
                countPlay,
            )
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        db.streamed().deleteAll()
        artDir.listFiles()?.forEach { it.delete() }
    }

    private fun saveArtwork(key: String, bitmap: Bitmap): String? = runCatching {
        artDir.mkdirs()
        val name = "$key.jpg"
        val file = File(artDir, name)
        if (!file.exists()) {
            val scaled = if (maxOf(bitmap.width, bitmap.height) > MAX_ART) {
                val r = MAX_ART.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * r).toInt().coerceAtLeast(1), (bitmap.height * r).toInt().coerceAtLeast(1), true)
            } else bitmap
            file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        }
        name
    }.getOrNull()

    private companion object {
        const val MAX_TEXT = 300
        const val MAX_ART = 384
    }
}
