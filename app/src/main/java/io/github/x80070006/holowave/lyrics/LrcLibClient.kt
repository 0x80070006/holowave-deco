package io.github.x80070006.holowave.lyrics

import io.github.x80070006.holowave.BuildConfig
import io.github.x80070006.holowave.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * Client for LRCLIB (https://lrclib.net), a free and open lyrics database
 * that needs no account and no API key.
 *
 * Only the title, artist, album and duration of the track are sent, over HTTPS,
 * to a single hard-coded host. Responses are size-capped before parsing.
 */
class LrcLibClient {

    sealed interface Result {
        data class Found(val synced: String?, val plain: String?, val instrumental: Boolean) : Result
        data object NotFound : Result
        data object Unavailable : Result
    }

    suspend fun lookup(track: Track): Result = withContext(Dispatchers.IO) {
        try {
            lookupBlocking(track)
        } catch (_: org.json.JSONException) {
            Result.Unavailable
        }
    }

    private fun lookupBlocking(track: Track): Result {
        val exact = get(
            "/api/get",
            "artist_name" to track.artist,
            "track_name" to track.title,
            "album_name" to track.album,
            "duration" to (track.durationMs / 1000).toString(),
        )
        return when (exact) {
            is Response.Ok -> parseRecord(JSONObject(exact.body))
            Response.NotFound -> search(track)
            Response.Failed -> Result.Unavailable
        }
    }

    /** Fallback when tags differ slightly: pick the closest duration among search hits. */
    private fun search(track: Track): Result {
        val res = get("/api/search", "track_name" to track.title, "artist_name" to track.artist)
        if (res !is Response.Ok) return if (res == Response.NotFound) Result.NotFound else Result.Unavailable
        val hits = JSONArray(res.body)
        val targetSec = track.durationMs / 1000.0
        var best: JSONObject? = null
        var bestDelta = Double.MAX_VALUE
        for (i in 0 until minOf(hits.length(), 20)) {
            val hit = hits.optJSONObject(i) ?: continue
            val delta = kotlin.math.abs(hit.optDouble("duration", Double.NaN) - targetSec)
            if (delta.isNaN() || delta > 4.0) continue
            if (delta < bestDelta) {
                bestDelta = delta
                best = hit
            }
        }
        return best?.let(::parseRecord) ?: Result.NotFound
    }

    private fun parseRecord(o: JSONObject): Result {
        val synced = o.optString("syncedLyrics").takeIf { !o.isNull("syncedLyrics") && it.isNotBlank() }
        val plain = o.optString("plainLyrics").takeIf { !o.isNull("plainLyrics") && it.isNotBlank() }
        val instrumental = o.optBoolean("instrumental", false)
        if (synced == null && plain == null && !instrumental) return Result.NotFound
        return Result.Found(synced, plain, instrumental)
    }

    private sealed interface Response {
        data class Ok(val body: String) : Response
        data object NotFound : Response
        data object Failed : Response
    }

    private fun get(path: String, vararg params: Pair<String, String>): Response {
        val query = params.joinToString("&") { (k, v) -> "$k=" + URLEncoder.encode(v.take(300), "UTF-8") }
        val connection = try {
            URL("https", HOST, "$path?$query").openConnection() as HttpsURLConnection
        } catch (_: IOException) {
            return Response.Failed
        }
        return try {
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> readCapped(connection)?.let { Response.Ok(it) } ?: Response.Failed
                HttpURLConnection.HTTP_NOT_FOUND -> Response.NotFound
                else -> Response.Failed
            }
        } catch (_: IOException) {
            Response.Failed
        } catch (_: SecurityException) {
            Response.Failed
        } finally {
            connection.disconnect()
        }
    }

    private fun readCapped(connection: HttpURLConnection): String? {
        val declared = connection.contentLengthLong
        if (declared > MAX_RESPONSE_BYTES) return null
        connection.inputStream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (out.size() + n > MAX_RESPONSE_BYTES) return null
                out.write(buffer, 0, n)
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }

    private companion object {
        const val HOST = "lrclib.net"
        const val MAX_RESPONSE_BYTES = 1024 * 1024
        val USER_AGENT = "HOLOWAVE/${BuildConfig.VERSION_NAME} (https://github.com/0x80070006/holowave-deco)"
    }
}
