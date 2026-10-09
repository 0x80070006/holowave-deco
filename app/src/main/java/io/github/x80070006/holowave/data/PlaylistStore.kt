package io.github.x80070006.holowave.data

import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Playlist(val id: String, val name: String, val trackIds: List<Long>, val createdAt: Long)

data class Collections(val playlists: List<Playlist> = emptyList(), val favorites: Set<Long> = emptySet())

/**
 * Playlists and favourites, stored as one JSON document in the app's private storage.
 * Writes go through [AtomicFile] so a crash mid-write never corrupts the file.
 */
class PlaylistStore(dir: File, private val scope: CoroutineScope) {

    private val file = AtomicFile(File(dir, "collections.json"))
    private val writeLock = Mutex()
    private val _state = MutableStateFlow(Collections())
    val state: StateFlow<Collections> = _state.asStateFlow()

    fun load() {
        scope.launch(Dispatchers.IO) {
            val loaded = runCatching { if (file.baseFile.exists()) parse(String(file.readFully(), Charsets.UTF_8)) else null }
                .onFailure { Log.w(TAG, "Unreadable collections file, starting fresh", it) }
                .getOrNull()
            if (loaded != null) _state.value = loaded
        }
    }

    fun create(name: String): String {
        val id = UUID.randomUUID().toString()
        mutate { it.copy(playlists = it.playlists + Playlist(id, sanitizeName(name), emptyList(), System.currentTimeMillis())) }
        return id
    }

    fun rename(id: String, name: String) = mutatePlaylist(id) { it.copy(name = sanitizeName(name)) }

    fun delete(id: String) = mutate { c -> c.copy(playlists = c.playlists.filterNot { it.id == id }) }

    fun add(id: String, trackIds: List<Long>) = mutatePlaylist(id) { p ->
        val existing = p.trackIds.toHashSet()
        p.copy(trackIds = (p.trackIds + trackIds.filter { existing.add(it) }).take(MAX_TRACKS_PER_PLAYLIST))
    }

    fun remove(id: String, trackId: Long) = mutatePlaylist(id) { p -> p.copy(trackIds = p.trackIds - trackId) }

    fun toggleFavorite(trackId: Long) = mutate { c ->
        c.copy(favorites = if (trackId in c.favorites) c.favorites - trackId else c.favorites + trackId)
    }

    private fun mutatePlaylist(id: String, block: (Playlist) -> Playlist) = mutate { c ->
        c.copy(playlists = c.playlists.map { if (it.id == id) block(it) else it })
    }

    private fun mutate(block: (Collections) -> Collections) {
        _state.update(block)
        val snapshot = _state.value
        scope.launch(Dispatchers.IO) { writeLock.withLock { write(snapshot) } }
    }

    private fun write(c: Collections) {
        val stream = runCatching { file.startWrite() }.getOrElse { Log.w(TAG, "Cannot open collections file", it); return }
        try {
            stream.write(serialize(c).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            Log.w(TAG, "Cannot save collections", e)
        }
    }

    companion object {
        private const val TAG = "PlaylistStore"
        private const val MAX_PLAYLISTS = 500
        private const val MAX_TRACKS_PER_PLAYLIST = 10_000
        const val MAX_NAME_LENGTH = 60

        /** Drops control characters and clamps the length: names are displayed, never interpreted. */
        fun sanitizeName(raw: String): String =
            raw.filterNot { it.isISOControl() }.trim().take(MAX_NAME_LENGTH).ifEmpty { "Nouvelle playlist" }

        internal fun serialize(c: Collections): String = JSONObject().apply {
            put("version", 1)
            put("favorites", JSONArray(c.favorites.toList()))
            put("playlists", JSONArray().apply {
                c.playlists.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("createdAt", p.createdAt)
                        put("tracks", JSONArray(p.trackIds))
                    })
                }
            })
        }.toString()

        /** Tolerant parser: skips malformed entries instead of failing the whole file. */
        internal fun parse(json: String): Collections {
            val root = JSONObject(json)
            val favorites = root.optJSONArray("favorites")?.longs()?.toSet().orEmpty()
            val playlists = ArrayList<Playlist>()
            val array = root.optJSONArray("playlists") ?: JSONArray()
            for (i in 0 until minOf(array.length(), MAX_PLAYLISTS)) {
                val o = array.optJSONObject(i) ?: continue
                val id = o.optString("id").takeIf { it.isNotBlank() && it.length <= 64 } ?: continue
                playlists += Playlist(
                    id = id,
                    name = sanitizeName(o.optString("name")),
                    trackIds = o.optJSONArray("tracks")?.longs()?.distinct()?.take(MAX_TRACKS_PER_PLAYLIST).orEmpty(),
                    createdAt = o.optLong("createdAt"),
                )
            }
            return Collections(playlists, favorites)
        }

        private fun JSONArray.longs(): List<Long> =
            (0 until length()).mapNotNull { idx -> opt(idx).let { v -> (v as? Number)?.toLong() } }
    }
}
