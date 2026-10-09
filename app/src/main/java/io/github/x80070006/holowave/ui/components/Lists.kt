package io.github.x80070006.holowave.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.data.formatDuration
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

/** Three brass bars bouncing next to the track that is playing. */
@Composable
fun EqualizerBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = Deco.Gold) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 380, 640).mapIndexed { i, ms ->
        t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(ms), RepeatMode.Reverse), label = "bar$i")
    }
    val heights = bars.map { if (playing) it.value else 0.3f }
    Canvas(modifier.size(14.dp)) {
        val w = size.width / 5
        heights.forEachIndexed { i, h ->
            val bh = size.height * h
            drawRect(color, Offset(i * 2 * w, size.height - bh), Size(w, bh))
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    leadingNumber: Int? = null,
) {
    val shape = remember { SteppedShape(3.dp) }
    Row(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onMore)
            .padding(start = 20.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingNumber != null) {
            Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                if (isCurrent) EqualizerBars(isPlaying)
                else Text("%02d".format(leadingNumber), style = DecoType.Numeral, color = Deco.GoldDim)
            }
        } else {
            Box(Modifier.size(48.dp).clip(shape)) {
                CoverImage(track, Modifier.size(48.dp), size = 192)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title, style = DecoType.Title, color = if (isCurrent) Deco.GoldBright else Deco.Cream,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent && leadingNumber == null) {
                    EqualizerBars(isPlaying, Modifier.size(11.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "${track.artist}  ·  ${formatDuration(track.durationMs)}",
                    style = DecoType.Small, color = Deco.CreamDim, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DecoIconButton(DecoIcons.More, "Options pour ${track.title}", onMore, tint = Deco.CreamDim, iconSize = 20.dp)
    }
}

@Composable
fun CollectionCard(collection: TrackCollection, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.combinedClickable(onClick = onClick).padding(bottom = 6.dp)) {
        CoverImage(
            collection.cover,
            Modifier.fillMaxWidth().aspectRatio(1f).decoFrame(Deco.Gold, inset = 4.dp, alpha = 0.75f),
            size = 384,
            fallbackSeed = collection.title,
        )
        Spacer(Modifier.height(10.dp))
        Text(collection.title, style = DecoType.Title, color = Deco.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(collection.subtitle, style = DecoType.Small, color = Deco.CreamDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Artist row: a round "medallion" from one of their covers, framed in brass. */
@Composable
fun ArtistRow(collection: TrackCollection, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            CoverImage(collection.cover, Modifier.size(56.dp).clip(androidx.compose.foundation.shape.CircleShape), size = 192, fallbackSeed = collection.title)
            Canvas(Modifier.size(56.dp)) {
                drawCircle(Deco.Gold, size.minDimension / 2 - 1f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f))
                drawCircle(Deco.Gold, size.minDimension / 2 - 4.dp.toPx(), alpha = 0.45f, style = androidx.compose.ui.graphics.drawscope.Stroke(0.8f))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(collection.title, style = DecoType.Title, color = Deco.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(collection.subtitle, style = DecoType.Small, color = Deco.CreamDim, maxLines = 1)
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp, 70.dp)) {
            Sunburst(Modifier.matchParentSize(), alpha = 0.5f, rays = 13)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = DecoType.Headline, color = Deco.GoldBright, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = DecoType.Body, color = Deco.CreamDim, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Deco.goldLine, alpha = 0.35f))
}

@Composable
fun TabsRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        items(labels.size) { i ->
            val active = i == selected
            Column(
                Modifier.combinedClickable(onClick = { onSelect(i) }).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(labels[i].uppercase(), style = DecoType.Caps, color = if (active) Deco.GoldBright else Deco.Muted)
                Spacer(Modifier.height(7.dp))
                Canvas(Modifier.width(26.dp).height(5.dp)) {
                    if (active) {
                        val cy = size.height / 2
                        drawLine(Deco.Gold, Offset(0f, cy), Offset(size.width, cy), 1.2f)
                        val d = size.height / 2
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2, cy - d); lineTo(size.width / 2 + d, cy); lineTo(size.width / 2, cy + d); lineTo(size.width / 2 - d, cy); close()
                        }
                        drawPath(p, Deco.GoldBright)
                    }
                }
            }
        }
    }
}
