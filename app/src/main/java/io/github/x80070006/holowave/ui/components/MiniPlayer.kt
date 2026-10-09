package io.github.x80070006.holowave.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

@Composable
fun MiniPlayer(
    track: Track?,
    playback: PlaybackState,
    position: State<Long>,
    spin: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember { SteppedShape(5.dp) }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .background(Brush.horizontalGradient(listOf(Deco.TealDeep, Deco.InkRaised)), shape)
            .border(1.dp, Deco.Gold.copy(alpha = 0.55f), shape)
            .clickable(onClick = onOpen)
            .semantics { contentDescription = "Ouvrir le lecteur" },
    ) {
        Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniRecord(track, playback.isPlaying && spin, Modifier.size(46.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track?.title ?: playback.title, style = DecoType.Title, color = Deco.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(track?.artist ?: playback.artist, style = DecoType.Small, color = Deco.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DecoIconButton(
                if (playback.isPlaying) DecoIcons.Pause else DecoIcons.Play,
                if (playback.isPlaying) "Pause" else "Lecture",
                onToggle, tint = Deco.GoldBright, iconSize = 26.dp,
            )
            DecoIconButton(DecoIcons.Next, "Titre suivant", onNext, tint = Deco.Cream, iconSize = 22.dp)
        }
        // Progress hairline along the bottom edge.
        Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).padding(horizontal = 14.dp)) {
            val f = if (playback.durationMs > 0) (position.value.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f
            drawLine(Deco.Gold.copy(alpha = 0.18f), Offset(0f, 0f), Offset(size.width, 0f), 2f)
            drawLine(Deco.GoldBright, Offset(0f, 0f), Offset(size.width * f, 0f), 2f)
        }
    }
}
