package io.github.x80070006.holowave.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionClockTest {
    @Test fun playing_extrapolatesFromUpdateTime() =
        assertEquals(12_000L, PositionClock.estimate(10_000, 1_000, 1f, true, 3_000, 200_000))

    @Test fun paused_staysWhereItWas() =
        assertEquals(10_000L, PositionClock.estimate(10_000, 1_000, 1f, false, 9_000, 200_000))

    @Test fun speedScalesElapsedTime() =
        assertEquals(14_000L, PositionClock.estimate(10_000, 0, 2f, true, 2_000, 200_000))

    @Test fun neverPastTheDuration() =
        assertEquals(30_000L, PositionClock.estimate(29_000, 0, 1f, true, 60_000, 30_000))

    @Test fun unknownPositionIsZero() =
        assertEquals(0L, PositionClock.estimate(-1, 0, 1f, true, 5_000, 30_000))

    @Test fun clockGoingBackwardsDoesNotRewind() =
        assertEquals(10_000L, PositionClock.estimate(10_000, 5_000, 1f, true, 1_000, 0))
}
