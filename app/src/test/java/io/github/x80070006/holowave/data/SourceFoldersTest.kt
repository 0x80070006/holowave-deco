package io.github.x80070006.holowave.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceFoldersTest {

    private val P = SourceFolders.PRIMARY

    private fun track(id: Long, path: String, volume: String = P) =
        Track(id, "t$id", "a", "al", 1, 60_000, 1, 0, path.trimEnd('/').substringAfterLast('/'), path, volume, 0)

    @Test fun emptySelectionIncludesEverything() {
        assertTrue(SourceFolders.includes("$P:Download/", emptySet()))
    }

    @Test fun parentIncludesChildrenButNotSiblingsOrOtherVolumes() {
        val sel = setOf("$P:Music/Jazz/")
        assertTrue(SourceFolders.includes("$P:Music/Jazz/", sel))
        assertTrue(SourceFolders.includes("$P:Music/Jazz/Live/", sel))
        assertFalse(SourceFolders.includes("$P:Music/Jazzy/", sel))
        assertFalse(SourceFolders.includes("$P:Music/", sel))
        assertFalse(SourceFolders.includes("1a2b-3c4d:Music/Jazz/", sel))
    }

    @Test fun toggleSelectsDeselectsAndCollapsesChildren() {
        var sel = SourceFolders.toggle("$P:Music/Jazz/Live/", emptySet())
        sel = SourceFolders.toggle("$P:Podcasts/", sel)
        assertEquals(setOf("$P:Music/Jazz/Live/", "$P:Podcasts/"), sel)
        sel = SourceFolders.toggle("$P:Music/", sel) // parent replaces its sub-folder
        assertEquals(setOf("$P:Music/", "$P:Podcasts/"), sel)
        assertEquals(sel, SourceFolders.toggle("$P:Music/Jazz/", sel)) // covered by parent: no-op
        assertEquals(setOf("$P:Podcasts/"), SourceFolders.toggle("$P:Music/", sel))
    }

    @Test fun normalizeMigratesLegacyKeysAndRejectsBadInput() {
        assertEquals("$P:Music/Jazz/", SourceFolders.normalize("Music/Jazz")) // v2.1 format
        assertEquals("1a2b-3c4d:Music/", SourceFolders.normalize("1A2B-3C4D:/Music"))
        assertNull(SourceFolders.normalize("$P:Music/../Android/"))
        assertNull(SourceFolders.normalize("$P:"))
        assertNull(SourceFolders.normalize("bad volume:Music"))
        assertNull(SourceFolders.normalize("$P:Mu\u0000sic"))
    }

    @Test fun treeListsAncestorsWithCountsInternalStorageFirst() {
        val tree = SourceFolders.tree(
            listOf(track(1, "Music/Jazz/"), track(2, "Music/Jazz/Live/"), track(3, "Download/"), track(4, "Music/", "1a2b-3c4d")),
        )
        assertEquals(
            listOf("$P:Download/", "$P:Music/", "$P:Music/Jazz/", "$P:Music/Jazz/Live/", "1a2b-3c4d:Music/"),
            tree.map { it.key },
        )
        assertEquals(2, tree.first { it.key == "$P:Music/" }.trackCount)
        assertEquals(2, tree.first { it.key == "$P:Music/Jazz/Live/" }.depth)
        assertEquals("Carte SD · Music", SourceFolders.label("1a2b-3c4d:Music/"))
    }

    @Test fun documentIdsFromThePickerBecomeKeys() {
        assertEquals("$P:Music/Cave/", SourceFolders.keyOfDocumentId("primary:Music/Cave"))
        assertEquals("1a2b-3c4d:Music/", SourceFolders.keyOfDocumentId("1A2B-3C4D:Music"))
        assertNull(SourceFolders.keyOfDocumentId("primary:"))
        assertNull(SourceFolders.keyOfDocumentId("downloads"))
    }
}
