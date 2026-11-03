package io.github.x80070006.holowave.recommend

import io.github.x80070006.holowave.data.MixedItem

/**
 * A queue that mixes files of the device with songs of YouTube Music. Files are played by the local
 * player in runs (consecutive files are handed over together, so they stay gapless); a YouTube song
 * is played by the embedded page, one at a time. This class only decides which item comes next.
 * Pure Kotlin, tested without a device.
 */
class MixPlan(val items: List<MixedItem>) {
    /** The last index of the run of local files that starts at [from] (== [from] for a YouTube song). */
    fun runEnd(from: Int): Int {
        if (items.getOrNull(from) !is MixedItem.Local) return from
        var i = from
        while (items.getOrNull(i + 1) is MixedItem.Local) i++
        return i
    }

    /** The first index of the local run that contains [index]. */
    fun runStart(index: Int): Int {
        if (items.getOrNull(index) !is MixedItem.Local) return index
        var i = index
        while (i > 0 && items[i - 1] is MixedItem.Local) i--
        return i
    }

    /** What follows the item at [index], or null at the end. */
    fun after(index: Int): Int? = (index + 1).takeIf { it in items.indices }

    /** What precedes the run or song at [index], or null at the start. */
    fun before(index: Int): Int? = (runStart(index) - 1).takeIf { it >= 0 }?.let { runStart(it) }

    fun localTracks(from: Int, to: Int) = items.subList(from, to + 1).filterIsInstance<MixedItem.Local>().map { it.track }
}
