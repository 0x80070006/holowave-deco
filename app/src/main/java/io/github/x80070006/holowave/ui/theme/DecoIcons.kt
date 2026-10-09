package io.github.x80070006.holowave.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Hand-drawn line icons on a 24-unit grid, thin strokes to match the brass hairlines. */
object DecoIcons {
    private fun icon(name: String, stroke: String? = null, fill: String? = null, width: Float = 1.6f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        if (fill != null) b.addPath(PathParser().parsePathString(fill).toNodes(), fill = SolidColor(Color.White))
        if (stroke != null) b.addPath(
            PathParser().parsePathString(stroke).toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        return b.build()
    }

    val Play = icon("play", fill = "M8 5.2v13.6c0 .6.6.9 1.1.6l10.4-6.8c.5-.3.5-.9 0-1.2L9.1 4.6C8.6 4.3 8 4.6 8 5.2z")
    val Pause = icon("pause", fill = "M6.5 5h3.6v14H6.5zM13.9 5h3.6v14h-3.6z")
    val Next = icon("next", fill = "M5 6.4v11.2c0 .5.5.8.9.5l8.1-5.6c.4-.3.4-.8 0-1L5.9 5.9C5.5 5.6 5 5.9 5 6.4zM16.2 6h2.3v12h-2.3z")
    val Previous = icon("previous", fill = "M19 6.4v11.2c0 .5-.5.8-.9.5L10 12.5c-.4-.3-.4-.8 0-1l8.1-5.6c.4-.3.9 0 .9.5zM5.5 6h2.3v12H5.5z")
    val Shuffle = icon(
        "shuffle",
        stroke = "M3.5 7h3.6c2 0 3 1 4.2 3l1.5 3c1.2 2 2.2 3 4.2 3h3.5M3.5 17h3.6c1.4 0 2.3-.5 3.1-1.5M13.8 8.5c.8-1 1.7-1.5 3.1-1.5h3.6M18 4.5 20.5 7 18 9.5M18 14.5l2.5 2.5-2.5 2.5",
    )
    val Repeat = icon(
        "repeat",
        stroke = "M4.5 11.5V10a3 3 0 0 1 3-3h12M17 4.5 19.5 7 17 9.5M19.5 12.5V14a3 3 0 0 1-3 3h-12M7 19.5 4.5 17 7 14.5",
    )
    val Microphone = icon(
        "lyrics",
        stroke = "M12 3.5a2.8 2.8 0 0 1 2.8 2.8v5.2a2.8 2.8 0 0 1-5.6 0V6.3A2.8 2.8 0 0 1 12 3.5zM9.2 7.5h1.6M13.2 7.5h1.6M9.2 10h1.6M13.2 10h1.6M6.2 11.5a5.8 5.8 0 0 0 11.6 0M12 17.3v3.2M8.5 20.5h7",
    )
    val Search = icon("search", stroke = "M10.5 4a6.5 6.5 0 1 1 0 13 6.5 6.5 0 0 1 0-13zM15.4 15.4 20 20")
    val Back = icon("back", stroke = "M15 4.5 7.5 12l7.5 7.5")
    val ChevronDown = icon("down", stroke = "M5.5 9 12 15.5 18.5 9")
    val More = icon("more", fill = "M12 4.6a1.6 1.6 0 1 1 0 3.2 1.6 1.6 0 0 1 0-3.2zM12 10.4a1.6 1.6 0 1 1 0 3.2 1.6 1.6 0 0 1 0-3.2zM12 16.2a1.6 1.6 0 1 1 0 3.2 1.6 1.6 0 0 1 0-3.2z")
    val Tune = icon("settings", stroke = "M4 7h9M17 7h3M15 4.8v4.4M4 17h3M11 17h9M9 14.8v4.4M4 12h11M19 12h1M17 9.8v4.4")
    val Heart = icon("heart", stroke = "M12 19.5s-7.5-4.6-7.5-10.2A4.1 4.1 0 0 1 12 7a4.1 4.1 0 0 1 7.5 2.3c0 5.6-7.5 10.2-7.5 10.2z")
    val HeartFilled = icon("heart_filled", fill = "M12 19.5s-7.5-4.6-7.5-10.2A4.1 4.1 0 0 1 12 7a4.1 4.1 0 0 1 7.5 2.3c0 5.6-7.5 10.2-7.5 10.2z")
    val Plus = icon("plus", stroke = "M12 5v14M5 12h14")
    val Close = icon("close", stroke = "M6.5 6.5l11 11M17.5 6.5l-11 11")
    val Queue = icon("queue", stroke = "M4 6.5h16M4 11.5h16M4 16.5h9M17 14.5v5l3.5-2.5z")
    val Import = icon("import", stroke = "M12 4v10.5M7.5 10 12 14.5 16.5 10M5 19.5h14")
    val Refresh = icon("refresh", stroke = "M19 12a7 7 0 1 1-2.1-5M19.2 4.5v3.4h-3.4")
    val Sync = icon("sync", stroke = "M12 8.2a3.8 3.8 0 1 1 0 7.6 3.8 3.8 0 0 1 0-7.6zM12 3.5v3M12 17.5v3M3.5 12h3M17.5 12h3")
    val Trash = icon("trash", stroke = "M5 7h14M9.5 7V4.8h5V7M7 7l.8 12.2h8.4L17 7M10.3 10.5v5.5M13.7 10.5v5.5")
    val Edit = icon("edit", stroke = "M5 19l1-4 9.5-9.5 3 3L9 18zM13.5 7.5l3 3")
    val Disc = icon("disc", stroke = "M12 3.5a8.5 8.5 0 1 1 0 17 8.5 8.5 0 0 1 0-17zM12 9.5a2.5 2.5 0 1 1 0 5 2.5 2.5 0 0 1 0-5z")
}
