package io.github.x80070006.holowave.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LibraryState {
    data object Idle : LibraryState
    data object Loading : LibraryState
    data class Ready(val tracks: List<Track>) : LibraryState {
        val byId: Map<Long, Track> = tracks.associateBy { it.id }
    }
    data class Failed(val reason: String) : LibraryState
}

/**
 * Read-only index of the device's music, built from MediaStore.
 * Nothing is copied: playback streams straight from the content URIs.
 */
class MediaLibrary(
    private val resolver: ContentResolver,
    private val scope: CoroutineScope,
    private val scanner: AudioScanner,
) {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Idle)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _scan = MutableStateFlow<ScanProgress?>(null)
    /** Progress of the last storage scan; null when none ran in this session. */
    val scan: StateFlow<ScanProgress?> = _scan.asStateFlow()
    private var scanJob: Job? = null

    val isScanning: Boolean get() = scanJob?.isActive == true

    /**
     * Walks [selection] (or all storage) for audio files, gets new ones indexed by
     * Android, then reloads the library. Runs in the app scope so leaving the
     * settings screen does not interrupt it.
     */
    fun scanStorage(selection: Set<String>) {
        if (isScanning) return
        _scan.value = ScanProgress.Searching(0, 0, "")
        scanJob = scope.launch {
            try {
                scanner.scan(selection) { p -> _scan.value = p }
            } finally {
                refresh()
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scan.value = null
    }

    private var observer: ContentObserver? = null
    private var pendingRescan: Job? = null

    fun start() {
        if (observer == null) {
            observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = scheduleRescan()
            }.also {
                runCatching {
                    resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, it)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        scope.launch {
            if (_state.value !is LibraryState.Ready) _state.value = LibraryState.Loading
            _state.value = try {
                LibraryState.Ready(withContext(Dispatchers.IO) { query() })
            } catch (e: SecurityException) {
                LibraryState.Failed("permission")
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore query failed", e)
                LibraryState.Failed("query")
            }
        }
    }

    /** MediaStore fires bursts of change notifications during a scan: coalesce them. */
    private fun scheduleRescan() {
        pendingRescan?.cancel()
        pendingRescan = scope.launch {
            delay(1_500)
            refresh()
        }
    }

    private fun query(): List<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.VOLUME_NAME,
        )
        // Selection is constant: no user input is ever concatenated into SQL.
        // Files registered but not analysed yet have NULL type/duration: keep them, the player
        // reads the real duration, and the next scan completes their metadata.
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.IS_MUSIC} IS NULL)" +
            " AND (${MediaStore.Audio.Media.DURATION} >= ? OR ${MediaStore.Audio.Media.DURATION} IS NULL)"
        val args = arrayOf(MIN_DURATION_MS.toString())
        val tracks = ArrayList<Track>()
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, args,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iVolume = c.getColumnIndexOrThrow(MediaStore.Audio.Media.VOLUME_NAME)
            while (c.moveToNext() && tracks.size < MAX_TRACKS) {
                val path = c.getString(iPath).orEmpty().trimEnd('/')
                tracks += Track(
                    id = c.getLong(iId),
                    title = c.getString(iTitle).clean() ?: "Sans titre",
                    artist = c.getString(iArtist).clean()?.takeUnless { it == "<unknown>" } ?: UNKNOWN_ARTIST,
                    album = c.getString(iAlbum).clean() ?: UNKNOWN_ALBUM,
                    albumId = c.getLong(iAlbumId),
                    durationMs = c.getLong(iDuration),
                    // MediaStore encodes disc * 1000 + track.
                    trackNumber = c.getInt(iTrack) % 1000,
                    year = c.getInt(iYear),
                    folder = path.substringAfterLast('/').ifBlank { "Racine" },
                    relativePath = if (path.isEmpty()) "" else "$path/",
                    volume = c.getString(iVolume) ?: SourceFolders.PRIMARY,
                    dateAdded = c.getLong(iAdded),
                )
            }
        }
        return tracks
    }

    private fun String?.clean(): String? = this?.trim()?.take(MAX_FIELD)?.takeIf { it.isNotEmpty() }

    companion object {
        private const val TAG = "MediaLibrary"
        private const val MIN_DURATION_MS = 20_000L
        private const val MAX_TRACKS = 50_000
        private const val MAX_FIELD = 300
        const val UNKNOWN_ARTIST = "Artiste inconnu"
        const val UNKNOWN_ALBUM = "Album inconnu"
    }
}
