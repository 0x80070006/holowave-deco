package io.github.x80070006.holowave

import android.app.Application
import io.github.x80070006.holowave.data.ArtworkLoader
import io.github.x80070006.holowave.data.MediaLibrary
import io.github.x80070006.holowave.data.PlaylistStore
import io.github.x80070006.holowave.data.Settings
import io.github.x80070006.holowave.lyrics.LrcLibClient
import io.github.x80070006.holowave.lyrics.LyricsRepository
import io.github.x80070006.holowave.playback.PlayerConnection
import kotlinx.coroutines.MainScope

/** Manual dependency container: a handful of singletons, no reflection, no DI framework. */
class HolowaveApp : Application() {
    val appScope = MainScope()

    val settings by lazy { Settings(this) }
    val library by lazy { MediaLibrary(contentResolver, appScope) }
    val playlists by lazy { PlaylistStore(filesDir, appScope).also { it.load() } }
    val artwork by lazy { ArtworkLoader(contentResolver) }
    val lyrics by lazy { LyricsRepository(contentResolver, filesDir, LrcLibClient()) }
    val player by lazy { PlayerConnection(this, appScope) }
}
