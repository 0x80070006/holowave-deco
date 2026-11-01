package io.github.x80070006.holowave.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTrackerTest {

    private val out = ArrayList<Listen>()
    private val tracker = ListenTracker { out += it }
    private val song = 200_000L

    @Test fun playedToTheEndIsCompleted() {
        tracker.trackStarted(1_000, song, isPlaying = true)
        tracker.trackEnded(EndReason.NATURAL, song)
        val l = out.single()
        assertEquals(Outcome.COMPLETED, l.outcome)
        assertEquals(song, l.listenedMs)
        assertEquals(1_000L, l.startedAt)
    }

    @Test fun skippedWithinThirtySecondsIsAnEarlySkip() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.trackEnded(EndReason.USER_SKIP, 12_000)
        assertEquals(Outcome.EARLY_SKIP, out.single().outcome)
        assertEquals(12_000L, out.single().listenedMs)
        assertEquals(12_000L, out.single().leftAtMs)
    }

    @Test fun skippedAfterARealListenIsPartial() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.trackEnded(EndReason.USER_SKIP, 90_000)
        assertEquals(Outcome.PARTIAL, out.single().outcome)
    }

    @Test fun ninetyPercentCountsAsCompleted() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.trackEnded(EndReason.USER_SKIP, 185_000)
        assertEquals(Outcome.COMPLETED, out.single().outcome)
    }

    @Test fun pausedTimeIsNotListeningTime() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.playingChanged(false, 40_000) // paused at 0:40
        tracker.playingChanged(true, 40_000)  // resumed later, same position
        tracker.trackEnded(EndReason.STOPPED, 70_000)
        assertEquals(70_000L, out.single().listenedMs)
    }

    @Test fun seekingForwardDoesNotInflateListeningTime() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.seeked(from = 20_000, to = 150_000)
        tracker.trackEnded(EndReason.USER_SKIP, 160_000)
        val l = out.single()
        assertEquals(30_000L, l.listenedMs) // 20 s before the seek + 10 s after
        assertEquals(1, l.seeks)
        assertEquals(Outcome.PARTIAL, l.outcome)
    }

    @Test fun seekingBackReplaysAreCountedOnce() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.seeked(from = 60_000, to = 0)
        tracker.trackEnded(EndReason.NATURAL, song)
        assertEquals(60_000L + song, out.single().listenedMs)
    }

    @Test fun repeatOneIsAVeryPositiveSingleRecord() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.repeated(song)
        tracker.trackEnded(EndReason.STOPPED, 30_000)
        val l = out.single()
        assertEquals(1, l.repeats)
        assertEquals(Outcome.COMPLETED, l.outcome)
        assertEquals(song + 30_000L, l.listenedMs)
    }

    @Test fun aBlinkIsIgnored() {
        tracker.trackStarted(0, song, isPlaying = true)
        tracker.trackEnded(EndReason.USER_SKIP, 400)
        assertTrue(out.isEmpty())
    }

    @Test fun neverPlayedTrackLeavesNoRecord() {
        tracker.trackStarted(0, song, isPlaying = false)
        tracker.trackEnded(EndReason.REPLACED, 0)
        assertTrue(out.isEmpty())
        assertTrue(!tracker.isTracking)
    }

    @Test fun trackerIsReusableAfterEachListen() {
        repeat(3) { i ->
            tracker.trackStarted(i * 1_000L, song, isPlaying = true)
            tracker.trackEnded(EndReason.NATURAL, song)
        }
        assertEquals(3, out.size)
    }

    @Test fun endingWithoutStartingIsHarmless() {
        tracker.trackEnded(EndReason.NATURAL, 1_000)
        tracker.playingChanged(true, 5)
        assertTrue(out.isEmpty())
    }
}
