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

    /**
     * Adds the account's most played songs (already ranked). The play counter keeps the larger of the
     * known and the imported value, and the rank orders songs with the same count. Covers are fetched
     * from YouTube's image hosts, one by one.
     */
    fun importRanked(songs: List<io.github.x80070006.holowave.tube.LibrarySong>, now: Long) {
        scope.launch(Dispatchers.IO) {
            for (s in songs) {
                val key = StreamKey.of(s.artist, s.title)
                val old = db.streamed().find(key)
                val cover = if (old?.artwork == null) io.github.x80070006.holowave.tube.Covers.fetch(s.thumbnail) else null
                val art = cover?.let { saveArtwork(key, it) } ?: old?.artwork
                // Newer than anything heard by rank: rank 0 is the most played.
                val seen = now - s.rank * 1_000L
                if (old == null) {
                    db.streamed().insert(
                        StreamedEntity(
                            trackKey = key, title = s.title.take(MAX_TEXT), artist = s.artist.take(MAX_TEXT), album = s.album.take(MAX_TEXT),
                            durationMs = s.durationSec * 1000L, videoId = s.videoId, artwork = art,
                            firstSeen = seen, lastSeen = seen, plays = s.plays,
                        ),
                    )
                } else {
                    db.streamed().update(
                        old.copy(
                            videoId = old.videoId ?: s.videoId, artwork = art, plays = maxOf(old.plays, s.plays),
                            durationMs = if (old.durationMs > 0) old.durationMs else s.durationSec * 1000L,
                            album = old.album.ifBlank { s.album.take(MAX_TEXT) },
                        ),
                    )
                }
            }
        }
    }

    /** Replaces the cover of a song already in the list; does nothing for a song that is not. */
    fun updateArtwork(title: String, artist: String, bitmap: Bitmap) {
        val key = StreamKey.of(artist, title)
        scope.launch(Dispatchers.IO) {
            val old = db.streamed().find(key) ?: return@launch
            val name = saveArtwork(key, bitmap) ?: return@launch
            db.streamed().update(old.copy(artwork = name, lastSeen = old.lastSeen + 1))
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
        run {
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
