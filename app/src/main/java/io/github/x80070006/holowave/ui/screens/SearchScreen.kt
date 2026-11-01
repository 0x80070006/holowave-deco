package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.playback.PlaybackState
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.EmptyState
import io.github.x80070006.holowave.ui.components.TrackRow
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme

/** Local results first. Categories and the YouTube Music line come with the search step. */
@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    playback: PlaybackState,
    bottomPadding: State<Int>,
    onQuery: (String) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onTrackMenu: (Track) -> Unit,
) {
    val c = HoloTheme.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = (bottomPadding.value + 24).dp)) {
        item {
            Column {
                Text("Rechercher", style = DecoType.Display, color = c.text, modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 20.dp, bottom = 16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceHigh)
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(DecoIcons.Search, null, Modifier.size(22.dp), tint = c.textSecondary)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Titre, artiste, album…", style = DecoType.Body, color = c.textDisabled)
                        BasicTextField(
                            query, onQuery,
                            singleLine = true,
                            textStyle = DecoType.Body.copy(color = c.text),
                            cursorBrush = SolidColor(c.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotEmpty()) DecoIconButton(DecoIcons.Close, "Effacer la recherche", { onQuery("") }, tint = c.textSecondary, iconSize = 20.dp)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        when {
            query.isBlank() -> item {
                EmptyState("Que voulez-vous écouter ?", "Cherchez un titre, un artiste ou un album dans votre musique.")
            }
            results.isEmpty() -> item {
                EmptyState("Rien trouvé", "Aucun titre, artiste ou album ne correspond à « $query ».")
            }
            else -> {
                item {
                    Text(
                        "${results.size} résultat${if (results.size > 1) "s" else ""}",
                        style = DecoType.Small, color = c.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(results.size, key = { results[it].id }) { i ->
                    val t = results[i]
                    TrackRow(t, t.id == playback.currentId, playback.isPlaying, { onPlay(results, i) }, { onTrackMenu(t) })
                }
            }
        }
    }
}
