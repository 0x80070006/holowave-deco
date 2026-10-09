package io.github.x80070006.holowave.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/** A decoded cover plus the accent colour pulled from it, used to tint the player. */
class Artwork(val bitmap: Bitmap, val accent: Int)

/**
 * Loads embedded cover art through the platform thumbnail API, which decodes
 * inside MediaProvider and never hands raw file paths to the app.
 */
class ArtworkLoader(private val resolver: ContentResolver) {

    private val cache = object : LruCache<String, Artwork>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Artwork) = value.bitmap.allocationByteCount
    }
    private val missing = LruCache<String, Boolean>(2_000)
    private val decodePermits = Semaphore(3)

    fun cached(uri: Uri, size: Int): Artwork? = cache.get(key(uri, size))

    suspend fun load(uri: Uri, size: Int): Artwork? {
        val key = key(uri, size)
        cache.get(key)?.let { return it }
        if (missing.get(key) == true) return null
        return withContext(Dispatchers.IO) {
            decodePermits.withPermit {
                val bitmap = try {
                    resolver.loadThumbnail(uri, Size(size, size), null)
                } catch (_: FileNotFoundException) {
                    null
                } catch (_: Exception) {
                    null
                }
                if (bitmap == null) {
                    missing.put(key, true)
                    null
                } else {
                    Artwork(bitmap, accentOf(bitmap)).also { cache.put(key, it) }
                }
            }
        }
    }

    fun clear() {
        cache.evictAll()
        missing.evictAll()
    }

    private fun key(uri: Uri, size: Int) = "$uri@$size"

    companion object {
        private const val MAX_CACHE_BYTES = 24 * 1024 * 1024

        /** Picks the most colourful of a few averaged regions; falls back to a dark teal. */
        fun accentOf(bitmap: Bitmap): Int {
            val small = runCatching { bitmap.scale(6, 6) }.getOrNull() ?: return DEFAULT_ACCENT
            var best = DEFAULT_ACCENT
            var bestScore = -1f
            val hsv = FloatArray(3)
            for (y in 0 until small.height) for (x in 0 until small.width) {
                val c = small.getPixel(x, y)
                Color.colorToHSV(c, hsv)
                val score = hsv[1] * (1f - kotlin.math.abs(hsv[2] - 0.55f))
                if (score > bestScore) {
                    bestScore = score
                    best = c
                }
            }
            if (small !== bitmap) small.recycle()
            return best
        }

        private const val DEFAULT_ACCENT = 0xFF1E5A57.toInt()
    }
}
