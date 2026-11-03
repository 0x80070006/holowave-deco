package io.github.x80070006.holowave.recommend

import io.github.x80070006.holowave.data.MixedItem
import io.github.x80070006.holowave.data.StreamedTrack
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackKey
import io.github.x80070006.holowave.history.Outcome
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.random.Random

/** The three things a [FlowEngine] run can favour. */
enum class FlowVariant(val label: String, val hint: String) {
    /** Everything the history says you like, plus some unheard songs. */
    MIX("Flow", "Ce que vous aimez, avec un peu de nouveau"),
    /** Only songs never played yet. */
    DISCOVER("Découvertes", "Des titres que vous n'avez jamais écoutés"),
    /** Only songs you keep coming back to. */
    FAVOURITES("Valeurs sûres", "Les titres que vous écoutez jusqu'au bout"),
    /** Songs you usually play around this hour. */
    NOW("À cette heure", "Ce que vous écoutez d'habitude à cette heure"),
}

/** One finished listen, as stored in the history. */
class ListenRow(val trackKey: String, val startedAt: Long, val outcome: Int, val hour: Int)

class TrackStats(
    val plays: Int,
    val skips: Int,
    val completes: Int,
    val lastPlayedAt: Long,
    /** Number of listens that started in each hour of the day. */
    val hours: IntArray,
)

/**
 * Picks songs from the device library from the on-device listening history only.
 * Pure Kotlin so the rules (taste, skips, novelty, variety) are tested without a device.
 * No audio analysis is involved: it cannot tell a calm song from an energetic one.
 */
object FlowEngine {
    private const val MIN_DURATION_MS = 45_000L
    private const val RECENT_PENALTY_HOURS = 12.0
    private const val HALF_LIFE_DAYS = 45.0

    fun aggregate(rows: List<ListenRow>): Map<String, TrackStats> {
        class Acc { var plays = 0; var skips = 0; var completes = 0; var last = 0L; val hours = IntArray(24) }
        val acc = HashMap<String, Acc>()
        for (r in rows) {
            val a = acc.getOrPut(r.trackKey) { Acc() }
            a.plays++
            when (r.outcome) {
                Outcome.EARLY_SKIP.code -> a.skips++
                Outcome.COMPLETED.code -> a.completes++
            }
            if (r.startedAt > a.last) a.last = r.startedAt
            a.hours[r.hour.coerceIn(0, 23)]++
        }
        return acc.mapValues { (_, a) -> TrackStats(a.plays, a.skips, a.completes, a.last, a.hours) }
    }

    /** Relative chance for a song to be picked; 0 means never. */
    fun weight(stats: TrackStats?, variant: FlowVariant, nowMs: Long, hour: Int): Double {
        val heard = stats != null && stats.plays > 0
        if (!heard) {
            return when (variant) {
                FlowVariant.DISCOVER -> 1.0
                FlowVariant.MIX -> 0.45
                else -> 0.0
            }
        }
        if (variant == FlowVariant.DISCOVER) return 0.0
        stats!!
        val skipRate = stats.skips.toDouble() / stats.plays
        val liking = (stats.completes + 1.0) / (stats.plays + 2.0)
        var w = ln1p(stats.plays.toDouble()) * liking * (1.0 - 0.85 * skipRate)

        // Long ago is fine, a few hours ago is not: do not replay what was just heard.
        val ageHours = (nowMs - stats.lastPlayedAt).coerceAtLeast(0L) / 3_600_000.0
        w *= (ageHours / RECENT_PENALTY_HOURS).coerceAtMost(1.0).coerceAtLeast(0.05)
        // Slowly fade what has not been touched for months.
        w *= exp(-ln(2.0) * (ageHours / 24.0) / HALF_LIFE_DAYS).coerceAtLeast(0.25)

        return when (variant) {
            FlowVariant.FAVOURITES -> if (skipRate > 0.4 || stats.plays < 2) 0.0 else w * w
            FlowVariant.NOW -> {
                var near = 0
                for (d in -2..2) near += stats.hours[(hour + d + 24) % 24]
                w * (0.2 + 4.0 * near / stats.plays)
            }
            else -> w
        }
    }

    /**
     * Up to [count] songs, never two by the same artist in a row when avoidable.
     * [blockedKeys] and [blockedArtists] (lower case) are never proposed.
     */
    fun pick(
        tracks: List<Track>,
        stats: Map<String, TrackStats>,
        variant: FlowVariant,
        nowMs: Long,
        hour: Int,
        count: Int,
        random: Random,
        blockedKeys: Set<String> = emptySet(),
        blockedArtists: Set<String> = emptySet(),
        exclude: Set<Long> = emptySet(),
    ): List<Track> =
        pickMixed(tracks, emptyList(), stats, variant, nowMs, hour, count, random, blockedKeys, blockedArtists, exclude)
            .filterIsInstance<MixedItem.Local>().map { it.track }

    /**
     * Same as [pick] over the files of the device and the songs heard on YouTube Music together.
     * A streamed song can only be proposed when its video id is known: the page needs it to start it.
     */
    fun pickMixed(
        tracks: List<Track>,
        streams: List<StreamedTrack>,
        stats: Map<String, TrackStats>,
        variant: FlowVariant,
        nowMs: Long,
        hour: Int,
        count: Int,
        random: Random,
        blockedKeys: Set<String> = emptySet(),
        blockedArtists: Set<String> = emptySet(),
        exclude: Set<Long> = emptySet(),
    ): List<MixedItem> {
        val scored = ArrayList<Pair<MixedItem, Double>>()
        for (t in tracks) {
            if (!t.playable || t.durationMs < MIN_DURATION_MS || t.id in exclude) continue
            val key = TrackKey.of(t)
            if (key in blockedKeys || t.artist.trim().lowercase() in blockedArtists) continue
            val w = weight(stats[key], variant, nowMs, hour)
            if (w > 0.0) scored += MixedItem.Local(t) to w
        }
        for (s in streams) {
            if (s.videoId.isNullOrBlank() || s.key in blockedKeys || s.artist.trim().lowercase() in blockedArtists) continue
            // The history of a streamed song is kept under its own key; a never-finished one still counts as heard once.
            val w = weight(stats[s.key] ?: if (s.plays > 0) TrackStats(s.plays, 0, s.plays, s.lastSeen, IntArray(24)) else null, variant, nowMs, hour)
            if (w > 0.0) scored += MixedItem.Stream(s) to w
        }
        // Weighted sampling without replacement: the larger ln(u) / w, the likelier to be picked.
        val sampled = scored
            .map { (item, w) -> item to ln(random.nextDouble().coerceAtLeast(1e-12)) / w }
            .sortedByDescending { it.second }
            .take(count * 3)
            .map { it.first }
            .toMutableList()
        return separateArtists(sampled, count)
    }

    private fun artistOf(i: MixedItem) = when (i) { is MixedItem.Local -> i.track.artist; is MixedItem.Stream -> i.track.artist }

    private fun separateArtists(pool: MutableList<MixedItem>, count: Int): List<MixedItem> {
        val out = ArrayList<MixedItem>(count)
        while (out.size < count && pool.isNotEmpty()) {
            val last = out.lastOrNull()?.let(::artistOf)
            val i = pool.indexOfFirst { artistOf(it) != last }.takeIf { it >= 0 } ?: 0
            out += pool.removeAt(i)
        }
        return out
    }

    /** Hour-distance on the 24 h clock, for tests and callers that compare hours. */
    fun hourDistance(a: Int, b: Int): Int = abs(a - b).let { minOf(it, 24 - it) }
}
