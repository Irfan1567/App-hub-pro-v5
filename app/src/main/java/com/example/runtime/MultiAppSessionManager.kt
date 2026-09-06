package com.example.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.data.db.AppDao
import com.example.data.db.ConsoleLogDao
import com.example.data.db.DownloadDao
import com.example.data.model.ActiveAppSession
import com.example.data.model.AppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class LiveAppSession(
    val appId: String,
    val appName: String,
    val webView: WebView,
    var currentUrl: String,
    var currentTitle: String,
    var isDesktopMode: Boolean,
    var zoomPercent: Int,
    var lastActiveTime: Long,
    var isLoading: Boolean = false,
    var progress: Int = 100,
    var loadError: String? = null
)

class MultiAppSessionManager(
    private val context: Context,
    private val appDao: AppDao,
    private val downloadDao: DownloadDao,
    private val consoleLogDao: ConsoleLogDao
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    val resourceManager = AppResourceManager(context)
    val downloadPipeline = DownloadPipeline(context, downloadDao)

    private val sessions = ConcurrentHashMap<String, LiveAppSession>()

    private val _activeSessionsFlow = MutableStateFlow<List<ActiveAppSession>>(emptyList())
    val activeSessionsFlow: StateFlow<List<ActiveAppSession>> = _activeSessionsFlow.asStateFlow()

    private val _activeAppId = MutableStateFlow<String?>(null)
    val activeAppId: StateFlow<String?> = _activeAppId.asStateFlow()

    companion object {
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 AppHubPro/1.0"
        const val MAX_CONCURRENT_SESSIONS = 8
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateSession(
        app: AppEntity,
        onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { _, _ -> false },
        onCustomViewShown: (View, WebChromeClient.CustomViewCallback) -> Unit = { _, _ -> },
        onCustomViewHidden: () -> Unit = {}
    ): LiveAppSession {
        sessions[app.id]?.let { existing ->
            existing.lastActiveTime = System.currentTimeMillis()
            _activeAppId.value = app.id
            updateSessionList()
            return existing
        }

        // Check if eviction needed
        if (sessions.size >= MAX_CONCURRENT_SESSIONS) {
            evictOldestSession()
        }

        val webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                setGeolocationEnabled(true)
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                displayZoomControls = false
                builtInZoomControls = true
                setSupportZoom(true)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
            }
        }

        val session = LiveAppSession(
            appId = app.id,
            appName = app.name,
            webView = webView,
            currentUrl = AppResourceManager.getAppBaseUrl(app.id, app.entryPoint),
            currentTitle = app.name,
            isDesktopMode = app.isDesktopMode,
            zoomPercent = app.zoomLevelPercent,
            lastActiveTime = System.currentTimeMillis(),
            isLoading = true,
            progress = 10
        )

        applyDesktopMode(webView, app.isDesktopMode)
        applyZoom(webView, app.zoomLevelPercent)

        // Attach safe bridge
        val bridge = AppRuntimeBridge(
            context = context,
            appId = app.id,
            appName = app.name,
            downloadPipeline = downloadPipeline,
            onFullscreenRequested = { /* handled via UI */ },
            onPickFileRequested = { /* handled via UI */ }
        )
        webView.addJavascriptInterface(bridge, "AppHub")

        // Attach WebClients
        webView.webViewClient = AppWebClient(
            appId = app.id,
            appName = app.name,
            resourceManager = resourceManager,
            consoleLogDao = consoleLogDao,
            onPageFinishedListener = { url ->
                session.isLoading = false
                session.progress = 100
                session.currentUrl = url
                session.loadError = null
                updateSessionList()
            },
            onPageErrorListener = { error ->
                session.isLoading = false
                session.loadError = error
                updateSessionList()
            },
            onRendererCrashListener = {
                session.loadError = "WebView Renderer crashed. Reloading session..."
                reloadSession(app.id, hardReload = true)
            }
        )

        webView.webChromeClient = AppWebChromeClient(
            appId = app.id,
            consoleLogDao = consoleLogDao,
            onProgressChangedListener = { p ->
                session.progress = p
                session.isLoading = (p < 100)
                updateSessionList()
            },
            onTitleReceivedListener = { title ->
                session.currentTitle = title
                updateSessionList()
            },
            onShowFileChooserCallback = onFileChooser,
            onCustomViewShownListener = onCustomViewShown,
            onCustomViewHiddenListener = onCustomViewHidden
        )

        // Load initial entry point
        webView.loadUrl(session.currentUrl)

        sessions[app.id] = session
        _activeAppId.value = app.id
        updateSessionList()

        scope.launch(Dispatchers.IO) {
            appDao.updateLastUsed(app.id)
        }

        return session
    }

    fun getSession(appId: String): LiveAppSession? = sessions[appId]

    fun switchToApp(appId: String) {
        sessions[appId]?.let {
            it.lastActiveTime = System.currentTimeMillis()
            _activeAppId.value = appId
            updateSessionList()
            scope.launch(Dispatchers.IO) {
                appDao.updateLastUsed(appId)
            }
        }
    }

    fun closeSession(appId: String) {
        sessions.remove(appId)?.let { session ->
            session.webView.stopLoading()
            session.webView.loadUrl("about:blank")
            session.webView.destroy()
        }
        if (_activeAppId.value == appId) {
            _activeAppId.value = sessions.keys.firstOrNull()
        }
        updateSessionList()
    }

    fun reloadSession(appId: String, hardReload: Boolean = false) {
        sessions[appId]?.let { session ->
            if (hardReload) {
                session.webView.clearCache(true)
            }
            session.isLoading = true
            session.loadError = null
            session.webView.reload()
            updateSessionList()
        }
    }

    fun toggleDesktopMode(appId: String) {
        sessions[appId]?.let { session ->
            val newMode = !session.isDesktopMode
            session.isDesktopMode = newMode
            applyDesktopMode(session.webView, newMode)
            session.webView.reload()
            updateSessionList()
            scope.launch(Dispatchers.IO) {
                appDao.updateDesktopMode(appId, newMode)
            }
        }
    }

    fun setZoom(appId: String, newZoom: Int) {
        sessions[appId]?.let { session ->
            val clamped = newZoom.coerceIn(50, 250)
            session.zoomPercent = clamped
            applyZoom(session.webView, clamped)
            updateSessionList()
            scope.launch(Dispatchers.IO) {
                appDao.updateZoomLevel(appId, clamped)
            }
        }
    }

    fun clearAppData(appId: String) {
        sessions[appId]?.let { session ->
            session.webView.clearCache(true)
            session.webView.clearFormData()
            session.webView.clearHistory()
            session.webView.clearSslPreferences()
            // Clear localStorage & IndexedDB in webview
            session.webView.evaluateJavascript("try { localStorage.clear(); sessionStorage.clear(); } catch(e){}", null)
            reloadSession(appId, hardReload = true)
        }
    }

    private fun applyDesktopMode(webView: WebView, isDesktop: Boolean) {
        webView.settings.apply {
            userAgentString = if (isDesktop) DESKTOP_USER_AGENT else null
            useWideViewPort = isDesktop
            loadWithOverviewMode = isDesktop
        }
    }

    private fun applyZoom(webView: WebView, zoomPercent: Int) {
        webView.setInitialScale(zoomPercent)
    }

    private fun evictOldestSession() {
        val oldest = sessions.values
            .filter { it.appId != _activeAppId.value }
            .minByOrNull { it.lastActiveTime }
            ?: sessions.values.firstOrNull()

        oldest?.let { closeSession(it.appId) }
    }

    private fun updateSessionList() {
        _activeSessionsFlow.value = sessions.values.map {
            ActiveAppSession(
                appId = it.appId,
                appName = it.appName,
                entryUrl = AppResourceManager.getAppBaseUrl(it.appId),
                currentUrl = it.currentUrl,
                title = it.currentTitle,
                isDesktopMode = it.isDesktopMode,
                zoomLevelPercent = it.zoomPercent,
                memoryEstimateMb = 14.5 + (it.zoomPercent / 50.0), // reasonable estimation per active webview
                lastActiveTimestamp = it.lastActiveTime,
                errorCount = if (it.loadError != null) 1 else 0
            )
        }.sortedByDescending { it.lastActiveTimestamp }
    }

    fun destroyAll() {
        sessions.values.forEach { session ->
            session.webView.stopLoading()
            session.webView.destroy()
        }
        sessions.clear()
        updateSessionList()
    }
}
