package io.github.x80070006.holowave.data

/** A directory holding music, as seen by MediaStore (relative to the storage volume root). */
data class FolderNode(val path: String, val name: String, val depth: Int, val trackCount: Int)

/**
 * Source-folder selection. Paths are MediaStore relative paths ending with '/',
 * e.g. "Music/Jazz/". An empty selection means "every folder".
 * Selecting a folder includes all of its sub-folders.
 */
object SourceFolders {
    const val MAX_SELECTED = 200
    private const val MAX_PATH = 512

    fun normalize(raw: String): String? {
        val p = raw.trim().trimStart('/')
        if (p.isEmpty() || p.length > MAX_PATH || p.split('/').any { it == ".." }) return null
        return if (p.endsWith('/')) p else "$p/"
    }

    fun includes(trackPath: String, selected: Set<String>): Boolean =
        selected.isEmpty() || selected.any { trackPath.startsWith(it) }

    /** Selected through one of its ancestors (shown as checked but not toggleable). */
    fun coveredByAncestor(path: String, selected: Set<String>): Boolean =
        selected.any { it != path && path.startsWith(it) }

    /** Toggles [path]; selecting a folder drops its now-redundant sub-folders. */
    fun toggle(path: String, selected: Set<String>): Set<String> = when {
        path in selected -> selected - path
        coveredByAncestor(path, selected) -> selected
        else -> (selected.filterNot { it.startsWith(path) } + path).take(MAX_SELECTED).toSet()
    }

    /** Every directory containing music, with its ancestors, sorted as a tree. */
    fun tree(tracks: List<Track>): List<FolderNode> {
        val counts = HashMap<String, Int>()
        for (t in tracks) {
            val parts = t.relativePath.trimEnd('/').split('/').filter { it.isNotEmpty() }
            var prefix = ""
            for (part in parts) {
                prefix += "$part/"
                counts[prefix] = (counts[prefix] ?: 0) + 1
            }
        }
        return counts.keys
            .sortedWith(compareBy<String> { it.lowercase() })
            .map { p ->
                val segments = p.trimEnd('/').split('/')
                FolderNode(p, segments.last(), segments.size - 1, counts.getValue(p))
            }
    }
}
