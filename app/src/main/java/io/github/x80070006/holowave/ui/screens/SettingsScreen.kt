package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.BuildConfig
import io.github.x80070006.holowave.data.AppSettings
import io.github.x80070006.holowave.ui.PrefetchProgress
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.Hairline
import io.github.x80070006.holowave.ui.components.SectionLabel
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

@Composable
fun SettingsScreen(
    settings: AppSettings,
    trackCount: Int,
    storedLyrics: Int,
    prefetch: PrefetchProgress?,
    bottomPadding: State<Int>,
    onBack: () -> Unit,
    onOnlineLyrics: (Boolean) -> Unit,
    onSpinning: (Boolean) -> Unit,
    onPrefetch: () -> Unit,
    onCancelPrefetch: () -> Unit,
    onClearLyrics: () -> Unit,
    onRescan: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 32).dp)) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = Deco.Gold)
                }
                Text("Réglages", style = DecoType.Display, color = Deco.GoldBright, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(14.dp))
                DecoDivider(Modifier.padding(horizontal = 24.dp))
            }
        }

        item { Section("Paroles") }
        item {
            ToggleRow(
                "Recherche en ligne (LRCLIB)",
                "Si un morceau n'a pas de paroles intégrées, HOLOWAVE interroge lrclib.net, base libre et sans compte. " +
                    "Seuls le titre, l'artiste, l'album et la durée sont envoyés, en HTTPS. Les paroles trouvées restent sur l'appareil.",
                settings.onlineLyrics, onOnlineLyrics,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Paroles hors ligne", style = DecoType.Title, color = Deco.Cream)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Télécharge d'avance les paroles de toute la discothèque, pour les retrouver sans réseau. $storedLyrics fiche${if (storedLyrics > 1) "s" else ""} enregistrée${if (storedLyrics > 1) "s" else ""}.",
                    style = DecoType.Small, color = Deco.CreamDim,
                )
                Spacer(Modifier.height(14.dp))
                if (prefetch?.running == true) {
                    ProgressLine(prefetch.done.toFloat() / prefetch.total.coerceAtLeast(1))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${prefetch.done} / ${prefetch.total}  ·  ${prefetch.found} trouvées", style = DecoType.Numeral, color = Deco.CreamDim, modifier = Modifier.weight(1f))
                        DecoButton("Arrêter", onCancelPrefetch, filled = false)
                    }
                } else {
                    if (prefetch != null) {
                        Text("Terminé : ${prefetch.found} paroles disponibles sur ${prefetch.total} titres.", style = DecoType.Small, color = Deco.Gold)
                        Spacer(Modifier.height(10.dp))
                    }
                    Row {
                        DecoButton("Tout préparer", onPrefetch, icon = DecoIcons.Import, enabled = settings.onlineLyrics && trackCount > 0)
                        Spacer(Modifier.width(12.dp))
                        DecoButton("Effacer", onClearLyrics, icon = DecoIcons.Trash, filled = false)
                    }
                }
            }
        }

        item { Section("Lecture") }
        item {
            ToggleRow("Disque en rotation", "Le vinyle tourne pendant la lecture. Désactivez pour un affichage immobile.", settings.spinningRecord, onSpinning)
        }

        item { Section("Discothèque") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$trackCount titres indexés", style = DecoType.Title, color = Deco.Cream)
                    Spacer(Modifier.height(4.dp))
                    Text("Les fichiers restent où ils sont : rien n'est copié ni envoyé.", style = DecoType.Small, color = Deco.CreamDim)
                }
                DecoButton("Analyser", onRescan, icon = DecoIcons.Refresh, filled = false)
            }
        }

        item { Section("Confidentialité") }
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                listOf(
                    "Aucun compte, aucune publicité, aucun traqueur, aucune statistique.",
                    "Accès en lecture seule à l'audio de l'appareil, rien d'autre.",
                    "Une seule connexion possible : lrclib.net en HTTPS, désactivable ci-dessus.",
                    "Playlists, favoris et paroles sont stockés dans l'espace privé de l'application et exclus des sauvegardes cloud.",
                ).forEach { Bullet(it) }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DecoDivider(Modifier.width(160.dp))
                Spacer(Modifier.height(14.dp))
                Text("HOLOWAVE  ${BuildConfig.VERSION_NAME}", style = DecoType.Caps, color = Deco.Gold)
                Spacer(Modifier.height(6.dp))
                Text("Polices Poiret One & Josefin Sans (SIL OFL) · Paroles LRCLIB", style = DecoType.Small, color = Deco.Muted)
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Column(Modifier.padding(top = 28.dp, bottom = 4.dp)) {
        SectionLabel(title, Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(10.dp))
        Hairline(Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .semantics { stateDescription = if (checked) "Activé" else "Désactivé" }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DecoType.Title, color = Deco.Cream)
            Spacer(Modifier.height(6.dp))
            Text(body, style = DecoType.Small, color = Deco.CreamDim)
        }
        Spacer(Modifier.width(18.dp))
        DecoSwitch(checked)
    }
}

/** Brass rail with a diamond that slides across. */
@Composable
private fun DecoSwitch(checked: Boolean) {
    val t by animateFloatAsState(if (checked) 1f else 0f, label = "switch")
    Canvas(Modifier.size(48.dp, 26.dp)) {
        val h = size.height
        val cut = h * 0.3f
        val rail = Path().apply {
            moveTo(cut, 0f); lineTo(size.width - cut, 0f); lineTo(size.width, cut); lineTo(size.width, h - cut)
            lineTo(size.width - cut, h); lineTo(cut, h); lineTo(0f, h - cut); lineTo(0f, cut); close()
        }
        drawPath(rail, Deco.Gold.copy(alpha = 0.12f + 0.25f * t))
        drawPath(rail, Deco.Gold.copy(alpha = 0.5f + 0.5f * t), style = Stroke(1.2f))
        val d = h * 0.32f
        val cx = h / 2 + (size.width - h) * t
        val knob = Path().apply { moveTo(cx, h / 2 - d); lineTo(cx + d, h / 2); lineTo(cx, h / 2 + d); lineTo(cx - d, h / 2); close() }
        drawPath(knob, if (t > 0.5f) Deco.GoldBright else Deco.Muted)
    }
}

@Composable
private fun ProgressLine(fraction: Float) {
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        drawRect(Deco.Gold.copy(alpha = 0.15f), size = size)
        drawRect(Deco.goldBrush, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height))
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.padding(top = 5.dp).size(6.dp)) {
            Canvas(Modifier.size(6.dp)) {
                val p = Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close() }
                drawPath(p, Deco.Gold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = DecoType.Small, color = Deco.CreamDim)
    }
}
