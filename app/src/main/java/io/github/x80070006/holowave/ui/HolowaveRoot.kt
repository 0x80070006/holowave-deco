package io.github.x80070006.holowave.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import io.github.x80070006.holowave.ui.components.HoloBottomBar
import io.github.x80070006.holowave.ui.components.MiniPlayer
import io.github.x80070006.holowave.ui.components.StreamMiniPlayer
import io.github.x80070006.holowave.ui.screens.StreamNowPlayingScreen
import io.github.x80070006.holowave.ui.screens.YoutubeAccountScreen
import io.github.x80070006.holowave.ui.components.TubeSurface
import io.github.x80070006.holowave.playback.SourceKind
import io.github.x80070006.holowave.stream.NouTubeBridge
import io.github.x80070006.holowave.ui.components.NameDialog
import io.github.x80070006.holowave.ui.components.PlaylistPicker
import io.github.x80070006.holowave.ui.components.QueueSheet
import io.github.x80070006.holowave.ui.components.SleepSheet
import io.github.x80070006.holowave.ui.components.SortSheet
import io.github.x80070006.holowave.ui.components.rememberArtwork
import io.github.x80070006.holowave.ui.components.SheetAction
import io.github.x80070006.holowave.ui.components.SheetRow
import io.github.x80070006.holowave.ui.components.decoShape
import io.github.x80070006.holowave.ui.components.TrackMenu
import io.github.x80070006.holowave.ui.screens.DetailScreen
import io.github.x80070006.holowave.ui.screens.HomeScreen
import io.github.x80070006.holowave.ui.screens.LibraryCallbacks
import io.github.x80070006.holowave.ui.screens.LibraryScreen
import io.github.x80070006.holowave.ui.screens.LyricsScreen
import io.github.x80070006.holowave.ui.screens.NowPlayingScreen
import io.github.x80070006.holowave.ui.screens.PlayerCallbacks
import io.github.x80070006.holowave.ui.screens.SearchScreen
import io.github.x80070006.holowave.ui.screens.SettingsScreen
import io.github.x80070006.holowave.ui.screens.SourceFoldersScreen
import io.github.x80070006.holowave.ui.screens.ThemePreviewScreen
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.stream.NotificationAccess
import io.github.x80070006.holowave.ui.theme.Appearance
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

private sealed interface Overlay {
    data class TrackOptions(val track: Track, val playlistKey: String?) : Overlay
    data class PickPlaylist(val track: Track) : Overlay
    data class NewPlaylist(val track: Track?) : Overlay
    data class PlaylistOptions(val key: String, val name: String) : Overlay
    data class RenamePlaylist(val key: String, val name: String) : Overlay
    data class DeletePlaylist(val key: String, val name: String) : Overlay
    data object Queue : Overlay
    data object Sort : Overlay
    data object Sleep : Overlay
}

