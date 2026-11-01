package io.github.x80070006.holowave.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** The themes. Each one is five colours (see [RoleColors]); everything else is derived. */
enum class ThemeId(val label: String, val tagline: String) {
    HOLOWAVE("Holowave", "Violet électrique, cyan et corail sur nuit profonde"),
    SUNSET("Sunset", "Mandarine, fuchsia et or sur prune"),
    LAGOON("Lagoon", "Turquoise, océan et menthe sur bleu marine"),
    FOREST("Forest", "Émeraude, tilleul et ocre sur vert sombre"),
    MONO("Mono+", "Un seul accent vif, le reste en niveaux de gris"),
}

enum class ModePref(val label: String) { SYSTEM("Système"), LIGHT("Clair"), DARK("Sombre") }

enum class MotionPref(val label: String) { FULL("Complètes"), REDUCED("Réduites"), OFF("Désactivées") }

/** The accent of the Mono+ theme, chosen by the user. Each has a dark-mode and a light-mode shade. */
enum class MonoAccent(val label: String, val dark: Color, val light: Color) {
    VIOLET("Violet", Color(0xFFA38BFF), Color(0xFF5B2EE6)),
    ROSE("Rose", Color(0xFFFF7FA8), Color(0xFFB8124F)),
    ORANGE("Orange", Color(0xFFFF9A55), Color(0xFFA03C00)),
    JAUNE("Jaune", Color(0xFFF2CC2E), Color(0xFF6F5400)),
    VERT("Vert", Color(0xFF3DD68C), Color(0xFF06693C)),
    BLEU("Bleu", Color(0xFF5BA8FF), Color(0xFF1259B3)),
}

/** The five roles of a theme in one mode. */
class RoleColors(
    /** Main actions, play button, active tab, progress. */
    val primary: Color,
    /** Active filter chips, secondary accents, links. */
    val secondary: Color,
    /** Badges, illustrations, animated bands. */
    val tertiary: Color,
    val background: Color,
    val text: Color,
)

/**
 * Every colour the UI may use. Surfaces and text levels come from the five roles by
 * luminance or opacity, so a theme never needs a sixth hue.
 */
class HoloColors(roles: RoleColors, val isDark: Boolean) {
    val primary = roles.primary
    val secondary = roles.secondary
    val tertiary = roles.tertiary
    val background = roles.background
    val text = roles.text

    val surface = blend(background, text, 0.06f)
    val surfaceHigh = blend(background, text, 0.12f)
    val outline = blend(background, text, 0.20f)

    val textSecondary = blend(background, text, 0.76f)
    /** Placeholders and disabled elements: exempt from contrast rules, still legible. */
    val textDisabled = blend(background, text, 0.45f)

    val onPrimary = Contrast.bestOn(primary)
    val onSecondary = Contrast.bestOn(secondary)
    val onTertiary = Contrast.bestOn(tertiary)

    /** Errors keep a reserved hue, not tied to the theme. */
    val error = if (isDark) Color(0xFFFF8A80) else Color(0xFFB3261E)

    fun pressed(c: Color): Color = blend(c, text, 0.12f)

    companion object {
        fun blend(a: Color, b: Color, t: Float) = Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = 1f,
        )
    }
}

object Contrast {
    private val nearBlack = Color(0xFF0A0A0F)
    private val white = Color(0xFFFFFFFF)

    fun luminance(c: Color): Double {
        fun f(v: Float): Double = v.toDouble().let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * f(c.red) + 0.7152 * f(c.green) + 0.0722 * f(c.blue)
    }

    /** WCAG contrast ratio, between 1 and 21. */
    fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Text colour with the best contrast on [bg]. */
    fun bestOn(bg: Color): Color = if (ratio(nearBlack, bg) >= ratio(white, bg)) nearBlack else white

