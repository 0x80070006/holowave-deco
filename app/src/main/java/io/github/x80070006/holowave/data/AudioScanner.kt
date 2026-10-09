package io.github.x80070006.holowave.data

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

sealed interface ScanProgress {
    /** Walking the folders: the total is not known yet. */
    data class Searching(val folders: Int, val audioFiles: Int, val current: String) : ScanProgress
    /** Handing new or changed files to the system indexer. */
    data class Indexing(val done: Int, val total: Int, val audioFiles: Int) : ScanProgress
    data class Finished(val audioFiles: Int, val indexed: Int, val skippedFolders: Int) : ScanProgress
    data object Failed : ScanProgress
}

/**
 * Real scan of the storage. Finds audio files and asks Android's media scanner to
 * index those MediaStore does not know, knows only partially (never analysed),
 * or that changed since. Read-only: nothing is written, moved, copied or sent.
 *
 * Two ways to list a folder:
 *  - folders the user picked in the system picker are listed through that
 *    read-only grant, which also sees files Android hides from apps because
 *    they were never indexed;
 *  - everything else is walked with the audio permission.
 */
class AudioScanner(private val context: Context) {

    private class Walk(
        val known: Map<String, Long>,
        val onProgress: (ScanProgress) -> Unit,
    ) {
        val seenFiles = HashSet<String>()
        val seenFolders = HashSet<String>()
        val toIndex = ArrayList<String>()
        var audio = 0
        var folders = 0
        var skipped = 0
        private var lastEmit = 0L

        val full: Boolean get() = folders >= MAX_FOLDERS || audio >= MAX_FILES

        /** [relDir] ends with '/' (or is empty for a volume root). */
        fun audioFile(volume: String, relDir: String, name: String, modifiedSec: Long, absolutePath: String) {
            val id = "$volume|$relDir|$name"
            if (!seenFiles.add(id)) return
            audio++
            val knownModified = known[id]
            if (knownModified == null || modifiedSec > knownModified) toIndex += absolutePath
        }

        fun folderVisited(name: String) {
            folders++
            val now = System.currentTimeMillis()
            if (now - lastEmit > 120) {
                lastEmit = now
                onProgress(ScanProgress.Searching(folders, audio, name))
            }
        }
    }

    suspend fun scan(selection: Set<String>, onProgress: (ScanProgress) -> Unit): ScanProgress = withContext(Dispatchers.IO) {
        try {
            val walk = Walk(knownFiles(), onProgress)
            val volumes = volumes()
            val grants = grantedFolders()

            // Selected folders (or whole volumes), then every picked folder they include:
            // the grant sees files the plain walk cannot. Duplicates are skipped by the walk.
            val base = selection.ifEmpty { volumes.keys.map { "$it:" }.toSet() }
            val targets = base + grants.keys.filter { SourceFolders.includes(it, selection) }
            for (target in targets) {
                coroutineContext.ensureActive()
                if (walk.full) break
                val volume = target.substringBefore(':')
                val volumeDir = volumes[volume] ?: continue
                val grant = grants.entries.firstOrNull { (grantKey, _) -> target.startsWith(grantKey) }
                if (grant != null) walkTree(grant.value, target, volumeDir, walk)
                else walkFiles(volume, volumeDir, target.substringAfter(':'), walk)
            }

            onProgress(ScanProgress.Indexing(0, walk.toIndex.size, walk.audio))
            var done = 0
            for (chunk in walk.toIndex.chunked(CHUNK)) {
                coroutineContext.ensureActive()
                scanChunk(chunk) { inChunk -> onProgress(ScanProgress.Indexing(done + inChunk, walk.toIndex.size, walk.audio)) }
                done += chunk.size
                onProgress(ScanProgress.Indexing(done, walk.toIndex.size, walk.audio))
            }
            ScanProgress.Finished(walk.audio, walk.toIndex.size, walk.skipped).also(onProgress)
        } catch (e: SecurityException) {
            Log.w(TAG, "Scan not permitted", e)
            ScanProgress.Failed.also(onProgress)
        }
    }

    // --------------------------------------------------------- plain walk

    private suspend fun walkFiles(volume: String, volumeDir: File, relativeStart: String, walk: Walk) {
        val start = if (relativeStart.isEmpty()) volumeDir else File(volumeDir, relativeStart)
        val volumeRoot = runCatching { volumeDir.canonicalPath }.getOrNull() ?: return
        // Defence in depth: keys are validated, but never walk outside the volume.
        val startCanonical = runCatching { start.canonicalPath }.getOrNull() ?: return
        if (startCanonical != volumeRoot && !startCanonical.startsWith("$volumeRoot/")) return

        val queue = ArrayDeque<Pair<File, Int>>()
        queue.addLast(start to 0)
        while (queue.isNotEmpty() && !walk.full) {
            coroutineContext.ensureActive()
            val (dir, depth) = queue.removeFirst()
            val canonical = runCatching { dir.canonicalPath }.getOrNull() ?: continue
            if (!walk.seenFolders.add("$volume|$canonical")) continue
            val children = dir.listFiles()
            if (children == null) {
                walk.skipped++
                continue
            }
            walk.folderVisited(dir.name)
            if (children.any { it.name == ".nomedia" }) continue
            val relDir = dir.absolutePath.removePrefix(volumeDir.absolutePath).trim('/').let { if (it.isEmpty()) "" else "$it/" }
            for (f in children) {
                val name = f.name
                if (name.startsWith('.')) continue
                if (f.isDirectory) {
                    if (depth < MAX_DEPTH && !(relDir.isEmpty() && name == "Android")) queue.addLast(f to depth + 1)
                } else if (isAudio(name, null)) {
                    walk.audioFile(volume, relDir, name, f.lastModified() / 1000, f.absolutePath)
                }
            }
        }
    }

