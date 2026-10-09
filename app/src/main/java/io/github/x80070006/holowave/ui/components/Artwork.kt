package io.github.x80070006.holowave.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import io.github.x80070006.holowave.data.ArtworkLoader
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.ui.theme.Deco
import kotlin.math.abs

val LocalArtwork = compositionLocalOf<ArtworkLoader> { error("ArtworkLoader not provided") }

class LoadedArt(val image: ImageBitmap, val accent: Color)

/** Loads (and memoises) the cover of [track]; null while loading or when the file has none. */
@Composable
fun rememberArtwork(track: Track?, size: Int = 512): LoadedArt? {
    val loader = LocalArtwork.current
    val uri = track?.uri
    var art by remember(uri, size) {
        mutableStateOf(uri?.let { loader.cached(it, size) }?.let { LoadedArt(it.bitmap.asImageBitmap(), Color(it.accent)) })
    }
    LaunchedEffect(uri, size) {
        if (uri != null && art == null) {
            loader.load(uri, size)?.let { art = LoadedArt(it.bitmap.asImageBitmap(), Color(it.accent)) }
        }
    }
    return art
}

@Composable
fun CoverImage(track: Track?, modifier: Modifier = Modifier, size: Int = 512, fallbackSeed: String? = null) {
    val art = rememberArtwork(track, size)
    Box(modifier.background(Deco.TealDeep), contentAlignment = Alignment.Center) {
        if (art != null) {
            Image(art.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CoverPlaceholder(fallbackSeed ?: track?.album.orEmpty(), Modifier.fillMaxSize())
        }
    }
}

/**
 * Generated "label" for tracks without artwork: a sunburst over one of the house
 * colours, picked from the album name so it stays stable between launches.
 */
@Composable
fun CoverPlaceholder(seed: String, modifier: Modifier = Modifier) {
    val palette = listOf(Deco.TealDeep to Deco.Gold, Deco.Velvet to Deco.GoldBright, Color(0xFF1B2140) to Deco.Copper, Deco.Teal to Deco.GoldBright)
    val (bg, fg) = palette[abs(seed.hashCode()) % palette.size]
    Box(modifier.background(Brush.verticalGradient(listOf(bg, Deco.Ink)))) {
        Sunburst(Modifier.fillMaxSize(), color = fg, rays = 17, origin = Offset(0.5f, 0.82f), spreadDegrees = 150f, alpha = 0.55f)
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.16f
            val c = Offset(size.width / 2, size.height * 0.82f)
            drawCircle(Deco.Ink, r * 1.25f, c)
            drawCircle(fg, r, c, style = Stroke(1.5f))
            drawCircle(fg, r * 0.55f, c, style = Stroke(1f), alpha = 0.7f)
        }
    }
}
