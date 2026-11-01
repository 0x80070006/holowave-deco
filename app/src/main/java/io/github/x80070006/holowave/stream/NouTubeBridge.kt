package io.github.x80070006.holowave.stream

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.x80070006.holowave.data.StreamedTrack

/**
 * The only door between HOLOWAVE and NouTube: public intents. HOLOWAVE never reads
 * NouTube's data and never plays YouTube itself; it asks NouTube to open a song or a search.
 */
object NouTubeBridge {
    const val PACKAGE = "jp.nonbili.noutube"
    const val FDROID_URL = "https://f-droid.org/packages/jp.nonbili.noutube"

    private val videoId = Regex("^[A-Za-z0-9_-]{11}$")

    fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(PACKAGE, 0) }.isSuccess

    /** The song's page when its id is known (and well-formed), a search for it otherwise. */
    fun trackUri(track: StreamedTrack): Uri =
        track.videoId?.takeIf { videoId.matches(it) }?.let {
            Uri.Builder().scheme("https").authority("music.youtube.com").appendPath("watch").appendQueryParameter("v", it).build()
        } ?: searchUri("${track.artist} ${track.title}")

    fun searchUri(query: String): Uri =
        Uri.Builder().scheme("https").authority("music.youtube.com").appendPath("search")
            .appendQueryParameter("q", query.trim().take(200)).build()

    /** Opens [uri] in NouTube directly (no chooser); false when NouTube is not installed. */
    fun open(context: Context, uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** Android's "notification access", needed to see NouTube's media session. */
object NotificationAccess {
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** The system page where the user switches HOLOWAVE on. */
    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
