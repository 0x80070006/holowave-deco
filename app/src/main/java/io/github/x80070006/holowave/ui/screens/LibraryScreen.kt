package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.LibrarySort
import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.ScanProgress
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.LibraryContent
import io.github.x80070006.holowave.ui.LibraryTab
import io.github.x80070006.holowave.ui.components.CollectionCard
import io.github.x80070006.holowave.ui.components.CoverImage
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.ScanBanner
import io.github.x80070006.holowave.ui.components.SourceBadge
import io.github.x80070006.holowave.ui.components.StreamedCover
import io.github.x80070006.holowave.ui.components.TrackRow
import io.github.x80070006.holowave.ui.components.running
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme

class LibraryCallbacks(
    val onTab: (LibraryTab) -> Unit,
    val onOpen: (CollectionKind, String) -> Unit,
    val onPlay: (List<Track>, Int, Boolean) -> Unit,
    val onTrackMenu: (Track, String?) -> Unit,
    val onCreatePlaylist: () -> Unit,
    val onToggleFilter: () -> Unit,
    val onFilter: (String) -> Unit,
    val onSort: () -> Unit,
    val onToggleGrid: () -> Unit,
    val onChooseFolders: () -> Unit,
    val onOpenStream: (StreamedTrack) -> Unit,
    val onNouTubeSettings: () -> Unit,
)

@Composable
fun LibraryScreen(
    content: LibraryContent,
    totalTracks: Int,
    tab: LibraryTab,
    grid: Boolean,
    sort: LibrarySort,
    filter: String,
    filterOpen: Boolean,
    loading: Boolean,
    foldersRestricted: Boolean,
    captureEnabled: Boolean,
    scan: ScanProgress?,
    playback: PlaybackState,
    bottomPadding: State<Int>,
    cb: LibraryCallbacks,
) {
    val c = HoloTheme.colors
    // Tracks and the mixed NouTube list are always lists; collections follow the user's choice.
    val asGrid = grid && content is LibraryContent.Collections
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (asGrid) 2 else 1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = if (asGrid) 16.dp else 0.dp, end = if (asGrid) 16.dp else 0.dp, bottom = (bottomPadding.value + 24).dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(if (asGrid) 18.dp else 0.dp),
    ) {
        full("header", asGrid) {
            Header(totalTracks, filterOpen, cb.onToggleFilter)
        }
        if (scan != null && scan.running) full("scan", asGrid) { ScanBanner(scan) }
        if (filterOpen) full("filter", asGrid) { FilterField(filter, cb.onFilter) }
        full("tabs", asGrid) { Chips(tab, cb.onTab) }
        full("controls", asGrid) {
            Controls(sort, grid, showToggle = content is LibraryContent.Collections, cb.onSort, cb.onToggleGrid)
        }

        when (content) {
            is LibraryContent.Collections -> collections(content, tab, asGrid, filter, loading, totalTracks, foldersRestricted, cb)
            is LibraryContent.Tracks -> tracks(content.items, filter, loading, totalTracks, foldersRestricted, playback, cb)
            is LibraryContent.Mixed -> mixed(content.items, filter, captureEnabled, playback, cb)
        }
    }
}

private fun LazyGridScope.full(key: String, asGrid: Boolean, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) {
        // Full-width rows ignore the grid's side gutters.
        Box(if (asGrid) Modifier.padding(horizontal = 0.dp) else Modifier) { content() }
    }

@Composable
private fun Header(total: Int, filterOpen: Boolean, onToggleFilter: () -> Unit) {
    val c = HoloTheme.colors
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 0.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text("Bibliothèque", style = DecoType.Display, color = c.text)
            Spacer(Modifier.height(4.dp))
            Text(
                if (total > 0) "$total titres sur l'appareil" else "Votre musique, hors ligne",
                style = DecoType.Small, color = c.textSecondary,
            )
        }
        DecoIconButton(
            if (filterOpen) DecoIcons.Close else DecoIcons.Search,
            if (filterOpen) "Fermer la recherche" else "Rechercher dans la bibliothèque", onToggleFilter, tint = c.text,
        )
    }
}

@Composable
private fun FilterField(value: String, onChange: (String) -> Unit) {
    val c = HoloTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceHigh)
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(DecoIcons.Search, null, Modifier.size(20.dp), tint = c.textSecondary)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("Filtrer cette liste…", style = DecoType.Body, color = c.textDisabled)
            BasicTextField(
                value, onChange, singleLine = true, textStyle = DecoType.Body.copy(color = c.text),
                cursorBrush = SolidColor(c.primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) DecoIconButton(DecoIcons.Close, "Effacer le filtre", { onChange("") }, tint = c.textSecondary, iconSize = 18.dp)
    }
}

