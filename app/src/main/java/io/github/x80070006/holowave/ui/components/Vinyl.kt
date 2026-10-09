package io.github.x80070006.holowave.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.ui.theme.Deco
import kotlin.math.roundToInt

/** Rotation angle that advances only while [spinning], so the record stops where it was. */
@Composable
fun rememberRecordRotation(spinning: Boolean, degreesPerSecond: Float = 110f): Float {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                angle = (angle + (now - last) / 1_000_000_000f * degreesPerSecond) % 360f
                last = now
            }
        }
    }
    return angle
}

/** A 12" record: grooves, a fixed light sheen, and the cover printed on the centre label. */
@Composable
fun VinylRecord(label: ImageBitmap?, rotation: Float, modifier: Modifier = Modifier, labelTint: Color = Deco.Teal) {
    Canvas(modifier) { drawVinyl(label, rotation, labelTint) }
}

private fun DrawScope.drawVinyl(label: ImageBitmap?, rotation: Float, labelTint: Color) {
    val r = size.minDimension / 2f
    val c = center

    drawCircle(Brush.radialGradient(listOf(Color(0xFF1C1C1C), Color(0xFF0B0B0B), Color(0xFF050505)), c, r), r, c)

    // Grooves, with darker "gaps" between tracks.
    var gr = r * 0.36f
    var i = 0
    while (gr < r * 0.965f) {
        val gap = i % 23 == 22
        drawCircle(
            color = if (gap) Color.Black else Color.White,
            radius = gr,
            center = c,
            alpha = if (gap) 0.9f else if (i % 3 == 0) 0.075f else 0.035f,
            style = Stroke(if (gap) 2.2f else 0.8f),
        )
        gr += r * 0.0105f
        i++
    }

    // Light reflection stays still while the record turns underneath: that is what sells the motion.
    drawCircle(
        Brush.sweepGradient(
            0f to Color.Transparent, 0.08f to Color.White.copy(alpha = 0.10f), 0.16f to Color.Transparent,
            0.5f to Color.Transparent, 0.58f to Color.White.copy(alpha = 0.07f), 0.66f to Color.Transparent,
            1f to Color.Transparent,
            center = c,
        ),
        r * 0.97f, c,
    )
    drawCircle(Color.White, r - 1f, c, alpha = 0.10f, style = Stroke(1.2f))

    // Centre label.
    val lr = r * 0.34f
    rotate(rotation, c) {
        val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, lr)) }
        clipPath(clip) {
            if (label != null) {
                drawImage(
                    label,
                    dstOffset = IntOffset((c.x - lr).roundToInt(), (c.y - lr).roundToInt()),
                    dstSize = IntSize((lr * 2).roundToInt(), (lr * 2).roundToInt()),
                )
            } else {
                drawCircle(Brush.radialGradient(listOf(labelTint, Deco.TealDeep), c, lr), lr, c)
                for (k in 0 until 12) {
                    rotate(k * 30f, c) {
                        drawLine(Deco.Gold.copy(alpha = 0.45f), Offset(c.x, c.y - lr * 0.2f), Offset(c.x, c.y - lr), 1.2f)
                    }
                }
            }
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), c, lr), lr, c)
        }
    }
    drawCircle(Deco.Gold, lr, c, style = Stroke(2f))
    drawCircle(Deco.Gold, lr * 0.93f, c, alpha = 0.5f, style = Stroke(0.8f))
    drawCircle(Deco.Ink, r * 0.028f, c)
    drawCircle(Deco.GoldBright, r * 0.028f, c, style = Stroke(1.4f))
}

/**
 * Album sleeve with the record sliding out of it. While playing the record
 * slides further out and turns; when paused it tucks back in and stops.
 */
@Composable
fun SleeveAndRecord(
    track: Track?,
    playing: Boolean,
    spin: Boolean,
    modifier: Modifier = Modifier,
) {
    val art = rememberArtwork(track, 768)
    val slide by animateFloatAsState(
        if (playing) 0.44f else 0.24f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "slide",
    )
    val rotation = rememberRecordRotation(playing && spin)

    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1.36f)) {
        val sleeve = maxWidth / 1.48f
        val record = sleeve * 0.95f
        val top = (maxHeight - sleeve) / 2
        Box(
            Modifier
                .size(record)
                .offset(x = (sleeve - record) / 2 + sleeve * slide, y = top + (sleeve - record) / 2),
        ) {
            VinylRecord(art?.image, rotation, Modifier.size(record), labelTint = art?.accent ?: Deco.Teal)
        }
        Box(
            Modifier
                .offset(y = top)
                .size(sleeve)
                .shadow(18.dp, RectangleShape, ambientColor = Color.Black, spotColor = Color.Black),
        ) {
            CoverImage(track, Modifier.size(sleeve).decoFrame(Deco.Gold, inset = 6.dp, alpha = 0.9f), size = 768)
            // Sleeve opening: a soft shadow along the right edge.
            Canvas(Modifier.size(sleeve)) {
                val w = size.width * 0.06f
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), size.width - w, size.width),
                    Offset(size.width - w, 0f), Size(w, size.height),
                )
            }
        }
    }
}

/** Small record used by the mini player and list rows of the current track. */
@Composable
fun MiniRecord(track: Track?, spinning: Boolean, modifier: Modifier = Modifier) {
    val art = rememberArtwork(track, 256)
    val rotation = rememberRecordRotation(spinning)
    Box(modifier, contentAlignment = Alignment.Center) {
        VinylRecord(art?.image, rotation, Modifier.matchParentSizeCompat(), labelTint = art?.accent ?: Deco.Teal)
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxWidth().aspectRatio(1f))
