package io.github.x80070006.holowave.recommend

import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.data.TrackKey
import kotlin.random.Random

/** One row of the home screen: songs to play, or albums / artists / playlists to open. */
data class Shelf(
    val id: String,
    val title: String,
    val songs: List<MixedItem> = emptyList(),
    val collections: List<TrackCollection> = emptyList(),
)

/** Everything a shelf can be built from. All of it is on the device. */
class ShelfInputs(
    val tracks: List<Track>,
    val albums: List<TrackCollection>,
    val artists: List<TrackCollection>,
    val playlists: List<TrackCollection>,
    val folders: List<TrackCollection>,
    val favorites: TrackCollection?,
    val streams: List<StreamedTrack>,
    val stats: Map<String, TrackStats>,
    val nowMs: Long,
    val hour: Int,
)

/**
 * The rows of the "Tout" chip, Spotify style: each one built from the library and the listening
 * history, never from made-up content. A row with too little in it is left out, so a small library
 * shows fewer rows rather than empty ones. Rows that shuffle are stable for the whole day.
 */
object Shelves {
    private const val SONGS = 20
    private const val CARDS = 12
    private const val MIN = 3
    private const val DAY = 86_400_000L
    const val MAX_SHELVES = 20

    fun build(i: ShelfInputs): List<Shelf> {
        val out = ArrayList<Shelf>()
        fun add(s: Shelf) { if (s.songs.size >= MIN || s.collections.size >= MIN) out += s }

        val day = i.nowMs / DAY
        val playable = i.tracks.filter { it.playable && it.durationMs >= 45_000 }
        val key = HashMap<Long, String>(playable.size).also { m -> playable.forEach { m[it.id] = TrackKey.of(it) } }
        fun statsOf(t: Track) = i.stats[key[t.id]]
        fun plays(t: Track) = statsOf(t)?.plays ?: 0
        fun weight(t: Track, v: FlowVariant) = FlowEngine.weight(statsOf(t), v, i.nowMs, i.hour)
        fun streamWeight(s: StreamedTrack, v: FlowVariant) =
            FlowEngine.weight(i.stats[s.key] ?: TrackStats(s.plays, 0, s.plays, s.lastSeen, IntArray(24)), v, i.nowMs, i.hour)
        val streams = i.streams.filter { !it.videoId.isNullOrBlank() }
        fun local(list: List<Track>) = list.map { MixedItem.Local(it) }
        fun stream(list: List<StreamedTrack>) = list.map { MixedItem.Stream(it) }

        // 1. What you play these days, both sides mixed.
        val moment = (playable.filter { plays(it) > 0 }.map { MixedItem.Local(it) to weight(it, FlowVariant.MIX) } +
            streams.map { MixedItem.Stream(it) to streamWeight(it, FlowVariant.MIX) })
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(SONGS).map { it.first }
        add(Shelf("moment", "Vos titres du moment", moment))

        // 2. The account's most played songs on YouTube Music.
        add(Shelf("yt-top", "Top YouTube Music", stream(streams.sortedWith(compareByDescending<StreamedTrack> { it.plays }.thenByDescending { it.lastSeen }).take(SONGS))))

        // 3–4. Artists and albums you come back to.
        fun rank(cols: List<TrackCollection>) = cols.map { c -> c to c.tracks.sumOf { plays(it) } }.filter { it.second > 0 }
            .sortedByDescending { it.second }.take(CARDS).map { it.first }
        val topArtists = rank(i.artists)
        add(Shelf("artists", "Vos artistes", collections = topArtists))
        add(Shelf("albums", "Vos albums", collections = rank(i.albums)))

        // 5. Around this hour.
        add(Shelf("now", "À cette heure", local(playable.map { it to weight(it, FlowVariant.NOW) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(SONGS).map { it.first })))

        // 6. Loved, then left aside for a month.
        add(Shelf("rediscover", "Redécouvertes", local(playable.filter { t ->
            val s = statsOf(t) ?: return@filter false
            s.completes >= 2 && i.nowMs - s.lastPlayedAt > 30 * DAY
        }.shuffled(Random(day)).take(SONGS))))

        // 7. Never played yet.
        add(Shelf("unheard", "Jamais écoutés", local(playable.filter { plays(it) == 0 }.shuffled(Random(day + 1)).take(SONGS))))

        // 8. Newest files.
        add(Shelf("added", "Derniers ajouts", local(playable.sortedByDescending { it.dateAdded }.take(SONGS))))

        // 9. Liked.
        i.favorites?.let { f -> add(Shelf("liked", "Titres aimés", local(f.tracks.filter { it.playable }.take(SONGS)))) }

        // 10–11. Mixes around your two most played artists, files and YouTube together.
        topArtists.take(2).forEachIndexed { n, a ->
            val name = a.title.trim().lowercase()
            val songs = local(a.tracks.filter { it.playable }.shuffled(Random(day + 10 + n))) +
                stream(streams.filter { it.artist.trim().lowercase().contains(name) })
            add(Shelf("mix-$n", "Mix ${a.title}", songs.shuffled(Random(day + 20 + n)).take(SONGS)))
        }

        // 12. Recently heard on YouTube Music.
        add(Shelf("yt-recent", "Récemment sur YouTube Music", stream(streams.sortedByDescending { it.lastSeen }.take(SONGS))))

        // 13. Playlists and folders.
        add(Shelf("playlists", "Vos playlists", collections = i.playlists.take(CARDS)))
        add(Shelf("folders", "Vos dossiers", collections = i.folders.sortedByDescending { it.tracks.size }.take(CARDS)))

        // 14–19. By decade, from the year in the tags.
        playable.filter { it.year in 1950..2099 }.groupBy { it.year / 10 * 10 }
            .filter { it.value.size >= 5 }.toSortedMap(compareByDescending { it })
            .forEach { (decade, list) ->
                val label = if (decade >= 2000) "Années $decade" else "Années ${decade % 100}"
                add(Shelf("decade-$decade", label, local(list.shuffled(Random(day + decade)).take(SONGS))))
            }

        // 20. Long tracks, to settle in.
        add(Shelf("long", "Morceaux au long cours", local(playable.filter { it.durationMs >= 6 * 60_000 }.shuffled(Random(day + 3)).take(SONGS))))

        // 21. A daily draw across everything.
        add(Shelf("draw", "La pioche du jour", (local(playable) + stream(streams)).shuffled(Random(day + 4)).take(SONGS)))

        return out.take(MAX_SHELVES)
    }
}
