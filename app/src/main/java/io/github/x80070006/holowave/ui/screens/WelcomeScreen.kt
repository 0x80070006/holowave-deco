package io.github.x80070006.holowave.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.R
import io.github.x80070006.holowave.ui.components.DecoButton
import io.github.x80070006.holowave.ui.components.DecoDivider
import io.github.x80070006.holowave.ui.components.Sunburst
import io.github.x80070006.holowave.ui.theme.Deco
import io.github.x80070006.holowave.ui.theme.DecoType

/** First launch: explains the one permission the app needs, poster-style. */
@Composable
fun WelcomeScreen(permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Deco.TealDeep, Deco.Ink, Deco.Ink))),
    ) {
        Sunburst(Modifier.fillMaxSize(), rays = 37, origin = Offset(0.5f, 0.38f), spreadDegrees = 360f, alpha = 0.10f)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(176.dp))
            Spacer(Modifier.height(26.dp))
            Text("HOLOWAVE", style = DecoType.Marquee.copy(brush = Deco.goldBrush), modifier = Modifier.padding(start = 9.dp))
            Spacer(Modifier.height(12.dp))
            DecoDivider(Modifier.width(200.dp))
            Spacer(Modifier.height(12.dp))
            Text("SALON D'ÉCOUTE HORS LIGNE", style = DecoType.Caps, color = Deco.Gold)
            Spacer(Modifier.height(34.dp))
            Text(
                if (permanentlyDenied)
                    "L'accès à la musique a été refusé. Autorisez « Musique et audio » dans les réglages Android pour ouvrir la discothèque."
                else
                    "Vos morceaux restent sur votre téléphone. HOLOWAVE a seulement besoin de lire les fichiers audio de l'appareil — rien d'autre.",
                style = DecoType.Body, color = Deco.CreamDim, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            if (permanentlyDenied) DecoButton("Ouvrir les réglages", onOpenSettings, Modifier.fillMaxWidth())
            else DecoButton("Ouvrir la discothèque", onRequest, Modifier.fillMaxWidth())
        }
    }
}
