// Derived from NouTube (https://github.com/nonbili/NouTube, AGPL-3.0): modules/nou-tube-view, NouTubeView.kt and NouJsInterface.kt.
package io.github.x80070006.holowave.tube

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.github.x80070006.holowave.stream.Heard
import io.github.x80070006.holowave.stream.StreamCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.util.UUID

/** The song YouTube Music is playing in the embedded page. */
data class TubeState(
    /** The page is loaded and its player reported a song. */
    val hasTrack: Boolean = false,
    val videoId: String? = null,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val thumbnailUrl: String = "",
    val artwork: Bitmap? = null,
    val playing: Boolean = false,
    /** Last reported position and when it was true ([SystemClock.elapsedRealtime]). */
    val positionMs: Long = 0L,
    val positionAt: Long = 0L,
)

/**
 * YouTube Music, played inside HOLOWAVE by a WebView that lives as long as the app does.
 * The page is the real music.youtube.com with NouTube's content scripts injected (see NOTICE.md);
 * the user's Google session is the WebView's own cookie jar and never leaves the device.
 */
class TubeHost(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val capture: StreamCapture,
    private val onEnded: () -> Unit,
    private val onCover: (title: String, artist: String, cover: Bitmap) -> Unit = { _, _, _ -> },
    private val onLibrary: (List<LibrarySong>) -> Unit = {},
) {
    private val main = Handler(Looper.getMainLooper())
    private val holder = MutableContextWrapper(appContext)
    private val token = UUID.randomUUID().toString()
    private val audio = appContext.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(TubeState())
    val state: StateFlow<TubeState> = _state.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _loggedIn = MutableStateFlow(readLoggedIn())
    /** True when the WebView holds a YouTube session (the SAPISID cookie exists). */
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val mainScript: String by lazy { asset("tube/main.js") }
    private val holoScript: String by lazy { asset("tube/hw.js") }
    private var coverFor: String? = null
    private var recordedId: String? = null

    private fun asset(name: String) = appContext.assets.open(name).bufferedReader().use { it.readText() }

    /** Main thread only. */
    val webView: WebView by lazy { create() }

    private class HoloWebView(context: Context) : WebView(context) {
        // The page must believe it is on screen: YouTube pauses what it thinks is hidden.
        override fun onWindowVisibilityChanged(visibility: Int) = super.onWindowVisibilityChanged(VISIBLE)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun create(): WebView {
        val view = HoloWebView(holder)
        view.settings.run {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(false)
            // Nothing from the page may reach the device's files.
            allowFileAccess = false
            allowContentAccess = false
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) view.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        setUserAgent(view, USER_AGENT)
        view.addJavascriptInterface(Bridge(), "NouTubeI")
        view.webViewClient = object : WebViewClient() {
            override fun doUpdateVisitedHistory(v: WebView, url: String, isReload: Boolean) {
                _url.value = url
                _canGoBack.value = v.canGoBack()
                refreshLogin()
            }

            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                if (!isYouTubeHost(Uri.parse(url).host)) return
                v.evaluateJavascript("window.isAndroid=true;window.NouTubeToken='$token';window.NouTubeBackground=false;$mainScript;$holoScript", null)
            }

            override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse? =
                if (request.url.host in BLOCKED_HOSTS) WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))) else null

            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && (isYouTubeHost(uri.host) || isGoogleSignInHost(uri.host))) return false
                // Anything else (an artist's site, a store) opens outside, never inside the player.
                if (uri.scheme == "https" || uri.scheme == "http") {
                    runCatching { v.context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                return true
            }
        }
        return view
    }

    // The user agent string passes for Chrome, but the client hints would still announce
    // "Android WebView" and Google may refuse the sign-in. Report the Chrome the string names.
    private fun setUserAgent(view: WebView, ua: String) {
        view.settings.userAgentString = ua
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
        val default = WebSettingsCompat.getUserAgentMetadata(view.settings)
        val version = Regex("""Chrome/(\d+)""").find(ua)?.groupValues?.get(1) ?: return
        val brands = default.brandVersionList.map { brand ->
            val b = UserAgentMetadata.BrandVersion.Builder(brand)
            when (brand.brand) {
                "Android WebView" -> b.setBrand("Google Chrome").setMajorVersion(version).setFullVersion("$version.0.0.0")
                "Chromium" -> b.setMajorVersion(version).setFullVersion("$version.0.0.0")
            }
            b.build()
        }
        WebSettingsCompat.setUserAgentMetadata(
            view.settings,
            UserAgentMetadata.Builder(default).setBrandVersionList(brands).setFullVersion("$version.0.0.0").setMobile(true).build(),
        )
    }

    /** The messages of the injected scripts. Called on a WebView thread: the page is untrusted. */
    private inner class Bridge {
        @JavascriptInterface
        fun onMessage(payload: String) {
            when (val e = TubeMessages.parse(payload) ?: return) {
                is TubeEvent.Progress -> main.post { onProgress(e) }
                is TubeEvent.PlayState -> main.post { onPlayState(e.playing) }
                TubeEvent.Ended -> main.post {
                    capture.stop(System.currentTimeMillis())
                    onEnded()
                }
                is TubeEvent.Loaded -> main.post {
                    refreshLogin()
                    maybeAutoImport()
                }
                is TubeEvent.Library -> {
                    imported += e.songs.size
                    onLibrary(e.songs)
                }
                is TubeEvent.LibraryDone -> main.post {
                    _importStatus.value = when {
                        e.error == "signed-out" -> "Connectez-vous pour importer vos titres."
                        e.error != null -> "Import impossible pour l'instant (${e.error})."
                        e.count == 0 -> "Aucun titre trouvé dans votre historique."
                        else -> "${e.count} titres importés depuis votre historique."
                    }
                    if (e.error == null && e.count > 0) prefs.edit().putLong(KEY_IMPORTED_AT, System.currentTimeMillis()).apply()
                }
            }
        }

        @JavascriptInterface
        fun notify(title: String, author: String, seconds: Long, thumbnail: String) {
            main.post {
                val s = _state.value
                _state.value = s.copy(
                    hasTrack = title.isNotBlank(), title = title.take(300), artist = author.take(200),
                    durationMs = seconds.coerceIn(0, 86_400) * 1000, thumbnailUrl = thumbnail.take(500),
                    artwork = if (thumbnail == s.thumbnailUrl) s.artwork else null,
                )
                loadCover(thumbnail)
            }
        }

        @JavascriptInterface
        fun notifyProgress(playing: Boolean, pos: Long) {
            main.post {
                _state.value = _state.value.copy(playing = playing, positionMs = (pos * 1000).coerceAtLeast(0), positionAt = SystemClock.elapsedRealtime())
            }
        }

        // The background guard asks before resuming a pause YouTube caused: never during a call.
        @JavascriptInterface
        fun canAutoResume(t: String?): Boolean = t == token && audio.mode == AudioManager.MODE_NORMAL
    }

    private fun onProgress(e: TubeEvent.Progress) {
        val s = _state.value
        _state.value = s.copy(
            hasTrack = true, videoId = e.videoId, title = e.title.ifBlank { s.title }, artist = e.author.ifBlank { s.artist },
            durationMs = if (e.durationSec > 0) (e.durationSec * 1000).toLong() else s.durationMs,
            positionMs = (e.currentSec * 1000).toLong(), positionAt = SystemClock.elapsedRealtime(),
        )
        loadCover(e.thumbnail)
        if (recordedId != e.videoId) {
            recordedId = e.videoId
            val st = _state.value
            capture.onMetadata(Heard(st.title, st.artist, "", st.durationMs, null, e.videoId), System.currentTimeMillis(), st.playing)
        }
    }

    private fun onPlayState(playing: Boolean) {
        _state.value = _state.value.copy(playing = playing, positionAt = SystemClock.elapsedRealtime())
        capture.onPlayback(playing, System.currentTimeMillis())
    }

    /** Fetches the cover once per song (largest size, square), from YouTube's image hosts only. */
    private fun loadCover(url: String) {
        if (url.isBlank() || url == coverFor || !TubeMessages.isCoverUrl(url)) return
        coverFor = url
        scope.launch(Dispatchers.IO) {
            val bmp = Covers.fetch(url) ?: return@launch
            main.post {
                val s = _state.value
                if (s.thumbnailUrl == url || s.thumbnailUrl.isBlank()) {
                    _state.value = s.copy(artwork = bmp)
                    // The cover arrives after the song was announced: hand it to the recorder and the list.
                    s.videoId?.let {
                        capture.onMetadata(Heard(s.title, s.artist, "", s.durationMs, bmp, it), System.currentTimeMillis(), s.playing)
                        onCover(s.title, s.artist, bmp)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- controls (main thread)

    private fun js(code: String) = main.post { runCatching { webView.evaluateJavascript(code, null) } }

    fun play() { js("window.NouTube&&window.NouTube.play()") }
    fun pause() { js("window.NouTube&&window.NouTube.pause()") }
    fun next() { js("window.NouTube&&window.NouTube.skipToNext()") }
    fun previous() { js("window.NouTube&&window.NouTube.prev()") }
    fun seekTo(ms: Long) { js("window.NouTube&&window.NouTube.seekTo(${ms.coerceAtLeast(0) / 1000.0})") }

    /** Loads [url] if it is a YouTube Music page. */
    fun open(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host?.lowercase() != "music.youtube.com") return
        main.post { webView.loadUrl(url) }
    }

    fun goBack() = main.post { if (webView.canGoBack()) webView.goBack() }
    fun goHome() = main.post { webView.loadUrl(HOME) }
    fun goLibrary() = main.post { webView.loadUrl("https://music.youtube.com/library") }
    fun goSearch() = main.post { webView.loadUrl("https://music.youtube.com/search") }
    fun reload() = main.post { webView.reload() }

    fun watch(videoId: String) { open("https://music.youtube.com/watch?v=$videoId") }

    /** The Google sign-in page that comes back to YouTube Music. */
    fun openSignIn() = main.post {
        webView.loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F")
    }

    fun signOut() = main.post {
        CookieManager.getInstance().removeAllCookies { refreshLogin() }
        CookieManager.getInstance().flush()
        webView.loadUrl("https://music.youtube.com/")
        _state.value = TubeState()
    }

    // ---------------------------------------------------------------- hosting in a view tree

    private var anchor: ViewGroup? = null

    /**
     * Puts the WebView in [container] (taking it from where it was); [activity] is used for dialogs.
     * The [isAnchor] container is where it rests, invisible, when no screen shows it.
     */
    fun attach(container: ViewGroup, activity: Context, isAnchor: Boolean = false) {
        holder.setBaseContext(activity)
        if (isAnchor) anchor = container
        if (webView.url == null) webView.loadUrl(HOME)
        (webView.parent as? ViewGroup)?.takeIf { it !== container }?.removeView(webView)
        if (webView.parent == null) container.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun detach(container: ViewGroup) {
        if (anchor === container) anchor = null
        if (webView.parent === container) {
            container.removeView(webView)
            // Back to the resting place, so the page (and the music) is never left without a window.
            anchor?.takeIf { it.isAttachedToWindow }?.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    // ---------------------------------------------------------------- account library

    private val prefs = appContext.getSharedPreferences("tube", Context.MODE_PRIVATE)
    @Volatile private var imported = 0
    private val _importStatus = MutableStateFlow<String?>(null)
    /** What the last import of the account's most played songs gave, for the account screen. */
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    /** Asks the page for the account's 100 most played songs (see hw.js); they arrive as [TubeEvent.Library]. */
    fun importTop() {
        imported = 0
        _importStatus.value = "Import en cours…"
        js("window.HoloWave&&window.HoloWave.importTop(100)")
    }

    /** Once signed in, the Top 100 is refreshed on its own, at most twice a day. */
    private fun maybeAutoImport() {
        if (!_loggedIn.value) return
        val last = prefs.getLong(KEY_IMPORTED_AT, 0L)
        if (System.currentTimeMillis() - last < AUTO_IMPORT_EVERY_MS) return
        val page = _url.value
        if (!page.startsWith("https://music.youtube.com/")) return
        main.postDelayed({ importTop() }, 4_000)
    }

    // ---------------------------------------------------------------- account

    private fun readLoggedIn(): Boolean =
        CookieManager.getInstance().getCookie("https://music.youtube.com")?.contains("SAPISID=") == true

    private fun refreshLogin() { _loggedIn.value = readLoggedIn() }

    companion object {
        // Same as NouTube: a recent mobile Chrome.
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Mobile Safari/537.36"
        const val HOME = "https://music.youtube.com/"
        private const val KEY_IMPORTED_AT = "top_imported_at"
        private const val AUTO_IMPORT_EVERY_MS = 12 * 3_600_000L
        private val BLOCKED_HOSTS = setOf("www.googletagmanager.com", "googleads.g.doubleclick.net")

        fun isYouTubeHost(host: String?): Boolean {
            val h = host?.lowercase() ?: return false
            return h == "youtube.com" || h.endsWith(".youtube.com") || h == "youtu.be"
        }

        fun isGoogleSignInHost(host: String?): Boolean {
            val h = host?.lowercase() ?: return false
            return h.startsWith("accounts.google.") || h.startsWith("gds.google.") || h == "myaccount.google.com"
        }
    }
}
