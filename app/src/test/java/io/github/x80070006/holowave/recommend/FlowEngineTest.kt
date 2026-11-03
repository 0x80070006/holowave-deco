package io.github.x80070006.holowave.recommend

import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackKey
import io.github.x80070006.holowave.history.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FlowEngineTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun track(id: Long, artist: String, ms: Long = 200_000, mime: String = "audio/mpeg") = Track(
        id, "Titre $id", artist, "Album $artist", 1, ms, 1, 2020, "", "", "external_primary", 0, mime = mime,
    )

    private fun key(t: Track) = TrackKey.of(t)
    private fun row(t: Track, outcome: Outcome, daysAgo: Int = 10, hour: Int = 20) =
        ListenRow(key(t), now - daysAgo * day, outcome.code, hour)

    @Test fun aggregateCountsPlaysSkipsAndHours() {
        val t = track(1, "A")
        val s = FlowEngine.aggregate(
            listOf(row(t, Outcome.COMPLETED, hour = 8), row(t, Outcome.EARLY_SKIP, hour = 8), row(t, Outcome.PARTIAL, hour = 21)),
        )[key(t)]!!
        assertEquals(3, s.plays); assertEquals(1, s.skips); assertEquals(1, s.completes)
        assertEquals(2, s.hours[8]); assertEquals(1, s.hours[21])
    }

    @Test fun lovedSongOutweighsSkippedSong() {
        val loved = track(1, "A"); val skipped = track(2, "B")
        val stats = FlowEngine.aggregate(
            List(5) { row(loved, Outcome.COMPLETED) } + List(5) { row(skipped, Outcome.EARLY_SKIP) },
        )
        val wl = FlowEngine.weight(stats[key(loved)], FlowVariant.MIX, now, 12)
        val ws = FlowEngine.weight(stats[key(skipped)], FlowVariant.MIX, now, 12)
        assertTrue("$wl should beat $ws", wl > ws * 3)
    }

    @Test fun justPlayedSongIsLessLikely() {
        val t = track(1, "A")
        val fresh = FlowEngine.aggregate(List(3) { row(t, Outcome.COMPLETED, daysAgo = 0) })[key(t)]
        val old = FlowEngine.aggregate(List(3) { row(t, Outcome.COMPLETED, daysAgo = 5) })[key(t)]
        assertTrue(FlowEngine.weight(old, FlowVariant.MIX, now, 12) > FlowEngine.weight(fresh, FlowVariant.MIX, now, 12))
    }

    @Test fun discoverOnlyProposesUnheardSongs() {
        val heard = track(1, "A"); val fresh = track(2, "B")
        val stats = FlowEngine.aggregate(listOf(row(heard, Outcome.COMPLETED)))
        val out = FlowEngine.pick(listOf(heard, fresh), stats, FlowVariant.DISCOVER, now, 12, 10, Random(1))
        assertEquals(listOf(fresh.id), out.map { it.id })
    }

    @Test fun favouritesNeedRepeatedCompletedListens() {
        val once = track(1, "A"); val loved = track(2, "B"); val unheard = track(3, "C")
        val stats = FlowEngine.aggregate(listOf(row(once, Outcome.COMPLETED)) + List(4) { row(loved, Outcome.COMPLETED) })
        val out = FlowEngine.pick(listOf(once, loved, unheard), stats, FlowVariant.FAVOURITES, now, 12, 10, Random(1))
        assertEquals(listOf(loved.id), out.map { it.id })
    }

    @Test fun nowVariantPrefersTheCurrentHour() {
        val morning = track(1, "A"); val evening = track(2, "B")
        val stats = FlowEngine.aggregate(
            List(6) { row(morning, Outcome.COMPLETED, hour = 8) } + List(6) { row(evening, Outcome.COMPLETED, hour = 22) },
        )
        val wm = FlowEngine.weight(stats[key(morning)], FlowVariant.NOW, now, 9)
        val we = FlowEngine.weight(stats[key(evening)], FlowVariant.NOW, now, 9)
        assertTrue(wm > we * 3)
    }

    @Test fun blockedTracksAndArtistsAreNeverProposed() {
        val a = track(1, "Alpha"); val b = track(2, "Beta"); val c = track(3, "Gamma")
        val out = FlowEngine.pick(
            listOf(a, b, c), emptyMap(), FlowVariant.MIX, now, 12, 10, Random(3),
            blockedKeys = setOf(key(a)), blockedArtists = setOf("beta"),
        )
        assertEquals(listOf(c.id), out.map { it.id })
    }

    @Test fun unplayableAndTooShortSongsAreSkipped() {
        val wma = track(1, "A", mime = "audio/x-ms-wma"); val jingle = track(2, "B", ms = 5_000); val ok = track(3, "C")
        val out = FlowEngine.pick(listOf(wma, jingle, ok), emptyMap(), FlowVariant.MIX, now, 12, 10, Random(1))
        assertEquals(listOf(ok.id), out.map { it.id })
    }

    @Test fun noTwoSongsByTheSameArtistInARowWhenAvoidable() {
        val tracks = (1L..12L).map { track(it, if (it <= 6) "Alpha" else "Beta") }
        val out = FlowEngine.pick(tracks, emptyMap(), FlowVariant.MIX, now, 12, 12, Random(7))
        assertEquals(12, out.size)
        assertFalse(out.zipWithNext().any { (x, y) -> x.artist == y.artist })
    }

    @Test fun neverReturnsMoreThanAskedNorDuplicates() {
        val tracks = (1L..50L).map { track(it, "Artiste ${it % 9}") }
        val out = FlowEngine.pick(tracks, emptyMap(), FlowVariant.MIX, now, 12, 20, Random(5))
        assertEquals(20, out.size)
        assertEquals(20, out.map { it.id }.toSet().size)
    }

    @Test fun excludedIdsAreLeftOut() {
        val tracks = (1L..5L).map { track(it, "Artiste $it") }
        val out = FlowEngine.pick(tracks, emptyMap(), FlowVariant.MIX, now, 12, 10, Random(1), exclude = setOf(1L, 2L))
        assertTrue(out.none { it.id in setOf(1L, 2L) })
    }
}
