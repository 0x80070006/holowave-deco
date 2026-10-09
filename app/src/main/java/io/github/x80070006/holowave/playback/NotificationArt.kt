package io.github.x80070006.holowave.playback

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import io.github.x80070006.holowave.data.ArtworkLoader

/**
 * Artwork handed to the system media controls (notification shade and lock screen):
 * the album sleeve with the record sliding out, over a backdrop tinted with the
 * album's colour, so the system player picks up the same look as the app.
 */
object NotificationArt {
    private const val SIZE = 1024
    private val GOLD = Color.rgb(216, 178, 110)
    private val INK = Color.rgb(10, 12, 12)

    fun render(cover: Bitmap?): Bitmap {
        val out = createBitmap(SIZE, SIZE)
        val c = Canvas(out)
        val accent = cover?.let { ArtworkLoader.accentOf(it) } ?: Color.rgb(21, 67, 63)
        val deep = ColorUtils.blendARGB(accent, INK, 0.55f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        paint.shader = LinearGradient(0f, 0f, 0f, SIZE.toFloat(), deep, INK, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)
        paint.shader = null

        // Sunburst behind the sleeve.
        paint.color = ColorUtils.setAlphaComponent(GOLD, 30)
        paint.strokeWidth = 3f
        val ox = SIZE * 0.5f
        val oy = SIZE * 0.5f
        for (i in 0 until 36) {
            val a = Math.toRadians(i * 10.0)
            c.drawLine(ox, oy, ox + (Math.cos(a) * SIZE).toFloat(), oy + (Math.sin(a) * SIZE).toFloat(), paint)
        }

        // System players crop the artwork to a wide band: keep everything in the middle third.
        val sleeve = SIZE * 0.44f
        val top = (SIZE - sleeve) / 2f
        val left = SIZE * 0.16f
        val r = sleeve * 0.47f
        val cx = left + sleeve * 0.5f + sleeve * 0.42f
        val cy = top + sleeve / 2f
        drawRecord(c, cover, cx, cy, r, paint)

        // Sleeve: the cover with a brass double frame.
        val rect = RectF(left, top, left + sleeve, top + sleeve)
        paint.color = Color.BLACK
        paint.setShadowLayer(28f, 6f, 10f, Color.argb(160, 0, 0, 0))
        c.drawRect(rect, paint)
        paint.clearShadowLayer()
        if (cover != null) {
            c.drawBitmap(cover, null, rect, paint)
        } else {
            paint.color = ColorUtils.blendARGB(accent, INK, 0.3f)
            c.drawRect(rect, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.color = GOLD
        paint.strokeWidth = 4f
        c.drawRect(rect, paint)
        paint.strokeWidth = 2f
        paint.alpha = 140
        c.drawRect(RectF(rect.left + 14, rect.top + 14, rect.right - 14, rect.bottom - 14), paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        return out
    }

    private fun drawRecord(c: Canvas, cover: Bitmap?, cx: Float, cy: Float, r: Float, paint: Paint) {
        paint.shader = RadialGradient(cx, cy, r, intArrayOf(Color.rgb(28, 28, 28), Color.rgb(8, 8, 8)), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        var gr = r * 0.38f
        var i = 0
        while (gr < r * 0.96f) {
            paint.color = if (i % 3 == 0) Color.argb(26, 255, 255, 255) else Color.argb(12, 255, 255, 255)
            paint.strokeWidth = 1.5f
            c.drawCircle(cx, cy, gr, paint)
            gr += r * 0.012f
            i++
        }
        paint.style = Paint.Style.FILL
        paint.shader = SweepGradient(
            cx, cy,
            intArrayOf(Color.TRANSPARENT, Color.argb(34, 255, 255, 255), Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(24, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.08f, 0.16f, 0.5f, 0.58f, 0.66f),
        )
        c.drawCircle(cx, cy, r * 0.97f, paint)
        paint.shader = null

        val lr = r * 0.36f
        if (cover != null) {
            val shader = BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val scale = (lr * 2) / minOf(cover.width, cover.height)
            shader.setLocalMatrix(Matrix().apply {
                setScale(scale, scale)
                postTranslate(cx - cover.width * scale / 2, cy - cover.height * scale / 2)
            })
            paint.shader = shader
            c.drawCircle(cx, cy, lr, paint)
            paint.shader = null
        } else {
            paint.color = GOLD
            c.drawCircle(cx, cy, lr, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.color = GOLD
        paint.strokeWidth = 4f
        c.drawCircle(cx, cy, lr, paint)
        paint.style = Paint.Style.FILL
        paint.color = INK
        c.drawCircle(cx, cy, r * 0.03f, paint)
    }
}
