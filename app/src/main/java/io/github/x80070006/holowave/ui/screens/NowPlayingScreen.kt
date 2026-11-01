package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.data.formatDuration
import io.github.x80070006.holowave.data.formatLongDuration
import io.github.x80070006.holowave.lyrics.Lyrics
import io.github.x80070006.holowave.lyrics.LyricsLookup
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.playback.RepeatMode
import io.github.x80070006.holowave.ui.LyricsUi
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.PlayPauseIcon
import io.github.x80070006.holowave.ui.components.rememberArtwork
import io.github.x80070006.holowave.ui.theme.Appearance
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme
import io.github.x80070006.holowave.ui.theme.MotionPref
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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
    val onQueue: () -> Unit,
    val onSleep: () -> Unit,
    val onPlayTracks: (List<Track>, Int) -> Unit,
    val onOpenAlbum: (String) -> Unit,
)

/**
 * Colour that washes the top of player-like screens: the cover's when the cover option is
 * on, the theme's primary otherwise.
 */
@Composable
fun accentBackground(track: Track?): Color {
    val c = HoloTheme.colors
    val cover = rememberArtwork(track, 768)?.accent
    val target = if (Appearance.coverColors && cover != null) cover else c.primary
    val animated by animateColorAsState(target, tween(900), label = "accent")
    return animated
}

/** How strongly the accent tints a player background. */
internal fun tintStrength(): Float = if (Appearance.isDark) 0.46f else 0.34f