@Composable
fun HolowaveRoot(vm: MainViewModel) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val libraryState by vm.libraryState.collectAsStateWithLifecycle()
    val routes by vm.route.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val mainTab by vm.mainTab.collectAsStateWithLifecycle()
    val homeFilter by vm.homeFilter.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val position = vm.position.collectAsStateWithLifecycle()
    val streamActive by vm.streamActive.collectAsStateWithLifecycle()
    val flowVariant by vm.flowVariant.collectAsStateWithLifecycle()
    val youtubeSignedIn by vm.youtubeSignedIn.collectAsStateWithLifecycle()
    val importStatus by vm.tube.importStatus.collectAsStateWithLifecycle()
    val shelves by vm.shelves.collectAsStateWithLifecycle()
    val streamNow by vm.sources.now.collectAsStateWithLifecycle()
    val streamPosition = vm.streamPosition.collectAsStateWithLifecycle()
    val current by vm.currentTrack.collectAsStateWithLifecycle()
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playerOpen by vm.playerOpen.collectAsStateWithLifecycle()
    val lyricsOpen by vm.lyricsOpen.collectAsStateWithLifecycle()
    val prefetch by vm.prefetch.collectAsStateWithLifecycle()
    val message by vm.messages.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val sleepEndsAt by vm.sleepEndsAt.collectAsStateWithLifecycle()
    val libraryContent by vm.libraryContent.collectAsStateWithLifecycle()
    val librarySort by vm.librarySort.collectAsStateWithLifecycle()
    val libraryFilter by vm.libraryFilter.collectAsStateWithLifecycle()
    val libraryFilterOpen by vm.libraryFilterOpen.collectAsStateWithLifecycle()
    val streamedCount by vm.streamedCount.collectAsStateWithLifecycle()

    // Notification access and NouTube's presence are system state: re-read them every time the app comes back.
    val appContext = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var accessGranted by remember { mutableStateOf(NotificationAccess.isGranted(appContext)) }
    var nouTubeInstalled by remember { mutableStateOf(NouTubeBridge.isInstalled(appContext)) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                accessGranted = NotificationAccess.isGranted(appContext)
                nouTubeInstalled = NouTubeBridge.isInstalled(appContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val listenCount by vm.listenCount.collectAsStateWithLifecycle()
    var confirmClearHistory by remember { mutableStateOf(false) }
    val exportHistory = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportHistory(uri)
    }

    val context = LocalContext.current
    val videoPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else null
    val askVideo = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setVideoContainers(true, granted)
    }
    val onVideoContainers: (Boolean) -> Unit = { enable ->
        when {
            !enable -> {
                vm.setVideoContainers(false, true)
                // Secure by default: hand the video permission back when the feature is off.
                if (videoPermission != null) runCatching { context.revokeSelfPermissionOnKill(videoPermission) }
            }
            videoPermission == null ||
                ContextCompat.checkSelfPermission(context, videoPermission) == PackageManager.PERMISSION_GRANTED ->
                vm.setVideoContainers(true, true)
            else -> askVideo.launch(videoPermission)
        }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addPickedFolder(uri)
    }

    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val miniHeight = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val hasTrack = playback.currentId != null || streamActive

    val importLrc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importLyrics(uri)
    }

    BackHandler(enabled = playerOpen || lyricsOpen || routes.size > 1 || query.isNotEmpty() || mainTab != MainTab.HOME) { vm.back() }

    val onTrackMenu: (Track, String?) -> Unit = { t, key -> overlay = Overlay.TrackOptions(t, key) }

    CompositionLocalProvider(LocalArtwork provides vm.artwork) {
        CoverColorSync(current)
        Box(Modifier.fillMaxSize().background(Deco.Ink)) {
            // Where the embedded YouTube Music page rests when no screen shows it; it keeps playing from there.
            TubeSurface(vm.tube, Modifier.size(1.dp), anchor = true)
            AnimatedContent(
                targetState = routes.last(),
                transitionSpec = {
                    if (targetState != Route.Library) (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith fadeOut(tween(200))
                    else fadeIn(tween(260)) togetherWith (slideOutHorizontally(tween(320)) { it / 4 } + fadeOut(tween(260)))
                },
                label = "routes",
            ) { route ->
                when (route) {
                    Route.Library -> AnimatedContent(
                        targetState = mainTab,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "tabs",
                    ) { t ->
                        when (t) {
                            MainTab.HOME -> HomeScreen(
                                home = home, filter = homeFilter, onFilter = vm::setHomeFilter,
                                libraryEmpty = catalog.tracks.isEmpty(),
                                loading = libraryState is LibraryState.Loading || libraryState is LibraryState.Idle,
                                scan = scan, bottomPadding = miniHeight,
                                onOpen = { kind, key -> vm.open(Route.Detail(kind, key)) },
                                onPlay = { list, i -> vm.play(list, i) },
                                onOpenStream = { vm.playStream(it) },
                                onSeeAll = { vm.selectMainTab(MainTab.LIBRARY) },
                                onChooseFolders = { vm.selectMainTab(MainTab.SETTINGS); vm.open(Route.SourceFolders) },
                                onScan = vm::scanStorage,
                                flowActive = flowVariant, onFlow = vm::startFlow,
                                tube = vm.tube, youtubeSignedIn = youtubeSignedIn,
                                onSignIn = { vm.open(Route.YoutubeAccount) }, onImportTop = { vm.tube.importTop() },
                                importStatus = importStatus,
                                shelves = shelves, onPlayShelf = vm::playShelf,
                            )
                            MainTab.SEARCH -> SearchScreen(
                                query = query, results = results, playback = playback, bottomPadding = miniHeight,
                                onQuery = vm::setQuery,
                                onPlay = { list, i -> vm.play(list, i) },
                                onTrackMenu = { t -> onTrackMenu(t, null) },
                            )
                            MainTab.LIBRARY -> LibraryScreen(
                                content = libraryContent,
                                totalTracks = catalog.tracks.size,
                                tab = tab,
                                grid = settings.libraryGrid,
                                sort = librarySort,
                                filter = libraryFilter,
                                filterOpen = libraryFilterOpen,
                                loading = libraryState is LibraryState.Loading || libraryState is LibraryState.Idle,
                                foldersRestricted = settings.sourceFolders.isNotEmpty(),
                                captureEnabled = settings.captureStreaming,
                                scan = scan,
                                playback = playback,
                                bottomPadding = miniHeight,
                                cb = LibraryCallbacks(
                                    onTab = vm::selectTab,
                                    onOpen = { kind, key -> vm.open(Route.Detail(kind, key)) },
                                    onPlay = { list, i, shuffle -> vm.play(list, i, shuffle) },
                                    onTrackMenu = onTrackMenu,
                                    onCreatePlaylist = { overlay = Overlay.NewPlaylist(null) },
                                    onToggleFilter = vm::toggleLibraryFilter,
                                    onFilter = vm::setLibraryFilter,
                                    onSort = { overlay = Overlay.Sort },
                                    onToggleGrid = vm::toggleLibraryGrid,
                                    onChooseFolders = { vm.selectMainTab(MainTab.SETTINGS); vm.open(Route.SourceFolders) },
                                    onOpenStream = { vm.playStream(it) },
                                    onNouTubeSettings = { vm.selectMainTab(MainTab.SETTINGS) },
                                ),
                            )
                            MainTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                trackCount = catalog.tracks.size,
                                storedLyrics = remember(prefetch, lyrics) { vm.storedLyricsCount() },
                                prefetch = prefetch,
                                bottomPadding = miniHeight,
                                onBack = null,
                                onOnlineLyrics = vm::setOnlineLyrics,
                                onPrefetch = vm::prefetchAllLyrics,
                                onCancelPrefetch = vm::cancelPrefetch,
                                onClearLyrics = vm::clearDownloadedLyrics,
                                scan = scan,
                                onScan = vm::scanStorage,
                                onStopScan = vm::cancelScan,
                                onSourceFolders = { vm.open(Route.SourceFolders) },
                                onVideoContainers = onVideoContainers,
                                listenCount = listenCount,
                                onRecordHistory = vm::setRecordHistory,
                                onExportHistory = { runCatching { exportHistory.launch("holowave-historique.json") } },
                                onClearHistory = { confirmClearHistory = true },
                                onPreviewThemes = { vm.open(Route.ThemePreview) },
                                onTheme = vm::setTheme,
                                onMode = vm::setMode,
                                onMonoAccent = vm::setMonoAccent,
                                onCoverColors = vm::setCoverColors,
                                onMotion = vm::setMotion,
                                youtubeSignedIn = youtubeSignedIn,
                                onYoutubeAccount = { vm.open(Route.YoutubeAccount) },
                                nouTubeInstalled = nouTubeInstalled,
                                accessGranted = accessGranted,
                                streamedCount = streamedCount,
                                onCaptureStreaming = vm::setCaptureStreaming,
                                onOpenAccess = { runCatching { appContext.startActivity(NotificationAccess.settingsIntent(appContext)) } },
                                onClearStreamed = vm::clearStreamed,
                                onGetNouTube = {
                                    runCatching {
                                        appContext.startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(NouTubeBridge.FDROID_URL))
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                    }
                                },
                            )
                        }
                    }
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
                    Route.YoutubeAccount -> YoutubeAccountScreen(
                        host = vm.tube, signedIn = youtubeSignedIn, bottomPadding = miniHeight.intValue,
                        onBack = { vm.back() }, onSignIn = { vm.tube.openSignIn() }, onSignOut = { vm.tube.signOut() },
                    )
                    Route.ThemePreview -> ThemePreviewScreen(bottomPadding = miniHeight, onBack = { vm.back() }, onApply = vm::setTheme)
                    Route.SourceFolders -> {
                        val tree by vm.folderTree.collectAsStateWithLifecycle()
                        val total by vm.allTrackCount.collectAsStateWithLifecycle()
                        SourceFoldersScreen(
                            tree = tree,
                            selected = settings.sourceFolders,
                            shownTracks = catalog.tracks.size,
                            totalTracks = total,
                            bottomPadding = miniHeight,
                            onBack = { vm.back() },
                            scan = scan,
                            onToggle = vm::toggleSourceFolder,
                            onRemove = vm::removeSourceFolder,
                            onIncludeAll = vm::includeAllFolders,
                            onPick = { runCatching { pickFolder.launch(null) } },
                            onScan = vm::scanStorage,
                            onStopScan = vm::cancelScan,
                        )
                    }
                }
            }

            // Scrim so scrolled content never collides with the status bar icons.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Brush.verticalGradient(listOf(Deco.Ink.copy(alpha = 0.85f), Color.Transparent))),
            )

            AnimatedVisibility(
                visible = !playerOpen && !lyricsOpen,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
            ) {
                Column(Modifier.onSizeChanged { miniHeight.intValue = with(density) { it.height.toDp().value.toInt() } }) {
                    AnimatedVisibility(visible = hasTrack, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        if (streamActive) {
                            val yt = vm.sources.source(SourceKind.NOUTUBE)
                            StreamMiniPlayer(
                                state = streamNow, position = streamPosition,
                                onOpen = { vm.openPlayer(true) },
                                onToggle = { if (streamNow.isPlaying) yt.pause() else yt.play() },
                                onNext = vm::next, onPrevious = vm::previous,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        } else {
                            MiniPlayer(
                                track = current, playback = playback, position = position,
                                onOpen = { vm.openPlayer(true) }, onToggle = vm.player::togglePlay,
                                onNext = vm::next, onPrevious = vm::previous,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                    HoloBottomBar(mainTab, vm::selectMainTab)
                }
            }

            AnimatedVisibility(
                visible = playerOpen,
                enter = slideInVertically(tween(380)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(320)) { it } + fadeOut(tween(250)),
            ) {
                if (streamActive) {
                    val yt = vm.sources.source(SourceKind.NOUTUBE)
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    StreamNowPlayingScreen(
                        state = streamNow, position = streamPosition,
                        nouTubeAvailable = remember { NouTubeBridge.isInstalled(ctx) },
                        onClose = { vm.openPlayer(false) },
                        onToggle = { if (streamNow.isPlaying) yt.pause() else yt.play() },
                        onNext = vm::next, onPrevious = vm::previous, onSeek = yt::seekTo,
                        onOpenNouTube = { vm.openInNouTube() },
                    )
                    return@AnimatedVisibility
                }
                val upNext = remember(playback.queueIds, playback.queueIndex, catalog) {
                    if (playback.shuffle) emptyList()
                    else playback.queueIds.withIndex()
                        .drop(playback.queueIndex + 1).take(4)
                        .mapNotNull { (i, id) -> catalog.byId[id]?.let { i to it } }
                }
                val album = remember(current, catalog) { current?.let { catalog.albumsByKey[it.albumId.toString()] } }
                val sameArtist = remember(current, catalog) {
                    current?.let { t -> catalog.artists.firstOrNull { it.key == t.artist }?.tracks?.filter { it.id != t.id }?.take(12) }.orEmpty()
                }
                NowPlayingScreen(
                    track = current, playback = playback, position = position, lyrics = lyrics,
                    isFavorite = current?.id?.let { it in catalog.favoriteIds } == true,
                    upNext = upNext, album = album, sameArtist = sameArtist, sleepActive = sleepEndsAt != null,
                    cb = PlayerCallbacks(
                        onClose = { vm.openPlayer(false) },
                        onToggle = vm.player::togglePlay,
                        onNext = vm::next,
                        onPrevious = vm::previous,
                        onSeek = vm.player::seekTo,
                        onShuffle = vm.player::toggleShuffle,
                        onRepeat = vm.player::cycleRepeat,
                        onFavorite = { current?.let { vm.toggleFavorite(it.id) } },
                        onLyrics = { vm.openLyrics(true) },
                        onMenu = { current?.let { overlay = Overlay.TrackOptions(it, null) } },
                        onSkipTo = vm.player::skipTo,
                        onQueue = { overlay = Overlay.Queue },
                        onSleep = { overlay = Overlay.Sleep },
                        onPlayTracks = { list, i -> vm.play(list, i) },
                        onOpenAlbum = { key ->
                            vm.openPlayer(false)
                            vm.open(Route.Detail(CollectionKind.ALBUM, key))
                        },
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

        if (confirmClearHistory) {
            ConfirmDialog(
                "Effacer l'historique ?", "Les $listenCount écoutes enregistrées seront supprimées définitivement. Vos playlists et favoris ne changent pas.", "Effacer",
                onConfirm = { vm.clearHistory() },
                onDismiss = { confirmClearHistory = false },
            )
        }

        when (val o = overlay) {
            null -> Unit
            Overlay.Sort -> SortSheet(
                current = librarySort,
                onPick = vm::setLibrarySort,
                onDismiss = { if (overlay == o) overlay = null },
            )
            Overlay.Queue -> QueueSheet(
                items = playback.queueIds.mapIndexedNotNull { i, id -> catalog.byId[id]?.let { i to it } },
                currentIndex = playback.queueIndex,
                onSkip = vm.player::skipTo,
                onDismiss = { if (overlay == o) overlay = null },
            )
            Overlay.Sleep -> SleepSheet(
                endsAt = sleepEndsAt,
                onPick = vm::setSleepTimer,
                onDismiss = { if (overlay == o) overlay = null },
            )
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
    val shape = remember(Appearance.colors) { decoShape(4.dp) }
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


/** Animates the cover option's colours toward the current track's and publishes them to [Appearance]. */
@Composable
private fun CoverColorSync(track: Track?) {
    val coverArt = rememberArtwork(track, 256)
    val base = Appearance.baseColors
    val coverPrimary by animateColorAsState(coverArt?.accent ?: base.primary, tween(700), label = "coverPrimary")
    val coverSecondary by animateColorAsState(coverArt?.secondary ?: base.secondary, tween(700), label = "coverSecondary")
    // Read here, in composition, so every animation frame re-runs the effect below.
    val p = coverPrimary
    val sec = coverSecondary
    val hasCover = coverArt != null
    SideEffect {
        Appearance.coverPrimary = if (hasCover) p else null
        Appearance.coverSecondary = if (hasCover) sec else null
    }
}
