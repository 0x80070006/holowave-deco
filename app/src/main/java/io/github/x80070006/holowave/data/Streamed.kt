package io.github.x80070006.holowave.data

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * A song listened to in NouTube (YouTube Music) rather than found on the device.
 * HOLOWAVE cannot play it itself: tapping it opens NouTube.
 */
data class StreamedTrack(
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** YouTube video id when known (import), otherwise a search is used to reopen it. */
    val videoId: String?,
    /** Absolute path of the cover kept in the app's private storage, if one was captured. */
    val artworkPath: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val plays: Int,
)

/** One row of a list that mixes files on the device with songs streamed in NouTube. */
sealed interface MixedItem {
    data class Local(val track: Track) : MixedItem
    data class Stream(val track: StreamedTrack) : MixedItem
}

/** Identity of a streamed song: no duration (the web player does not always report it). */
object StreamKey {
    fun of(artist: String, title: String): String {
        val identity = "stream\u001F" + listOf(artist, title).joinToString("\u001F") { it.trim().lowercase(Locale.ROOT) }
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }
}

/** Light clean-up of what YouTube Music puts in its metadata. */
object StreamText {
    private val topic = Regex("""\s*-\s*Topic\s*$""", RegexOption.IGNORE_CASE)
    private val noise = Regex(
        """\s*[(\[]\s*(official|lyrics?|audio|video|visuali[sz]er|hd|4k|remaster(ed)?|music video|clip officiel)[^)\]]*[)\]]\s*""",
        RegexOption.IGNORE_CASE,
    )

    fun artist(raw: String): String = raw.replace(topic, "").trim().take(200)

    fun title(raw: String): String = raw.replace(noise, " ").replace(Regex("\\s+"), " ").trim().take(300)
}

/** Sort orders offered by the library. */
enum class LibrarySort(val label: String) {
    RECENT("Récemment écoutés"),
    AZ("A à Z"),
    ARTIST("Artiste"),
    ADDED("Date d'ajout"),
}

/** Pure sorting and filtering of library lists, kept free of Android so it can be tested. */
object LibrarySorting {
    private val accents = Regex("\\p{Mn}+")

    fun fold(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(accents, "").lowercase(Locale.ROOT).trim()

    fun matches(query: String, vararg fields: String): Boolean {
        val q = fold(query)
        return q.isEmpty() || fields.any { fold(it).contains(q) }
    }

    private val noRank = Int.MAX_VALUE

    fun tracks(list: List<Track>, sort: LibrarySort, rank: (Track) -> Int?): List<Track> = when (sort) {
        LibrarySort.AZ -> list.sortedBy { fold(it.title) }
        LibrarySort.ARTIST -> list.sortedWith(compareBy({ fold(it.artist) }, { fold(it.album) }, { it.trackNumber }, { fold(it.title) }))
        LibrarySort.ADDED -> list.sortedWith(compareByDescending<Track> { it.dateAdded }.thenBy { fold(it.title) })
        LibrarySort.RECENT -> list.sortedWith(compareBy<Track> { rank(it) ?: noRank }.thenBy { fold(it.title) })
    }

    fun collections(list: List<TrackCollection>, sort: LibrarySort, rank: (Track) -> Int?): List<TrackCollection> = when (sort) {
        LibrarySort.AZ -> list.sortedBy { fold(it.title) }
        LibrarySort.ARTIST -> list.sortedWith(
            compareBy<TrackCollection>({ fold(if (it.kind == CollectionKind.ALBUM) it.subtitle else it.title) }, { fold(it.title) }),
        )
        LibrarySort.ADDED -> list.sortedWith(
            compareByDescending<TrackCollection> { c -> c.tracks.maxOfOrNull { it.dateAdded } ?: 0L }.thenBy { fold(it.title) },
        )
        LibrarySort.RECENT -> list.sortedWith(
            compareBy<TrackCollection> { c -> c.tracks.minOfOrNull { rank(it) ?: noRank } ?: noRank }.thenBy { fold(it.title) },
        )
    }

    /** NouTube downloads and streamed songs in one list. */
    fun mixed(list: List<MixedItem>, sort: LibrarySort, rank: (Track) -> Int?, streamRank: (StreamedTrack) -> Int?): List<MixedItem> {
        fun title(m: MixedItem) = fold(when (m) { is MixedItem.Local -> m.track.title; is MixedItem.Stream -> m.track.title })
        fun artist(m: MixedItem) = fold(when (m) { is MixedItem.Local -> m.track.artist; is MixedItem.Stream -> m.track.artist })
        fun added(m: MixedItem) = when (m) { is MixedItem.Local -> m.track.dateAdded; is MixedItem.Stream -> m.track.firstSeen / 1000 }
        fun rk(m: MixedItem) = when (m) { is MixedItem.Local -> rank(m.track); is MixedItem.Stream -> streamRank(m.track) } ?: noRank
        return when (sort) {
            LibrarySort.AZ -> list.sortedBy(::title)
            LibrarySort.ARTIST -> list.sortedWith(compareBy(::artist, ::title))
            LibrarySort.ADDED -> list.sortedWith(compareByDescending<MixedItem> { added(it) }.thenBy(::title))
            LibrarySort.RECENT -> list.sortedWith(compareBy<MixedItem> { rk(it) }.thenBy(::title))
        }
    }
}
