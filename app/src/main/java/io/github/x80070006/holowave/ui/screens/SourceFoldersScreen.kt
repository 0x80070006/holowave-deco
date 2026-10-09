package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import io.github.x80070006.holowave.data.SourceFolders
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.Hairline
import io.github.x80070006.holowave.ui.components.SectionLabel
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
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onIncludeAll: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 32).dp)) {
        item {
            Column(Modifier.statusBarsPadding()) {
                DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = Deco.Gold, modifier = Modifier.padding(horizontal = 6.dp))
                Text("Dossiers sources", style = DecoType.Display, color = Deco.GoldBright, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Choisissez un ou plusieurs dossiers à afficher dans la discothèque. Un dossier coché inclut ses sous-dossiers. " +
                        "Les autres fichiers ne sont ni supprimés ni déplacés, seulement masqués.",
                    style = DecoType.Small, color = Deco.CreamDim, modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(14.dp))
                DecoDivider(Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "$shownTracks titre${if (shownTracks > 1) "s" else ""} affiché${if (shownTracks > 1) "s" else ""} sur $totalTracks",
                    style = DecoType.Numeral, color = Deco.Gold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
            }
        }

        item {
            FolderRow(
                name = "Tous les dossiers",
                detail = "Tout ce que l'appareil a indexé",
                count = totalTracks,
                depth = 0,
                state = if (selected.isEmpty()) CheckState.ON else CheckState.OFF,
                onClick = onIncludeAll,
            )
            Hairline(Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
            SectionLabel("Dossiers de l'appareil", Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
        }

        if (tree.isEmpty()) {
            item { EmptyState("Aucun dossier", "Aucun fichier audio n'a encore été indexé sur cet appareil.") }
        }

        items(tree, key = { it.path }) { node ->
            val state = when {
                node.path in selected -> CheckState.ON
                SourceFolders.coveredByAncestor(node.path, selected) -> CheckState.INHERITED
                else -> CheckState.OFF
            }
            FolderRow(
                name = node.name,
                detail = node.path.trimEnd('/'),
                count = node.trackCount,
                depth = node.depth,
                state = state,
                onClick = { onToggle(node.path) },
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
