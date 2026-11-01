package io.github.x80070006.holowave.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.min

/**
 * Picks two lively, distinct colours from a cover. Pure Kotlin on an ARGB sample
 * (a few hundred pixels) so it is testable and costs nothing on the main thread.
 */
object CoverColors {

    class Picked(val primary: Color, val secondary: Color)

    fun pick(argb: IntArray): Picked? {
        if (argb.isEmpty()) return null
        class Px(val c: Color, val hsv: FloatArray, val weight: Float)
        val px = argb.map { v ->
            val c = Color(v or (0xFF shl 24))
            val hsv = Contrast.toHsv(c)
            // Lively = saturated and neither near-black nor near-white.
            val brightness = 1f - abs(hsv[2] - 0.62f) * 1.2f
            Px(c, hsv, hsv[1] * brightness.coerceIn(0.05f, 1f))
        }
        val best = px.maxByOrNull { it.weight } ?: return null
        if (best.weight < 0.06f) {
            // A grey cover: keep it neutral rather than inventing a hue.
            return Picked(best.c, best.c)
        }
        val secondary = px
            .filter { hueDistance(it.hsv[0], best.hsv[0]) >= 35f && it.hsv[1] > 0.25f }
            .maxByOrNull { it.weight }
            ?.c
            ?: Contrast.fromHsv((best.hsv[0] + 40f) % 360f, min(best.hsv[1], 0.8f), best.hsv[2])
        return Picked(best.c, secondary)
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }
}
