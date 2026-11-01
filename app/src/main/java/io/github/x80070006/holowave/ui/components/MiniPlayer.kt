package io.github.x80070006.holowave.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating mini player. Swipe sideways to change track (left = next, right = previous),
 * swipe up or tap to open the full player. The card follows the finger and springs back.
 */
@Composable
fun MiniPlayer(
    track: Track?,
    playback: PlaybackState,
    position: State<Long>,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = HoloTheme.colors
    val shape = remember { RoundedCornerShape(16.dp) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 88.dp.toPx() }

    Box(
        modifier
            .padding(horizontal = 12.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(shape)
            .background(c.surfaceHigh)
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                        scope.launch {
                            if (abs(dx) >= abs(dy)) offsetX.snapTo(dx * 0.6f) else offsetY.snapTo(dy.coerceAtMost(0f) * 0.4f)
                        }
                    },
                    onDragEnd = {
                        when {
                            abs(dx) > threshold && abs(dx) > abs(dy) -> {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                if (dx < 0) onNext() else onPrevious()
                            }
                            -dy > threshold * 0.7f && abs(dy) > abs(dx) -> onOpen()
                        }
                        scope.launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                        scope.launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f) }
                        scope.launch { offsetY.animateTo(0f) }
                    },
                )
            }
            .clickable(onClick = onOpen)
            .semantics { contentDescription = "Ouvrir le lecteur. Glisser sur le côté pour changer de titre." },
    ) {
        Row(Modifier.padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverImage(track, Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)), size = 192)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track?.title ?: playback.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(track?.artist ?: playback.artist, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(c.primary).clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onToggle()
                },
                contentAlignment = Alignment.Center,
            ) {
                PlayPauseIcon(playback.isPlaying, c.onPrimary, Modifier.size(24.dp))
            }
        }
        // Thin progress line along the bottom edge.
        Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).padding(horizontal = 12.dp)) {
            val f = if (playback.durationMs > 0) (position.value.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f
            val r = CornerRadius(size.height / 2)
            drawRoundRect(c.text.copy(alpha = 0.14f), Offset.Zero, Size(size.width, size.height), r)
            if (f > 0f) drawRoundRect(c.primary, Offset.Zero, Size(size.width * f, size.height), r)
        }
    }
}
