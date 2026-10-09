package io.github.x80070006.holowave.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.ScanProgress
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoType

val ScanProgress.running: Boolean get() = this is ScanProgress.Searching || this is ScanProgress.Indexing

/**
 * Brass progress rail with stepped end caps. [fraction] null = indeterminate:
 * a gold segment sweeps across while the total is still unknown.
 */
@Composable
fun DecoProgressBar(fraction: Float?, modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "sweep").animateFloat(
        -0.3f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x",
    )
    val shown by animateFloatAsState(fraction ?: 0f, tween(250), label = "fraction")
    Canvas(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .semantics { if (fraction != null) progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) else progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        val cy = size.height / 2
        val h = 4.dp.toPx()
        val cap = 4.dp.toPx()
        // Rail.
        drawRect(Deco.Gold.copy(alpha = 0.14f), Offset(cap, cy - h / 2), Size(size.width - 2 * cap, h))
        // End caps: small diamonds.
        listOf(cap, size.width - cap).forEach { x ->
            val p = Path().apply { moveTo(x, cy - cap); lineTo(x + cap, cy); lineTo(x, cy + cap); lineTo(x - cap, cy); close() }
            drawPath(p, Deco.Gold)
        }
        val track = size.width - 2 * cap
        if (fraction == null) {
            val start = (sweep.value * track).coerceIn(0f, track)
            val end = ((sweep.value + 0.3f) * track).coerceIn(0f, track)
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Deco.GoldBright, Color.Transparent), cap + start, cap + end),
                Offset(cap + start, cy - h / 2), Size((end - start).coerceAtLeast(0f), h),
            )
        } else {
            drawRect(Brush.horizontalGradient(listOf(Deco.Copper, Deco.GoldBright)), Offset(cap, cy - h / 2), Size(track * shown, h))
        }
    }
}

/** Full scan report used in the settings and source-folder screens. */
@Composable
fun ScanPanel(progress: ScanProgress, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val shape = remember { SteppedShape(5.dp) }
    Column(
        modifier
            .fillMaxWidth()
            .background(Deco.TealDeep.copy(alpha = 0.6f), shape)
            .border(1.dp, Deco.Gold.copy(alpha = 0.45f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        val (title, detail, fraction) = describe(progress)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), style = DecoType.Caps, color = Deco.Gold, modifier = Modifier.weight(1f))
            if (progress.running) DecoButton("Arrêter", onStop, filled = false, modifier = Modifier.height(36.dp))
        }
        Spacer(Modifier.height(14.dp))
        DecoProgressBar(fraction)
        Spacer(Modifier.height(10.dp))
        Text(detail, style = DecoType.Small, color = Deco.CreamDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** One-line variant shown under the library header while a scan runs. */
@Composable
fun ScanBanner(progress: ScanProgress, modifier: Modifier = Modifier) {
    val (title, detail, fraction) = describe(progress)
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
        DecoProgressBar(fraction)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(title, style = DecoType.Small, color = Deco.Gold)
            Spacer(Modifier.width(8.dp))
            Text(detail, style = DecoType.Small, color = Deco.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun plural(n: Int, word: String) = "$n $word${if (n > 1) "s" else ""}"

private fun describe(p: ScanProgress): Triple<String, String, Float?> = when (p) {
    is ScanProgress.Searching -> Triple(
        "Recherche des morceaux…",
        "${plural(p.folders, "dossier")} parcouru${if (p.folders > 1) "s" else ""} · ${plural(p.audioFiles, "fichier")} audio" +
            if (p.current.isNotEmpty()) " · ${p.current}" else "",
        null,
    )
    is ScanProgress.Indexing -> Triple(
        "Indexation ${p.done} / ${p.total}",
        "${plural(p.audioFiles, "fichier")} audio trouvé${if (p.audioFiles > 1) "s" else ""}, ajout des nouveaux à la discothèque",
        if (p.total == 0) 1f else p.done.toFloat() / p.total,
    )
    is ScanProgress.Finished -> Triple(
        "Recherche terminée",
        "${plural(p.audioFiles, "fichier")} audio trouvé${if (p.audioFiles > 1) "s" else ""} · " +
            (when (p.indexed) { 0 -> "tout était déjà à jour"; 1 -> "1 fichier ajouté ou mis à jour"; else -> "${p.indexed} fichiers ajoutés ou mis à jour" }) +
            if (p.skippedFolders > 0) " · ${plural(p.skippedFolders, "dossier")} inaccessible${if (p.skippedFolders > 1) "s" else ""}" else "",
        1f,
    )
    ScanProgress.Failed -> Triple("Recherche impossible", "L'accès aux fichiers audio a été refusé.", 0f)
}
