package io.github.x80070006.holowave.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceFoldersTest {

    private fun track(id: Long, path: String) =
        Track(id, "t$id", "a", "al", 1, 60_000, 1, 0, path.trimEnd('/').substringAfterLast('/'), path, 0)

    @Test fun emptySelectionIncludesEverything() {
        assertTrue(SourceFolders.includes("Download/", emptySet()))
    }

    @Test fun parentIncludesChildrenButNotSiblingsWithSamePrefix() {
        val sel = setOf("Music/Jazz/")
        assertTrue(SourceFolders.includes("Music/Jazz/", sel))
        assertTrue(SourceFolders.includes("Music/Jazz/Live/", sel))
        assertFalse(SourceFolders.includes("Music/Jazzy/", sel))
        assertFalse(SourceFolders.includes("Music/", sel))
    }

    @Test fun toggleSelectsDeselectsAndCollapsesChildren() {
        var sel = SourceFolders.toggle("Music/Jazz/Live/", emptySet())
        sel = SourceFolders.toggle("Podcasts/", sel)
        assertEquals(setOf("Music/Jazz/Live/", "Podcasts/"), sel)
        sel = SourceFolders.toggle("Music/", sel) // parent replaces its sub-folder
        assertEquals(setOf("Music/", "Podcasts/"), sel)
        assertEquals(sel, SourceFolders.toggle("Music/Jazz/", sel)) // covered by parent: no-op
        assertEquals(setOf("Podcasts/"), SourceFolders.toggle("Music/", sel))
    }

    @Test fun normalizeRejectsTraversalAndAddsSlash() {
        assertEquals("Music/Jazz/", SourceFolders.normalize("/Music/Jazz"))
        assertNull(SourceFolders.normalize("Music/../Android/"))
        assertNull(SourceFolders.normalize("  "))
    }

    @Test fun treeListsAncestorsWithCountsInOrder() {
        val tree = SourceFolders.tree(
            listOf(track(1, "Music/Jazz/"), track(2, "Music/Jazz/Live/"), track(3, "Music/A b/"), track(4, "Download/")),
        )
        assertEquals(listOf("Download/", "Music/", "Music/A b/", "Music/Jazz/", "Music/Jazz/Live/"), tree.map { it.path })
        assertEquals(3, tree.first { it.path == "Music/" }.trackCount)
        assertEquals(2, tree.first { it.path == "Music/Jazz/Live/" }.depth)
    }
}
