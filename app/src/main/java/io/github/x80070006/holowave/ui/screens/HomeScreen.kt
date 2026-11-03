package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.x80070006.holowave.tube.TubeHost
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.TubeSurface
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
import androidx.compose.foundation.layout.heightIn
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
import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.ScanProgress
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.ui.HomeData
import io.github.x80070006.holowave.ui.HomeFilter
import io.github.x80070006.holowave.ui.components.CollectionCard
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.ScanBanner
import io.github.x80070006.holowave.ui.components.SourceBadge
import io.github.x80070006.holowave.ui.components.StreamedCover
import io.github.x80070006.holowave.ui.components.rememberArtwork
import io.github.x80070006.holowave.ui.components.running
import io.github.x80070006.holowave.recommend.FlowVariant
import io.github.x80070006.holowave.recommend.Shelf
import io.github.x80070006.holowave.ui.theme.DecoIcons
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
    onOpenStream: (StreamedTrack) -> Unit,
    onSeeAll: () -> Unit,
    onChooseFolders: () -> Unit,
    onScan: () -> Unit,
    flowActive: FlowVariant?,
    onFlow: (FlowVariant) -> Unit,
    tube: TubeHost,
    youtubeSignedIn: Boolean,
    onSignIn: () -> Unit,
    onImportTop: () -> Unit,
    importStatus: String?,
    shelves: List<Shelf> = emptyList(),
    onPlayShelf: (Shelf, Int) -> Unit = { _, _ -> },
) {
    var explore by rememberSaveable { mutableStateOf(false) }
    if (filter == HomeFilter.NOUTUBE && explore) {
        ExploreYoutube(tube, youtubeSignedIn, bottomPadding, filter, onFilter, onSignIn, onShowList = { explore = false })
        return
    }
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
                FilterChips(filter, onFilter)
                if (filter == HomeFilter.NOUTUBE) {
                    Spacer(Modifier.height(12.dp))
                    NouTubeViews(explore = false, onChange = { explore = it }, Modifier.padding(horizontal = 16.dp))
                }
                if (scan != null && scan.running) {
                    Spacer(Modifier.height(12.dp))
                    ScanBanner(scan)
                }
                if (filter != HomeFilter.MIXES && filter != HomeFilter.NOUTUBE && !libraryEmpty) {
                    Spacer(Modifier.height(16.dp))
                    FlowButton(active = flowActive != null, onClick = { onFlow(FlowVariant.MIX) }, Modifier.padding(horizontal = 16.dp))
                }
                if (home.resume.isNotEmpty() && filter != HomeFilter.MIXES) {
                    Spacer(Modifier.height(18.dp))
                    ResumeGrid(home.resume, onOpen, Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        when {
            filter == HomeFilter.MIXES && libraryEmpty -> item {
                EmptyState("Pas encore de mixes", "Ajoutez de la musique à l'appareil pour que les mixes puissent se construire.")
            }
            filter == HomeFilter.MIXES -> {
                item { Spacer(Modifier.height(8.dp)) }
                items(FlowVariant.entries.toList()) { v -> MixRow(v, active = flowActive == v) { onFlow(v) } }
            }
            filter == HomeFilter.NOUTUBE && home.streams.isEmpty() -> item {
                EmptyState(
                    "Vos titres YouTube Music",
                    if (youtubeSignedIn) "Importez vos 100 titres les plus écoutés depuis votre compte, ou écoutez-en dans l'onglet Explorer."
                    else "Connectez votre compte YouTube pour retrouver ici vos 100 titres les plus écoutés.",
                ) {
                    if (youtubeSignedIn) DecoButton("Importer mon Top 100", onImportTop) else DecoButton("Se connecter", onSignIn)
                }
            }
            filter == HomeFilter.NOUTUBE -> {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Vos titres les plus écoutés", style = DecoType.Headline, color = c.text)
                            importStatus?.let { Text(it, style = DecoType.Small, color = c.textSecondary) }
                        }
                        if (youtubeSignedIn) {
                            Text(
                                "Actualiser", style = DecoType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = c.secondary,
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onImportTop).padding(12.dp),
                            )
                        }
                    }
                }
                items(home.streams, key = { it.key }) { s -> StreamRow(s) { onOpenStream(s) } }
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
                if (home.recent.isNotEmpty()) {
                    item { SectionHeader("Écoutés récemment", onSeeAll) }
                    item {
                        val locals = home.recent.filterIsInstance<MixedItem.Local>().map { it.track }
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(home.recent.size) { i ->
                                when (val m = home.recent[i]) {
                                    is MixedItem.Local -> TrackCard(m.track) { onPlay(locals, locals.indexOf(m.track)) }
                                    is MixedItem.Stream -> StreamCard(m.track) { onOpenStream(m.track) }
                                }
                            }
                        }
                    }
                }
                if (filter == HomeFilter.ALL) {
                    shelves.forEach { shelf ->
                        item(key = "shelf-h-" + shelf.id) { ShelfHeader(shelf.title) }
                        item(key = "shelf-" + shelf.id) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (shelf.collections.isNotEmpty()) {
                                    items(shelf.collections, key = { it.kind.name + it.key }) { col ->
                                        CollectionCard(col, { onOpen(col.kind, col.key) }, Modifier.width(144.dp))
                                    }
                                } else {
                                    items(shelf.songs.size) { i ->
                                        when (val m = shelf.songs[i]) {
                                            is MixedItem.Local -> TrackCard(m.track) { onPlayShelf(shelf, i) }
                                            is MixedItem.Stream -> StreamCard(m.track) { onPlayShelf(shelf, i) }
                                        }
                                    }
                                }
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

/** The one big action of the home screen: start a Flow, an endless queue picked from what you listen to. */
@Composable
private fun FlowButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = HoloTheme.colors
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(c.primary)
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(DecoIcons.Play, null, Modifier.size(22.dp), tint = c.onPrimary)
        Spacer(Modifier.width(10.dp))
        Text(if (active) "Flow en cours" else "Flow", style = DecoType.Title, color = c.onPrimary)
    }
}

