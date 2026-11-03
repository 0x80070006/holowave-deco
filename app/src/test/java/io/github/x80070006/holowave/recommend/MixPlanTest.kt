package io.github.x80070006.holowave.recommend

import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MixPlanTest {
    private fun local(id: Long, artist: String = "A$id") =
        MixedItem.Local(Track(id, "T$id", artist, "Al", 1, 200_000, 1, 2020, "", "", "v", 0, mime = "audio/mpeg"))

    private fun stream(n: Int, videoId: String? = "vid$n-abcdefg", artist: String = "S$n") =
        MixedItem.Stream(StreamedTrack("k$n", "S$n", artist, "", 200_000, videoId, null, 0, 0, 3))

    private val plan = MixPlan(listOf(local(1), local(2), stream(3), local(4), stream(5), stream(6)))

    @Test fun localRunsAreGroupedAndYoutubeSongsAreAlone() {
        assertEquals(1, plan.runEnd(0)); assertEquals(2, plan.runEnd(2)); assertEquals(3, plan.runEnd(3))
        assertEquals(4, plan.runEnd(4)); assertEquals(5, plan.runEnd(5))
    }

    @Test fun runStartFindsTheBeginningOfTheRun() {
        assertEquals(0, plan.runStart(1)); assertEquals(2, plan.runStart(2)); assertEquals(3, plan.runStart(3))
    }

    @Test fun afterAndBefore() {
        assertEquals(2, plan.after(1)); assertNull(plan.after(5))
        assertEquals(0, plan.before(2)); assertNull(plan.before(0)); assertEquals(2, plan.before(3)); assertEquals(0, plan.before(1).let { plan.runStart(it ?: 0) })
    }

    @Test fun localTracksOfARun() = assertEquals(listOf(1L, 2L), plan.localTracks(0, 1).map { it.id })

    @Test fun mixedPickContainsBothKindsAndOnlyKnownVideoIds() {
        val tracks = (1L..10L).map { (local(it) as MixedItem.Local).track }
        val streams = (1..10).map { (stream(it, videoId = if (it % 2 == 0) "vid$it-abcdefg" else null) as MixedItem.Stream).track }
        val out = FlowEngine.pickMixed(tracks, streams, emptyMap(), FlowVariant.MIX, 1_800_000_000_000L, 12, 20, Random(3))
        // 10 files + the 5 streamed songs whose video id is known
        assertEquals(15, out.size)
        assertTrue(out.any { it is MixedItem.Local } && out.any { it is MixedItem.Stream })
        assertTrue(out.filterIsInstance<MixedItem.Stream>().all { !it.track.videoId.isNullOrBlank() })
    }

    @Test fun mixedPickWithoutKnownVideoIdsHasOnlyLocalFiles() {
        val tracks = (1L..5L).map { (local(it) as MixedItem.Local).track }
        val streams = (1..5).map { (stream(it, videoId = null) as MixedItem.Stream).track }
        val out = FlowEngine.pickMixed(tracks, streams, emptyMap(), FlowVariant.MIX, 1_800_000_000_000L, 12, 10, Random(1))
        assertTrue(out.all { it is MixedItem.Local })
    }

    @Test fun discoverNeverProposesAPlayedStreamedSong() {
        val streams = listOf((stream(1) as MixedItem.Stream).track)
        val out = FlowEngine.pickMixed(emptyList(), streams, emptyMap(), FlowVariant.DISCOVER, 1_800_000_000_000L, 12, 5, Random(1))
        assertTrue(out.isEmpty())
    }

    @Test fun keysOfTheEngineMatchTheHistory() {
        val t = (local(1) as MixedItem.Local).track
        assertEquals(TrackKey.of(t), TrackKey.of(t.artist, t.title, t.album, t.durationMs))
    }
}
