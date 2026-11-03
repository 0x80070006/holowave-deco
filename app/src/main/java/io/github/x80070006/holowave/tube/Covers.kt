package io.github.x80070006.holowave.tube

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL

/**
 * Covers of YouTube Music songs. Album art (googleusercontent) is square and only needs a larger size;
 * video thumbnails (ytimg) are 16:9, and the small ones are even letterboxed in 4:3 with black bars,
 * so they are fetched at the largest size and cropped to a centred square.
 */
object Covers {
    private val sizeSuffix = Regex("""=w\d+-h\d+[^/?#]*$""")
    private val ytimg = Regex("""^https://i\d?\.ytimg\.com/vi(?:_webp)?/([A-Za-z0-9_-]{6,20})/[^/?#]+$""")
    private const val MAX_BYTES = 2_000_000

    /** The URLs to try, best first. Pure, tested. */
    fun candidates(url: String): List<String> {
        val clean = url.substringBefore('?')
        ytimg.find(clean)?.let { m ->
            val id = m.groupValues[1]
            return listOf("https://i.ytimg.com/vi/$id/maxresdefault.jpg", "https://i.ytimg.com/vi/$id/hqdefault.jpg")
        }
        if (sizeSuffix.containsMatchIn(url)) return listOf(url.replace(sizeSuffix, "=w544-h544-l90-rj"), url)
        return listOf(url)
    }

    /** True for the small ytimg sizes, which carry black bars above and below a 16:9 picture. */
    fun isLetterboxed(url: String) = url.contains("ytimg.com") && (url.contains("/hqdefault") || url.contains("/sddefault"))

    /** Downloads the best available cover, square. Network, call off the main thread. */
    fun fetch(url: String): Bitmap? {
        if (!TubeMessages.isCoverUrl(url)) return null
        for (u in candidates(url)) {
            if (!TubeMessages.isCoverUrl(u)) continue
            val bmp = download(u) ?: continue
            // maxresdefault exists only for HD videos; YouTube answers the others with a 120x90 grey image.
            if (u.endsWith("/maxresdefault.jpg") && bmp.width < 200) continue
            return square(io.github.x80070006.holowave.data.Letterbox.trim(if (isLetterboxed(u)) square(bmp, true) else bmp), false)
        }
        return null
    }

    private fun download(u: String): Bitmap? = runCatching {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000; c.readTimeout = 8_000; c.instanceFollowRedirects = false
        if (c.responseCode != 200) return@runCatching null
        c.inputStream.use { input ->
            val bytes = input.readNBytes(MAX_BYTES)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }.getOrNull()

    /** A centred square; for letterboxed thumbnails the bars are cut first. */
    fun square(b: Bitmap, letterboxed: Boolean): Bitmap {
        var top = 0
        var h = b.height
        if (letterboxed) {
            h = (b.width * 9 / 16).coerceAtMost(b.height)
            top = (b.height - h) / 2
        }
        val side = minOf(b.width, h)
        if (side == b.width && side == h && top == 0) return b
        return Bitmap.createBitmap(b, (b.width - side) / 2, top + (h - side) / 2, side, side)
    }
}
