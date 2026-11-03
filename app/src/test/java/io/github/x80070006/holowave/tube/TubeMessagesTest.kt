package io.github.x80070006.holowave.tube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TubeMessagesTest {
    @Test fun parsesProgress() {
        val e = TubeMessages.parse(
            """{"type":"progress","data":{"videoId":"dQw4w9WgXcQ","title":"T","author":"A","thumbnail":"https://i.ytimg.com/x.jpg","current":12.5,"duration":212}}""",
        ) as TubeEvent.Progress
        assertEquals("dQw4w9WgXcQ", e.videoId); assertEquals(12.5, e.currentSec, 0.0); assertEquals(212.0, e.durationSec, 0.0)
    }

    @Test fun rejectsBadVideoId() =
        assertNull(TubeMessages.parse("""{"type":"progress","data":{"videoId":"../etc","title":"T"}}"""))

    @Test fun parsesPlayStateAndEnd() {
        assertEquals(TubeEvent.PlayState(true), TubeMessages.parse("""{"type":"play-state","data":{"playing":true}}"""))
        assertEquals(TubeEvent.Ended, TubeMessages.parse("""{"type":"hw-ended"}"""))
    }

    @Test fun ignoresUnknownMalformedAndHuge() {
        assertNull(TubeMessages.parse("""{"type":"whatever"}"""))
        assertNull(TubeMessages.parse("not json"))
        assertNull(TubeMessages.parse("""{"type":"play-state","pad":"${"x".repeat(30_000)}"}"""))
    }

    @Test fun clampsNegativeTimes() {
        val e = TubeMessages.parse("""{"type":"progress","data":{"videoId":"abcdefghijk","current":-4,"duration":-1}}""") as TubeEvent.Progress
        assertEquals(0.0, e.currentSec, 0.0); assertEquals(0.0, e.durationSec, 0.0)
    }

    @Test fun parsesALibraryBatchAndDropsBadRows() {
        val e = TubeMessages.parse(
            """{"type":"hw-library","data":{"items":[
              {"videoId":"dQw4w9WgXcQ","title":"T","artist":"A","album":"B","duration":212,"thumbnail":"https://x","plays":3,"rank":0},
              {"videoId":"bad id","title":"X"},
              {"videoId":"abcdefghijk","title":""}
            ]}}""",
        ) as TubeEvent.Library
        assertEquals(1, e.songs.size)
        assertEquals(3, e.songs[0].plays); assertEquals(212, e.songs[0].durationSec)
    }

    @Test fun parsesLibraryDone() {
        assertEquals(TubeEvent.LibraryDone(100, null), TubeMessages.parse("""{"type":"hw-library-done","data":{"count":100}}"""))
        assertEquals(TubeEvent.LibraryDone(0, "signed-out"), TubeMessages.parse("""{"type":"hw-library-done","data":{"count":0,"error":"signed-out"}}"""))
    }

    @Test fun coverUrlsMustBeYouTubeImageHostsOverHttps() {
        assertTrue(TubeMessages.isCoverUrl("https://i.ytimg.com/vi/x/hq.jpg"))
        assertTrue(TubeMessages.isCoverUrl("https://lh3.googleusercontent.com/abc"))
        assertFalse(TubeMessages.isCoverUrl("http://i.ytimg.com/x.jpg"))
        assertFalse(TubeMessages.isCoverUrl("https://evil.example.com/ytimg.com"))
        assertFalse(TubeMessages.isCoverUrl("https://ytimg.com.evil.example/x"))
    }

    @Test fun readsVideoIdFromWatchUrl() {
        assertEquals("dQw4w9WgXcQ", TubeMessages.videoIdOf("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVM"))
        assertNull(TubeMessages.videoIdOf("https://music.youtube.com/search?q=x"))
    }
}
