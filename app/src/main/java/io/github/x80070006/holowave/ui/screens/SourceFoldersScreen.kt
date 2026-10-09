package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.FolderNode
import io.github.x80070006.holowave.data.ScanProgress
import io.github.x80070006.holowave.data.SourceFolders
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.Hairline
import io.github.x80070006.holowave.ui.components.ScanPanel
import io.github.x80070006.holowave.ui.components.SectionLabel
import io.github.x80070006.holowave.ui.components.running
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

@Composable
fun SourceFoldersScreen(
    tree: List<FolderNode>,
    selected: Set<String>,
    shownTracks: Int,
    totalTracks: Int,
    bottomPadding: State<Int>,
    scan: ScanProgress?,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onIncludeAll: () -> Unit,
    onPick: () -> Unit,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    val counts = tree.associate { it.key to it.trackCount }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 32).dp)) {
        item {
            Column(Modifier.statusBarsPadding()) {
                DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = Deco.Gold, modifier = Modifier.padding(horizontal = 6.dp))
                Text("Dossiers sources", style = DecoType.Display, color = Deco.GoldBright, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ajoutez les dossiers de votre choix, ou cochez-les dans la liste ci-dessous. Un dossier inclut ses sous-dossiers. " +
                        "Les autres fichiers ne sont ni supprimés ni déplacés, seulement masqués.",
                    style = DecoType.Small, color = Deco.CreamDim, modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(18.dp))
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DecoButton("Ajouter un dossier…", onPick, Modifier.fillMaxWidth(), icon = DecoIcons.Plus, enabled = scan?.running != true)
                    DecoButton(
                        if (selected.isEmpty()) "Rechercher sur tout l'appareil" else "Rechercher dans mes dossiers",
                        onScan, Modifier.fillMaxWidth(), icon = DecoIcons.Search, filled = false, enabled = scan?.running != true,
                    )
                }
                if (scan != null) ScanPanel(scan, onStopScan, Modifier.padding(horizontal = 24.dp, vertical = 14.dp))
                Spacer(Modifier.height(8.dp))
                DecoDivider(Modifier.padding(horizontal = 24.dp))
                Text(
                    "$shownTracks titre${if (shownTracks > 1) "s" else ""} affiché${if (shownTracks > 1) "s" else ""} sur $totalTracks",
                    style = DecoType.Numeral, color = Deco.Gold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }
        }

        item {
            FolderRow(
                name = "Tous les dossiers",
                detail = "Tout le stockage de l'appareil",
                count = totalTracks,
                depth = 0,
                state = if (selected.isEmpty()) CheckState.ON else CheckState.OFF,
                onClick = onIncludeAll,
            )
        }

        if (selected.isNotEmpty()) {
            item { SectionLabel("Mes dossiers", Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp)) }
            items(selected.sorted(), key = { "sel-$it" }) { key ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DecoCheck(CheckState.ON)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(key.substringAfter(':').trimEnd('/').substringAfterLast('/'), style = DecoType.Title, color = Deco.GoldBright, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        val n = counts[key] ?: 0
                        Text(
                            SourceFolders.label(key) + if (n == 0) " · aucun morceau indexé pour l'instant" else " · $n titre${if (n > 1) "s" else ""}",
                            style = DecoType.Small, color = Deco.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    DecoIconButton(DecoIcons.Close, "Retirer ${SourceFolders.label(key)}", { onRemove(key) }, tint = Deco.CreamDim, iconSize = 18.dp)
                }
            }
        }

        item {
            Hairline(Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
            SectionLabel("Dossiers détectés", Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (tree.isEmpty()) {
                Text(
                    "Aucun fichier audio indexé pour l'instant. Lancez une recherche ou ajoutez un dossier.",
                    style = DecoType.Small, color = Deco.CreamDim, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }

        items(tree, key = { it.key }) { node ->
            val state = when {
                node.key in selected -> CheckState.ON
                SourceFolders.coveredByAncestor(node.key, selected) -> CheckState.INHERITED
                else -> CheckState.OFF
            }
            FolderRow(
                name = node.name,
                detail = SourceFolders.label(node.key),
                count = node.trackCount,
                depth = node.depth,
                state = state,
                onClick = { onToggle(node.key) },
            )
        }
    }
}

private enum class CheckState { OFF, ON, INHERITED }

@Composable
private fun FolderRow(name: String, detail: String, count: Int, depth: Int, state: CheckState, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = state != CheckState.INHERITED, role = Role.Checkbox, onClick = onClick)
            .semantics {
                stateDescription = when (state) {
                    CheckState.ON -> "Inclus"
                    CheckState.INHERITED -> "Inclus via le dossier parent"
                    CheckState.OFF -> "Exclu"
                }
            }
            .padding(start = 24.dp + (18 * depth.coerceAtMost(6)).dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecoCheck(state)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name, style = DecoType.Title,
                color = if (state == CheckState.OFF) Deco.Cream else Deco.GoldBright,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(detail, style = DecoType.Small, color = Deco.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Text("$count", style = DecoType.Numeral, color = Deco.GoldDim)
    }
}

/** Diamond-shaped checkbox: brass outline, filled when included, hollow tick when inherited. */
@Composable
private fun DecoCheck(state: CheckState) {
    Canvas(Modifier.size(22.dp)) {
        val c = size.width / 2
        val diamond = Path().apply { moveTo(c, 0f); lineTo(size.width, c); lineTo(c, size.height); lineTo(0f, c); close() }
        when (state) {
            CheckState.ON -> drawPath(diamond, Deco.goldBrush)
            CheckState.INHERITED -> drawPath(diamond, Deco.Gold.copy(alpha = 0.25f))
            CheckState.OFF -> Unit
        }
        drawPath(diamond, Deco.Gold.copy(alpha = if (state == CheckState.OFF) 0.6f else 1f), style = Stroke(1.5f))
        if (state != CheckState.OFF) {
            val tick = Path().apply { moveTo(size.width * 0.32f, c); lineTo(size.width * 0.46f, size.height * 0.64f); lineTo(size.width * 0.70f, size.height * 0.36f) }
            drawPath(tick, if (state == CheckState.ON) Deco.Ink else Deco.GoldBright, style = Stroke(2f, cap = StrokeCap.Round))
        }
    }
}
