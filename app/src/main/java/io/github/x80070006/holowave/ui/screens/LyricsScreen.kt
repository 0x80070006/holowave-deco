package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.lyrics.Lyrics
import io.github.x80070006.holowave.lyrics.LyricsLookup
import io.github.x80070006.holowave.lyrics.LyricsSource
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.LyricsUi
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.Sunburst
import io.github.x80070006.holowave.ui.components.decoFrame
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

@Composable
fun LyricsScreen(
    track: Track?,
    playback: PlaybackState,
    position: State<Long>,
    lyrics: LyricsUi,
    onlineEnabled: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onImport: () -> Unit,
    onRetry: () -> Unit,
    onEnableOnline: () -> Unit,
) {
    val accent = accentBackground(track)
    Box(
        Modifier
            .fillMaxSize()
            .background(Deco.Ink)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.50f), Deco.TealDeep.copy(alpha = 0.85f), Deco.Ink))),
    ) {
        Sunburst(Modifier.fillMaxSize(), rays = 21, origin = Offset(0.5f, 1.05f), spreadDegrees = 120f, alpha = 0.06f)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                DecoIconButton(DecoIcons.ChevronDown, "Fermer les paroles", onClose, tint = Deco.Gold)
                CoverImage(track, Modifier.size(40.dp).decoFrame(inset = 2.dp, alpha = 0.7f), size = 192)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track?.title ?: playback.title, style = DecoType.Title, color = Deco.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track?.artist ?: playback.artist, style = DecoType.Small, color = Deco.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DecoIconButton(DecoIcons.Import, "Importer un fichier .lrc", onImport, tint = Deco.Gold)
                DecoIconButton(DecoIcons.Refresh, "Rechercher à nouveau", onRetry, tint = Deco.Gold)
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (lyrics) {
                    LyricsUi.NoTrack -> Message("Aucun morceau en lecture", null)
                    LyricsUi.Loading -> Message("Recherche des paroles…", null)
                    is LyricsUi.Ready -> when (val l = lyrics.lookup) {
                        is LyricsLookup.Available -> when (val ly = l.lyrics) {
                            is Lyrics.Synced -> SyncedLyrics(ly, position, onSeek)
                            is Lyrics.Plain -> PlainLyrics(ly)
                            is Lyrics.Instrumental -> Message("Instrumental", "Pas de paroles : laissez parler les cuivres.")
                        }
                        LyricsLookup.Missing -> Message(
                            "Aucune parole trouvée",
                            "Ni le fichier ni LRCLIB ne connaissent ce morceau. Vous pouvez importer un fichier .lrc : il sera gardé sur l'appareil.",
                        ) { DecoButton("Importer un .lrc", onImport, icon = DecoIcons.Import) }
                        LyricsLookup.Offline -> Message(
                            "Hors ligne",
                            if (onlineEnabled) "LRCLIB est injoignable pour le moment. Les paroles déjà téléchargées restent disponibles sans réseau."
                            else "La recherche en ligne est désactivée. Activez-la ou importez un fichier .lrc.",
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (!onlineEnabled) {
                                    DecoButton("Activer LRCLIB", onEnableOnline)
                                    Spacer(Modifier.height(12.dp))
                                }
                                DecoButton("Importer un .lrc", onImport, icon = DecoIcons.Import, filled = onlineEnabled)
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SeekBar(position, playback.durationMs, onSeek, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                DecoIconButton(
                    if (playback.isPlaying) DecoIcons.Pause else DecoIcons.Play,
                    if (playback.isPlaying) "Pause" else "Lecture",
                    onToggle, tint = Deco.GoldBright, size = 52.dp, iconSize = 30.dp,
                )
            }
            (lyrics as? LyricsUi.Ready)?.lookup?.let { it as? LyricsLookup.Available }?.lyrics?.let {
                Text(
                    sourceLabel(it.source).uppercase(), style = DecoType.Caps.copy(fontSize = DecoType.Caps.fontSize * 0.85f),
                    color = Deco.GoldDim, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun sourceLabel(s: LyricsSource) = when (s) {
    LyricsSource.EMBEDDED -> "Paroles intégrées au fichier"
    LyricsSource.IMPORTED -> "Paroles importées"
    LyricsSource.LRCLIB -> "Paroles · LRCLIB · disponibles hors ligne"
}

@Composable
private fun SyncedLyrics(lyrics: Lyrics.Synced, position: State<Long>, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val current by remember(lyrics) { derivedStateOf { lyrics.indexAt(position.value) } }
    // Manual scrolling pauses the auto-follow for a few seconds, like on streaming apps.
    var lastUserScroll by remember { mutableLongStateOf(0L) }
    var following by remember { mutableStateOf(true) }

    // Drag interactions are only emitted for touch scrolling, never for animateScrollToItem.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) {
                following = false
                lastUserScroll = System.currentTimeMillis()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        LaunchedEffect(current, following, lastUserScroll) {
            if (!following) {
                kotlinx.coroutines.delay(4_000 - (System.currentTimeMillis() - lastUserScroll).coerceAtMost(4_000))
                following = true
                return@LaunchedEffect
            }
            // The list's top padding already places item 0 of the viewport at the anchor line.
            listState.animateScrollToItem(current.coerceAtLeast(0))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // Fade lines out at both edges instead of cutting them under the header.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.08f to Color.Black, 0.88f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = maxHeight * 0.32f, bottom = maxHeight * 0.6f),
        ) {
            itemsIndexed(lyrics.lines, key = { i, _ -> i }) { i, line ->
                LyricLineText(line.text, i == current, i < current) { onSeek(line.timeMs) }
            }
        }
        if (!following) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                DecoButton("Resynchroniser", { following = true }, icon = DecoIcons.Sync, filled = false)
            }
        }
    }
}

@Composable
private fun LyricLineText(text: String, active: Boolean, past: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        when {
            active -> Deco.GoldBright
            past -> Deco.Cream.copy(alpha = 0.55f)
            else -> Deco.Cream.copy(alpha = 0.30f)
        },
        tween(350), label = "lyricColor",
    )
    val scale by animateFloatAsState(if (active) 1f else 0.94f, tween(350), label = "lyricScale")
    Text(
        text.ifEmpty { "♪  ♪  ♪" },
        style = DecoType.Lyric,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun PlainLyrics(lyrics: Lyrics.Plain) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp)) {
        Text(
            "Paroles non synchronisées",
            style = DecoType.Caps, color = Deco.GoldDim,
        )
        Spacer(Modifier.height(18.dp))
        Text(lyrics.text, style = DecoType.Lyric.copy(fontSize = DecoType.Lyric.fontSize * 0.8f, lineHeight = DecoType.Lyric.lineHeight * 0.85f), color = Deco.Cream.copy(alpha = 0.9f))
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun Message(title: String, body: String?, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(title, style = DecoType.Display, color = Deco.GoldBright, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(14.dp))
            Text(body, style = DecoType.Body, color = Deco.CreamDim, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(26.dp))
            action()
        }
    }
}
