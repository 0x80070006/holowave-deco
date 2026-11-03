package io.github.x80070006.holowave.ui

import android.app.Application
import android.net.Uri
import io.github.x80070006.holowave.playback.SourceKind
import io.github.x80070006.holowave.recommend.FlowEngine
import io.github.x80070006.holowave.recommend.FlowVariant
import io.github.x80070006.holowave.recommend.MixPlan
import io.github.x80070006.holowave.recommend.Shelf
import io.github.x80070006.holowave.recommend.ShelfInputs
import io.github.x80070006.holowave.recommend.Shelves
import io.github.x80070006.holowave.stream.NouTubeBridge
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.x80070006.holowave.HolowaveApp
import io.github.x80070006.holowave.data.AudioScanner
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.FolderNode
import io.github.x80070006.holowave.data.SourceFolders
import io.github.x80070006.holowave.data.Collections
import io.github.x80070006.holowave.data.LibraryState
import io.github.x80070006.holowave.data.MediaLibrary
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.LibrarySort
import io.github.x80070006.holowave.data.LibrarySorting
import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.data.TrackKey
import io.github.x80070006.holowave.data.TrackSource
import io.github.x80070006.holowave.lyrics.LyricsLookup
import io.github.x80070006.holowave.ui.theme.ModePref
import io.github.x80070006.holowave.ui.theme.MotionPref
import io.github.x80070006.holowave.ui.theme.MonoAccent
import io.github.x80070006.holowave.ui.theme.ThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"), ALBUMS("Albums"), ARTISTS("Artistes"), TRACKS("Titres"), FOLDERS("Dossiers"), NOUTUBE("NouTube")
}

/** What the library shows for the selected chip, already filtered and sorted. */
sealed interface LibraryContent {
    data class Collections(val items: List<TrackCollection>, val pinned: TrackCollection? = null) : LibraryContent
    data class Tracks(val items: List<Track>) : LibraryContent
    /** NouTube: downloaded files and streamed songs in one list. */
    data class Mixed(val items: List<MixedItem>) : LibraryContent
}

/** Position of each song in the listening history (0 = most recent), for the "recently played" sort. */
class RecentRank(val local: Map<Long, Int> = emptyMap(), val stream: Map<String, Int> = emptyMap())

/** The four tabs of the bottom bar. */
enum class MainTab(val label: String) { HOME("Accueil"), SEARCH("Recherche"), LIBRARY("Bibliothèque"), SETTINGS("Réglages") }

/** Filter chips at the top of the home screen. */
enum class HomeFilter(val label: String) { ALL("Tout"), LOCAL("Musique locale"), NOUTUBE("NouTube"), MIXES("Mixes") }

/** Everything the home screen shows, already filtered. */
data class HomeData(
    /** Recently played: files on the device and songs streamed in NouTube, interleaved by recency. */
    val recent: List<MixedItem> = emptyList(),
    /** Compact tiles of the "resume" grid: favourites first, then recent and newly added albums. */
    val resume: List<TrackCollection> = emptyList(),
    val addedAlbums: List<TrackCollection> = emptyList(),
    /** Last thing played: its cover colours the top of the screen. */
    val lastTrack: Track? = null,
    /** NouTube chip: every song heard on YouTube Music, most recent first. */
    val streams: List<StreamedTrack> = emptyList(),
)

sealed interface Route {
    /** Root of the selected bottom tab. */
    data object Library : Route
    data class Detail(val kind: CollectionKind, val key: String) : Route
    data object SourceFolders : Route
    data object ThemePreview : Route
    data object YoutubeAccount : Route
}

data class Catalog(
    val tracks: List<Track> = emptyList(),
    val albums: List<TrackCollection> = emptyList(),
    val artists: List<TrackCollection> = emptyList(),
    val folders: List<TrackCollection> = emptyList(),
    val playlists: List<TrackCollection> = emptyList(),
    val favorites: TrackCollection? = null,
    val favoriteIds: Set<Long> = emptySet(),
    val byId: Map<Long, Track> = emptyMap(),
    /** Stable identity (see TrackKey) to track, to match the listening history. */
    val byKey: Map<String, Track> = emptyMap(),
    val albumsByKey: Map<String, TrackCollection> = emptyMap(),
)

