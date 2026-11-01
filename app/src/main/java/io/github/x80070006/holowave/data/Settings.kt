package io.github.x80070006.holowave.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    /** Ask lrclib.net for lyrics that are neither embedded nor cached. */
    val onlineLyrics: Boolean = true,
    /** Folders to index (MediaStore relative paths); empty means all of them. */
    val sourceFolders: Set<String> = emptySet(),
    /** Also list sound-only WebM / 3GP files, which Android files under videos. */
    val videoContainers: Boolean = false,
    /** Keep a private, on-device journal of what is played (taste learning, exportable, erasable). */
    val recordHistory: Boolean = true,
    /** Learn which songs are streamed in NouTube (needs notification access). Off until the user opts in. */
    val captureStreaming: Boolean = false,
    /** Library: collections as a grid (true) or a list, and the sort order (a LibrarySort name). */
    val libraryGrid: Boolean = true,
    val librarySort: String = "AZ",
)

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        AppSettings(
            onlineLyrics = prefs.getBoolean(KEY_ONLINE_LYRICS, true),
            sourceFolders = prefs.getStringSet(KEY_FOLDERS, null).orEmpty()
                .mapNotNull(SourceFolders::normalize).take(SourceFolders.MAX_SELECTED).toSet(),
            videoContainers = prefs.getBoolean(KEY_VIDEO_CONTAINERS, false),
            recordHistory = prefs.getBoolean(KEY_RECORD_HISTORY, true),
            captureStreaming = prefs.getBoolean(KEY_CAPTURE, false),
            libraryGrid = prefs.getBoolean(KEY_GRID, true),
            librarySort = prefs.getString(KEY_SORT, null) ?: "AZ",
        )
    )
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun setOnlineLyrics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ONLINE_LYRICS, enabled).apply()
        _state.value = _state.value.copy(onlineLyrics = enabled)
    }

    fun setSourceFolders(folders: Set<String>) {
        val clean = folders.mapNotNull(SourceFolders::normalize).take(SourceFolders.MAX_SELECTED).toSet()
        // getStringSet returns a live set: always store a fresh copy.
        prefs.edit().putStringSet(KEY_FOLDERS, HashSet(clean)).apply()
        _state.value = _state.value.copy(sourceFolders = clean)
    }

    fun setVideoContainers(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_CONTAINERS, enabled).apply()
        _state.value = _state.value.copy(videoContainers = enabled)
    }

    fun setRecordHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RECORD_HISTORY, enabled).apply()
        _state.value = _state.value.copy(recordHistory = enabled)
    }

    fun setCaptureStreaming(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CAPTURE, enabled).apply()
        _state.value = _state.value.copy(captureStreaming = enabled)
    }

    fun setLibraryGrid(grid: Boolean) {
        prefs.edit().putBoolean(KEY_GRID, grid).apply()
        _state.value = _state.value.copy(libraryGrid = grid)
    }

    fun setLibrarySort(name: String) {
        prefs.edit().putString(KEY_SORT, name).apply()
        _state.value = _state.value.copy(librarySort = name)
    }

    /** True only the first time it is called: the very first launch gets a full storage scan. */
    fun consumeFirstScan(): Boolean {
        if (prefs.getBoolean(KEY_FIRST_SCAN_DONE, false)) return false
        prefs.edit().putBoolean(KEY_FIRST_SCAN_DONE, true).apply()
        return true
    }

    private companion object {
        const val KEY_ONLINE_LYRICS = "online_lyrics"
        const val KEY_FOLDERS = "source_folders"
        const val KEY_FIRST_SCAN_DONE = "first_scan_done"
        const val KEY_VIDEO_CONTAINERS = "video_containers"
        const val KEY_RECORD_HISTORY = "record_history"
        const val KEY_CAPTURE = "capture_streaming"
        const val KEY_GRID = "library_grid"
        const val KEY_SORT = "library_sort"
    }
}
