package io.github.x80070006.holowave.stream

import io.github.x80070006.holowave.history.Listen
import io.github.x80070006.holowave.history.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamCaptureTest {

    private val done = ArrayList<Pair<Heard, Listen>>()
    private val capture = StreamCapture { h, l -> done += h to l }

    private fun song(title: String, artist: String = "Nova Tide", durationMs: Long = 200_000, art: Any? = null) =
        Heard(title, artist, "Velvet Static", durationMs, art)

    @Test fun aSongPlayedThenReplacedIsReportedWithItsListeningTime() {
        capture.onMetadata(song("Afterglow"), now = 0, playing = true)
        capture.onMetadata(song("Signal Fade"), now = 90_000, playing = true)
        val (heard, listen) = done.single()
        assertEquals("Afterglow", heard.title)
        assertEquals(90_000L, listen.listenedMs)
        assertEquals(Outcome.PARTIAL, listen.outcome)
    }

    @Test fun reAnnouncingTheSameSongDoesNotRestartIt() {
        capture.onMetadata(song("Afterglow", art = null, durationMs = 0), now = 0, playing = true)
        capture.onMetadata(song("Afterglow", art = "cover", durationMs = 200_000), now = 1_000, playing = true)
        capture.stop(60_000)
        val (heard, listen) = done.single()
        assertEquals(60_000L, listen.listenedMs)
        assertEquals("the cover that arrived later is kept", "cover", heard.artwork)
        assertEquals("the duration that arrived later is kept", 200_000L, heard.durationMs)
    }

    @Test fun anEarlySkipIsReportedAsOne() {
        capture.onMetadata(song("Afterglow"), 0, true)
        capture.onMetadata(song("Signal Fade"), 8_000, true)
        assertEquals(Outcome.EARLY_SKIP, done.single().second.outcome)
    }

    @Test fun reachingTheEndCountsAsCompleted() {
        capture.onMetadata(song("Afterglow"), 0, true)
        capture.onMetadata(song("Signal Fade"), 200_000, true)
        assertEquals(Outcome.COMPLETED, done.single().second.outcome)
    }

    @Test fun pausedTimeIsNotCounted() {
        capture.onMetadata(song("Afterglow"), 0, true)
        capture.onPlayback(false, 40_000)
        capture.onPlayback(true, 40_000)
        capture.stop(70_000)
        assertEquals(70_000L, done.single().second.listenedMs)
    }

    @Test fun nothingLoadedEndsTheCurrentSong() {
        capture.onMetadata(song("Afterglow"), 0, true)
        capture.onMetadata(null, 50_000, false)
        assertNotNull(done.singleOrNull())
        capture.onMetadata(song("Signal Fade"), 60_000, true)
        capture.stop(70_000)
        assertEquals(2, done.size)
        assertTrue(done.last().first.title == "Signal Fade")
    }

    @Test fun stoppingWithoutASongIsHarmless() {
        capture.stop(0)
        capture.onPlayback(true, 0)
        assertTrue(done.isEmpty())
    }
}
