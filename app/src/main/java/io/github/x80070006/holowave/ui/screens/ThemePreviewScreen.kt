package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.theme.Appearance
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloColors
import io.github.x80070006.holowave.ui.theme.HoloTheme
import io.github.x80070006.holowave.ui.theme.HoloThemes
import io.github.x80070006.holowave.ui.theme.LocalHoloColors
import io.github.x80070006.holowave.ui.theme.ThemeId

/**
 * Internal screen: every theme, dark and light side by side, with its five colours, a
 * small mock-up and the lowest contrast ratio it guarantees. Each card is drawn only
 * from its own [HoloColors], so what you see is exactly what the theme will look like.
 */
@Composable
fun ThemePreviewScreen(bottomPadding: State<Int>, onBack: () -> Unit, onApply: (ThemeId) -> Unit) {
    val c = HoloTheme.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = (bottomPadding.value + 32).dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = c.text)
                Text("Palettes", style = DecoType.Display, color = c.text, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cinq couleurs par thème, en version sombre et claire. Touchez un thème pour l'appliquer.",
                    style = DecoType.Small, color = c.textSecondary, modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        items(ThemeId.entries.size) { i ->
            val id = ThemeId.entries[i]
            Column(Modifier.clickable { onApply(id) }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(id.label, style = DecoType.Headline, color = c.text)
                    if (Appearance.themeId == id) {
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.clip(RoundedCornerShape(50)).background(c.primary).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text("Actif", style = DecoType.Small, color = c.onPrimary)
                        }
                    }
                }
                Text(id.tagline, style = DecoType.Small, color = c.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaletteCard(HoloThemes.colors(id, dark = true, Appearance.monoAccent), "Sombre", Modifier.weight(1f))
                    PaletteCard(HoloThemes.colors(id, dark = false, Appearance.monoAccent), "Clair", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(colors: HoloColors, label: String, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalHoloColors provides colors) {
        val c = HoloTheme.colors
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier
                .clip(shape)
                .background(c.background)
                .border(1.dp, c.outline, shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(label, style = DecoType.Caps, color = c.textSecondary)

            // The five colours.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(c.primary, c.secondary, c.tertiary, c.background, c.text).forEach {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(it).border(1.dp, c.outline, CircleShape))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Bonsoir", style = DecoType.Headline, color = c.text)
                Text("Votre musique, hors ligne", style = DecoType.Small, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Filter chips and a provenance badge.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("Tout", c.secondary, c.onSecondary)
                Chip("Mixes", c.surfaceHigh, c.text)
            }
            Chip("NouTube", c.tertiary, c.onTertiary)

            // Primary action and progress.
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50)).background(c.primary),
                contentAlignment = Alignment.Center,
            ) { Text("Lecture", style = DecoType.Title, color = c.onPrimary) }
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(c.surfaceHigh)) {
                Box(Modifier.fillMaxWidth(0.42f).height(5.dp).clip(RoundedCornerShape(50)).background(c.primary))
            }

            // Mini player.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(c.primary, c.tertiary))),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Titre", style = DecoType.Title, color = c.text, maxLines = 1)
                    Text("Artiste", style = DecoType.Small, color = c.textSecondary, maxLines = 1)
                }
                Icon(DecoIcons.Play, null, Modifier.size(22.dp), tint = c.primary)
            }

            val ratio = HoloThemes.minContrast(c)
            Text(
                "Contraste min. ${"%.1f".format(ratio)}:1  ${if (ratio >= 4.5) "AA ✓" else "AA ✗"}",
                style = DecoType.Small, color = c.textSecondary,
            )
        }
    }
}

@Composable
private fun Chip(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, style = DecoType.Small, color = fg)
    }
}
