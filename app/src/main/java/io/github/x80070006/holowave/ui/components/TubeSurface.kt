package io.github.x80070006.holowave.ui.components

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.x80070006.holowave.tube.TubeHost

/**
 * Shows the embedded YouTube Music page. One page exists for the whole app: the anchor instance
 * (tiny, invisible) keeps it attached to a window so that it keeps playing when no screen shows it,
 * and the visible instance of a screen borrows it for as long as the screen is on.
 */
@Composable
fun TubeSurface(host: TubeHost, modifier: Modifier = Modifier, anchor: Boolean = false) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx -> FrameLayout(ctx).also { host.attach(it, context, isAnchor = anchor) } },
        onRelease = { host.detach(it as FrameLayout) },
        modifier = modifier,
    )
}
