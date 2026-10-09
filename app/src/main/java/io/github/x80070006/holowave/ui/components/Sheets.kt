package io.github.x80070006.holowave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.x80070006.holowave.data.PlaylistStore
import io.github.x80070006.holowave.data.Track
import io.github.x80070006.holowave.data.TrackCollection
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType

class SheetAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Deco.InkRaised,
        contentColor = Deco.Cream,
        shape = SteppedShape(8.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).width(40.dp)) { DecoDivider() }
        },
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) { content() }
    }
}

@Composable
fun TrackMenu(track: Track, actions: List<SheetAction>, onDismiss: () -> Unit) {
    DecoSheet(onDismiss) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverImage(track, Modifier.size(56.dp).decoFrame(inset = 3.dp, alpha = 0.7f), size = 192)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = DecoType.Title, color = Deco.Cream, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${track.artist} · ${track.album}", style = DecoType.Small, color = Deco.CreamDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Hairline(Modifier.padding(vertical = 8.dp))
        actions.forEach { a -> SheetRow(a.icon, a.label) { onDismiss(); a.onClick() } }
    }
}

@Composable
fun SheetRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = Deco.Gold)
        Spacer(Modifier.width(20.dp))
        Text(label, style = DecoType.Body, color = Deco.Cream)
    }
}

@Composable
fun PlaylistPicker(
    playlists: List<TrackCollection>,
    onPick: (TrackCollection) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    DecoSheet(onDismiss) {
        SectionLabel("Ajouter à une playlist", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        SheetRow(DecoIcons.Plus, "Nouvelle playlist…") { onDismiss(); onCreate() }
        LazyColumn(Modifier.heightIn(max = 380.dp)) {
            items(playlists, key = { it.key }) { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { onDismiss(); onPick(p) }.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverImage(p.cover, Modifier.size(44.dp).decoFrame(inset = 3.dp, alpha = 0.6f), size = 192, fallbackSeed = p.title)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(p.title, style = DecoType.Title, color = Deco.Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.subtitle, style = DecoType.Small, color = Deco.CreamDim)
                    }
                }
            }
        }
    }
}

/** Text prompt in a brass-framed card; the value is sanitised by [PlaylistStore] anyway. */
@Composable
fun NameDialog(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        val shape = remember { SteppedShape(6.dp) }
        Column(
            Modifier
                .fillMaxWidth()
                .background(Deco.InkRaised, shape)
                .border(1.dp, Deco.Gold.copy(alpha = 0.6f), shape)
                .padding(24.dp),
        ) {
            Text(title, style = DecoType.Headline, color = Deco.GoldBright)
            Spacer(Modifier.height(20.dp))
            BasicTextField(
                value = value,
                onValueChange = { value = it.take(PlaylistStore.MAX_NAME_LENGTH) },
                singleLine = true,
                textStyle = DecoType.Body.copy(color = Deco.Cream),
                cursorBrush = SolidColor(Deco.Gold),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onConfirm(value) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Column {
                        Box(Modifier.padding(vertical = 8.dp)) {
                            if (value.isEmpty()) Text("Nom de la playlist", style = DecoType.Body, color = Deco.Muted)
                            inner()
                        }
                        Hairline()
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                DecoButton("Annuler", onDismiss, filled = false)
                DecoButton(confirm, { onConfirm(value) }, enabled = value.isNotBlank())
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = remember { SteppedShape(6.dp) }
        Column(
            Modifier.fillMaxWidth().background(Deco.InkRaised, shape).border(1.dp, Deco.Gold.copy(alpha = 0.6f), shape).padding(24.dp),
        ) {
            Text(title, style = DecoType.Headline, color = Deco.GoldBright)
            Spacer(Modifier.height(12.dp))
            Text(body, style = DecoType.Body, color = Deco.CreamDim)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                DecoButton("Annuler", onDismiss, filled = false)
                DecoButton(confirm, { onDismiss(); onConfirm() })
            }
        }
    }
}