@Composable
private fun Chips(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    val c = HoloTheme.colors
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        items(LibraryTab.entries.toList()) { t ->
            val active = t == selected
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (active) c.secondary else c.surfaceHigh)
                    .clickable(role = Role.Tab) { onSelect(t) }.padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(t.label, style = DecoType.Small.copy(fontWeight = FontWeight.Bold), color = if (active) c.onSecondary else c.text)
            }
        }
    }
}

@Composable
private fun Controls(sort: LibrarySort, grid: Boolean, showToggle: Boolean, onSort: () -> Unit, onToggleGrid: () -> Unit) {
    val c = HoloTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onSort).padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(DecoIcons.Sort, null, Modifier.size(20.dp), tint = c.text)
            Spacer(Modifier.width(8.dp))
            Text(sort.label, style = DecoType.Small.copy(fontWeight = FontWeight.Bold), color = c.text)
        }
        Spacer(Modifier.weight(1f))
        if (showToggle) {
            DecoIconButton(
                if (grid) DecoIcons.Rows else DecoIcons.Grid,
                if (grid) "Afficher en liste" else "Afficher en grille", onToggleGrid, tint = c.text,
            )
        }
    }
}

// ------------------------------------------------------------------ collections

private fun LazyGridScope.collections(
    content: LibraryContent.Collections, tab: LibraryTab, asGrid: Boolean, filter: String, loading: Boolean,
    total: Int, restricted: Boolean, cb: LibraryCallbacks,
) {
    if (tab == LibraryTab.PLAYLISTS) {
        full("create", asGrid) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${content.items.size} playlist${if (content.items.size > 1) "s" else ""}", style = DecoType.Small, color = HoloTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                DecoButton("Nouvelle playlist", cb.onCreatePlaylist, icon = DecoIcons.Plus, filled = false)
            }
        }
        content.pinned?.let { liked ->
            if (asGrid) item(key = "liked") { LikedTile(liked, tall = true) { cb.onOpen(CollectionKind.FAVORITES, liked.key) } }
            else item(key = "liked", span = { GridItemSpan(maxLineSpan) }) { LikedTile(liked, tall = false) { cb.onOpen(CollectionKind.FAVORITES, liked.key) } }
        }
    }
    if (content.items.isEmpty() && content.pinned == null) {
        full("empty", asGrid) { emptyState(filter, loading, total, restricted, tab, cb) }
        return
    }
    if (asGrid) {
        items(content.items, key = { it.kind.name + it.key }) { col -> CollectionCard(col, { cb.onOpen(col.kind, col.key) }) }
    } else {
        items(content.items, key = { it.kind.name + it.key }, span = { GridItemSpan(maxLineSpan) }) { col ->
            CollectionRow(col) { cb.onOpen(col.kind, col.key) }
        }
    }
}

