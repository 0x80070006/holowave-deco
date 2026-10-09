package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.formatDuration
import io.github.x80070006.holowave.lyrics.Lyrics
import io.github.x80070006.holowave.lyrics.LyricsLookup
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.playback.RepeatMode
import io.github.x80070006.holowave.ui.LyricsUi
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.SectionLabel
import io.github.x80070006.holowave.ui.components.SleeveAndRecord
import io.github.x80070006.holowave.ui.components.SteppedShape
import io.github.x80070006.holowave.ui.components.Sunburst
import io.github.x80070006.holowave.ui.components.rememberArtwork
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

class PlayerCallbacks(
    val onClose: () -> Unit,
    val onToggle: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onFavorite: () -> Unit,
    val onLyrics: () -> Unit,
    val onMenu: () -> Unit,
    val onSkipTo: (Int) -> Unit,
)

/** Background shared by the player and the lyrics view: ink, warmed by the cover's colour. */
@Composable
fun accentBackground(track: Track?): Color {
    val accent = rememberArtwork(track, 768)?.accent ?: Deco.Teal
    val c by animateColorAsState(accent, tween(900), label = "accent")
    return c
}

@Composable
fun NowPlayingScreen(
    track: Track?,
    playback: PlaybackState,
    position: State<Long>,
    lyrics: LyricsUi,
    isFavorite: Boolean,
    spin: Boolean,
    upNext: List<Pair<Int, Track>>,
    cb: PlayerCallbacks,
) {
    val accent = accentBackground(track)
    Box(
        Modifier
            .fillMaxSize()
            .background(Deco.Ink)
            .background(Brush.verticalGradient(0f to accent.copy(alpha = 0.55f), 0.55f to Deco.TealDeep.copy(alpha = 0.5f), 1f to Deco.Ink)),
    ) {
        Sunburst(Modifier.fillMaxWidth().height(420.dp), rays = 31, origin = Offset(0.5f, 0.0f), spreadDegrees = 360f, alpha = 0.07f)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                DecoIconButton(DecoIcons.ChevronDown, "Réduire le lecteur", cb.onClose, tint = Deco.Gold)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("EN LECTURE", style = DecoType.Caps, color = Deco.Gold)
                    Spacer(Modifier.height(4.dp))
                    Text(track?.album.orEmpty(), style = DecoType.Small, color = Deco.CreamDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DecoIconButton(DecoIcons.More, "Options du titre", cb.onMenu, tint = Deco.Gold)
            }

            Spacer(Modifier.height(18.dp))
            SleeveAndRecord(track, playback.isPlaying, spin, Modifier.padding(horizontal = 22.dp))
            Spacer(Modifier.height(26.dp))

            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track?.title ?: playback.title, style = DecoType.Display, color = Deco.Cream,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        (track?.artist ?: playback.artist).uppercase(), style = DecoType.Caps.copy(letterSpacing = DecoType.Caps.letterSpacing),
                        color = Deco.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                DecoIconButton(
                    if (isFavorite) DecoIcons.HeartFilled else DecoIcons.Heart,
                    if (isFavorite) "Retirer des coups de cœur" else "Ajouter aux coups de cœur",
                    cb.onFavorite, tint = if (isFavorite) Deco.Copper else Deco.Cream, size = 48.dp,
                )
            }

            Spacer(Modifier.height(20.dp))
            SeekBar(position, playback.durationMs, cb.onSeek, Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(14.dp))
            Controls(playback, cb)
            Spacer(Modifier.height(26.dp))

            LyricsCard(lyrics, position, cb.onLyrics, Modifier.padding(horizontal = 18.dp))

            if (upNext.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionLabel("À suivre", Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                upNext.forEach { (index, t) -> UpNextRow(t) { cb.onSkipTo(index) } }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Controls(playback: PlaybackState, cb: PlayerCallbacks) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecoIconButton(
            DecoIcons.Shuffle, if (playback.shuffle) "Lecture aléatoire activée" else "Lecture aléatoire désactivée",
            cb.onShuffle, tint = if (playback.shuffle) Deco.GoldBright else Deco.Muted, size = 48.dp, iconSize = 22.dp,
        )
        DecoIconButton(DecoIcons.Previous, "Titre précédent", cb.onPrevious, tint = Deco.Cream, size = 56.dp, iconSize = 30.dp)
        PlayButton(playback.isPlaying, cb.onToggle)
        DecoIconButton(DecoIcons.Next, "Titre suivant", cb.onNext, tint = Deco.Cream, size = 56.dp, iconSize = 30.dp)
        Box(contentAlignment = Alignment.Center) {
            DecoIconButton(
                DecoIcons.Repeat,
                when (playback.repeat) { RepeatMode.OFF -> "Répétition désactivée"; RepeatMode.ALL -> "Répéter la file"; RepeatMode.ONE -> "Répéter le titre" },
                cb.onRepeat, tint = if (playback.repeat == RepeatMode.OFF) Deco.Muted else Deco.GoldBright, size = 48.dp, iconSize = 22.dp,
            )
            if (playback.repeat == RepeatMode.ONE) {
                Text("1", style = DecoType.Numeral.copy(fontSize = DecoType.Caps.fontSize), color = Deco.GoldBright, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

/** Brass medallion: concentric rings with a stepped crown, like a 1930s radio dial. */
@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(84.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 42.dp, color = Deco.Ink),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = if (playing) "Pause" else "Lecture" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(84.dp)) {
            val r = size.minDimension / 2
            for (k in 0 until 24) {
                val a = Math.toRadians(k * 15.0)
                val inner = r * 0.86f
                val outer = if (k % 2 == 0) r else r * 0.93f
                drawLine(
                    Deco.Gold.copy(alpha = 0.55f),
                    Offset(center.x + (inner * kotlin.math.cos(a)).toFloat(), center.y + (inner * kotlin.math.sin(a)).toFloat()),
                    Offset(center.x + (outer * kotlin.math.cos(a)).toFloat(), center.y + (outer * kotlin.math.sin(a)).toFloat()),
                    1.2f,
                )
            }
            drawCircle(Deco.goldBrush, r * 0.78f)
            drawCircle(Deco.Ink, r * 0.70f, style = Stroke(1.2f), alpha = 0.6f)
        }
        Icon(if (playing) DecoIcons.Pause else DecoIcons.Play, null, Modifier.size(30.dp), tint = Deco.Ink)
    }
}

@Composable
fun SeekBar(position: State<Long>, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    val pos = position.value
    val fraction = when {
        dragging -> dragFraction
        durationMs > 0 -> (pos.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
                .pointerInput(durationMs) {
                    detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragging = true; dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { dragging = false; if (durationMs > 0) onSeek((dragFraction * durationMs).toLong()) },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { change, _ -> dragFraction = (change.position.x / size.width).coerceIn(0f, 1f) },
                    )
                },
        ) {
            width = size.width
            val cy = size.height / 2
            drawLine(Deco.Cream.copy(alpha = 0.16f), Offset(0f, cy), Offset(size.width, cy), 2f)
            val x = size.width * fraction
            drawLine(Brush.horizontalGradient(listOf(Deco.Copper, Deco.GoldBright), 0f, x.coerceAtLeast(1f)), Offset(0f, cy), Offset(x, cy), 3f)
            val d = if (dragging) 9.dp.toPx() else 6.dp.toPx()
            val diamond = Path().apply { moveTo(x, cy - d); lineTo(x + d, cy); lineTo(x, cy + d); lineTo(x - d, cy); close() }
            drawPath(diamond, Deco.GoldBright)
            drawCircle(Deco.Ink, d * 0.28f, Offset(x, cy))
        }
        Row(Modifier.fillMaxWidth()) {
            val shown = if (dragging) (dragFraction * durationMs).toLong() else pos
            Text(formatDuration(shown), style = DecoType.Numeral, color = Deco.CreamDim)
            Spacer(Modifier.weight(1f))
            Text("-" + formatDuration((durationMs - shown).coerceAtLeast(0)), style = DecoType.Numeral, color = Deco.CreamDim)
        }
    }
}

@Composable
private fun LyricsCard(lyrics: LyricsUi, position: State<Long>, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val shape = remember { SteppedShape(6.dp) }
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Deco.TealDeep.copy(alpha = 0.92f), Deco.Ink.copy(alpha = 0.92f))), shape)
            .border(1.dp, Deco.Gold.copy(alpha = 0.5f), shape)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(DecoIcons.Microphone, null, Modifier.size(18.dp), tint = Deco.Gold)
            Spacer(Modifier.width(10.dp))
            Text("PAROLES", style = DecoType.Caps, color = Deco.Gold, modifier = Modifier.weight(1f))
            Text("Plein écran", style = DecoType.Small, color = Deco.CreamDim)
        }
        Spacer(Modifier.height(14.dp))
        DecoDivider(Modifier.padding(horizontal = 30.dp))
        Spacer(Modifier.height(14.dp))
        when (lyrics) {
            LyricsUi.NoTrack, LyricsUi.Loading -> CardText("Recherche des paroles…", dim = true)
            is LyricsUi.Ready -> when (val l = lyrics.lookup) {
                is LyricsLookup.Available -> when (val ly = l.lyrics) {
                    is Lyrics.Synced -> {
                        val idx = ly.indexAt(position.value)
                        val from = idx.coerceAtLeast(0)
                        ly.lines.subList(from, minOf(ly.lines.size, from + 4)).forEachIndexed { k, line ->
                            val active = idx >= 0 && k == 0
                            Text(
                                line.text.ifEmpty { "♪" },
                                style = DecoType.Title.copy(fontSize = DecoType.Lyric.fontSize * 0.78f, lineHeight = DecoType.Lyric.lineHeight * 0.8f),
                                color = if (active) Deco.GoldBright else Deco.Cream.copy(alpha = 0.45f),
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                    is Lyrics.Plain -> CardText(ly.text.lineSequence().filter { it.isNotBlank() }.take(4).joinToString("\n"))
                    is Lyrics.Instrumental -> CardText("Instrumental — laissez parler les cuivres.", dim = true)
                }
                LyricsLookup.Missing -> CardText("Aucune parole trouvée pour ce morceau. Touchez pour importer un fichier .lrc.", dim = true)
                LyricsLookup.Offline -> CardText("Paroles indisponibles hors ligne. Touchez pour importer un fichier .lrc.", dim = true)
            }
        }
    }
}

@Composable
private fun CardText(text: String, dim: Boolean = false) {
    Text(
        text,
        style = if (dim) DecoType.Body else DecoType.Title.copy(fontSize = DecoType.Lyric.fontSize * 0.78f, lineHeight = DecoType.Lyric.lineHeight * 0.8f),
        color = if (dim) Deco.CreamDim else Deco.Cream,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun UpNextRow(track: Track, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(track, Modifier.size(42.dp).border(1.dp, Deco.Gold.copy(alpha = 0.4f)), size = 192)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = DecoType.Title, color = Deco.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = DecoType.Small, color = Deco.CreamDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(track.durationMs), style = DecoType.Numeral, color = Deco.GoldDim)
    }
}