sealed interface LyricsUi {
    data object NoTrack : LyricsUi
    data object Loading : LyricsUi
    data class Ready(val trackId: Long, val lookup: LyricsLookup) : LyricsUi
}

data class PrefetchProgress(val done: Int, val total: Int, val found: Int, val running: Boolean)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HolowaveApp
    val artwork = app.artwork
    val player = app.player
    val settings = app.settings.state
    val libraryState = app.library.state
    val playback = app.player.state
    val position = app.player.position

    /** What the app follows right now: local files, or a song played by NouTube. */
    val sources = app.sources
    val tube = app.tubeHost

    // Declared before the init block that watches the queue: initializers run in order.
    private val _flow = MutableStateFlow<FlowVariant?>(null)
    /** The Flow currently feeding the queue, or null when the user plays something of their own. */
    val flowVariant: StateFlow<FlowVariant?> = _flow.asStateFlow()
    private var refilling = false

    val youtubeSignedIn = app.tubeHost.loggedIn
    val streamActive: StateFlow<Boolean> = combine(sources.active, sources.now) { kind, now ->
        kind == SourceKind.NOUTUBE && now.hasTrack
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Position of the NouTube song, refreshed 4 times a second while one is playing. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val streamPosition: StateFlow<Long> = streamActive.transformLatest { active ->
        if (!active) {
            emit(0L)
            return@transformLatest
        }
        while (true) {
            emit(sources.source(SourceKind.NOUTUBE).position())
            kotlinx.coroutines.delay(250)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun openInNouTube(): Boolean {
        val s = sources.now.value
        val uri = NouTubeBridge.searchUri("${s.artist} ${s.title}")
        return NouTubeBridge.open(app, uri)
    }

    private val _route = MutableStateFlow<List<Route>>(listOf(Route.Library))
    val route: StateFlow<List<Route>> = _route.asStateFlow()

    private val _libraryFilter = MutableStateFlow("")
    val libraryFilter: StateFlow<String> = _libraryFilter.asStateFlow()
    private val _libraryFilterOpen = MutableStateFlow(false)
    val libraryFilterOpen: StateFlow<Boolean> = _libraryFilterOpen.asStateFlow()

    private val _tab = MutableStateFlow(LibraryTab.ALBUMS)
    val tab: StateFlow<LibraryTab> = _tab.asStateFlow()

    private val _mainTab = MutableStateFlow(MainTab.HOME)
    val mainTab: StateFlow<MainTab> = _mainTab.asStateFlow()

    private val _homeFilter = MutableStateFlow(HomeFilter.ALL)
    val homeFilter: StateFlow<HomeFilter> = _homeFilter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _playerOpen = MutableStateFlow(false)
    val playerOpen: StateFlow<Boolean> = _playerOpen.asStateFlow()

    private val _lyricsOpen = MutableStateFlow(false)
    val lyricsOpen: StateFlow<Boolean> = _lyricsOpen.asStateFlow()

    private val _prefetch = MutableStateFlow<PrefetchProgress?>(null)
    val prefetch: StateFlow<PrefetchProgress?> = _prefetch.asStateFlow()
    private var prefetchJob: Job? = null

    private val _messages = MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages.asStateFlow()

    private val sourceFolders = settings.map { it.sourceFolders }.distinctUntilChanged()

    val catalog: StateFlow<Catalog> = combine(app.library.state, app.playlists.state, sourceFolders) { lib, col, folders ->
        if (lib is LibraryState.Ready) buildCatalog(lib.filtered(folders), col) else Catalog()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, Catalog())

    /** Every music folder on the device, regardless of the current selection. */
    val folderTree: StateFlow<List<FolderNode>> = app.library.state
        .map { lib -> if (lib is LibraryState.Ready) SourceFolders.tree(lib.tracks) else emptyList() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** Total number of indexed tracks before folder filtering. */
    val allTrackCount: StateFlow<Int> = app.library.state
        .map { (it as? LibraryState.Ready)?.tracks?.size ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val streamed: StateFlow<List<StreamedTrack>> = app.streamed.all.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val streamedCount: StateFlow<Int> = app.streamed.count.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /** Listening order of everything heard, files and streamed songs alike. */
    val recentRank: StateFlow<RecentRank> = combine(catalog, app.history.recentKeys(500)) { c, keys ->
        val local = HashMap<Long, Int>()
        val stream = HashMap<String, Int>()
        keys.forEachIndexed { i, k ->
            val t = c.byKey[k]
            if (t != null) local[t.id] = i else stream[k] = i
        }
        RecentRank(local, stream)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, RecentRank())

    val home: StateFlow<HomeData> = combine(catalog, app.history.recentKeys(60), _homeFilter, streamed) { c, keys, filter, streams ->
        val allowed: (Track) -> Boolean = when (filter) {
            HomeFilter.NOUTUBE -> { _ -> false }
            HomeFilter.LOCAL -> { t -> t.source == TrackSource.LOCAL }
            else -> { _ -> true }
        }
        val streamsAllowed = filter == HomeFilter.ALL || filter == HomeFilter.NOUTUBE
        val byStreamKey = streams.associateBy { it.key }
        val recent = ArrayList<MixedItem>()
        val seen = HashSet<Any>()
        for (k in keys) {
            val local = c.byKey[k]
            if (local != null) {
                if (allowed(local) && seen.add(local.id)) recent += MixedItem.Local(local)
            } else if (streamsAllowed) {
                byStreamKey[k]?.let { if (seen.add(it.key)) recent += MixedItem.Stream(it) }
            }
        }
        val recentLocal = recent.filterIsInstance<MixedItem.Local>().map { it.track }
        val added = c.albums.filter { a -> a.tracks.any(allowed) }
            .sortedByDescending { a -> a.tracks.maxOf { it.dateAdded } }.take(14)
        val fromRecent = recentLocal.map { it.albumId.toString() }.distinct().mapNotNull { c.albumsByKey[it] }
        val tiles = ArrayList<TrackCollection>()
        c.favorites?.takeIf { it.tracks.any(allowed) && filter != HomeFilter.NOUTUBE }?.let { tiles += it }
        (fromRecent + added).forEach { a -> if (tiles.size < 8 && tiles.none { it.key == a.key && it.kind == a.kind }) tiles += a }
        HomeData(recent.take(20), tiles, added, recentLocal.firstOrNull(), if (filter == HomeFilter.NOUTUBE) streams.sortedWith(compareByDescending<StreamedTrack> { it.plays }.thenByDescending { it.lastSeen }) else emptyList())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, HomeData())

    /** The rows of the "Tout" chip, rebuilt when the library, the YouTube list or the history change. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val shelves: StateFlow<List<Shelf>> = combine(catalog, streamed, app.history.count) { c, s, _ -> c to s }
        .transformLatest { (c, s) ->
            kotlinx.coroutines.delay(300)
            val stats = FlowEngine.aggregate(app.history.taste())
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            emit(
                withContext(Dispatchers.Default) {
                    Shelves.build(ShelfInputs(c.tracks, c.albums, c.artists, c.playlists, c.folders, c.favorites, s, stats, System.currentTimeMillis(), hour))
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Plays a row from the song that was tapped; YouTube songs and files follow each other. */
    fun playShelf(shelf: Shelf, index: Int) {
        _flow.value = null
        playMix(shelf.songs, index)
    }

    private class LibraryInputs(val c: Catalog, val tab: LibraryTab, val sort: LibrarySort, val filter: String)

    val librarySort: StateFlow<LibrarySort> = settings.map { s -> LibrarySort.entries.firstOrNull { it.name == s.librarySort } ?: LibrarySort.AZ }
        .distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, LibrarySort.AZ)

    val libraryContent: StateFlow<LibraryContent> = combine(
        combine(catalog, _tab, librarySort, _libraryFilter) { c, t, s, f -> LibraryInputs(c, t, s, f) },
        streamed, recentRank,
    ) { i, streams, rank ->
        val rankOf: (Track) -> Int? = { rank.local[it.id] }
        val f = i.filter
        fun cols(list: List<TrackCollection>) =
            LibrarySorting.collections(list.filter { LibrarySorting.matches(f, it.title, it.subtitle) }, i.sort, rankOf)
        when (i.tab) {
            LibraryTab.PLAYLISTS -> LibraryContent.Collections(
                cols(i.c.playlists),
                i.c.favorites?.takeIf { LibrarySorting.matches(f, "Titres aimés") || f.isBlank() },
            )
            LibraryTab.ALBUMS -> LibraryContent.Collections(cols(i.c.albums))
            LibraryTab.ARTISTS -> LibraryContent.Collections(cols(i.c.artists))
            LibraryTab.FOLDERS -> LibraryContent.Collections(cols(i.c.folders))
            LibraryTab.TRACKS -> LibraryContent.Tracks(
                LibrarySorting.tracks(i.c.tracks.filter { LibrarySorting.matches(f, it.title, it.artist, it.album) }, i.sort, rankOf),
            )
            LibraryTab.NOUTUBE -> {
                val items: List<MixedItem> = streams.map { MixedItem.Stream(it) }
                val kept = items.filter {
                    when (it) {
                        is MixedItem.Local -> LibrarySorting.matches(f, it.track.title, it.track.artist, it.track.album)
                        is MixedItem.Stream -> LibrarySorting.matches(f, it.track.title, it.track.artist, it.track.album)
                    }
                }
                LibraryContent.Mixed(LibrarySorting.mixed(kept, i.sort, rankOf) { rank.stream[it.key] })
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, LibraryContent.Collections(emptyList()))

    val searchResults: StateFlow<List<Track>> = combine(catalog, _query) { c, q ->
        val needle = q.fold()
        if (needle.isBlank()) emptyList()
        else c.tracks.filter { "${it.title} ${it.artist} ${it.album}".fold().contains(needle) }.take(200)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentTrack: StateFlow<Track?> = combine(playback, catalog) { p, c -> p.currentId?.let { c.byId[it] } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lyricsTrigger = combine(currentTrack, settings.map { it.onlineLyrics }.distinctUntilChanged(), ::Pair)

    private val reloadLyrics = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val lyrics: StateFlow<LyricsUi> = combine(lyricsTrigger, reloadLyrics) { t, _ -> t }
        .transformLatest { (track, online) ->
            if (track == null) {
                emit(LyricsUi.NoTrack)
                return@transformLatest
            }
            emit(LyricsUi.Loading)
            emit(LyricsUi.Ready(track.id, app.lyrics.lyricsFor(track, online)))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LyricsUi.NoTrack)

    init {
        player.connect()
        watchMixEnds()
        viewModelScope.launch {
            player.errors.collect { title ->
                _messages.value = "Lecture impossible" + (if (title.isNotBlank()) " : « $title »" else "") + " — format non pris en charge ou fichier abîmé"
            }
        }
    }

    val scan = app.library.scan
    val listenCount: StateFlow<Int> = app.history.count.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun onPermissionGranted() {
        app.library.start()
        if (app.settings.consumeFirstScan()) app.library.scanStorage(settings.value.sourceFolders)
    }

    /** Real scan of the storage (selected folders, or everything), then reload. */
    fun scanStorage() = app.library.scanStorage(settings.value.sourceFolders)
    fun cancelScan() = app.library.cancelScan()

    // ---------------------------------------------------------- navigation

    fun open(route: Route) {
        _route.value = _route.value + route
    }

    fun back(): Boolean {
        when {
            _lyricsOpen.value -> _lyricsOpen.value = false
            _playerOpen.value -> _playerOpen.value = false
            _route.value.size > 1 -> _route.value = _route.value.dropLast(1)
            _query.value.isNotEmpty() -> _query.value = ""
            _mainTab.value != MainTab.HOME -> _mainTab.value = MainTab.HOME
            else -> return false
        }
        return true
    }

    /** Switching tabs always returns to the tab's own root screen. */
    fun selectMainTab(tab: MainTab) {
        _route.value = listOf(Route.Library)
        _mainTab.value = tab
    }

    fun setHomeFilter(filter: HomeFilter) {
        _homeFilter.value = filter
    }

    fun toggleLibraryFilter() {
        _libraryFilterOpen.value = !_libraryFilterOpen.value
        if (!_libraryFilterOpen.value) _libraryFilter.value = ""
    }

    fun setLibraryFilter(q: String) {
        _libraryFilter.value = q.take(100)
    }

    fun setLibrarySort(sort: LibrarySort) = app.settings.setLibrarySort(sort.name)
    fun toggleLibraryGrid() = app.settings.setLibraryGrid(!settings.value.libraryGrid)

    // --- NouTube
    fun setCaptureStreaming(enabled: Boolean) = app.settings.setCaptureStreaming(enabled)

    fun openInNouTube(track: StreamedTrack): Boolean {
        val ok = NouTubeBridge.open(getApplication(), NouTubeBridge.trackUri(track))
        if (!ok) _messages.value = "NouTube n'est pas installé : impossible d'ouvrir ce morceau"
        return ok
    }

    fun clearStreamed() {
        viewModelScope.launch {
            app.streamed.clear()
            _messages.value = "Morceaux streamés effacés"
        }
    }

    fun selectTab(tab: LibraryTab) {
        _tab.value = tab
        _libraryFilter.value = ""
    }

    fun setQuery(q: String) {
        _query.value = q.take(100)
    }

    fun openPlayer(open: Boolean) {
        _playerOpen.value = open
        if (!open) _lyricsOpen.value = false
    }

    fun openLyrics(open: Boolean) {
        _lyricsOpen.value = open
    }

    fun consumeMessage() {
        _messages.value = null
    }

    fun collection(kind: CollectionKind, key: String): TrackCollection? {
        val c = catalog.value
        return when (kind) {
            CollectionKind.ALBUM -> c.albums.firstOrNull { it.key == key }
            CollectionKind.ARTIST -> c.artists.firstOrNull { it.key == key }
            CollectionKind.FOLDER -> c.folders.firstOrNull { it.key == key }
            CollectionKind.PLAYLIST -> c.playlists.firstOrNull { it.key == key }
            CollectionKind.FAVORITES -> c.favorites
        }
    }

    // ------------------------------------------------------------ flow


    fun startFlow(variant: FlowVariant = FlowVariant.MIX) {
        viewModelScope.launch {
            val picked = pickFlow(variant, 30, emptySet())
            if (picked.isEmpty()) {
                _messages.value = if (catalog.value.tracks.isEmpty()) "Votre bibliothèque est vide" else "Pas assez de titres pour ce Flow"
                return@launch
            }
            _flow.value = variant
            playMix(picked)
        }
    }

    private suspend fun pickFlow(variant: FlowVariant, count: Int, exclude: Set<Long>): List<MixedItem> = withContext(Dispatchers.Default) {
        val stats = FlowEngine.aggregate(app.history.taste())
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        FlowEngine.pickMixed(
            catalog.value.tracks, streamed.value, stats, variant, System.currentTimeMillis(), hour, count,
            kotlin.random.Random.Default, exclude = exclude,
        )
    }

    // ------------------------------------------------------------ mixed queue
    // Files of the device and YouTube songs in one queue: runs of files go to the local player,
    // each YouTube song to the embedded page; the next run starts when the previous one ends.

    private var plan: MixPlan? = null
    private var planIndex = 0
    private var planRunEnd = 0
    private var handledEnd = -1

    fun playMix(items: List<MixedItem>, start: Int = 0) {
        if (items.isEmpty()) return
        plan = MixPlan(items)
        handledEnd = -1
        startItem(start.coerceIn(0, items.lastIndex))
    }

    private fun startItem(i: Int) {
        val p = plan ?: return
        val item = p.items.getOrNull(i) ?: return
        planIndex = i
        when (item) {
            is MixedItem.Local -> {
                planRunEnd = p.runEnd(i)
                player.play(p.localTracks(i, planRunEnd), 0)
            }
            is MixedItem.Stream -> {
                planRunEnd = i
                item.track.videoId?.let { app.tubeHost.watch(it) }
            }
        }
        if (_flow.value != null && p.items.size - i <= 4) refillPlan()
    }

    private fun refillPlan() {
        val variant = _flow.value ?: return
        if (refilling) return
        refilling = true
        viewModelScope.launch {
            try {
                val p = plan ?: return@launch
                val have = p.items.filterIsInstance<MixedItem.Local>().map { it.track.id }.toSet()
                val more = pickFlow(variant, 20, have)
                if (more.isNotEmpty()) plan = MixPlan(p.items + more)
            } finally {
                refilling = false
            }
        }
    }

    /** The local player played its last song of the run, or a YouTube song ended: go on with the plan. */
    private fun watchMixEnds() {
        viewModelScope.launch {
            playback.collect { p ->
                val pl = plan ?: return@collect
                if (p.ended && sources.active.value == SourceKind.LOCAL && handledEnd != planIndex) {
                    handledEnd = planIndex
                    pl.after(planRunEnd)?.let { startItem(it) }
                }
            }
        }
        viewModelScope.launch {
            app.youtubeEnded.collect {
                val pl = plan ?: return@collect
                if (pl.items.getOrNull(planIndex) is MixedItem.Stream) pl.after(planIndex)?.let { startItem(it) }
            }
        }
    }

    /** Next and previous that cross from local files to a YouTube song and back. */
    fun next() {
        val pl = plan
        if (pl == null) {
            sources.activeSource.next()
            return
        }
        val onYoutube = pl.items.getOrNull(planIndex) is MixedItem.Stream
        val lastOfRun = playback.value.let { it.queueIds.isEmpty() || it.queueIndex >= it.queueIds.size - 1 }
        if (onYoutube || lastOfRun) {
            pl.after(planRunEnd)?.let { startItem(it) } ?: sources.activeSource.next()
        } else {
            player.next()
        }
    }

    fun previous() {
        val pl = plan
        if (pl == null) {
            sources.activeSource.previous()
            return
        }
        val onYoutube = pl.items.getOrNull(planIndex) is MixedItem.Stream
        val firstOfRun = playback.value.queueIndex <= 0
        // Like any player: the first press restarts what is playing, a press right after goes back.
        if ((onYoutube || firstOfRun) && sources.activeSource.position() < 4_000) {
            pl.before(planIndex)?.let { startItem(it) } ?: sources.activeSource.previous()
        } else {
            sources.activeSource.previous()
        }
    }

    /** A song heard on YouTube Music: played by the embedded page when its id is known, found by a search otherwise. */
    fun playStream(track: StreamedTrack) {
        _flow.value = null
        plan = null
        val id = track.videoId
        if (id != null) app.tubeHost.watch(id)
        else {
            app.tubeHost.open(NouTubeBridge.searchUri("${track.artist} ${track.title}").toString())
            open(Route.YoutubeAccount)
        }
    }

    // ------------------------------------------------------------ playback

    fun play(tracks: List<Track>, index: Int, shuffle: Boolean = false) {
        _flow.value = null
        plan = null
        player.play(tracks, if (shuffle) (tracks.indices).random() else index, shuffle)
    }

    fun playNext(track: Track) {
        player.playNext(track)
        _messages.value = "« ${track.title} » sera joué ensuite"
    }

    fun enqueue(track: Track) {
        player.enqueue(track)
        _messages.value = "Ajouté à la file d'attente"
    }

    // ----------------------------------------------------------- playlists

    fun toggleFavorite(trackId: Long) = app.playlists.toggleFavorite(trackId)

    fun createPlaylist(name: String, withTrack: Track? = null) {
        val id = app.playlists.create(name)
        if (withTrack != null) {
            app.playlists.add(id, listOf(withTrack.id))
            _messages.value = "Ajouté à « ${name.trim()} »"
        }
    }

    fun addToPlaylist(playlistKey: String, track: Track) {
        app.playlists.add(playlistKey, listOf(track.id))
        val name = catalog.value.playlists.firstOrNull { it.key == playlistKey }?.title ?: "la playlist"
        _messages.value = "Ajouté à « $name »"
    }

    fun removeFromPlaylist(playlistKey: String, track: Track) = app.playlists.remove(playlistKey, track.id)
    fun renamePlaylist(playlistKey: String, name: String) = app.playlists.rename(playlistKey, name)

    fun deletePlaylist(playlistKey: String) {
        app.playlists.delete(playlistKey)
        _route.value = _route.value.filterNot { it is Route.Detail && it.key == playlistKey }.ifEmpty { listOf(Route.Library) }
    }

    // -------------------------------------------------------------- lyrics

    fun setOnlineLyrics(enabled: Boolean) = app.settings.setOnlineLyrics(enabled)

    fun toggleSourceFolder(key: String) =
        app.settings.setSourceFolders(SourceFolders.toggle(key, settings.value.sourceFolders))

    fun removeSourceFolder(key: String) {
        app.settings.setSourceFolders(settings.value.sourceFolders - key)
        releaseGrant(key)
    }

    /** Gives back the picker grant of a folder the user no longer wants. */
    private fun releaseGrant(key: String) {
        val resolver = getApplication<Application>().contentResolver
        resolver.persistedUriPermissions
            .filter { AudioScanner.keyOfTree(it.uri) == key }
            .forEach { runCatching { resolver.releasePersistableUriPermission(it.uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    }

    /**
     * Adds a folder chosen in the system picker and keeps its read-only grant, so
     * later scans can list it fully (including files Android hides because they
     * were never indexed). Then the folder is scanned right away.
     */
    fun addPickedFolder(treeUri: Uri) {
        val key = AudioScanner.keyOfTree(treeUri)
        if (key == null) {
            _messages.value = "Choisissez un dossier précis de la mémoire du téléphone ou de la carte SD"
            return
        }
        runCatching {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(treeUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val selection = SourceFolders.add(key, settings.value.sourceFolders)
        app.settings.setSourceFolders(selection)
        _messages.value = "« ${SourceFolders.label(key)} » ajouté, recherche des morceaux…"
        app.library.scanStorage(setOf(key))
    }

    /** Turns WebM / 3GP support on or off; [granted] tells whether the video permission is available. */
    fun setVideoContainers(enabled: Boolean, granted: Boolean) {
        if (enabled && !granted) {
            _messages.value = "Autorisation refusée : les fichiers WebM et 3GP restent masqués"
            return
        }
        app.settings.setVideoContainers(enabled)
        app.library.refresh()
        if (enabled) app.library.scanStorage(settings.value.sourceFolders)
    }

    fun setTheme(id: ThemeId) = app.appearance.setTheme(id)
    fun setMode(mode: ModePref) = app.appearance.setMode(mode)
    fun setMonoAccent(accent: MonoAccent) = app.appearance.setMonoAccent(accent)
    fun setCoverColors(enabled: Boolean) = app.appearance.setCoverColors(enabled)
    fun setMotion(motion: MotionPref) = app.appearance.setMotion(motion)

    val sleepEndsAt = app.player.sleepEndsAt
    fun setSleepTimer(minutes: Int?) = app.player.setSleepTimer(minutes)

    fun setRecordHistory(enabled: Boolean) = app.settings.setRecordHistory(enabled)

    /** Writes the whole listening journal to a file the user picked, as JSON. */
    fun exportHistory(target: Uri) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val written = runCatching {
                resolver.openOutputStream(target, "wt")?.use { app.history.export(it) } ?: -1
            }.getOrDefault(-1)
            _messages.value = if (written >= 0) "Historique exporté ($written écoute${if (written > 1) "s" else ""})" else "Export impossible"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            app.history.clear()
            _messages.value = "Historique d'écoute effacé"
        }
    }

    fun includeAllFolders() = app.settings.setSourceFolders(emptySet())

    fun importLyrics(uri: Uri) {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            val ok = app.lyrics.import(track, uri)
            _messages.value = if (ok) "Paroles importées" else "Fichier de paroles illisible"
            if (ok) reloadLyrics.value++
        }
    }

    fun retryLyrics() {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            app.lyrics.forget(track)
            reloadLyrics.value++
        }
    }

    fun storedLyricsCount(): Int = app.lyrics.storedCount()

    fun prefetchAllLyrics() {
        if (prefetchJob?.isActive == true) return
        val tracks = catalog.value.tracks
        prefetchJob = viewModelScope.launch {
            var found = 0
            tracks.forEachIndexed { i, t ->
                _prefetch.value = PrefetchProgress(i, tracks.size, found, true)
                if (app.lyrics.prefetch(t)) found++
            }
            _prefetch.value = PrefetchProgress(tracks.size, tracks.size, found, false)
        }
    }

    fun cancelPrefetch() {
        prefetchJob?.cancel()
        _prefetch.value = _prefetch.value?.copy(running = false)
    }

    fun clearDownloadedLyrics() {
        viewModelScope.launch {
            app.lyrics.clearDownloaded()
            _messages.value = "Paroles téléchargées effacées"
            reloadLyrics.value++
        }
    }

    override fun onCleared() {
        prefetchJob?.cancel()
        super.onCleared()
    }

    companion object {
        private fun String.fold(): String =
            Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT).trim()

        private fun LibraryState.Ready.filtered(folders: Set<String>): LibraryState.Ready =
            if (folders.isEmpty()) this
            else LibraryState.Ready(tracks.filter { SourceFolders.includes(SourceFolders.key(it.volume, it.relativePath), folders) })

        private val byNumberThenTitle = compareBy<Track>({ it.trackNumber }, { it.title.lowercase() })

        fun buildCatalog(lib: LibraryState.Ready, col: Collections): Catalog {
            val tracks = lib.tracks
            val albums = tracks.groupBy { it.albumId }.map { (id, list) ->
                val artists = list.map { it.artist }.distinct()
                TrackCollection(
                    CollectionKind.ALBUM, id.toString(), list.first().album,
                    if (artists.size == 1) artists.first() else "Artistes variés",
                    list.sortedWith(byNumberThenTitle),
                )
            }.sortedBy { it.title.lowercase() }
            val artists = tracks.groupBy { it.artist }.map { (name, list) ->
                val albumCount = list.map { it.albumId }.distinct().size
                TrackCollection(
                    CollectionKind.ARTIST, name, name,
                    "$albumCount album${if (albumCount > 1) "s" else ""} · ${list.size} titre${if (list.size > 1) "s" else ""}",
                    list.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNumber })),
                )
            }.sortedWith(compareBy({ it.title == MediaLibrary.UNKNOWN_ARTIST }, { it.title.lowercase() }))
            val folders = tracks.groupBy { it.folder }.map { (name, list) ->
                TrackCollection(CollectionKind.FOLDER, name, name, "${list.size} titres", list.sortedBy { it.title.lowercase() })
            }.sortedBy { it.title.lowercase() }
            val playlists = col.playlists.map { p ->
                val list = p.trackIds.mapNotNull { lib.byId[it] }
                TrackCollection(CollectionKind.PLAYLIST, p.id, p.name, "${list.size} titre${if (list.size > 1) "s" else ""}", list)
            }
            val favTracks = col.favorites.mapNotNull { lib.byId[it] }.sortedBy { it.title.lowercase() }
            val favorites = TrackCollection(
                CollectionKind.FAVORITES, "favorites", "Coups de cœur",
                "${favTracks.size} titre${if (favTracks.size > 1) "s" else ""}", favTracks,
            )
            return Catalog(
                tracks, albums, artists, folders, playlists, favorites, col.favorites, lib.byId,
                byKey = tracks.associateBy { TrackKey.of(it) },
                albumsByKey = albums.associateBy { it.key },
            )
        }
    }
}
