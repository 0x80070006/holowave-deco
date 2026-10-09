package io.github.x80070006.holowave.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    /** Ask lrclib.net for lyrics that are neither embedded nor cached. */
    val onlineLyrics: Boolean = true,
    /** Keep the record spinning animation (some people find it distracting). */
    val spinningRecord: Boolean = true,
)

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        AppSettings(
            onlineLyrics = prefs.getBoolean(KEY_ONLINE_LYRICS, true),
            spinningRecord = prefs.getBoolean(KEY_SPINNING, true),
        )
    )
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun setOnlineLyrics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ONLINE_LYRICS, enabled).apply()
        _state.value = _state.value.copy(onlineLyrics = enabled)
    }

    fun setSpinningRecord(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPINNING, enabled).apply()
        _state.value = _state.value.copy(spinningRecord = enabled)
    }

    private companion object {
        const val KEY_ONLINE_LYRICS = "online_lyrics"
        const val KEY_SPINNING = "spinning_record"
    }
}
