package io.github.x80070006.holowave.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import io.github.x80070006.holowave.R

/**
 * What the user chose in Réglages → Apparence. Reading these in composition or drawing
 * tracks the state, so changing a choice repaints the whole app instantly.
 */
object Appearance {
    var themeId by mutableStateOf(ThemeId.HOLOWAVE)
    var mode by mutableStateOf(ModePref.SYSTEM)
    var monoAccent by mutableStateOf(MonoAccent.VIOLET)
    var coverColors by mutableStateOf(true)
    var motion by mutableStateOf(MotionPref.FULL)

    /** Follows the device; fed by the root composable. */
    var systemDark by mutableStateOf(true)
    /** True when the system accessibility settings ask for no animation. */
    var systemReducedMotion by mutableStateOf(false)

    val isDark: Boolean
        get() = when (mode) {
            ModePref.SYSTEM -> systemDark
            ModePref.LIGHT -> false
            ModePref.DARK -> true
        }

    val colors: HoloColors by derivedStateOf { HoloThemes.colors(themeId, isDark, monoAccent) }

    /** "Reduced" is applied automatically when the system asks for it. */
    val effectiveMotion: MotionPref
        get() = if (systemReducedMotion && motion == MotionPref.FULL) MotionPref.REDUCED else motion

    fun readSystemMotion(context: Context) {
        val scale = runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)
        systemReducedMotion = scale == 0f
    }
}

val LocalHoloColors = compositionLocalOf<HoloColors> { Appearance.colors }

/** Entry point for new code: `HoloTheme.colors.primary`. */
object HoloTheme {
    val colors: HoloColors @Composable get() = LocalHoloColors.current
}

/**
 * Names used by screens written before the design system. They now map onto the five
 * roles, so old screens follow the theme until they are redesigned.
 */
object Deco {
    val Ink get() = Appearance.colors.background
    val InkRaised get() = Appearance.colors.surface
    val InkLine get() = Appearance.colors.outline
    val TealDeep get() = Appearance.colors.surfaceHigh
    val Teal get() = Appearance.colors.primary
    val TealBright get() = Appearance.colors.secondary
    val Gold get() = Appearance.colors.primary
    val GoldBright get() = Appearance.colors.primary
    val GoldDim get() = Appearance.colors.textSecondary
    val Copper get() = Appearance.colors.tertiary
    val Velvet get() = Appearance.colors.error
    val Cream get() = Appearance.colors.text
    val CreamDim get() = Appearance.colors.textSecondary
    val Muted get() = Appearance.colors.textSecondary
    val OnAccent get() = Appearance.colors.onPrimary

    /** Always dark: the hole and grooves of a vinyl do not depend on the theme. */
    val Vinyl = Color(0xFF0A0A0A)

    val goldBrush: Brush get() = Brush.linearGradient(listOf(Gold, Gold))
    val goldLine: Brush get() = Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))
}

/** Compatibility for screens that still branch on the old Art Déco look (it no longer exists). */
object Look {
    const val ornate = false
    val isLight: Boolean get() = !Appearance.isDark
    val id: ThemeId get() = Appearance.themeId
}

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
private val noPadding = PlatformTextStyle(includeFontPadding = false)

/** One family, two weights. */
private val Manrope = FontFamily(
    listOf(400, 700).map {
        Font(R.font.manrope, weight = FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
    },
)

private fun style(weight: Int, size: Float, line: Float, spacing: Float = 0f, features: String? = null) = TextStyle(
    fontFamily = Manrope, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
    fontFeatureSettings = features, platformStyle = noPadding, lineHeightStyle = tight,
)

/** Four sizes (28, 20, 16, 13), two weights (400, 700). */
object DecoType {
    val Marquee = style(700, 28f, 34f, -0.3f)
    val Display = style(700, 28f, 34f, -0.3f)
    val Headline = style(700, 20f, 26f, -0.1f)
    val Title = style(700, 16f, 22f)
    val Body = style(400, 16f, 24f)
    val Small = style(400, 13f, 18f)
    val Caps = style(700, 13f, 18f, 0.2f)
    val Numeral = style(400, 13f, 16f, 0f, "tnum")
    val Lyric = style(700, 28f, 36f, -0.3f)
}

/** Labels keep the writer's case: nothing is shouted in capitals any more. */
fun String.caps(): String = this

@Composable
fun HolowaveTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    SideEffect { Appearance.systemDark = systemDark }
    val c = Appearance.colors
    val scheme = if (c.isDark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.secondary, onSecondary = c.onSecondary,
            tertiary = c.tertiary, onTertiary = c.onTertiary, background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
            outline = c.outline, error = c.error,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.secondary, onSecondary = c.onSecondary,
            tertiary = c.tertiary, onTertiary = c.onTertiary, background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
            outline = c.outline, error = c.error,
        )
    }
    CompositionLocalProvider(LocalHoloColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography.copy(
                bodyLarge = DecoType.Body, bodyMedium = DecoType.Body, labelLarge = DecoType.Caps, titleMedium = DecoType.Title,
            ),
            content = content,
        )
    }
}
