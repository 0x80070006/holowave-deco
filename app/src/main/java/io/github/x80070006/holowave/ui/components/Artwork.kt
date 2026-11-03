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
import androidx.compose.runtime.produceState
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

class LoadedArt(val image: ImageBitmap, val accent: Color, val secondary: Color)

/** Loads (and memoises) the cover of [track]; null while loading or when the file has none. */
@Composable
fun rememberArtwork(track: Track?, size: Int = 512): LoadedArt? {
    val loader = LocalArtwork.current
    val uri = track?.uri
    var art by remember(uri, size) {
        mutableStateOf(uri?.let { loader.cached(it, size) }?.let { LoadedArt(it.bitmap.asImageBitmap(), Color(it.accent), Color(it.secondary)) })
    }
    LaunchedEffect(uri, size) {
        if (uri != null && art == null) {
            loader.load(uri, size)?.let { art = LoadedArt(it.bitmap.asImageBitmap(), Color(it.accent), Color(it.secondary)) }
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

/** Cover of a streamed song: the bitmap NouTube published, or the generated label when there is none. */
@Composable
fun StreamCover(bitmap: android.graphics.Bitmap?, seed: String, modifier: Modifier = Modifier) {
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(modifier.background(Deco.TealDeep), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CoverPlaceholder(seed, Modifier.fillMaxSize())
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


/** Decoded covers of streamed songs, shared by every list, so scrolling back never decodes twice. */
private object StreamedCovers {
    private val cache = object : android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: androidx.compose.ui.graphics.ImageBitmap) = value.width * value.height * 4
    }

    fun key(path: String, version: Long, size: Int) = "$path#$version@$size"
    fun get(key: String) = cache.get(key)

    /** Decodes at about [size] pixels (not the stored 384) to keep memory and decode time low. */
    fun load(key: String, path: String, size: Int): androidx.compose.ui.graphics.ImageBitmap? {
        cache.get(key)?.let { return it }
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
        val bmp = android.graphics.BitmapFactory.decodeFile(path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        return bmp.asImageBitmap().also { cache.put(key, it) }
    }
}

/** Cover of a streamed song: the small JPEG kept on the device, or a generated one when there is none. */
@Composable
fun StreamedCover(track: io.github.x80070006.holowave.data.StreamedTrack, modifier: Modifier = Modifier, size: Int = 192) {
    val path = track.artworkPath
    val key = path?.let { StreamedCovers.key(it, track.lastSeen, size) }
    // A cover already decoded shows on the first frame: no placeholder flashing while scrolling.
    val bitmap by produceState(key?.let(StreamedCovers::get), key) {
        if (value == null && key != null && path != null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { StreamedCovers.load(key, path, size) }.getOrNull()
            }
        }
    }
    Box(modifier.background(Deco.TealDeep), contentAlignment = Alignment.Center) {
        val b = bitmap
        when {
            b != null -> Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // Still loading: a plain tile, the generated label only for songs that really have no cover.
            path == null -> CoverPlaceholder(track.artist + track.title, Modifier.fillMaxSize())
        }
    }
}
