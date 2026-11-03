package io.github.x80070006.holowave.tube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoversTest {
    @Test fun videoThumbnailsAskForTheLargestFirst() = assertEquals(
        listOf("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg", "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"),
        Covers.candidates("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg?sqp=abc&rs=xyz"),
    )

    @Test fun albumArtIsAskedLarger() = assertEquals(
        "https://lh3.googleusercontent.com/abc=w544-h544-l90-rj",
        Covers.candidates("https://lh3.googleusercontent.com/abc=w60-h60-l90-rj").first(),
    )

    @Test fun unknownUrlsAreKept() =
        assertEquals(listOf("https://yt3.ggpht.com/x"), Covers.candidates("https://yt3.ggpht.com/x"))

    @Test fun onlyTheSmallYtimgSizesAreLetterboxed() {
        assertTrue(Covers.isLetterboxed("https://i.ytimg.com/vi/x/hqdefault.jpg"))
        assertFalse(Covers.isLetterboxed("https://i.ytimg.com/vi/x/maxresdefault.jpg"))
        assertFalse(Covers.isLetterboxed("https://lh3.googleusercontent.com/abc=w544-h544"))
    }
}
