package io.github.x80070006.holowave.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverColorsTest {

    private fun px(vararg colors: Long) = colors.map { it.toInt() }.toIntArray()

    @Test fun picksTheLivelyColourOverGreysAndBlacks() {
        val picked = CoverColors.pick(px(0xFF101010, 0xFF808080, 0xFF101010, 0xFFE91E63, 0xFFEEEEEE, 0xFF101010))!!
        val hue = Contrast.toHsv(picked.primary)[0]
        assertEquals(340.0, hue.toDouble(), 6.0) // the pink one
    }

    @Test fun secondaryHasADifferentHue() {
        val picked = CoverColors.pick(px(0xFFE53935, 0xFFE53935, 0xFFE53935, 0xFF1E88E5, 0xFF111111))!!
        val a = Contrast.toHsv(picked.primary)[0]
        val b = Contrast.toHsv(picked.secondary)[0]
        val d = Math.abs(a - b).let { if (it > 180) 360 - it else it }
        assertTrue("hue distance $d", d >= 35f)
    }

    @Test fun aSingleColourCoverStillGivesASecondary() {
        val picked = CoverColors.pick(px(0xFF2E7D32, 0xFF2E7D32, 0xFF2E7D32))!!
        assertTrue(picked.primary != picked.secondary)
    }

    @Test fun aGreyCoverStaysNeutral() {
        val picked = CoverColors.pick(px(0xFF777777, 0xFF888888, 0xFF999999))!!
        assertEquals(0f, Contrast.toHsv(picked.primary)[1], 0.05f)
    }

    @Test fun emptyInputGivesNothing() {
        assertNull(CoverColors.pick(IntArray(0)))
        assertNotNull(CoverColors.pick(px(0xFFFF0000)))
    }

    /** Whatever the cover, the cover option must never break AA on any theme or mode. */
    @Test fun anyCoverStaysReadableOnEveryTheme() {
        val awkward = listOf(
            Color(0xFFFFEB3B), Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF1A237E),
            Color(0xFF00E676), Color(0xFF880E4F), Color(0xFF9E9E9E), Color(0xFFFF6F00),
        )
        ThemeId.entries.forEach { id ->
            listOf(true, false).forEach { dark ->
                val base = HoloThemes.colors(id, dark)
                awkward.forEach { p ->
                    awkward.forEach { s ->
                        val c = HoloThemes.withCover(base, p, s)
                        listOf(c.background, c.surface, c.surfaceHigh).forEach { bg ->
                            assertTrue("${id.label} dark=$dark primary ${p.toArgb().toString(16)} on bg", Contrast.ratio(c.primary, bg) >= 4.5)
                            assertTrue("${id.label} dark=$dark secondary ${s.toArgb().toString(16)} on bg", Contrast.ratio(c.secondary, bg) >= 4.5)
                        }
                        assertTrue(Contrast.ratio(c.onPrimary, c.primary) >= 4.5)
                        assertEquals("text is the theme's", base.text, c.text)
                        assertEquals("background is the theme's", base.background, c.background)
                    }
                }
            }
        }
    }
}
