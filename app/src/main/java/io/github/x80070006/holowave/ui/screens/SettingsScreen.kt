package io.github.x80070006.holowave.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.x80070006.holowave.ui.theme.Appearance
import io.github.x80070006.holowave.ui.theme.Look
import io.github.x80070006.holowave.ui.theme.ModePref
import io.github.x80070006.holowave.ui.theme.MotionPref
import io.github.x80070006.holowave.ui.theme.MonoAccent
import io.github.x80070006.holowave.ui.theme.ThemeId
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import io.github.x80070006.holowave.BuildConfig
import io.github.x80070006.holowave.data.AppSettings
import io.github.x80070006.holowave.data.ScanProgress
import io.github.x80070006.holowave.ui.PrefetchProgress
import io.github.x80070006.holowave.ui.components.ScanPanel
import io.github.x80070006.holowave.ui.components.running
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
    onBack: (() -> Unit)?,
    onOnlineLyrics: (Boolean) -> Unit,
    onPrefetch: () -> Unit,
    onCancelPrefetch: () -> Unit,
    onClearLyrics: () -> Unit,
    scan: ScanProgress?,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onSourceFolders: () -> Unit,
    onVideoContainers: (Boolean) -> Unit,
    listenCount: Int,
    onRecordHistory: (Boolean) -> Unit,
    onExportHistory: () -> Unit,
    onClearHistory: () -> Unit,
    onPreviewThemes: () -> Unit,
    onTheme: (ThemeId) -> Unit,
    onMode: (ModePref) -> Unit,
    onMonoAccent: (MonoAccent) -> Unit,
    onCoverColors: (Boolean) -> Unit,
    nouTubeInstalled: Boolean,
    accessGranted: Boolean,
    streamedCount: Int,
    onCaptureStreaming: (Boolean) -> Unit,
    onOpenAccess: () -> Unit,
    onClearStreamed: () -> Unit,
    onGetNouTube: () -> Unit,
    onMotion: (MotionPref) -> Unit,
    youtubeSignedIn: Boolean,
    onYoutubeAccount: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 32).dp)) {
        item {
            Column(Modifier.statusBarsPadding()) {
                if (onBack != null) {
                    Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = Deco.Gold)
                    }
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                Text("Réglages", style = DecoType.Display, color = Deco.Cream, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(14.dp))
                DecoDivider(Modifier.padding(horizontal = 24.dp))
            }
        }

        item { Section("Apparence") }
        item {
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OptionRow("Mode", ModePref.entries.map { it.label }, ModePref.entries.indexOf(Appearance.mode)) { onMode(ModePref.entries[it]) }
                OptionRow("Thème", ThemeId.entries.map { it.label }, ThemeId.entries.indexOf(Appearance.themeId)) { onTheme(ThemeId.entries[it]) }
                if (Appearance.themeId == ThemeId.MONO) {
                    OptionRow("Accent", MonoAccent.entries.map { it.label }, MonoAccent.entries.indexOf(Appearance.monoAccent)) { onMonoAccent(MonoAccent.entries[it]) }
                }
                OptionRow("Animations", MotionPref.entries.map { it.label }, MotionPref.entries.indexOf(Appearance.motion)) { onMotion(MotionPref.entries[it]) }
                ToggleRow(
                    "Couleurs de la pochette",
                    "Le fond du lecteur et les accents prennent les couleurs de la pochette en cours, ajustées pour rester lisibles.",
                    Appearance.coverColors, onCoverColors,
                )
                Row(Modifier.padding(horizontal = 24.dp)) {
                    DecoButton("Voir les palettes", onPreviewThemes, filled = false)
                }
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

        item { Section("YouTube Music") }
        item {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onYoutubeAccount).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Compte YouTube", style = DecoType.Title, color = Deco.Cream)
                    Spacer(Modifier.height(4.dp))
                    Text(if (youtubeSignedIn) "Connecté" else "Non connecté", style = DecoType.Small, color = Deco.Gold)
                }
                Icon(DecoIcons.Back, null, Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }, tint = Deco.Gold)
            }
        }

        item { Section("Discothèque") }
        item {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onSourceFolders).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dossiers sources", style = DecoType.Title, color = Deco.Cream)
                    Spacer(Modifier.height(4.dp))
                    val n = settings.sourceFolders.size
                    Text(
                        if (n == 0) "Tous les dossiers de l'appareil" else "$n dossier${if (n > 1) "s" else ""} sélectionné${if (n > 1) "s" else ""}",
                        style = DecoType.Small, color = Deco.Gold,
                    )
                }
                Icon(DecoIcons.Back, null, Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }, tint = Deco.Gold)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rechercher les morceaux", style = DecoType.Title, color = Deco.Cream)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$trackCount titres dans la discothèque. Parcourt ${if (settings.sourceFolders.isEmpty()) "tout le stockage" else "les dossiers sources"} " +
                            "et ajoute les fichiers audio qu'Android n'avait pas encore repérés. Rien n'est copié ni envoyé.",
                        style = DecoType.Small, color = Deco.CreamDim,
                    )
                }
                Spacer(Modifier.width(12.dp))
                DecoButton("Lancer", onScan, icon = DecoIcons.Search, filled = false, enabled = scan?.running != true)
            }
            if (scan != null) ScanPanel(scan, onStopScan, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }

        item {
            ToggleRow(
                "Fichiers WebM et 3GP",
                "Android range ces formats parmi les vidéos, même quand ils ne contiennent que du son. Les activer demande l'accès aux vidéos ; " +
                    "seuls les fichiers sans image sont affichés, vos vraies vidéos restent ignorées. Désactiver rend l'autorisation à Android.",
                settings.videoContainers, onVideoContainers,
            )
        }

        item { Section("NouTube") }
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    if (nouTubeInstalled) "NouTube est installé : HOLOWAVE peut l'ouvrir sur un morceau ou une recherche."
                    else "NouTube est une application libre qui donne accès à YouTube Music sans pub ni compte. Installée, elle s'intègre ici.",
                    style = DecoType.Small, color = Deco.CreamDim,
                )
                if (!nouTubeInstalled) {
                    Spacer(Modifier.height(12.dp))
                    DecoButton("Obtenir NouTube (F-Droid)", onGetNouTube, filled = false)
                }
            }
        }
        item {
            ToggleRow(
                "Capturer ce que j'écoute dans NouTube",
                "Les morceaux écoutés en streaming apparaissent dans la catégorie NouTube, mélangés à vos fichiers. HOLOWAVE ne lit que le titre, " +
                    "l'artiste et la pochette du lecteur de NouTube, jamais vos notifications. Rien ne quitte l'appareil.",
                settings.captureStreaming, onCaptureStreaming,
            )
        }
        if (settings.captureStreaming) {
            item {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                    if (accessGranted) {
                        Text("Accès accordé : la capture est active dès qu'un morceau joue dans NouTube.", style = DecoType.Small, color = Deco.Gold)
                    } else {
                        Text(
                            "Dernière étape : autorisez HOLOWAVE dans « Accès aux notifications » d'Android. " +
                                "Si Android affiche « Paramètre restreint », ouvrez Infos sur l'appli HOLOWAVE, touchez ⋮ puis « Autoriser les paramètres restreints », et recommencez.",
                            style = DecoType.Small, color = Deco.CreamDim,
                        )
                        Spacer(Modifier.height(12.dp))
                        DecoButton("Ouvrir les réglages Android", onOpenAccess)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$streamedCount morceau${if (streamedCount > 1) "x" else ""} streamé${if (streamedCount > 1) "s" else ""} enregistré${if (streamedCount > 1) "s" else ""}",
                    style = DecoType.Title, color = Deco.Cream, modifier = Modifier.weight(1f),
                )
                DecoButton("Effacer", onClearStreamed, icon = DecoIcons.Trash, filled = false, enabled = streamedCount > 0)
            }
        }

        item { Section("Historique d'écoute") }
        item {
            ToggleRow(
                "Enregistrer mes écoutes",
                "Garde sur l'appareil ce que vous écoutez, quand et jusqu'où : c'est ce qui permettra de proposer des mixes à votre goût. " +
                    "Rien n'est envoyé, rien n'est sauvegardé dans le cloud.",
                settings.recordHistory, onRecordHistory,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    "$listenCount écoute${if (listenCount > 1) "s" else ""} enregistrée${if (listenCount > 1) "s" else ""}",
                    style = DecoType.Title, color = Deco.Cream,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    DecoButton("Exporter", onExportHistory, icon = DecoIcons.Import, filled = false, enabled = listenCount > 0)
                    Spacer(Modifier.width(12.dp))
                    DecoButton("Effacer", onClearHistory, icon = DecoIcons.Trash, filled = false, enabled = listenCount > 0)
                }
            }
        }

        item { Section("Confidentialité") }
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                listOf(
                    "Aucun compte, aucune publicité, aucun traqueur, aucune statistique.",
                    "Accès en lecture seule à l'audio de l'appareil ; l'accès aux vidéos n'est demandé que si vous activez WebM / 3GP.",
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
                Text("Police Manrope (SIL OFL) · Paroles LRCLIB", style = DecoType.Small, color = Deco.Muted)
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
    if (!Look.ornate) {
        Canvas(Modifier.size(52.dp, 30.dp)) {
            val h = size.height
            drawRoundRect(
                androidx.compose.ui.graphics.lerp(Deco.TealDeep, Deco.Gold, t), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2),
            )
            if (t < 0.5f) drawRoundRect(Deco.Muted.copy(alpha = 0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2), style = Stroke(2f))
            val cx = h / 2 + (size.width - h) * t
            drawCircle(androidx.compose.ui.graphics.lerp(Deco.Muted, Deco.OnAccent, t), h * (0.28f + 0.1f * t), Offset(cx, h / 2))
        }
        return
    }
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


/** A label above a row of pill options; the selected one is filled with the primary colour. */
@Composable
private fun OptionRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label, style = DecoType.Small, color = Deco.CreamDim, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options.size) { i ->
                val active = i == selected
                Box(
                    Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(if (active) Deco.Gold else Deco.TealDeep)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(options[i], style = DecoType.Title.copy(fontSize = 14.sp), color = if (active) Deco.OnAccent else Deco.CreamDim)
                }
            }
        }
    }
}
