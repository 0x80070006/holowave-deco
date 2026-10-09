package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.data.formatLongDuration
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.Sunburst
import io.github.x80070006.holowave.ui.components.TrackRow
import io.github.x80070006.holowave.ui.components.decoFrame
import io.github.x80070006.holowave.ui.components.rememberArtwork
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

@Composable
fun DetailScreen(
    collection: TrackCollection?,
    playback: PlaybackState,
    bottomPadding: State<Int>,
    onBack: () -> Unit,
    onPlay: (List<Track>, Int, Boolean) -> Unit,
    onTrackMenu: (Track, String?) -> Unit,
    onCollectionMenu: (() -> Unit)?,
) {
    if (collection == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = Deco.Gold)
            EmptyState("Disque introuvable", "Cette collection n'existe plus.")
        }
        return
    }
    val accent = rememberArtwork(collection.cover, 384)?.accent ?: Deco.Teal
    val numbered = collection.kind == CollectionKind.ALBUM
    val playlistKey = collection.key.takeIf { collection.kind == CollectionKind.PLAYLIST }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 24).dp)) {
        item(key = "header") {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.45f), Deco.Ink)))) {
                Sunburst(Modifier.matchParentSize(), rays = 25, origin = Offset(0.5f, 0.55f), spreadDegrees = 360f, alpha = 0.08f)
                Column(Modifier.statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = Deco.Gold)
                        Spacer(Modifier.weight(1f))
                        if (onCollectionMenu != null) DecoIconButton(DecoIcons.More, "Options", onCollectionMenu, tint = Deco.Gold)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (collection.kind == CollectionKind.ARTIST) {
                        Box(contentAlignment = Alignment.Center) {
                            CoverImage(collection.cover, Modifier.size(196.dp).clip(CircleShape), size = 512, fallbackSeed = collection.title)
                            Canvas(Modifier.size(212.dp)) {
                                drawCircle(Deco.Gold, size.minDimension / 2 - 1f, style = Stroke(1.5f))
                                drawCircle(Deco.Gold, size.minDimension / 2 - 5.dp.toPx(), alpha = 0.5f, style = Stroke(1f))
                            }
                        }
                    } else {
                        CoverImage(
                            collection.cover,
                            Modifier.size(220.dp).decoFrame(Deco.Gold, inset = 6.dp),
                            size = 512, fallbackSeed = collection.title,
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(kindLabel(collection).uppercase(), style = DecoType.Caps, color = Deco.Gold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        collection.title, style = DecoType.Display, color = Deco.Cream, textAlign = TextAlign.Center,
                        maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 28.dp),
                    )
                    if (collection.kind == CollectionKind.ALBUM) {
                        Spacer(Modifier.height(8.dp))
                        Text(collection.subtitle, style = DecoType.Title, color = Deco.GoldBright, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${collection.tracks.size} titre${if (collection.tracks.size > 1) "s" else ""}  ·  ${formatLongDuration(collection.totalDurationMs)}",
                        style = DecoType.Small, color = Deco.CreamDim,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DecoButton("Lecture", { onPlay(collection.tracks, 0, false) }, icon = DecoIcons.Play, enabled = collection.tracks.isNotEmpty())
                        DecoButton("Aléatoire", { onPlay(collection.tracks, 0, true) }, icon = DecoIcons.Shuffle, filled = false, enabled = collection.tracks.isNotEmpty())
                    }
                    Spacer(Modifier.height(18.dp))
                    DecoDivider(Modifier.padding(horizontal = 40.dp))
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        if (collection.tracks.isEmpty()) {
            item {
                EmptyState(
                    if (collection.kind == CollectionKind.FAVORITES) "Pas encore de coup de cœur" else "Playlist vide",
                    if (collection.kind == CollectionKind.FAVORITES) "Touchez le cœur dans le lecteur pour garder un morceau ici."
                    else "Ajoutez des morceaux depuis le menu « ⋮ » d'un titre.",
                )
            }
        }
        itemsIndexed(collection.tracks, key = { i, t -> "$i-${t.id}" }) { i, t ->
            TrackRow(
                t, t.id == playback.currentId, playback.isPlaying,
                onClick = { onPlay(collection.tracks, i, false) },
                onMore = { onTrackMenu(t, playlistKey) },
                leadingNumber = if (numbered) (t.trackNumber.takeIf { it > 0 } ?: (i + 1)) else null,
            )
        }
    }
}

private fun kindLabel(c: TrackCollection): String = when (c.kind) {
    CollectionKind.ALBUM -> c.tracks.firstOrNull()?.year?.takeIf { it > 0 }?.let { "Album · $it" } ?: "Album"
    CollectionKind.ARTIST -> "Artiste"
    CollectionKind.FOLDER -> "Dossier"
    CollectionKind.PLAYLIST -> "Playlist"
    CollectionKind.FAVORITES -> "Collection"
}
