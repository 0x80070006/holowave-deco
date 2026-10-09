package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.Catalog
import io.github.x80070006.holowave.ui.LibraryTab
import io.github.x80070006.holowave.ui.components.ArtistRow
import io.github.x80070006.holowave.ui.components.CollectionCard
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.SectionLabel
import io.github.x80070006.holowave.ui.components.SteppedShape
import io.github.x80070006.holowave.ui.components.Sunburst
import io.github.x80070006.holowave.ui.components.TabsRow
import io.github.x80070006.holowave.ui.components.TrackRow
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

class LibraryCallbacks(
    val onTab: (LibraryTab) -> Unit,
    val onQuery: (String) -> Unit,
    val onOpen: (CollectionKind, String) -> Unit,
    val onPlay: (List<Track>, Int, Boolean) -> Unit,
    val onTrackMenu: (Track, String?) -> Unit,
    val onCreatePlaylist: () -> Unit,
    val onSettings: () -> Unit,
)

@Composable
fun LibraryScreen(
    catalog: Catalog,
    loading: Boolean,
    tab: LibraryTab,
    query: String,
    results: List<Track>,
    playback: PlaybackState,
    bottomPadding: State<Int>,
    cb: LibraryCallbacks,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    val searchActive = searching || query.isNotEmpty()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, bottom = (bottomPadding.value + 24).dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        full("header") {
            Header(
                trackCount = catalog.tracks.size,
                onSearch = { searching = !searching; if (!searching) cb.onQuery("") },
                onSettings = cb.onSettings,
            )
        }
        full("search") {
            AnimatedVisibility(searchActive) { SearchField(query, cb.onQuery) }
        }

        if (searchActive && query.isNotBlank()) {
            searchResults(results, playback, cb)
            return@LazyVerticalGrid
        }

        full("tabs") {
            TabsRow(LibraryTab.entries.map { it.label }, tab.ordinal, { cb.onTab(LibraryTab.entries[it]) }, Modifier.padding(top = 4.dp, bottom = 10.dp))
        }

        if (catalog.tracks.isEmpty()) {
            full("empty") {
                if (loading) {
                    EmptyState("Mise en place de la discothèque", "Lecture du catalogue de l'appareil…")
                } else {
                    EmptyState(
                        "La scène est vide",
                        "Aucun morceau trouvé. Copiez vos fichiers audio (MP3, FLAC, M4A, OGG…) dans le dossier Musique de l'appareil : ils apparaîtront ici automatiquement.",
                    )
                }
            }
            return@LazyVerticalGrid
        }

        when (tab) {
            LibraryTab.TRACKS -> tracksTab(catalog, playback, cb)
            LibraryTab.ALBUMS -> albumsTab(catalog, cb)
            LibraryTab.ARTISTS -> items(catalog.artists, key = { "ar-" + it.key }, span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.bleed(Gutter)) { ArtistRow(it) { cb.onOpen(CollectionKind.ARTIST, it.key) } }
            }
            LibraryTab.FOLDERS -> grid(catalog.folders, CollectionKind.FOLDER, cb)
            LibraryTab.PLAYLISTS -> playlistsTab(catalog, cb)
        }
    }
}

private val Gutter = 20.dp

private fun LazyGridScope.full(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.bleed(Gutter)) { content() } }

/** Lets full-width rows ignore the grid's side gutters. */
private fun Modifier.bleed(h: Dp): Modifier = layout { measurable, constraints ->
    val extra = h.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

private fun LazyGridScope.grid(list: List<TrackCollection>, kind: CollectionKind, cb: LibraryCallbacks) {
    items(list, key = { kind.name + it.key }) { c ->
        CollectionCard(c, { cb.onOpen(kind, c.key) }, Modifier.padding(bottom = 18.dp))
    }
}

private fun LazyGridScope.tracksTab(catalog: Catalog, playback: PlaybackState, cb: LibraryCallbacks) {
    full("tracks-actions") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${catalog.tracks.size} titres", style = DecoType.Small, color = Deco.CreamDim, modifier = Modifier.weight(1f))
            DecoButton("Aléatoire", { cb.onPlay(catalog.tracks, 0, true) }, icon = DecoIcons.Shuffle, filled = false)
        }
    }
    items(catalog.tracks, key = { "t" + it.id }, span = { GridItemSpan(maxLineSpan) }) { t ->
        TrackRow(
            t, t.id == playback.currentId, playback.isPlaying,
            onClick = { cb.onPlay(catalog.tracks, catalog.tracks.indexOf(t), false) },
            onMore = { cb.onTrackMenu(t, null) },
            modifier = Modifier.bleed(Gutter),
        )
    }
}

