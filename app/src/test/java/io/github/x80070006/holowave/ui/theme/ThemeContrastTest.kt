package io.github.x80070006.holowave.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG AA for every theme in both modes. Accents must be readable as text (4.5:1) on the
 * background and on both raised surfaces, not only as shapes (3:1).
 */
class ThemeContrastTest {

    private val text = 4.5
    private val ui = 3.0

    private fun check(name: String, c: HoloColors) {
        val surfaces = listOf("background" to c.background, "surface" to c.surface, "surfaceHigh" to c.surfaceHigh)
        surfaces.forEach { (sn, bg) ->
            assertAtLeast("$name text on $sn", Contrast.ratio(c.text, bg), text)
            assertAtLeast("$name textSecondary on $sn", Contrast.ratio(c.textSecondary, bg), text)
            assertAtLeast("$name primary on $sn", Contrast.ratio(c.primary, bg), text)
            assertAtLeast("$name secondary on $sn", Contrast.ratio(c.secondary, bg), text)
            assertAtLeast("$name tertiary on $sn", Contrast.ratio(c.tertiary, bg), ui)
            assertAtLeast("$name error on $sn", Contrast.ratio(c.error, bg), text)
        }
        assertAtLeast("$name onPrimary", Contrast.ratio(c.onPrimary, c.primary), text)
        assertAtLeast("$name onSecondary", Contrast.ratio(c.onSecondary, c.secondary), text)
        assertAtLeast("$name onTertiary", Contrast.ratio(c.onTertiary, c.tertiary), text)
    }

    private fun assertAtLeast(what: String, actual: Double, min: Double) =
        assertTrue("$what = ${"%.2f".format(actual)}:1, needs $min:1", actual >= min)

    @Test fun everyThemeMeetsAaInBothModes() {
        ThemeId.entries.forEach { id ->
            check("${id.label} dark", HoloThemes.colors(id, dark = true))
            check("${id.label} light", HoloThemes.colors(id, dark = false))
        }
    }

    @Test fun everyMonoAccentMeetsAaInBothModes() {
        MonoAccent.entries.forEach { accent ->
            check("Mono+ ${accent.label} dark", HoloThemes.colors(ThemeId.MONO, dark = true, mono = accent))
            check("Mono+ ${accent.label} light", HoloThemes.colors(ThemeId.MONO, dark = false, mono = accent))
        }
    }

    @Test fun lightModeIsNotAnInversionOfDark() {
        ThemeId.entries.forEach { id ->
            val d = HoloThemes.roles(id, dark = true)
            val l = HoloThemes.roles(id, dark = false)
            assertTrue("${id.label}: light background must be lighter than dark", Contrast.luminance(l.background) > Contrast.luminance(d.background))
            assertTrue("${id.label}: light text must be darker than dark text", Contrast.luminance(l.text) < Contrast.luminance(d.text))
            assertTrue("${id.label}: accents are re-tuned per mode", l.primary != d.primary)
        }
    }

    @Test fun ratioMatchesKnownWcagValues() {
        assertEquals(21.0, Contrast.ratio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, Contrast.ratio(Color.White, Color.White), 0.001)
        // #767676 on white is the textbook 4.54:1 threshold grey.
        assertEquals(4.54, Contrast.ratio(Color(0xFF767676), Color.White), 0.01)
    }

    @Test fun ensureLiftsALowContrastColourWithoutChangingItsHue() {
        val bg = Color(0xFF111111)
        val dim = Color(0xFF3A2A8C) // deep violet, unreadable on near-black
        val fixed = Contrast.ensure(dim, listOf(bg), 4.5, lighten = true)
        assertAtLeast("ensured violet", Contrast.ratio(fixed, bg), 4.5)
        val hueBefore = Contrast.toHsv(dim)[0]
        val hueAfter = Contrast.toHsv(fixed)[0]
        assertEquals(hueBefore.toDouble(), hueAfter.toDouble(), 3.0)
    }

    @Test fun ensureDarkensForLightBackgrounds() {
        val bg = Color(0xFFF5F5F5)
        val pale = Color(0xFFFFD54F)
        val fixed = Contrast.ensure(pale, listOf(bg), 4.5, lighten = false)
        assertAtLeast("ensured yellow", Contrast.ratio(fixed, bg), 4.5)
    }

    @Test fun ensureLeavesAlreadyReadableColoursAlone() {
        val c = Color(0xFF5B2EE6)
        assertEquals(c, Contrast.ensure(c, listOf(Color(0xFFF5F5F5)), 4.5, lighten = false))
    }

    @Test fun derivedSurfacesStayWithinTheThemeHue() {
        val c = HoloThemes.colors(ThemeId.SUNSET, dark = true)
        // Surfaces are blends of background and text: strictly between the two in luminance.
        val lo = Contrast.luminance(c.background)
        val hi = Contrast.luminance(c.text)
        listOf(c.surface, c.surfaceHigh, c.outline).forEach {
            val l = Contrast.luminance(it)
            assertTrue(l > lo && l < hi)
        }
    }

    @Test fun hsvRoundTripIsStable() {
        listOf(Color(0xFF9B7BFF), Color(0xFF1FD1C1), Color(0xFFFF8A3D), Color(0xFF808080)).forEach {
            val hsv = Contrast.toHsv(it)
            val back = Contrast.fromHsv(hsv[0], hsv[1], hsv[2])
            assertEquals(it.red.toDouble(), back.red.toDouble(), 0.01)
            assertEquals(it.green.toDouble(), back.green.toDouble(), 0.01)
            assertEquals(it.blue.toDouble(), back.blue.toDouble(), 0.01)
        }
    }
}
