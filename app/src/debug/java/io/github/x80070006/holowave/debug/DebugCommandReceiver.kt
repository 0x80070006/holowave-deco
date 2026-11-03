package io.github.x80070006.holowave.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.x80070006.holowave.HolowaveApp
import io.github.x80070006.holowave.playback.SourceKind

/**
 * Debug builds only. `adb shell am broadcast -n <pkg>/.debug.DebugCommandReceiver --es cmd pause`
 * sends a command to the source the app follows (or `--es src noutube|local` to pick one).
 * Commands: state, play, pause, next, previous, seek (with `--el ms 30000`).
 */
class DebugCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val router = (context.applicationContext as HolowaveApp).sources
        val source = when (intent.getStringExtra("src")) {
            "noutube" -> router.source(SourceKind.NOUTUBE)
            "local" -> router.source(SourceKind.LOCAL)
            else -> router.activeSource
        }
        when (val cmd = intent.getStringExtra("cmd")) {
            "play" -> source.play()
            "pause" -> source.pause()
            "next" -> source.next()
            "previous" -> source.previous()
            "seek" -> source.seekTo(intent.getLongExtra("ms", 0L))
            "import" -> (context.applicationContext as HolowaveApp).tubeHost.importTop()
            "state", null -> Unit
            else -> Log.w(TAG, "unknown command $cmd")
        }
        val s = source.state.value
        Log.i(TAG, "active=${router.active.value} src=${source.kind} available=${s.available} playing=${s.isPlaying} " +
            "title='${s.title}' artist='${s.artist}' dur=${s.durationMs} pos=${source.position()}")
    }

    private companion object { const val TAG = "HwDebug" }
}
