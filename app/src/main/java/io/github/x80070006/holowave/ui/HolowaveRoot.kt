package io.github.x80070006.holowave.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.LibraryState
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.ui.components.ConfirmDialog
import io.github.x80070006.holowave.ui.components.DecoSheet
import io.github.x80070006.holowave.ui.components.LocalArtwork
import io.github.x80070006.holowave.ui.components.MiniPlayer
import io.github.x80070006.holowave.ui.components.NameDialog
import io.github.x80070006.holowave.ui.components.PlaylistPicker
import io.github.x80070006.holowave.ui.components.SheetAction
import io.github.x80070006.holowave.ui.components.SheetRow
import io.github.x80070006.holowave.ui.components.SteppedShape
import io.github.x80070006.holowave.ui.components.TrackMenu
import io.github.x80070006.holowave.ui.screens.DetailScreen
import io.github.x80070006.holowave.ui.screens.LibraryCallbacks
import io.github.x80070006.holowave.ui.screens.LibraryScreen
import io.github.x80070006.holowave.ui.screens.LyricsScreen
import io.github.x80070006.holowave.ui.screens.NowPlayingScreen
import io.github.x80070006.holowave.ui.screens.PlayerCallbacks
import io.github.x80070006.holowave.ui.screens.SettingsScreen
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

private sealed interface Overlay {
    data class TrackOptions(val track: Track, val playlistKey: String?) : Overlay
    data class PickPlaylist(val track: Track) : Overlay
    data class NewPlaylist(val track: Track?) : Overlay
    data class PlaylistOptions(val key: String, val name: String) : Overlay
    data class RenamePlaylist(val key: String, val name: String) : Overlay
    data class DeletePlaylist(val key: String, val name: String) : Overlay
}

