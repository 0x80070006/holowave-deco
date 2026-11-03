package io.github.x80070006.holowave.recommend

import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.data.TrackKey
import io.github.x80070006.holowave.history.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelvesTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun track(id: Long, artist: String, year: Int = 0, ms: Long = 200_000, added: Long = id) =
        Track(id, "T$id", artist, "Al $artist", id, ms, 1, year, "", "", "v", added, mime = "audio/mpeg")

    private fun inputs(tracks: List<Track>, streams: List<StreamedTrack> = emptyList(), rows: List<ListenRow> = emptyList()): ShelfInputs {
        val artists = tracks.groupBy { it.artist }.map { (a, l) -> TrackCollection(CollectionKind.ARTIST, a, a, "", l) }
        val albums = tracks.groupBy { it.album }.map { (a, l) -> TrackCollection(CollectionKind.ALBUM, a, a, "", l) }
        return ShelfInputs(tracks, albums, artists, emptyList(), emptyList(), null, streams, FlowEngine.aggregate(rows), now, 20)
    }

    @Test fun emptyLibraryGivesNoShelf() = assertTrue(Shelves.build(inputs(emptyList())).isEmpty())

    @Test fun neverMoreThanTwentyShelvesAndNoneAlmostEmpty() {
        val tracks = (1L..400L).map { track(it, "Artiste ${it % 15}", year = 1960 + (it % 7).toInt() * 10, ms = if (it % 9 == 0L) 420_000 else 200_000) }
        val rows = tracks.take(120).flatMap { t -> List(3) { ListenRow(TrackKey.of(t), now - (it + 40) * day, Outcome.COMPLETED.code, 20) } }
        val streams = (1..30).map { StreamedTrack("k$it", "S$it", "Artiste ${it % 15}", "", 200_000, "vid$it-abcdefg", null, 0, now - it * 1000L, it) }
        val shelves = Shelves.build(inputs(tracks, streams, rows))
        assertTrue(shelves.size <= Shelves.MAX_SHELVES)
        assertTrue(shelves.size >= 15)
        assertTrue(shelves.all { it.songs.size >= 3 || it.collections.size >= 3 })
        assertEquals(shelves.size, shelves.map { it.id }.toSet().size)
    }

    @Test fun youtubeTopIsRankedByPlays() {
        val streams = listOf(1, 9, 4).map { StreamedTrack("k$it", "S$it", "A", "", 200_000, "vid$it-abcdefg", null, 0, now, it) }
        val top = Shelves.build(inputs(emptyList(), streams)).first { it.id == "yt-top" }
        assertEquals(listOf(9, 4, 1), top.songs.map { (it as MixedItem.Stream).track.plays })
    }

    @Test fun streamsWithoutAVideoIdAreLeftOut() {
        val streams = (1..5).map { StreamedTrack("k$it", "S$it", "A", "", 200_000, null, null, 0, now, 3) }
        assertFalse(Shelves.build(inputs(emptyList(), streams)).any { s -> s.songs.any { it is MixedItem.Stream } })
    }

    @Test fun decadesComeFromTheYearTag() {
        val tracks = (1L..6L).map { track(it, "A$it", year = 1985) } + (7L..12L).map { track(it, "B$it", year = 2014) }
        val ids = Shelves.build(inputs(tracks)).map { it.id }
        assertTrue("decade-1980" in ids && "decade-2010" in ids)
    }

    @Test fun sameDaySameShelves() {
        val tracks = (1L..50L).map { track(it, "A${it % 5}") }
        assertEquals(Shelves.build(inputs(tracks)), Shelves.build(inputs(tracks)))
    }
}
