package io.github.x80070006.holowave

import android.app.Application
import io.github.x80070006.holowave.data.ArtworkLoader
import io.github.x80070006.holowave.data.AudioScanner
import io.github.x80070006.holowave.data.MediaLibrary
import io.github.x80070006.holowave.data.PlaylistStore
import io.github.x80070006.holowave.data.Settings
import io.github.x80070006.holowave.history.HolowaveDb
import io.github.x80070006.holowave.history.ListenLog
import io.github.x80070006.holowave.history.StreamedStore
import io.github.x80070006.holowave.lyrics.LrcLibClient
import io.github.x80070006.holowave.lyrics.LyricsRepository
import io.github.x80070006.holowave.playback.LocalSource
import io.github.x80070006.holowave.playback.PlayerConnection
import io.github.x80070006.holowave.playback.SourceRouter
import io.github.x80070006.holowave.stream.NouTubeSource
import io.github.x80070006.holowave.stream.StreamCapture
import io.github.x80070006.holowave.stream.StreamSink
import io.github.x80070006.holowave.tube.TubeHost
import io.github.x80070006.holowave.tube.TubeSource
import io.github.x80070006.holowave.tube.YoutubeSource
import io.github.x80070006.holowave.ui.theme.AppearanceStore
import kotlinx.coroutines.MainScope

/** Manual dependency container: a handful of singletons, no reflection, no DI framework. */
class HolowaveApp : Application() {
    val appScope = MainScope()

    val settings by lazy { Settings(this) }
    val library by lazy { MediaLibrary(contentResolver, appScope, AudioScanner(this)) { settings.state.value.videoContainers } }
    val playlists by lazy { PlaylistStore(filesDir, appScope).also { it.load() } }
    val artwork by lazy { ArtworkLoader(contentResolver) }
    val lyrics by lazy { LyricsRepository(contentResolver, filesDir, LrcLibClient()) }
    val player by lazy { PlayerConnection(this, appScope) }
    val localSource by lazy { LocalSource(player, appScope) }
    /** NouTube, the app: seen through notification access. */
    val noutubeSource by lazy { NouTubeSource() }
    /** YouTube Music played inside HOLOWAVE. */
    val tubeHost by lazy { TubeHost(this, appScope, StreamCapture { heard, listen -> StreamSink(this, embedded = true).finished(heard, listen) }, { youtubeNext() }, { t, a, b -> streamed.updateArtwork(t, a, b) }, { songs -> streamed.importRanked(songs, System.currentTimeMillis()) }) }
    val tubeSource by lazy { TubeSource(tubeHost, appScope) }
    val youtubeSource by lazy { YoutubeSource(tubeSource, noutubeSource, appScope) }
    val sources by lazy { SourceRouter(localSource, youtubeSource) }

    private val _youtubeEnded = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when the embedded page reaches the end of a song. */
    val youtubeEnded: kotlinx.coroutines.flow.SharedFlow<Unit> = _youtubeEnded
    private fun youtubeNext() { _youtubeEnded.tryEmit(Unit) }
    val appearance by lazy { AppearanceStore(this) }
    private val db by lazy { HolowaveDb.open(this) }
    val history by lazy { ListenLog(this, db, appScope) }
    val streamed by lazy { StreamedStore(this, db, appScope) }

    override fun onCreate() {
        super.onCreate()
        sources.start(appScope)
    }
}
