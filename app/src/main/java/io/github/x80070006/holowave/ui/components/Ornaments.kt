package io.github.x80070006.holowave.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Rectangle with stepped (notched) corners, the signature Art Deco silhouette. */
class SteppedShape(private val step: Dp = 6.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { step.toPx() }.coerceAtMost(minOf(size.width, size.height) / 4f)
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(s * 2, 0f)
            lineTo(w - s * 2, 0f); lineTo(w - s * 2, s); lineTo(w - s, s); lineTo(w - s, s * 2); lineTo(w, s * 2)
            lineTo(w, h - s * 2); lineTo(w - s, h - s * 2); lineTo(w - s, h - s); lineTo(w - s * 2, h - s); lineTo(w - s * 2, h)
            lineTo(s * 2, h); lineTo(s * 2, h - s); lineTo(s, h - s); lineTo(s, h - s * 2); lineTo(0f, h - s * 2)
            lineTo(0f, s * 2); lineTo(s, s * 2); lineTo(s, s); lineTo(s * 2, s)
            close()
        }
        return Outline.Generic(p)
    }
}

/** Fan of thin rays rising from a point, like the sunbursts on 1920s facades and record sleeves. */
@Composable
fun Sunburst(
    modifier: Modifier = Modifier,
    color: Color = Deco.Gold,
    rays: Int = 23,
    origin: Offset = Offset(0.5f, 1f),
    spreadDegrees: Float = 180f,
    alpha: Float = 0.12f,
) {
    Canvas(modifier) {
        val o = Offset(size.width * origin.x, size.height * origin.y)
        val radius = size.maxDimension * 1.2f
        val start = -90f - spreadDegrees / 2f
        for (i in 0 until rays) {
            val a = (start + spreadDegrees * i / (rays - 1)) * (PI / 180.0)
            val end = Offset(o.x + (cos(a) * radius).toFloat(), o.y + (sin(a) * radius).toFloat())
            drawLine(
                brush = Brush.linearGradient(listOf(color.copy(alpha = alpha), Color.Transparent), o, end),
                start = o, end = end, strokeWidth = if (i % 2 == 0) 1.6f else 0.8f,
            )
        }
    }
}

/** Hairline — diamond — hairline. */
@Composable
fun DecoDivider(modifier: Modifier = Modifier, color: Color = Deco.Gold) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val cy = size.height / 2
        val cx = size.width / 2
        val d = 4.dp.toPx()
        val gap = 10.dp.toPx()
        val fade = Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.7f)), 0f, cx - gap)
        val fadeR = Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), Color.Transparent), cx + gap, size.width)
        drawLine(fade, Offset(0f, cy - 1.5f), Offset(cx - gap, cy - 1.5f), 1f)
        drawLine(fade, Offset(0f, cy + 1.5f), Offset(cx - gap, cy + 1.5f), 1f)
        drawLine(fadeR, Offset(cx + gap, cy - 1.5f), Offset(size.width, cy - 1.5f), 1f)
        drawLine(fadeR, Offset(cx + gap, cy + 1.5f), Offset(size.width, cy + 1.5f), 1f)
        val diamond = Path().apply {
            moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close()
        }
        drawPath(diamond, color)
        rotate(45f, Offset(cx, cy)) {
            drawRect(color, Offset(cx - d * 2.2f, cy - d * 2.2f), Size(d * 4.4f, d * 4.4f), style = Stroke(1f), alpha = 0.6f)
        }
    }
}

/** Double brass frame with corner studs, drawn over the content (album covers, cards). */
fun Modifier.decoFrame(color: Color = Deco.Gold, inset: Dp = 4.dp, alpha: Float = 1f): Modifier = drawWithContent {
    drawContent()
    val i = inset.toPx()
    val c = color.copy(alpha = color.alpha * alpha)
    drawRect(c, style = Stroke(1.2f))
    drawRect(c.copy(alpha = c.alpha * 0.55f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), style = Stroke(0.8f))
    val stud = 2.2.dp.toPx()
    listOf(Offset(i, i), Offset(size.width - i, i), Offset(i, size.height - i), Offset(size.width - i, size.height - i)).forEach {
        val p = Path().apply {
            moveTo(it.x, it.y - stud); lineTo(it.x + stud, it.y); lineTo(it.x, it.y + stud); lineTo(it.x - stud, it.y); close()
        }
        drawPath(p, c)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Deco.Gold) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(7.dp)) {
            val p = Path().apply {
                moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close()
            }
            drawPath(p, color)
        }
        Spacer(Modifier.width(10.dp))
        Text(text.uppercase(), style = DecoType.Caps, color = color)
    }
}

@Composable
fun DecoIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Deco.Cream,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2, color = Deco.Gold),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(iconSize), tint = if (enabled) tint else tint.copy(alpha = 0.35f))
    }
}

/** Primary action: brass-filled stepped plaque. Secondary: hairline outline. */
@Composable
fun DecoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val shape = remember { SteppedShape(4.dp) }
    val base = modifier
        .height(46.dp)
        .then(
            if (filled) Modifier.background(if (enabled) Deco.goldBrush else Brush.linearGradient(listOf(Deco.GoldDim, Deco.GoldDim)), shape)
            else Modifier.border(1.dp, Deco.Gold.copy(alpha = if (enabled) 0.8f else 0.3f), shape)
        )
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 20.dp)
    val fg = if (filled) Deco.Ink else Deco.Gold
    Row(base, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = fg)
            Spacer(Modifier.width(10.dp))
        }
        Text(text.uppercase(), style = DecoType.Caps, color = fg, textAlign = TextAlign.Center)
    }
}
