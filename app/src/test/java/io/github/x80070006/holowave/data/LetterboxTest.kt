package io.github.x80070006.holowave.data

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class LetterboxTest {
    private val black = 0xFF000000.toInt()
    private val red = 0xFFCC2222.toInt()

    /** A [w]x[h] image, red inside [l,t,r,b), black outside. */
    private fun image(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int) =
        IntArray(w * h) { i -> val x = i % w; val y = i / w; if (x in l until r && y in t until b) red else black }

    @Test fun cutsBandsAboveAndBelow() =
        assertArrayEquals(intArrayOf(0, 15, 100, 85), Letterbox.bounds(image(100, 100, 0, 15, 100, 85), 100, 100))

    @Test fun cutsBandsOnTheSides() =
        assertArrayEquals(intArrayOf(20, 0, 80, 100), Letterbox.bounds(image(100, 100, 20, 0, 80, 100), 100, 100))

    @Test fun keepsAThinDarkEdge() =
        assertArrayEquals(intArrayOf(0, 0, 100, 100), Letterbox.bounds(image(100, 100, 0, 2, 100, 98), 100, 100))

    @Test fun keepsAnAlmostBlackCover() =
        assertArrayEquals(intArrayOf(0, 0, 100, 100), Letterbox.bounds(image(100, 100, 45, 45, 55, 55), 100, 100))

    @Test fun keepsAPictureWithoutBands() =
        assertArrayEquals(intArrayOf(0, 0, 50, 50), Letterbox.bounds(image(50, 50, 0, 0, 50, 50), 50, 50))
}