@Composable
fun NowPlayingScreen(
    track: Track?,
    playback: PlaybackState,
    position: State<Long>,
    lyrics: LyricsUi,
    isFavorite: Boolean,
    upNext: List<Pair<Int, Track>>,
    album: TrackCollection?,
    sameArtist: List<Track>,
    sleepActive: Boolean,
    cb: PlayerCallbacks,
) {
    val c = HoloTheme.colors
    val accent = accentBackground(track)
    val haptic = LocalHapticFeedback.current

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .background(Brush.verticalGradient(0f to accent.copy(alpha = tintStrength()), 0.6f to accent.copy(alpha = tintStrength() * 0.22f), 1f to Color.Transparent)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ---- top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                DecoIconButton(DecoIcons.ChevronDown, "Réduire le lecteur", cb.onClose, tint = c.text)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lecture en cours", style = DecoType.Small, color = c.textSecondary)
                    Text(track?.album.orEmpty(), style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DecoIconButton(DecoIcons.More, "Options du titre", cb.onMenu, tint = c.text)
            }

            Spacer(Modifier.height(16.dp))
            SwipeCover(track, accent, cb, Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(28.dp))

            // ---- title and favourite
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track?.title ?: playback.title, style = DecoType.Display, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(track?.artist ?: playback.artist, style = DecoType.Title, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DecoIconButton(
                    if (isFavorite) DecoIcons.HeartFilled else DecoIcons.Heart,
                    if (isFavorite) "Retirer des titres aimés" else "Ajouter aux titres aimés",
                    { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); cb.onFavorite() },
                    tint = if (isFavorite) c.tertiary else c.text, size = 52.dp, iconSize = 28.dp,
                )
            }

            Spacer(Modifier.height(22.dp))
            SeekBar(position, playback.durationMs, cb.onSeek, Modifier.padding(horizontal = 24.dp), playing = playback.isPlaying)
            Spacer(Modifier.height(10.dp))
            Controls(playback, cb)
            Spacer(Modifier.height(18.dp))

            // ---- queue, lyrics, sleep timer
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Action(DecoIcons.Queue, "File d'attente", false, cb.onQueue)
                Action(DecoIcons.Microphone, "Paroles", false, cb.onLyrics)
                Action(DecoIcons.Moon, "Minuterie", sleepActive, cb.onSleep)
            }

            Spacer(Modifier.height(28.dp))
            LyricsCard(lyrics, position, cb.onLyrics, Modifier.padding(horizontal = 16.dp))

            if (upNext.isNotEmpty()) {
                Section("File d'attente", "Tout voir", cb.onQueue)
                Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(c.surface)) {
                    upNext.forEach { (index, t) -> UpNextRow(t) { cb.onSkipTo(index) } }
                }
            }

            if (album != null) {
                Section("À propos de l'album")
                AlbumCard(album, Modifier.padding(horizontal = 16.dp)) { cb.onOpenAlbum(album.key) }
            }

            if (sameArtist.size > 1) {
                Section("Du même artiste")
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(sameArtist.size) { i ->
                        val t = sameArtist[i]
                        Column(Modifier.width(132.dp).clickable { cb.onPlayTracks(sameArtist, i) }) {
                            CoverImage(t, Modifier.size(132.dp).clip(RoundedCornerShape(16.dp)), size = 384)
                            Spacer(Modifier.height(8.dp))
                            Text(t.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.album, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

/** Large cover with a coloured shadow. Swipe it sideways to change track. */
@Composable
private fun SwipeCover(track: Track?, accent: Color, cb: PlayerCallbacks, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val shape = remember { RoundedCornerShape(28.dp) }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .shadow(28.dp, shape, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { change, drag ->
                        change.consume()
                        total += drag
                        scope.launch { offset.snapTo(total * 0.5f) }
                    },
                    onDragEnd = {
                        if (abs(total) > threshold) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            if (total < 0) cb.onNext() else cb.onPrevious()
                        }
                        scope.launch { offset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                )
            }
            .semantics { contentDescription = "Pochette. Glisser sur le côté pour changer de titre." },
    ) {
        Crossfade(track?.id, animationSpec = tween(350), label = "cover") {
            CoverImage(track, Modifier.fillMaxSize(), size = 768)
        }
    }
}

@Composable
private fun Controls(playback: PlaybackState, cb: PlayerCallbacks) {
    val c = HoloTheme.colors
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecoIconButton(
            DecoIcons.Shuffle, if (playback.shuffle) "Lecture aléatoire activée" else "Lecture aléatoire désactivée",
            cb.onShuffle, tint = if (playback.shuffle) c.primary else c.textSecondary, size = 52.dp, iconSize = 24.dp,
        )
        DecoIconButton(DecoIcons.Previous, "Titre précédent", cb.onPrevious, tint = c.text, size = 60.dp, iconSize = 34.dp)
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
                    cb.onToggle()
                }
                .semantics { contentDescription = if (playback.isPlaying) "Pause" else "Lecture" },
            contentAlignment = Alignment.Center,
        ) {
            PlayPauseIcon(playback.isPlaying, c.onPrimary, Modifier.size(40.dp))
        }
        DecoIconButton(DecoIcons.Next, "Titre suivant", cb.onNext, tint = c.text, size = 60.dp, iconSize = 34.dp)
        Box(contentAlignment = Alignment.Center) {
            DecoIconButton(
                DecoIcons.Repeat,
                when (playback.repeat) { RepeatMode.OFF -> "Répétition désactivée"; RepeatMode.ALL -> "Répéter la file"; RepeatMode.ONE -> "Répéter le titre" },
                cb.onRepeat, tint = if (playback.repeat == RepeatMode.OFF) c.textSecondary else c.primary, size = 52.dp, iconSize = 24.dp,
            )
            if (playback.repeat == RepeatMode.ONE) {
                Text("1", style = DecoType.Small.copy(fontWeight = FontWeight.Bold), color = c.primary, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = if (active) c.primary else c.text)
        Spacer(Modifier.height(4.dp))
        Text(label, style = DecoType.Small, color = if (active) c.primary else c.textSecondary)
    }
}

@Composable
private fun Section(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = HoloTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = DecoType.Headline, color = c.text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = DecoType.Small.copy(fontWeight = FontWeight.Bold), color = c.secondary, modifier = Modifier.clickable(onClick = onAction).padding(8.dp))
        }
    }
}

@Composable
private fun AlbumCard(album: TrackCollection, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(album.cover, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)), size = 192, fallbackSeed = album.title)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(album.title, style = DecoType.Title, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(album.subtitle, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            val year = album.tracks.firstOrNull()?.year?.takeIf { it > 0 }
            Text(
                listOfNotNull(year?.toString(), "${album.tracks.size} titre${if (album.tracks.size > 1) "s" else ""}", formatLongDuration(album.totalDurationMs)).joinToString(" · "),
                style = DecoType.Small, color = c.textSecondary,
            )
        }
    }
}

