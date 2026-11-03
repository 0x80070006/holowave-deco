package io.github.x80070006.holowave.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandoffTest {
    private val L = SourceKind.LOCAL
    private val N = SourceKind.NOUTUBE

    @Test fun youtubeStartsWhileLocalPlays_localIsPaused() {
        val d = Handoff.decide(L, localPlaying = true, noutubePlaying = true, wasLocalPlaying = true, wasNoutubePlaying = false)
        assertEquals(N, d.active)
        assertEquals(L, d.pause)
    }

    @Test fun localStartsWhileYoutubePlays_youtubeIsPaused() {
        val d = Handoff.decide(N, localPlaying = true, noutubePlaying = true, wasLocalPlaying = false, wasNoutubePlaying = true)
        assertEquals(L, d.active)
        assertEquals(N, d.pause)
    }

    @Test fun nothingChanges_keepsActiveAndPausesNothing() {
        val d = Handoff.decide(L, localPlaying = true, noutubePlaying = false, wasLocalPlaying = true, wasNoutubePlaying = false)
        assertEquals(L, d.active)
        assertNull(d.pause)
    }

    @Test fun activeStopsAndOtherPlays_followsTheOther() {
        val d = Handoff.decide(L, localPlaying = false, noutubePlaying = true, wasLocalPlaying = false, wasNoutubePlaying = true)
        assertEquals(N, d.active)
        assertNull(d.pause)
    }

    @Test fun bothStartTogether_keepsActiveAndSilencesOther() {
        val d = Handoff.decide(L, localPlaying = true, noutubePlaying = true, wasLocalPlaying = false, wasNoutubePlaying = false)
        assertEquals(L, d.active)
        assertEquals(N, d.pause)
    }

    @Test fun emptyLocalFallsBackToYoutubeSong() = assertEquals(N, Handoff.withTrack(L, false, true))
    @Test fun emptyYoutubeFallsBackToLocalSong() = assertEquals(L, Handoff.withTrack(N, true, false))
    @Test fun bothLoaded_keepsActive() = assertEquals(N, Handoff.withTrack(N, true, true))
    @Test fun neitherLoaded_keepsActive() = assertEquals(L, Handoff.withTrack(L, false, false))

    @Test fun nothingPlays_keepsActive() {
        val d = Handoff.decide(N, false, false, false, false)
        assertEquals(N, d.active)
        assertNull(d.pause)
    }
}