    /**
     * Moves [c] toward lighter (or darker) until it reaches [min] against every colour in
     * [against], keeping hue and saturation. Used for user accents and cover colours.
     */
    fun ensure(c: Color, against: List<Color>, min: Double, lighten: Boolean): Color {
        var cur = c
        var step = 0
        while (against.any { ratio(cur, it) < min } && step < 40) {
            val hsv = toHsv(cur)
            val v = (if (lighten) hsv[2] + 0.025f else hsv[2] - 0.025f).coerceIn(0f, 1f)
            // Past full brightness, wash the colour out instead so it keeps getting lighter.
            val s = if (lighten && hsv[2] >= 0.98f) (hsv[1] - 0.04f).coerceAtLeast(0f) else hsv[1]
            cur = fromHsv(hsv[0], s, v)
            step++
        }
        return cur
    }

    fun toHsv(c: Color): FloatArray {
        val r = c.red
        val g = c.green
        val b = c.blue
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return floatArrayOf(h, if (mx == 0f) 0f else d / mx, mx)
    }

    fun fromHsv(h: Float, s: Float, v: Float): Color {
        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r + m, g + m, b + m, 1f)
    }
}

object HoloThemes {
    private fun roles(p: Long, s: Long, t: Long, bg: Long, tx: Long) =
        RoleColors(Color(p), Color(s), Color(t), Color(bg), Color(tx))

    /** Values tuned so that every theme meets WCAG AA in both modes (checked by a unit test). */
    fun roles(id: ThemeId, dark: Boolean, mono: MonoAccent = MonoAccent.VIOLET): RoleColors = when (id) {
        ThemeId.HOLOWAVE ->
            if (dark) roles(0xFF9B7BFF, 0xFF22D3EE, 0xFFFF7A90, 0xFF0D0B1F, 0xFFF4F1EA)
            else roles(0xFF5B2EE6, 0xFF066A7D, 0xFFC4304F, 0xFFF7F5FF, 0xFF17142B)
        ThemeId.SUNSET ->
            if (dark) roles(0xFFFF8A3D, 0xFFFF5FA8, 0xFFFFC83D, 0xFF1F0F1E, 0xFFFFF3E3)
            else roles(0xFFA64000, 0xFFB5155F, 0xFF7A5600, 0xFFFFF4E6, 0xFF3A1233)
        ThemeId.LAGOON ->
            if (dark) roles(0xFF1FD1C1, 0xFF4DA3FF, 0xFF7CF2B2, 0xFF061A2E, 0xFFF2FBFF)
            else roles(0xFF006B60, 0xFF1259B3, 0xFF12724A, 0xFFF4FBFD, 0xFF08263F)
        ThemeId.FOREST ->
            if (dark) roles(0xFF2ECC8F, 0xFFB5D95A, 0xFFE0A23B, 0xFF0C1A12, 0xFFF6F2E4)
            else roles(0xFF076A40, 0xFF4A620C, 0xFF8A5508, 0xFFF7F4E6, 0xFF12301F)
        ThemeId.MONO ->
            if (dark) RoleColors(mono.dark, Color(0xFFBDBDBD), Color(0xFF8E8E8E), Color(0xFF111111), Color(0xFFF2F2F2))
            else RoleColors(mono.light, Color(0xFF4A4A4A), Color(0xFF6A6A6A), Color(0xFFF5F5F5), Color(0xFF141414))
    }

    fun colors(id: ThemeId, dark: Boolean, mono: MonoAccent = MonoAccent.VIOLET) = HoloColors(roles(id, dark, mono), dark)

    /** Smallest contrast the theme guarantees, shown in the palette preview. */
    fun minContrast(c: HoloColors): Double {
        val onBg = listOf(c.background, c.surface, c.surfaceHigh)
        val pairs = buildList {
            onBg.forEach { bg ->
                add(Contrast.ratio(c.text, bg)); add(Contrast.ratio(c.textSecondary, bg))
                add(Contrast.ratio(c.primary, bg)); add(Contrast.ratio(c.secondary, bg))
            }
            add(Contrast.ratio(c.onPrimary, c.primary)); add(Contrast.ratio(c.onSecondary, c.secondary))
            add(Contrast.ratio(c.onTertiary, c.tertiary))
        }
        return pairs.min()
    }
}