/**
 * Thick, draggable progress bar. While playing, the elapsed part gently undulates
 * (disabled with the animations setting); dragging flattens it so the thumb stays exact.
 */
@Composable
fun SeekBar(position: State<Long>, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier, playing: Boolean = false) {
    val c = HoloTheme.colors
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val pos = position.value
    val fraction = when {
        dragging -> dragFraction
        durationMs > 0 -> (pos.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }
    val wavy = playing && !dragging && Appearance.effectiveMotion == MotionPref.FULL
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(0f, (2.0 * PI).toFloat(), infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "phase")
    val haptic = LocalHapticFeedback.current

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
                .pointerInput(durationMs) {
                    detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { o ->
                            dragging = true
                            dragFraction = (o.x / size.width).coerceIn(0f, 1f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = { dragging = false; if (durationMs > 0) onSeek((dragFraction * durationMs).toLong()) },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { change, _ -> dragFraction = (change.position.x / size.width).coerceIn(0f, 1f) },
                    )
                },
        ) {
            val cy = size.height / 2
            val stroke = 6.dp.toPx()
            val x = size.width * fraction
            val amp = if (wavy) 2.6.dp.toPx() else 0f
            val len = 24.dp.toPx()
            val k = (2.0 * PI).toFloat() / len
            // Remaining part: flat track.
            drawLine(c.text.copy(alpha = 0.18f), Offset(x, cy), Offset(size.width, cy), stroke, StrokeCap.Round)
            // Elapsed part: flat or undulating.
            if (amp == 0f) {
                drawLine(c.primary, Offset(0f, cy), Offset(x, cy), stroke, StrokeCap.Round)
            } else {
                val path = Path()
                var px = 0f
                path.moveTo(0f, cy)
                while (px <= x) {
                    path.lineTo(px, cy + amp * sin(k * px - phase).toFloat())
                    px += 3f
                }
                drawPath(path, c.primary, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val ty = cy + if (amp == 0f) 0f else amp * sin(k * x - phase).toFloat()
            drawCircle(c.primary, if (dragging) 13.dp.toPx() else 9.dp.toPx(), Offset(x, ty))
        }
        Row(Modifier.fillMaxWidth()) {
            val shown = if (dragging) (dragFraction * durationMs).toLong() else pos
            Text(formatDuration(shown), style = DecoType.Numeral, color = c.textSecondary)
            Spacer(Modifier.weight(1f))
            Text("-" + formatDuration((durationMs - shown).coerceAtLeast(0)), style = DecoType.Numeral, color = c.textSecondary)
        }
    }
}

@Composable
private fun LyricsCard(lyrics: LyricsUi, position: State<Long>, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = HoloTheme.colors
    val shape = remember { RoundedCornerShape(24.dp) }
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.surface).clickable(onClick = onOpen).padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(DecoIcons.Microphone, null, Modifier.size(20.dp), tint = c.primary)
            Spacer(Modifier.width(10.dp))
            Text("Paroles", style = DecoType.Title, color = c.primary, modifier = Modifier.weight(1f))
            Text("Plein écran", style = DecoType.Small, color = c.textSecondary)
        }
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
                                style = DecoType.Title.copy(fontSize = DecoType.Title.fontSize * 1.2f, lineHeight = DecoType.Title.lineHeight * 1.2f),
                                color = if (active) c.primary else c.text.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                    is Lyrics.Plain -> CardText(ly.text.lineSequence().filter { it.isNotBlank() }.take(4).joinToString("\n"))
                    is Lyrics.Instrumental -> CardText("Instrumental : pas de paroles pour celui-ci.", dim = true)
                }
                LyricsLookup.Missing -> CardText("Aucune parole trouvée pour ce morceau. Touchez pour importer un fichier .lrc.", dim = true)
                LyricsLookup.Offline -> CardText("Paroles indisponibles hors ligne. Touchez pour importer un fichier .lrc.", dim = true)
            }
        }
    }
}

@Composable
private fun CardText(text: String, dim: Boolean = false) {
    val c = HoloTheme.colors
    Text(text, style = DecoType.Body, color = if (dim) c.textSecondary else c.text)
}

@Composable
private fun UpNextRow(track: Track, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(track, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), size = 192)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(track.durationMs), style = DecoType.Numeral, color = c.textSecondary)
    }
}
