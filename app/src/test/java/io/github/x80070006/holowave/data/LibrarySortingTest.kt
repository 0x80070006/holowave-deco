package io.github.x80070006.holowave.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySortingTest {

    private fun t(id: Long, title: String, artist: String = "A", album: String = "Al", added: Long = 0, no: Int = 0) =
        Track(id, title, artist, album, 1, 180_000, no, 2020, "f", "Music/", "external_primary", added)

    private fun album(title: String, artist: String, vararg tracks: Track) =
        TrackCollection(CollectionKind.ALBUM, title, title, artist, tracks.toList())

    @Test fun alphabeticalIgnoresAccentsAndCase() {
        val sorted = LibrarySorting.tracks(listOf(t(1, "zèbre"), t(2, "Éclair"), t(3, "arbre"), t(4, "Ecume")), LibrarySort.AZ) { null }
        assertEquals(listOf("arbre", "Éclair", "Ecume", "zèbre"), sorted.map { it.title })
    }

    @Test fun artistSortKeepsAlbumsTogetherInTrackOrder() {
        val sorted = LibrarySorting.tracks(
            listOf(t(1, "b", "Zed", "X", no = 2), t(2, "a", "Abe", "Y", no = 1), t(3, "c", "Zed", "X", no = 1)),
            LibrarySort.ARTIST,
        ) { null }
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test fun addedSortPutsNewestFirst() {
        val sorted = LibrarySorting.tracks(listOf(t(1, "old", added = 10), t(2, "new", added = 99), t(3, "mid", added = 50)), LibrarySort.ADDED) { null }
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test fun recentSortUsesListeningRankThenAlphabet() {
        val ranks = mapOf(3L to 0, 1L to 1)
        val sorted = LibrarySorting.tracks(listOf(t(1, "b"), t(2, "a"), t(3, "c"), t(4, "d")), LibrarySort.RECENT) { ranks[it.id] }
        assertEquals(listOf(3L, 1L, 2L, 4L), sorted.map { it.id })
    }

    @Test fun albumsSortByTheirArtistOrBestRank() {
        val a = album("Zeta", "Abe", t(1, "x"))
        val b = album("Alpha", "Zed", t(2, "y"))
        assertEquals(listOf("Zeta", "Alpha"), LibrarySorting.collections(listOf(b, a), LibrarySort.ARTIST) { null }.map { it.title })
        assertEquals(listOf("Alpha", "Zeta"), LibrarySorting.collections(listOf(a, b), LibrarySort.AZ) { null }.map { it.title })
        val ranked = LibrarySorting.collections(listOf(a, b), LibrarySort.RECENT) { if (it.id == 2L) 0 else null }
        assertEquals("Alpha", ranked.first().title)
    }

    @Test fun mixedListInterleavesLocalAndStreamed() {
        val local = MixedItem.Local(t(1, "Beta", added = 100))
        val streamed = MixedItem.Stream(StreamedTrack("k", "Alpha", "Nova", "", 0, null, null, 5_000, 5_000, 1))
        val byTitle = LibrarySorting.mixed(listOf(local, streamed), LibrarySort.AZ, { null }, { null })
        assertTrue(byTitle.first() is MixedItem.Stream)
        val recent = LibrarySorting.mixed(listOf(local, streamed), LibrarySort.RECENT, { null }, { 0 })
        assertTrue("the streamed one was heard most recently", recent.first() is MixedItem.Stream)
    }

    @Test fun filterMatchesAnyFieldWithoutAccents() {
        assertTrue(LibrarySorting.matches("ecl", "Éclair", "x"))
        assertTrue(LibrarySorting.matches("", "anything"))
        assertTrue(LibrarySorting.matches("nova", "Title", "Nova Tide"))
        assertFalse(LibrarySorting.matches("zzz", "Title", "Nova Tide"))
    }

    @Test fun streamedIdentityIgnoresCaseAndKeepsItsOwnNamespace() {
        assertEquals(StreamKey.of("Nova Tide", "Afterglow"), StreamKey.of(" nova tide ", "AFTERGLOW"))
        assertNotEquals(StreamKey.of("Nova Tide", "Afterglow"), TrackKey.of("Nova Tide", "Afterglow", "", 0))
    }

    @Test fun youtubeNoiseIsRemovedFromTitlesAndArtists() {
        assertEquals("Afterglow", StreamText.title("Afterglow (Official Audio)"))
        assertEquals("Afterglow", StreamText.title("Afterglow [Lyrics]"))
        assertEquals("Le dernier tram (live)", StreamText.title("Le dernier tram (live)"))
        assertEquals("Nova Tide", StreamText.artist("Nova Tide - Topic"))
        assertEquals("Nova Tide", StreamText.artist("  Nova Tide "))
    }
}
