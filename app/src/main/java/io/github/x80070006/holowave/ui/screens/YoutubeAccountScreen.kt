package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.tube.TubeHost
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoIconButton
import io.github.x80070006.holowave.ui.components.TubeSurface
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme

/** The embedded YouTube Music page, full screen: where the user signs in to their Google account. */
@Composable
fun YoutubeAccountScreen(
    host: TubeHost,
    signedIn: Boolean,
    bottomPadding: Int,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val c = HoloTheme.colors
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            DecoIconButton(DecoIcons.Back, "Retour", onBack, tint = c.text)
            Text("Compte YouTube", style = DecoType.Headline, color = c.text, modifier = Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (signedIn) "Connecté" else "Non connecté", style = DecoType.Title, color = if (signedIn) c.primary else c.textSecondary)
            if (signedIn) DecoButton("Se déconnecter", onSignOut, filled = false) else DecoButton("Se connecter", onSignIn)
        }
        Text(
            "La connexion se fait sur la page de Google. HOLOWAVE ne voit jamais votre mot de passe : la session reste dans l'application, sur l'appareil.",
            style = DecoType.Small, color = c.textSecondary, modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        TubeSurface(host, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(bottomPadding.dp))
    }
}