@Composable
private fun MixRow(variant: FlowVariant, active: Boolean, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(variant.label, style = DecoType.Title, color = c.text)
            Text(variant.hint, style = DecoType.Small, color = c.textSecondary)
        }
        androidx.compose.material3.Icon(DecoIcons.Play, if (active) "En cours" else "Lancer", Modifier.size(24.dp), tint = if (active) c.primary else c.textSecondary)
    }
}

@Composable
private fun FilterChips(filter: HomeFilter, onFilter: (HomeFilter) -> Unit) {
    val c = HoloTheme.colors
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
}

/** "Mes titres" or "Explorer": the list of songs, or YouTube Music itself. */
@Composable
private fun NouTubeViews(explore: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = HoloTheme.colors
    Row(modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50)).background(c.surface)) {
        listOf(false to "Mes titres", true to "Explorer").forEach { (value, label) ->
            val on = value == explore
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(50))
                    .background(if (on) c.surfaceHigh else Color.Transparent)
                    .clickable(role = Role.Tab) { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = DecoType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = if (on) c.text else c.textSecondary)
            }
        }
    }
}

/**
 * YouTube Music, inside the NouTube chip: the page keeps playing when the user leaves this view,
 * the mini player and the bottom bar stay where they are, and a slim bar gives back the moves a
 * browser would (back, home, library, search, reload).
 */
@Composable
private fun ExploreYoutube(
    tube: TubeHost,
    signedIn: Boolean,
    bottomPadding: State<Int>,
    filter: HomeFilter,
    onFilter: (HomeFilter) -> Unit,
    onSignIn: () -> Unit,
    onShowList: () -> Unit,
) {
    val c = HoloTheme.colors
    val canGoBack by tube.canGoBack.collectAsState()
    BackHandler(enabled = canGoBack) { tube.goBack() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Spacer(Modifier.height(12.dp))
        FilterChips(filter, onFilter)
        Spacer(Modifier.height(10.dp))
        NouTubeViews(explore = true, onChange = { if (!it) onShowList() }, Modifier.padding(horizontal = 16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            DecoIconButton(DecoIcons.Back, "Page précédente", { tube.goBack() }, tint = if (canGoBack) c.text else c.textDisabled)
            DecoIconButton(DecoIcons.Home, "Accueil de YouTube Music", { tube.goHome() }, tint = c.text)
            DecoIconButton(DecoIcons.Shelf, "Bibliothèque YouTube Music", { tube.goLibrary() }, tint = c.text)
            DecoIconButton(DecoIcons.Search, "Rechercher sur YouTube Music", { tube.goSearch() }, tint = c.text)
            DecoIconButton(DecoIcons.Refresh, "Recharger", { tube.reload() }, tint = c.text)
            Spacer(Modifier.weight(1f))
            if (!signedIn) {
                Text(
                    "Se connecter", style = DecoType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(700)), color = c.secondary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onSignIn).padding(12.dp),
                )
            }
        }
        TubeSurface(
            tube,
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        )
        Spacer(Modifier.height(bottomPadding.value.dp))
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
private fun ShelfHeader(title: String) {
    Text(
        title, style = DecoType.Headline, color = HoloTheme.colors.text,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
    )
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

/** A song streamed in NouTube: tapping it opens NouTube, as HOLOWAVE cannot play YouTube itself. */
@Composable
private fun StreamCard(track: StreamedTrack, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Column(Modifier.width(144.dp).clickable(onClick = onClick)) {
        StreamedCover(track, Modifier.size(144.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SourceBadge("YouTube")
        }
        Text(track.artist, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
