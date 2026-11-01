package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.ScanProgress
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.ui.HomeData
import io.github.x80070006.holowave.ui.HomeFilter
import io.github.x80070006.holowave.ui.components.CollectionCard
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.ScanBanner
import io.github.x80070006.holowave.ui.components.rememberArtwork
import io.github.x80070006.holowave.ui.components.running
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme
import java.util.Calendar

@Composable
fun HomeScreen(
    home: HomeData,
    filter: HomeFilter,
    onFilter: (HomeFilter) -> Unit,
    libraryEmpty: Boolean,
    loading: Boolean,
    scan: ScanProgress?,
    bottomPadding: State<Int>,
    onOpen: (CollectionKind, String) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onSeeAll: () -> Unit,
    onChooseFolders: () -> Unit,
    onScan: () -> Unit,
) {
    val c = HoloTheme.colors
    // The top of the screen is washed with the colours of the last thing played.
    val tint by animateColorAsState(rememberArtwork(home.lastTrack, 256)?.accent ?: c.primary, tween(700), label = "homeTint")
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 24).dp)) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(0f to tint.copy(alpha = 0.40f), 1f to Color.Transparent))
                    .statusBarsPadding()
                    .padding(top = 20.dp, bottom = 12.dp),
            ) {
                Text(greeting(hour), style = DecoType.Display, color = c.text, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(16.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HomeFilter.entries.toList()) { f ->
                        val active = f == filter
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) c.secondary else c.surfaceHigh)
                                .clickable(role = Role.Tab) { onFilter(f) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        ) {
                            Text(f.label, style = DecoType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = if (active) c.onSecondary else c.text)
                        }
                    }
                }
                if (scan != null && scan.running) {
                    Spacer(Modifier.height(12.dp))
                    ScanBanner(scan)
                }
                if (home.resume.isNotEmpty() && filter != HomeFilter.MIXES) {
                    Spacer(Modifier.height(18.dp))
                    ResumeGrid(home.resume, onOpen, Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        when {
            filter == HomeFilter.MIXES -> item {
                EmptyState(
                    "Vos mixes arrivent",
                    "Ils se construiront à partir de ce que vous écoutez, sur l'appareil. Écoutez quelques morceaux pour commencer.",
                )
            }
            libraryEmpty && !loading -> item {
                EmptyState(
                    "Votre salon est vide",
                    "Choisissez les dossiers où se trouve votre musique, ou lancez une recherche sur tout l'appareil.",
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DecoButton("Choisir mes dossiers", onChooseFolders)
                        DecoButton("Lancer une recherche", onScan, filled = false)
                    }
                }
            }
            else -> {
                if (home.recentTracks.isNotEmpty()) {
                    item { SectionHeader("Écoutés récemment", onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(home.recentTracks.size) { i ->
                                val t = home.recentTracks[i]
                                TrackCard(t) { onPlay(home.recentTracks, i) }
                            }
                        }
                    }
                }
                if (home.addedAlbums.isNotEmpty()) {
                    item { SectionHeader("Ajoutés récemment", onSeeAll) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(home.addedAlbums, key = { it.key }) { a ->
                                CollectionCard(a, { onOpen(CollectionKind.ALBUM, a.key) }, Modifier.width(144.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun greeting(hour: Int) = when (hour) {
    in 5..11 -> "Bonjour"
    in 12..17 -> "Bon après-midi"
    in 18..22 -> "Bonsoir"
    else -> "Bonne nuit"
}

/** Two columns of compact tiles: the things you are most likely to want next. */
@Composable
private fun ResumeGrid(tiles: List<TrackCollection>, onOpen: (CollectionKind, String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { t -> ResumeTile(t, { onOpen(t.kind, t.key) }, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResumeTile(collection: TrackCollection, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = HoloTheme.colors
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(8.dp)).background(c.surfaceHigh).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(collection.cover, Modifier.size(56.dp), size = 192, fallbackSeed = collection.title)
        Text(
            collection.title, style = DecoType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = c.text,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    val c = HoloTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = DecoType.Headline, color = c.text, modifier = Modifier.weight(1f))
        Text("Tout voir", style = DecoType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = c.secondary, modifier = Modifier.clickable(onClick = onSeeAll).padding(8.dp))
    }
}

@Composable
private fun TrackCard(track: Track, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Column(Modifier.width(144.dp).clickable(onClick = onClick)) {
        CoverImage(track, Modifier.size(144.dp).clip(RoundedCornerShape(16.dp)), size = 384)
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
