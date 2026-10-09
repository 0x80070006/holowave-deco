package io.github.x80070006.holowave.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

/** One audio file indexed by MediaStore. Immutable, safe to share across threads. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val folder: String,
    val dateAdded: Long,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}

enum class CollectionKind { ALBUM, ARTIST, FOLDER, PLAYLIST, FAVORITES }

/** A group of tracks shown as a card: an album, an artist, a folder or a playlist. */
data class TrackCollection(
    val kind: CollectionKind,
    val key: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
) {
    val cover: Track? get() = tracks.firstOrNull()
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

fun formatLongDuration(ms: Long): String {
    val totalMinutes = ms.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours} h ${"%02d".format(minutes)}" else "$minutes min"
}
