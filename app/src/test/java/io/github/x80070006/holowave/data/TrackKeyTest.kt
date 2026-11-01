package io.github.x80070006.holowave.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TrackKeyTest {

    @Test fun keyIsStableAcrossCaseSpacingAndSmallDurationDrift() {
        val a = TrackKey.of("Miles Davis", "So What", "Kind of Blue", 562_000)
        assertEquals(a, TrackKey.of("  miles davis ", "SO WHAT", "kind of blue", 562_900))
    }

    @Test fun differentTracksGetDifferentKeys() {
        val a = TrackKey.of("A", "Song", "Album", 180_000)
        assertNotEquals(a, TrackKey.of("B", "Song", "Album", 180_000))
        assertNotEquals(a, TrackKey.of("A", "Song", "Album", 240_000))
    }

    /** Lyrics cached by earlier versions are filed under these exact keys: the format must never change. */
    @Test fun keyFormatIsFrozen() {
        assertEquals("aa5bbcb7bd87dabfdcbbe405c562defd", TrackKey.of("a", "b", "c", 1))
        assertEquals("dba610eec2440235bd5e63d2f26e19ba", TrackKey.of("Miles Davis", "So What", "Kind of Blue", 562_000))
    }

    @Test fun ownerPackageDecidesTheSource() {
        assertEquals(TrackSource.NOUTUBE, TrackSource.fromOwner("jp.nonbili.noutube"))
        assertEquals(TrackSource.LOCAL, TrackSource.fromOwner(null))
        assertEquals(TrackSource.LOCAL, TrackSource.fromOwner("com.android.chrome"))
        assertEquals(TrackSource.ONLINE, TrackSource.of(2))
        assertEquals(TrackSource.LOCAL, TrackSource.of(99))
    }
}
