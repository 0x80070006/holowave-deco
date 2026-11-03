package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.playback.SourceState
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.PlayPauseIcon
import io.github.x80070006.holowave.ui.components.SourceBadge
import io.github.x80070006.holowave.ui.components.StreamCover
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme

/**
 * Full player for a song played by NouTube. Same layout and controls as the local player;
 * shuffle, repeat, favourites and lyrics are left out because NouTube owns the queue.
 */
@Composable
fun StreamNowPlayingScreen(
    state: SourceState,
    position: State<Long>,
    nouTubeAvailable: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenNouTube: () -> Unit,
) {
    val c = HoloTheme.colors
    val haptic = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                DecoIconButton(DecoIcons.ChevronDown, "Réduire le lecteur", onClose, tint = c.text)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lecture en cours", style = DecoType.Small, color = c.textSecondary)
                    Text(state.album.ifBlank { "YouTube Music" }, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(16.dp))
            StreamCover(
                state.artwork, state.artist + state.title,
                Modifier.padding(horizontal = 24.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.height(28.dp))

            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(state.title, style = DecoType.Display, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge("YouTube")
                    Spacer(Modifier.size(8.dp))
                    Text(state.artist, style = DecoType.Title, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(22.dp))
            SeekBar(position, state.durationMs, onSeek, Modifier.padding(horizontal = 24.dp), playing = state.isPlaying)
            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DecoIconButton(DecoIcons.Previous, "Titre précédent", onPrevious, tint = c.text, size = 60.dp, iconSize = 34.dp)
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(c.primary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = c.onPrimary),
                            role = Role.Button,
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onToggle()
                        }
                        .semantics { contentDescription = if (state.isPlaying) "Pause" else "Lecture" },
                    contentAlignment = Alignment.Center,
                ) {
                    PlayPauseIcon(state.isPlaying, c.onPrimary, Modifier.size(40.dp))
                }
                DecoIconButton(DecoIcons.Next, "Titre suivant", onNext, tint = c.text, size = 60.dp, iconSize = 34.dp)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                if (nouTubeAvailable) "NouTube joue ce titre et garde la file d'attente." else "NouTube n'est pas installé.",
                style = DecoType.Small, color = c.textSecondary, modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (nouTubeAvailable) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.surfaceHigh)
                        .clickable(role = Role.Button, onClick = onOpenNouTube)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Ouvrir dans NouTube", style = DecoType.Title, color = c.text) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
