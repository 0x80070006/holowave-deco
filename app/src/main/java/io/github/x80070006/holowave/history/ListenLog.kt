package io.github.x80070006.holowave.history

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.JsonWriter
import io.github.x80070006.holowave.data.TrackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.Calendar

enum class OutputKind(val code: Int) { SPEAKER(0), WIRED(1), BLUETOOTH(2), OTHER(3) }

/** What is known about a track at the moment its listening session starts. */
data class ListenSubject(val trackKey: String, val title: String, val artist: String, val album: String, val source: TrackSource)

/**
 * The listening journal: everything lives in the app's private database, never leaves
 * the device, and can be exported or erased by the user at any time.
 */
class ListenLog(private val context: Context, private val db: HolowaveDb, private val scope: CoroutineScope) {

    val count: Flow<Int> = db.listens().count()

    fun recentKeys(limit: Int): Flow<List<String>> = db.listens().recentKeys(limit)

    fun record(subject: ListenSubject, listen: Listen) {
        val cal = Calendar.getInstance().apply { timeInMillis = listen.startedAt }
        val entity = ListenEntity(
            trackKey = subject.trackKey,
            title = subject.title.take(MAX_TEXT),
            artist = subject.artist.take(MAX_TEXT),
            album = subject.album.take(MAX_TEXT),
            startedAt = listen.startedAt,
            listenedMs = listen.listenedMs,
            durationMs = listen.durationMs,
            outcome = listen.outcome.code,
            leftAtMs = listen.leftAtMs,
            repeats = listen.repeats,
            seeks = listen.seeks,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = mondayFirst(cal.get(Calendar.DAY_OF_WEEK)),
            output = currentOutput().code,
            source = subject.source.code,
        )
        scope.launch(Dispatchers.IO) { db.listens().insert(entity) }
    }

    suspend fun clear() = withContext(Dispatchers.IO) { db.listens().deleteAll() }

    /** Streams the whole journal as JSON in pages, so a long history never sits in memory at once. */
    suspend fun export(out: OutputStream): Int = withContext(Dispatchers.IO) {
        var written = 0
        JsonWriter(out.bufferedWriter(Charsets.UTF_8)).use { w ->
            w.setIndent("  ")
            w.beginObject()
            w.name("app").value("HOLOWAVE")
            w.name("format").value(1)
            w.name("exportedAt").value(System.currentTimeMillis())
            w.name("listens").beginArray()
            var offset = 0
            while (true) {
                val page = db.listens().page(PAGE, offset)
                if (page.isEmpty()) break
                for (e in page) {
                    w.beginObject()
                    w.name("trackKey").value(e.trackKey)
                    w.name("title").value(e.title)
                    w.name("artist").value(e.artist)
                    w.name("album").value(e.album)
                    w.name("startedAt").value(e.startedAt)
                    w.name("listenedMs").value(e.listenedMs)
                    w.name("durationMs").value(e.durationMs)
                    w.name("outcome").value(Outcome.of(e.outcome).name.lowercase())
                    w.name("leftAtMs").value(e.leftAtMs)
                    w.name("repeats").value(e.repeats)
                    w.name("seeks").value(e.seeks)
                    w.name("hour").value(e.hour)
                    w.name("dayOfWeek").value(e.dayOfWeek)
                    w.name("output").value(OutputKind.entries.firstOrNull { it.code == e.output }?.name?.lowercase() ?: "other")
                    w.name("source").value(TrackSource.of(e.source).name.lowercase())
                    w.endObject()
                    written++
                }
                offset += page.size
            }
            w.endArray()
            w.endObject()
        }
        written
    }

    /** Headphones and Bluetooth are told apart without any permission. */
    private fun currentOutput(): OutputKind {
        val am = context.getSystemService(AudioManager::class.java) ?: return OutputKind.OTHER
        val types = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type }
        return when {
            types.any { it == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it == AudioDeviceInfo.TYPE_BLE_HEADSET || it == AudioDeviceInfo.TYPE_BLE_SPEAKER } -> OutputKind.BLUETOOTH
            types.any { it == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it == AudioDeviceInfo.TYPE_WIRED_HEADSET || it == AudioDeviceInfo.TYPE_USB_HEADSET } -> OutputKind.WIRED
            types.any { it == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } -> OutputKind.SPEAKER
            else -> OutputKind.OTHER
        }
    }

    private fun mondayFirst(calendarDay: Int): Int = if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1

    private companion object {
        const val PAGE = 500
        const val MAX_TEXT = 300
    }
}
