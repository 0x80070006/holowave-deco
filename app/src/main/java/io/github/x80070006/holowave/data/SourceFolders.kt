package io.github.x80070006.holowave.data

/** A directory holding music, as seen by MediaStore. [key] is "volume:relative/path/". */
data class FolderNode(val key: String, val name: String, val depth: Int, val trackCount: Int) {
    val volume: String get() = key.substringBefore(':')
    val path: String get() = key.substringAfter(':')
}

/**
 * Source-folder selection. A folder key is "<MediaStore volume>:<relative path>/",
 * e.g. "external_primary:Music/Jazz/" or "1a2b-3c4d:Music/" for an SD card.
 * An empty selection means "every folder"; a folder includes all of its sub-folders.
 */
object SourceFolders {
    const val PRIMARY = "external_primary"
    const val MAX_SELECTED = 200
    private const val MAX_PATH = 512
    private val volumePattern = Regex("[a-z0-9_-]{1,64}")

    fun key(volume: String, relativePath: String): String = "$volume:$relativePath"

    /**
     * Validates and canonicalises a stored or picked key. Keys from version 2.1
     * had no volume part: they referred to the internal storage.
     */
    fun normalize(raw: String): String? {
        val hasVolume = ':' in raw
        val volume = if (hasVolume) raw.substringBefore(':').lowercase() else PRIMARY
        if (!volumePattern.matches(volume)) return null
        val p = (if (hasVolume) raw.substringAfter(':') else raw).trim().trimStart('/')
        if (p.isEmpty() || p.length > MAX_PATH || p.split('/').any { it == ".." || it == "." }) return null
        if (p.any { it.isISOControl() }) return null
        return key(volume, if (p.endsWith('/')) p else "$p/")
    }

    fun includes(folderKey: String, selected: Set<String>): Boolean =
        selected.isEmpty() || selected.any { folderKey.startsWith(it) }

    /** Selected through one of its ancestors (shown as checked but not toggleable). */
    fun coveredByAncestor(key: String, selected: Set<String>): Boolean =
        selected.any { it != key && key.startsWith(it) }

    /** Toggles [key]; selecting a folder drops its now-redundant sub-folders. */
    fun toggle(key: String, selected: Set<String>): Set<String> = when {
        key in selected -> selected - key
        coveredByAncestor(key, selected) -> selected
        else -> add(key, selected)
    }

    fun add(key: String, selected: Set<String>): Set<String> =
        if (coveredByAncestor(key, selected)) selected
        else (selected.filterNot { it.startsWith(key) } + key).take(MAX_SELECTED).toSet()

    /** Every directory containing music, with its ancestors, sorted as a tree (internal storage first). */
    fun tree(tracks: List<Track>): List<FolderNode> {
        val counts = HashMap<String, Int>()
        for (t in tracks) {
            val parts = t.relativePath.trimEnd('/').split('/').filter { it.isNotEmpty() }
            var prefix = ""
            for (part in parts) {
                prefix += "$part/"
                val k = key(t.volume, prefix)
                counts[k] = (counts[k] ?: 0) + 1
            }
        }
        return counts.keys
            .sortedWith(compareBy<String>({ it.substringBefore(':') != PRIMARY }, { it.lowercase() }))
            .map { k ->
                val segments = k.substringAfter(':').trimEnd('/').split('/')
                FolderNode(k, segments.last(), segments.size - 1, counts.getValue(k))
            }
    }

    /**
     * Key of a shared-storage document id from the system picker:
     * "primary:Music/Jazz" -> "external_primary:Music/Jazz/", "1A2B-3C4D:Music" -> "1a2b-3c4d:Music/".
     * A volume root has no precise folder and gives null.
     */
    fun keyOfDocumentId(docId: String): String? {
        if (':' !in docId) return null
        val volume = docId.substringBefore(':').let { if (it == "primary") PRIMARY else it.lowercase() }
        val path = docId.substringAfter(':')
        if (volume.isEmpty() || path.isBlank()) return null
        return normalize("$volume:$path")
    }

    /** Human-readable location, e.g. "Music/Jazz" or "Carte SD · Music/Jazz". */
    fun label(key: String): String {
        val path = key.substringAfter(':').trimEnd('/')
        return if (key.substringBefore(':') == PRIMARY) path else "Carte SD · $path"
    }
}
