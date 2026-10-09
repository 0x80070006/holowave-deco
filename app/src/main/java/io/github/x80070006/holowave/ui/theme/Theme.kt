package io.github.x80070006.holowave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import io.github.x80070006.holowave.R

/** Palette lifted from the logo and the jazz-bar references: ink, deep teal, brass, copper, velvet. */
object Deco {
    val Ink = Color(0xFF0A0C0C)
    val InkRaised = Color(0xFF121716)
    val InkLine = Color(0xFF1F2725)
    val TealDeep = Color(0xFF0C2A29)
    val Teal = Color(0xFF15433F)
    val TealBright = Color(0xFF2E7D74)
    val Gold = Color(0xFFD8B26E)
    val GoldBright = Color(0xFFF2D9A0)
    val GoldDim = Color(0xFF8E7444)
    val Copper = Color(0xFFC9772F)
    val Velvet = Color(0xFF6F1C22)
    val Cream = Color(0xFFF2E8D5)
    val CreamDim = Color(0xFFB8AE9C)
    val Muted = Color(0xFF7A746A)

    val goldBrush = Brush.linearGradient(listOf(GoldBright, Gold, Copper))
    val goldLine = Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))
}

@OptIn(ExperimentalTextApi::class)
private fun josefin(weight: Int) = Font(
    R.font.josefin_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Poiret = FontFamily(Font(R.font.poiret_one))
val Josefin = FontFamily(josefin(300), josefin(400), josefin(500), josefin(600), josefin(700))

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
private val noPadding = PlatformTextStyle(includeFontPadding = false)

object DecoType {
    val Marquee = TextStyle(fontFamily = Poiret, fontSize = 34.sp, letterSpacing = 9.sp, lineHeight = 40.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Display = TextStyle(fontFamily = Poiret, fontSize = 30.sp, letterSpacing = 1.sp, lineHeight = 36.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Headline = TextStyle(fontFamily = Poiret, fontSize = 24.sp, letterSpacing = 1.sp, lineHeight = 30.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Title = TextStyle(fontFamily = Josefin, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 21.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Body = TextStyle(fontFamily = Josefin, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 21.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Small = TextStyle(fontFamily = Josefin, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 17.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Caps = TextStyle(fontFamily = Josefin, fontWeight = FontWeight(600), fontSize = 11.sp, letterSpacing = 2.6.sp, lineHeight = 14.sp, platformStyle = noPadding, lineHeightStyle = tight)
    val Numeral = TextStyle(fontFamily = Josefin, fontWeight = FontWeight(500), fontSize = 12.sp, letterSpacing = 0.5.sp, fontFeatureSettings = "tnum", platformStyle = noPadding, lineHeightStyle = tight)
    val Lyric = TextStyle(fontFamily = Josefin, fontWeight = FontWeight(700), fontSize = 25.sp, lineHeight = 33.sp, platformStyle = noPadding, lineHeightStyle = tight)
}

@Composable
fun HolowaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Deco.Gold,
            onPrimary = Deco.Ink,
            secondary = Deco.TealBright,
            background = Deco.Ink,
            onBackground = Deco.Cream,
            surface = Deco.InkRaised,
            onSurface = Deco.Cream,
            surfaceVariant = Deco.TealDeep,
            onSurfaceVariant = Deco.CreamDim,
            outline = Deco.GoldDim,
        ),
        typography = MaterialTheme.typography.copy(
            bodyLarge = DecoType.Body,
            bodyMedium = DecoType.Body,
            labelLarge = DecoType.Caps,
            titleMedium = DecoType.Title,
        ),
        content = content,
    )
}