@Composable
private fun CollectionRow(col: TrackCollection, onClick: () -> Unit) {
    val c = HoloTheme.colors
    val round = col.kind == CollectionKind.ARTIST
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverImage(col.cover, Modifier.size(56.dp).clip(if (round) CircleShape else RoundedCornerShape(10.dp)), size = 192, fallbackSeed = col.title)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(col.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(col.subtitle, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Pinned "Titres aimés": a gradient of the theme's own colours, so it never clashes. */
@Composable
private fun LikedTile(liked: TrackCollection, tall: Boolean, onClick: () -> Unit) {
    val c = HoloTheme.colors
    val shape = RoundedCornerShape(if (tall) 16.dp else 20.dp)
    val modifier = if (tall) Modifier.fillMaxWidth().aspectRatio(1f) else Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(88.dp)
    Row(
        modifier.clip(shape).background(Brush.linearGradient(listOf(c.primary, c.secondary))).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = if (tall) Alignment.Bottom else Alignment.CenterVertically,
    ) {
        Icon(DecoIcons.HeartFilled, null, Modifier.size(if (tall) 44.dp else 36.dp), tint = c.onPrimary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Titres aimés", style = DecoType.Headline, color = c.onPrimary)
            Text(liked.subtitle, style = DecoType.Small, color = c.onPrimary.copy(alpha = 0.85f))
        }
    }
}

// --------------------------------------------------------------------- tracks

private fun LazyGridScope.tracks(
    list: List<Track>, filter: String, loading: Boolean, total: Int, restricted: Boolean, playback: PlaybackState, cb: LibraryCallbacks,
) {
    if (list.isEmpty()) {
        full("empty", false) { emptyState(filter, loading, total, restricted, LibraryTab.TRACKS, cb) }
        return
    }
    full("shuffle", false) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${list.size} titres", style = DecoType.Small, color = HoloTheme.colors.textSecondary, modifier = Modifier.weight(1f))
            DecoButton("Aléatoire", { cb.onPlay(list, 0, true) }, icon = DecoIcons.Shuffle, filled = false)
        }
    }
    items(list, key = { "t" + it.id }, span = { GridItemSpan(maxLineSpan) }) { t ->
        TrackRow(t, t.id == playback.currentId, playback.isPlaying, { cb.onPlay(list, list.indexOf(t), false) }, { cb.onTrackMenu(t, null) })
    }
}

// ------------------------------------------------------------ NouTube (mixed)

private fun LazyGridScope.mixed(list: List<MixedItem>, filter: String, captureEnabled: Boolean, playback: PlaybackState, cb: LibraryCallbacks) {
    if (list.isEmpty()) {
        full("empty", false) {
            if (filter.isNotBlank()) {
                EmptyState("Rien trouvé", "Aucun morceau NouTube ne correspond à « $filter ».")
            } else {
                EmptyState(
                    "Rien de NouTube pour l'instant",
                    if (captureEnabled) "Écoutez de la musique dans NouTube : elle apparaîtra ici toute seule, avec ce que vous avez téléchargé."
                    else "Ici se retrouvent ce que vous téléchargez et ce que vous écoutez en streaming dans NouTube. Activez la capture pour que la liste se remplisse toute seule.",
                ) {
                    if (!captureEnabled) DecoButton("Activer dans les réglages", cb.onNouTubeSettings)
                }
            }
        }
        return
    }
    val localTracks = list.filterIsInstance<MixedItem.Local>().map { it.track }
    full("count", false) {
        Text(
            "${list.size} morceau${if (list.size > 1) "x" else ""}  ·  ${list.count { it is MixedItem.Stream }} en streaming",
            style = DecoType.Small, color = HoloTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
    items(list, key = { m -> when (m) { is MixedItem.Local -> "l" + m.track.id; is MixedItem.Stream -> "s" + m.track.key } }, span = { GridItemSpan(maxLineSpan) }) { m ->
        when (m) {
            is MixedItem.Local -> TrackRow(
                m.track, m.track.id == playback.currentId, playback.isPlaying,
                { cb.onPlay(localTracks, localTracks.indexOf(m.track), false) }, { cb.onTrackMenu(m.track, null) },
            )
            is MixedItem.Stream -> StreamRow(m.track) { cb.onOpenStream(m.track) }
        }
    }
}

@Composable
fun StreamRow(track: StreamedTrack, onClick: () -> Unit) {
    val c = HoloTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StreamedCover(track, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = DecoType.Title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge("Streaming")
                Spacer(Modifier.width(8.dp))
                Text(track.artist, style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(DecoIcons.OpenOut, "Écouter dans NouTube", Modifier.size(22.dp), tint = c.textSecondary)
        Spacer(Modifier.width(8.dp))
    }
}

// ------------------------------------------------------------------- empty

@Composable
private fun emptyState(filter: String, loading: Boolean, total: Int, restricted: Boolean, tab: LibraryTab, cb: LibraryCallbacks) {
    when {
        filter.isNotBlank() -> EmptyState("Rien trouvé", "Aucun résultat pour « $filter » dans cette liste.")
        loading -> EmptyState("Mise en place de la discothèque", "Lecture du catalogue de l'appareil…")
        tab == LibraryTab.PLAYLISTS -> EmptyState("Aucune playlist", "Créez-en une avec le bouton ci-dessus, puis ajoutez des titres depuis leur menu.")
        total == 0 && restricted -> EmptyState("Rien dans ces dossiers", "Les dossiers sources choisis ne contiennent aucun morceau.") {
            DecoButton("Dossiers sources", cb.onChooseFolders, filled = false)
        }
        total == 0 -> EmptyState("Votre salon est vide", "Choisissez les dossiers où se trouve votre musique, ou lancez une recherche sur tout l'appareil.") {
            DecoButton("Choisir mes dossiers", cb.onChooseFolders)
        }
        else -> EmptyState("Rien ici", "Cette liste est vide pour le moment.")
    }
}
