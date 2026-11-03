package io.github.x80070006.holowave.data

import android.graphics.Bitmap

/**
 * Covers taken from videos (files downloaded from YouTube, video thumbnails) often carry black bands
 * above and below, or on the sides. They are cut off so the picture fills its square like an album cover.
 * The detection is pure Kotlin on ARGB pixels so it is tested without a device.
 */
object Letterbox {
    private const val DARK = 28
    private const val DARK_SHARE = 0.97

    /** The picture inside the bands, as [left, top, right, bottom) (the whole image when there are none). */
    fun bounds(px: IntArray, w: Int, h: Int): IntArray {
        if (w < 8 || h < 8 || px.size < w * h) return intArrayOf(0, 0, w, h)
        fun dark(c: Int) = maxOf((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF) < DARK
        fun rowDark(y: Int): Boolean { var n = 0; for (x in 0 until w) if (dark(px[y * w + x])) n++; return n >= w * DARK_SHARE }
        fun colDark(x: Int, top: Int, bottom: Int): Boolean {
            var n = 0; for (y in top until bottom) if (dark(px[y * w + x])) n++; return n >= (bottom - top) * DARK_SHARE
        }
        var top = 0
        while (top < h / 2 && rowDark(top)) top++
        var bottom = h
        while (bottom > h / 2 && rowDark(bottom - 1)) bottom--
        // Thin dark edges are part of many real covers: only real bands (8 % or more) are cut.
        if (top + (h - bottom) < h * 0.08) { top = 0; bottom = h }
        var left = 0
        while (left < w / 2 && colDark(left, top, bottom)) left++
        var right = w
        while (right > w / 2 && colDark(right - 1, top, bottom)) right--
        if (left + (w - right) < w * 0.08) { left = 0; right = w }
        // A cover that is almost all black is left as it is.
        if (bottom - top < h * 0.4 || right - left < w * 0.4) return intArrayOf(0, 0, w, h)
        return intArrayOf(left, top, right, bottom)
    }

    /** [b] without its bands; the same bitmap when it has none. */
    fun trim(b: Bitmap): Bitmap {
        val w = b.width
        val h = b.height
        val px = IntArray(w * h)
        runCatching { b.getPixels(px, 0, w, 0, 0, w, h) }.getOrElse { return b }
        val (l, t, r, bt) = bounds(px, w, h).let { listOf(it[0], it[1], it[2], it[3]) }
        if (l == 0 && t == 0 && r == w && bt == h) return b
        return Bitmap.createBitmap(b, l, t, r - l, bt - t)
    }
}