@Composable
fun HolowaveRoot(vm: MainViewModel) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val libraryState by vm.libraryState.collectAsStateWithLifecycle()
    val routes by vm.route.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val position = vm.position.collectAsStateWithLifecycle()
    val current by vm.currentTrack.collectAsStateWithLifecycle()
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playerOpen by vm.playerOpen.collectAsStateWithLifecycle()
    val lyricsOpen by vm.lyricsOpen.collectAsStateWithLifecycle()
    val prefetch by vm.prefetch.collectAsStateWithLifecycle()
    val message by vm.messages.collectAsStateWithLifecycle()

    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val miniHeight = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val hasTrack = playback.currentId != null

    val importLrc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importLyrics(uri)
    }

    BackHandler(enabled = playerOpen || lyricsOpen || routes.size > 1 || query.isNotEmpty()) { vm.back() }

    val onTrackMenu: (Track, String?) -> Unit = { t, key -> overlay = Overlay.TrackOptions(t, key) }

    CompositionLocalProvider(LocalArtwork provides vm.artwork) {
        Box(Modifier.fillMaxSize().background(Deco.Ink)) {
            AnimatedContent(
                targetState = routes.last(),
                transitionSpec = {
                    if (targetState != Route.Library) (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith fadeOut(tween(200))
                    else fadeIn(tween(260)) togetherWith (slideOutHorizontally(tween(320)) { it / 4 } + fadeOut(tween(260)))
                },
                label = "routes",
            ) { route ->
                when (route) {
                    Route.Library -> LibraryScreen(
                        catalog = catalog,
                        loading = libraryState is LibraryState.Loading || libraryState is LibraryState.Idle,
                        tab = tab, query = query, results = results, playback = playback,
                        bottomPadding = miniHeight,
                        cb = LibraryCallbacks(
                            onTab = vm::selectTab,
                            onQuery = vm::setQuery,
                            onOpen = { kind, key -> vm.open(Route.Detail(kind, key)) },
                            onPlay = { list, i, shuffle -> vm.play(list, i, shuffle) },
                            onTrackMenu = onTrackMenu,
                            onCreatePlaylist = { overlay = Overlay.NewPlaylist(null) },
                            onSettings = { vm.open(Route.Settings) },
                        ),
                    )
                    is Route.Detail -> {
                        val collection = remember(catalog, route) { vm.collection(route.kind, route.key) }
                        DetailScreen(
                            collection = collection,
                            playback = playback,
                            bottomPadding = miniHeight,
                            onBack = { vm.back() },
                            onPlay = { list, i, shuffle -> vm.play(list, i, shuffle) },
                            onTrackMenu = onTrackMenu,
                            onCollectionMenu = if (route.kind == CollectionKind.PLAYLIST && collection != null) {
                                { overlay = Overlay.PlaylistOptions(collection.key, collection.title) }
                            } else null,
                        )
                    }
                    Route.Settings -> SettingsScreen(
                        settings = settings,
                        trackCount = catalog.tracks.size,
                        storedLyrics = remember(prefetch, lyrics) { vm.storedLyricsCount() },
                        prefetch = prefetch,
                        bottomPadding = miniHeight,
                        onBack = { vm.back() },
                        onOnlineLyrics = vm::setOnlineLyrics,
                        onSpinning = vm::setSpinningRecord,
                        onPrefetch = vm::prefetchAllLyrics,
                        onCancelPrefetch = vm::cancelPrefetch,
                        onClearLyrics = vm::clearDownloadedLyrics,
                        onRescan = vm::rescan,
                    )
                }
            }

            // Scrim so scrolled content never collides with the status bar icons.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Brush.verticalGradient(listOf(Deco.Ink, Deco.Ink.copy(alpha = 0.85f)))),
            )

            AnimatedVisibility(
                visible = hasTrack && !playerOpen,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                MiniPlayer(
                    track = current, playback = playback, position = position, spin = settings.spinningRecord,
                    onOpen = { vm.openPlayer(true) }, onToggle = vm.player::togglePlay, onNext = vm.player::next,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .onSizeChanged { miniHeight.intValue = with(density) { it.height.toDp().value.toInt() } + 16 },
                )
            }
            if (!hasTrack) LaunchedEffect(Unit) { miniHeight.intValue = 0 }

            AnimatedVisibility(
                visible = playerOpen,
                enter = slideInVertically(tween(380)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(320)) { it } + fadeOut(tween(250)),
            ) {
                val upNext = remember(playback.queueIds, playback.queueIndex, catalog) {
                    if (playback.shuffle) emptyList()
                    else playback.queueIds.withIndex()
                        .drop(playback.queueIndex + 1).take(4)
                        .mapNotNull { (i, id) -> catalog.byId[id]?.let { i to it } }
                }
                NowPlayingScreen(
                    track = current, playback = playback, position = position, lyrics = lyrics,
                    isFavorite = current?.id?.let { it in catalog.favoriteIds } == true,
                    spin = settings.spinningRecord, upNext = upNext,
                    cb = PlayerCallbacks(
                        onClose = { vm.openPlayer(false) },
                        onToggle = vm.player::togglePlay,
                        onNext = vm.player::next,
                        onPrevious = vm.player::previous,
                        onSeek = vm.player::seekTo,
                        onShuffle = vm.player::toggleShuffle,
                        onRepeat = vm.player::cycleRepeat,
                        onFavorite = { current?.let { vm.toggleFavorite(it.id) } },
                        onLyrics = { vm.openLyrics(true) },
                        onMenu = { current?.let { overlay = Overlay.TrackOptions(it, null) } },
                        onSkipTo = vm.player::skipTo,
                    ),
                )
            }

            AnimatedVisibility(
                visible = lyricsOpen,
                enter = slideInVertically(tween(360)) { it / 3 } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { it / 3 } + fadeOut(tween(250)),
            ) {
                LyricsScreen(
                    track = current, playback = playback, position = position, lyrics = lyrics,
                    onlineEnabled = settings.onlineLyrics,
                    onClose = { vm.openLyrics(false) },
                    onToggle = vm.player::togglePlay,
                    onSeek = vm.player::seekTo,
                    onImport = { importLrc.launch(arrayOf("text/*", "application/octet-stream")) },
                    onRetry = vm::retryLyrics,
                    onEnableOnline = { vm.setOnlineLyrics(true) },
                )
            }

            message?.let { text ->
                Toast(text, Modifier.align(Alignment.TopCenter))
                LaunchedEffect(text) {
                    kotlinx.coroutines.delay(2_200)
                    vm.consumeMessage()
                }
            }
        }

        when (val o = overlay) {
            null -> Unit
            is Overlay.TrackOptions -> TrackMenu(
                o.track,
                buildList {
                    add(SheetAction(DecoIcons.Play, "Lire ensuite") { vm.playNext(o.track) })
                    add(SheetAction(DecoIcons.Queue, "Ajouter à la file d'attente") { vm.enqueue(o.track) })
                    add(SheetAction(DecoIcons.Plus, "Ajouter à une playlist…") { overlay = Overlay.PickPlaylist(o.track) })
                    val fav = o.track.id in catalog.favoriteIds
                    add(SheetAction(if (fav) DecoIcons.HeartFilled else DecoIcons.Heart, if (fav) "Retirer des coups de cœur" else "Ajouter aux coups de cœur") { vm.toggleFavorite(o.track.id) })
                    add(SheetAction(DecoIcons.Disc, "Voir l'album") {
                        vm.openPlayer(false)
                        vm.open(Route.Detail(CollectionKind.ALBUM, o.track.albumId.toString()))
                    })
                    if (o.playlistKey != null) add(SheetAction(DecoIcons.Trash, "Retirer de la playlist") { vm.removeFromPlaylist(o.playlistKey, o.track) })
                },
                onDismiss = { if (overlay == o) overlay = null },
            )
            is Overlay.PickPlaylist -> PlaylistPicker(
                catalog.playlists,
                onPick = { vm.addToPlaylist(it.key, o.track) },
                onCreate = { overlay = Overlay.NewPlaylist(o.track) },
                onDismiss = { if (overlay == o) overlay = null },
            )
            is Overlay.NewPlaylist -> NameDialog(
                "Nouvelle playlist", "", "Créer",
                onConfirm = { vm.createPlaylist(it, o.track); overlay = null },
                onDismiss = { overlay = null },
            )
            is Overlay.PlaylistOptions -> DecoSheet({ if (overlay == o) overlay = null }) {
                SheetRow(DecoIcons.Edit, "Renommer") { overlay = Overlay.RenamePlaylist(o.key, o.name) }
                SheetRow(DecoIcons.Trash, "Supprimer la playlist") { overlay = Overlay.DeletePlaylist(o.key, o.name) }
            }
            is Overlay.RenamePlaylist -> NameDialog(
                "Renommer", o.name, "Valider",
                onConfirm = { vm.renamePlaylist(o.key, it); overlay = null },
                onDismiss = { overlay = null },
            )
            is Overlay.DeletePlaylist -> ConfirmDialog(
                "Supprimer « ${o.name} » ?", "Les morceaux restent dans la discothèque, seule la playlist disparaît.", "Supprimer",
                onConfirm = { vm.deletePlaylist(o.key) },
                onDismiss = { overlay = null },
            )
        }
    }
}

@Composable
private fun Toast(text: String, modifier: Modifier = Modifier) {
    val shape = remember { SteppedShape(4.dp) }
    Box(
        modifier
            .padding(top = 56.dp)
            .background(Deco.InkRaised, shape)
            .border(1.dp, Deco.Gold.copy(alpha = 0.7f), shape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(text, style = DecoType.Small, color = Deco.Cream)
    }
}