    // ------------------------------------------------- picked-folder walk

    /** Lists [target] ("volume:path/") through the read-only grant [treeUri] the user gave in the picker. */
    private suspend fun walkTree(treeUri: Uri, target: String, volumeDir: File, walk: Walk) {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        // Document ids look like "primary:Music/Jazz" or "1A2B-3C4D:Music": keep the provider's own volume spelling.
        val providerVolume = treeDocId.substringBefore(':')
        val volume = target.substringBefore(':')
        val startDocId = "$providerVolume:" + target.substringAfter(':').trimEnd('/')

        val queue = ArrayDeque<Pair<String, Int>>()
        queue.addLast(startDocId to 0)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        while (queue.isNotEmpty() && !walk.full) {
            coroutineContext.ensureActive()
            val (docId, depth) = queue.removeFirst()
            if (!walk.seenFolders.add("tree|$docId")) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val entries = ArrayList<Array<String?>>()
            val modified = ArrayList<Long>()
            val ok = runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        entries += arrayOf(c.getString(0), c.getString(1), c.getString(2))
                        modified += c.getLong(3)
                    }
                } != null
            }.getOrDefault(false)
            if (!ok) {
                walk.skipped++
                continue
            }
            walk.folderVisited(docId.substringAfterLast('/').substringAfter(':'))
            if (entries.any { it[1] == ".nomedia" }) continue
            val relPath = docId.substringAfter(':').trim('/')
            val relDir = if (relPath.isEmpty()) "" else "$relPath/"
            entries.forEachIndexed { i, (childId, name, mime) ->
                if (childId == null || name == null || name.startsWith('.')) return@forEachIndexed
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (depth < MAX_DEPTH) queue.addLast(childId to depth + 1)
                } else if (isAudio(name, mime)) {
                    walk.audioFile(volume, relDir, name, modified[i] / 1000, File(volumeDir, relDir + name).absolutePath)
                }
            }
        }
    }

    /** Folders the user granted through the picker: folder key -> tree URI. */
    private fun grantedFolders(): Map<String, Uri> =
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.uri.authority == EXTERNAL_STORAGE_AUTHORITY }
            .mapNotNull { p -> keyOfTree(p.uri)?.let { it to p.uri } }
            .toMap()

    // ----------------------------------------------------------- indexing

    /** Asks the platform scanner to index [paths]; resumes when all callbacks arrived or after a timeout. */
    private suspend fun scanChunk(paths: List<String>, onEach: (Int) -> Unit) {
        val counter = AtomicInteger(0)
        withTimeoutOrNull(CHUNK_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
                    val n = counter.incrementAndGet()
                    onEach(n)
                    if (n == paths.size && cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    /**
     * "volume|relative/path/|name" -> DATE_MODIFIED for audio files MediaStore has fully analysed.
     * Rows without a duration were registered but never analysed: they are left out so they get re-indexed.
     */
    private fun knownFiles(): Map<String, Long> {
        val out = HashMap<String, Long>()
        val projection = arrayOf(
            MediaStore.Audio.Media.VOLUME_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
        )
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.isNull(4) || c.getLong(4) <= 0) continue
                out["${c.getString(0)}|${c.getString(1).orEmpty()}|${c.getString(2)}"] = c.getLong(3)
            }
        }
        return out
    }

    /** Mounted shared-storage volumes: MediaStore volume name -> root directory. */
    private fun volumes(): Map<String, File> {
        val result = LinkedHashMap<String, File>()
        if (Build.VERSION.SDK_INT >= 30) {
            context.getSystemService(StorageManager::class.java)?.storageVolumes?.forEach { v ->
                val dir = v.directory ?: return@forEach
                val name = v.mediaStoreVolumeName ?: return@forEach
                if (v.state == Environment.MEDIA_MOUNTED || v.state == Environment.MEDIA_MOUNTED_READ_ONLY) result[name] = dir
            }
        }
        if (result.isEmpty()) {
            @Suppress("DEPRECATION")
            result[SourceFolders.PRIMARY] = Environment.getExternalStorageDirectory()
        }
        return result
    }

    companion object {
        private const val TAG = "AudioScanner"
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        private const val MAX_DEPTH = 16
        private const val MAX_FOLDERS = 30_000
        private const val MAX_FILES = 100_000
        private const val CHUNK = 64
        private const val CHUNK_TIMEOUT_MS = 60_000L
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "m4a", "m4b", "aac", "ogg", "oga", "opus", "wav", "aif", "aiff", "mka", "wma", "alac", "ape", "amr",
        )

        fun isAudio(name: String, mime: String?): Boolean =
            mime?.startsWith("audio/") == true || name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

        /** Folder key of a tree picked in the system picker, or null if it is not a precise shared-storage folder. */
        fun keyOfTree(treeUri: Uri): String? {
            if (treeUri.authority != EXTERNAL_STORAGE_AUTHORITY) return null
            val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
            return SourceFolders.keyOfDocumentId(docId)
        }
    }
}