private fun LazyGridScope.albumsTab(catalog: Catalog, cb: LibraryCallbacks) {
    val recent = catalog.albums.sortedByDescending { a -> a.tracks.maxOf { it.dateAdded } }.take(8)
    if (catalog.albums.size > 4) {
        full("recent-label") { SectionLabel("Nouveaux arrivages", Modifier.padding(start = 20.dp, top = 6.dp, bottom = 14.dp)) }
        full("recent") {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(recent, key = { "r" + it.key }) { a ->
                    CollectionCard(a, { cb.onOpen(CollectionKind.ALBUM, a.key) }, Modifier.width(132.dp))
                }
            }
        }
        full("all-label") { SectionLabel("Tous les albums", Modifier.padding(start = 20.dp, top = 22.dp, bottom = 14.dp)) }
    }
    grid(catalog.albums, CollectionKind.ALBUM, cb)
}

private fun LazyGridScope.playlistsTab(catalog: Catalog, cb: LibraryCallbacks) {
    full("pl-actions") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${catalog.playlists.size} playlist${if (catalog.playlists.size > 1) "s" else ""}", style = DecoType.Small, color = Deco.CreamDim, modifier = Modifier.weight(1f))
            DecoButton("Créer", cb.onCreatePlaylist, icon = DecoIcons.Plus, filled = false)
        }
        Spacer(Modifier.height(12.dp))
    }
    catalog.favorites?.let { fav ->
        item(key = "fav") {
            CollectionCard(fav, { cb.onOpen(CollectionKind.FAVORITES, fav.key) }, Modifier.padding(bottom = 18.dp))
        }
    }
    items(catalog.playlists, key = { "p" + it.key }) { c ->
        CollectionCard(c, { cb.onOpen(CollectionKind.PLAYLIST, c.key) }, Modifier.padding(bottom = 18.dp))
    }
}

private fun LazyGridScope.searchResults(results: List<Track>, playback: PlaybackState, cb: LibraryCallbacks) {
    if (results.isEmpty()) {
        full("no-results") { EmptyState("Rien à l'affiche", "Aucun titre, artiste ou album ne correspond à cette recherche.") }
        return
    }
    full("results-label") { SectionLabel("${results.size} résultat${if (results.size > 1) "s" else ""}", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)) }
    items(results, key = { "s" + it.id }, span = { GridItemSpan(maxLineSpan) }) { t ->
        TrackRow(
            t, t.id == playback.currentId, playback.isPlaying,
            { cb.onPlay(results, results.indexOf(t), false) }, { cb.onTrackMenu(t, null) },
            Modifier.bleed(Gutter),
        )
    }
}

@Composable
private fun Header(trackCount: Int, onSearch: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Sunburst(Modifier.matchParentSize(), rays = 29, origin = Offset(0.5f, 0.92f), spreadDegrees = 170f, alpha = 0.10f)
        Column(Modifier.statusBarsPadding().padding(top = 4.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                DecoIconButton(DecoIcons.Search, "Rechercher", onSearch, tint = Deco.Gold)
                Spacer(Modifier.weight(1f))
                DecoIconButton(DecoIcons.Tune, "Réglages", onSettings, tint = Deco.Gold)
            }
            Text(
                "HOLOWAVE",
                style = DecoType.Marquee.copy(brush = Deco.goldBrush),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 9.dp), // letter spacing trails the last glyph
            )
            Spacer(Modifier.height(10.dp))
            DecoDivider(Modifier.width(220.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                if (trackCount > 0) "SALON D'ÉCOUTE  ·  $trackCount TITRES" else "SALON D'ÉCOUTE",
                style = DecoType.Caps, color = Deco.CreamDim,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val shape = remember { SteppedShape(4.dp) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(1.dp, Deco.Gold.copy(alpha = 0.6f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(DecoIcons.Search, null, Modifier.size(18.dp), tint = Deco.Gold)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Titre, artiste, album…", style = DecoType.Body, color = Deco.Muted)
            BasicTextField(
                query, onQuery,
                singleLine = true,
                textStyle = DecoType.Body.copy(color = Deco.Cream),
                cursorBrush = SolidColor(Deco.Gold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (query.isNotEmpty()) DecoIconButton(DecoIcons.Close, "Effacer", { onQuery("") }, size = 28.dp, iconSize = 16.dp, tint = Deco.CreamDim)
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
