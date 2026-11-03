// Derived from NouTube (https://github.com/nonbili/NouTube, AGPL-3.0): message names follow its content scripts.
package io.github.x80070006.holowave.tube

import org.json.JSONObject

/** What the injected scripts tell the app. Pure data so the parsing can be tested without a device. */
sealed interface TubeEvent {
    /** A song was loaded or its position was saved (the script sends it every few seconds). */
    data class Progress(
        val videoId: String, val title: String, val author: String, val thumbnail: String,
        val currentSec: Double, val durationSec: Double,
    ) : TubeEvent

    data class PlayState(val playing: Boolean) : TubeEvent

    /** The `<video>` element of the player reached its end. */
    data object Ended : TubeEvent

    data class Loaded(val url: String) : TubeEvent

    /** Part of the account's most played songs, already ranked. */
    data class Library(val songs: List<LibrarySong>) : TubeEvent

    data class LibraryDone(val count: Int, val error: String?) : TubeEvent
}

data class LibrarySong(
    val videoId: String, val title: String, val artist: String, val album: String,
    val durationSec: Int, val thumbnail: String, val plays: Int, val rank: Int,
)

object TubeMessages {
    private val videoId = Regex("^[A-Za-z0-9_-]{6,20}$")

    /** Null for anything unknown or malformed: the page is untrusted input. */
    fun parse(payload: String): TubeEvent? {
        if (payload.length > 200_000) return null
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        if (payload.length > 20_000 && json.optString("type") != "hw-library") return null
        val data = json.optJSONObject("data")
        return when (json.optString("type")) {
            "progress" -> {
                data ?: return null
                val id = data.optString("videoId")
                if (!videoId.matches(id)) return null
                TubeEvent.Progress(
                    id,
                    data.optString("title").take(300),
                    data.optString("author").take(200),
                    data.optString("thumbnail").take(500),
                    data.optDouble("current", 0.0).coerceAtLeast(0.0),
                    data.optDouble("duration", 0.0).coerceAtLeast(0.0),
                )
            }
            "play-state" -> TubeEvent.PlayState(data?.optBoolean("playing", false) == true)
            "hw-ended" -> TubeEvent.Ended
            "onload" -> TubeEvent.Loaded("")
            "hw-library" -> {
                val arr = data?.optJSONArray("items") ?: return null
                val songs = (0 until minOf(arr.length(), 100)).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val id = o.optString("videoId")
                    val title = o.optString("title").take(300)
                    if (!videoId.matches(id) || title.isBlank()) return@mapNotNull null
                    LibrarySong(
                        id, title, o.optString("artist").take(200), o.optString("album").take(300),
                        o.optInt("duration", 0).coerceIn(0, 86_400), o.optString("thumbnail").take(500),
                        o.optInt("plays", 1).coerceIn(1, 10_000), o.optInt("rank", 0).coerceIn(0, 10_000),
                    )
                }
                TubeEvent.Library(songs)
            }
            "hw-library-done" -> TubeEvent.LibraryDone(data?.optInt("count", 0) ?: 0, data?.optString("error")?.takeIf { it.isNotBlank() }?.take(80))
            else -> null
        }
    }

    /** Only YouTube's own image hosts may be fetched for a cover. */
    fun isCoverUrl(url: String): Boolean {
        val host = runCatching { java.net.URI(url) }.getOrNull()?.takeIf { it.scheme == "https" }?.host?.lowercase() ?: return false
        return listOf("ytimg.com", "googleusercontent.com", "ggpht.com").any { host == it || host.endsWith(".$it") }
    }

    /** The video id of a `watch?v=` url, or null. */
    fun videoIdOf(url: String): String? {
        val q = runCatching { java.net.URI(url).rawQuery }.getOrNull() ?: return null
        return q.split('&').firstOrNull { it.startsWith("v=") }?.substring(2)?.takeIf { videoId.matches(it) }
    }
}
