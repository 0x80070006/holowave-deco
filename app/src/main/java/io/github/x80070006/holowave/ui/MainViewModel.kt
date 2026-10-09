package io.github.x80070006.holowave.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.x80070006.holowave.HolowaveApp
import io.github.x80070006.holowave.data.CollectionKind
import io.github.x80070006.holowave.data.FolderNode
import io.github.x80070006.holowave.data.SourceFolders
import io.github.x80070006.holowave.data.Collections
import io.github.x80070006.holowave.data.LibraryState
import io.github.x80070006.holowave.data.MediaLibrary
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.lyrics.LyricsLookup
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
import java.text.Normalizer
import java.util.Locale

enum class LibraryTab(val label: String) {
    TRACKS("Titres"), ALBUMS("Albums"), ARTISTS("Artistes"), FOLDERS("Dossiers"), PLAYLISTS("Playlists")
}

sealed interface Route {
    data object Library : Route
    data class Detail(val kind: CollectionKind, val key: String) : Route
    data object Settings : Route
    data object SourceFolders : Route
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

    private val _route = MutableStateFlow<List<Route>>(listOf(Route.Library))
    val route: StateFlow<List<Route>> = _route.asStateFlow()

    private val _tab = MutableStateFlow(LibraryTab.ALBUMS)
    val tab: StateFlow<LibraryTab> = _tab.asStateFlow()

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
    }

    fun onPermissionGranted() = app.library.start()
    fun rescan() = app.library.refresh()

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
            else -> return false
        }
        return true
    }

    fun selectTab(tab: LibraryTab) {
        _tab.value = tab
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

    // ------------------------------------------------------------ playback

    fun play(tracks: List<Track>, index: Int, shuffle: Boolean = false) {
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
    fun setSpinningRecord(enabled: Boolean) = app.settings.setSpinningRecord(enabled)

    fun toggleSourceFolder(path: String) =
        app.settings.setSourceFolders(SourceFolders.toggle(path, settings.value.sourceFolders))

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
            else LibraryState.Ready(tracks.filter { SourceFolders.includes(it.relativePath, folders) })

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
            return Catalog(tracks, albums, artists, folders, playlists, favorites, col.favorites, lib.byId)
        }
    }
}
