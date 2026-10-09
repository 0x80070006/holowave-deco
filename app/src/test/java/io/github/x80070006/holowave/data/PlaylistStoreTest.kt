package io.github.x80070006.holowave.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistStoreTest {

    @Test fun roundTrips() {
        val c = Collections(listOf(Playlist("a1", "Late night", listOf(3L, 1L, 2L), 42L)), setOf(7L, 9L))
        assertEquals(c, PlaylistStore.parse(PlaylistStore.serialize(c)))
    }

    @Test fun skipsMalformedEntriesAndSanitizes() {
        val json = """{"favorites":[1,"x",2],"playlists":[{"name":"no id"},{"id":"ok","name":"  Bad\u0007Name  ","tracks":[5,5,"y",6]},42]}"""
        val c = PlaylistStore.parse(json)
        assertEquals(setOf(1L, 2L), c.favorites)
        assertEquals(1, c.playlists.size)
        assertEquals("BadName", c.playlists[0].name)
        assertEquals(listOf(5L, 6L), c.playlists[0].trackIds)
    }

    @Test fun sanitizeNameClampsLength() {
        assertEquals(PlaylistStore.MAX_NAME_LENGTH, PlaylistStore.sanitizeName("x".repeat(500)).length)
        assertEquals("Nouvelle playlist", PlaylistStore.sanitizeName("\n\t "))
    }
}
