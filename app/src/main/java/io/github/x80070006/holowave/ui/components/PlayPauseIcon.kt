package io.github.x80070006.holowave.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.x80070006.holowave.ui.theme.Appearance
import io.github.x80070006.holowave.ui.theme.MotionPref

/**
 * Play and pause are drawn from the same two quadrilaterals, so toggling morphs one into
 * the other instead of swapping icons. With animations off it simply snaps.
 */
@Composable
fun PlayPauseIcon(playing: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val animated = Appearance.effectiveMotion != MotionPref.OFF
    val t by animateFloatAsState(
        if (playing) 1f else 0f,
        if (animated) spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "playPause",
    )
    Canvas(modifier) {
        val s = size.minDimension / 24f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val playLeft = listOf(p(7f, 5f), p(12f, 8f), p(12f, 16f), p(7f, 19f))
        val pauseLeft = listOf(p(7f, 5f), p(10.5f, 5f), p(10.5f, 19f), p(7f, 19f))
        val playRight = listOf(p(12f, 8f), p(19f, 12f), p(19f, 12f), p(12f, 16f))
        val pauseRight = listOf(p(13.5f, 5f), p(17f, 5f), p(17f, 19f), p(13.5f, 19f))
        fun poly(a: List<Offset>, b: List<Offset>) = Path().apply {
            a.indices.forEach { i ->
                val o = lerp(a[i], b[i], t)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            close()
        }
        listOf(poly(playLeft, pauseLeft), poly(playRight, pauseRight)).forEach {
            drawPath(it, tint)
            // A round join softens the corners like the rest of the icon set.
            drawPath(it, tint, style = Stroke(width = 1.6f * s, join = StrokeJoin.Round))
        }
    }
}
