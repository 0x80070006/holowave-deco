package io.github.x80070006.holowave.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Persists the appearance choices in a DataStore and mirrors them into [Appearance].
 *
 * Upgrading from v2 needs no migration code: v2 stored no appearance at all, so a user
 * who had the Art Déco look simply lands on the default theme.
 */
class AppearanceStore(context: Context) {

    // Its own I/O scope: the blocking read below runs on the main thread, so DataStore must
    // never need the main dispatcher (that would deadlock the very first frame).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val store = PreferenceDataStoreFactory.create(scope = scope) {
        File(context.filesDir, "datastore/appearance.preferences_pb")
    }

    /** Reads the saved choices before the first frame, so the app never flashes the wrong theme. */
    fun restoreBlocking() {
        val p = runCatching { runBlocking { store.data.first() } }.getOrNull() ?: return
        Appearance.themeId = p[THEME]?.let { name -> ThemeId.entries.firstOrNull { it.name == name } } ?: ThemeId.HOLOWAVE
        Appearance.mode = p[MODE]?.let { name -> ModePref.entries.firstOrNull { it.name == name } } ?: ModePref.SYSTEM
        Appearance.monoAccent = p[MONO]?.let { name -> MonoAccent.entries.firstOrNull { it.name == name } } ?: MonoAccent.VIOLET
        Appearance.coverColors = p[COVER] ?: true
        Appearance.motion = p[MOTION]?.let { name -> MotionPref.entries.firstOrNull { it.name == name } } ?: MotionPref.FULL
    }

    fun setTheme(id: ThemeId) {
        Appearance.themeId = id
        save { it[THEME] = id.name }
    }

    fun setMode(mode: ModePref) {
        Appearance.mode = mode
        save { it[MODE] = mode.name }
    }

    fun setMonoAccent(accent: MonoAccent) {
        Appearance.monoAccent = accent
        save { it[MONO] = accent.name }
    }

    fun setCoverColors(enabled: Boolean) {
        Appearance.coverColors = enabled
        save { it[COVER] = enabled }
    }

    fun setMotion(motion: MotionPref) {
        Appearance.motion = motion
        save { it[MOTION] = motion.name }
    }

    private fun save(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch(Dispatchers.IO) { store.edit { block(it) } }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
        val MONO = stringPreferencesKey("mono_accent")
        val COVER = booleanPreferencesKey("cover_colors")
        val MOTION = stringPreferencesKey("motion")
    }
}
