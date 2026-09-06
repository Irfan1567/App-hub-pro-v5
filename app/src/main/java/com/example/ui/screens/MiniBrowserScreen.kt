package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError
import com.example.ui.theme.NeomorphicCard
import com.example.ui.theme.isDarkTheme
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class QuickBookmark(
    val title: String,
    val url: String,
    val category: String,
    val color: Color
)

val PRESET_BOOKMARKS = listOf(
    QuickBookmark("Google", "https://www.google.com", "Search", Color(0xFF0284C7)),
    QuickBookmark("GitHub", "https://github.com", "Code", Color(0xFF64748B)),
    QuickBookmark("MDN Web", "https://developer.mozilla.org", "Docs", ElectricViolet),
    QuickBookmark("CodePen", "https://codepen.io/pen/", "Editor", EmeraldGlow),
    QuickBookmark("JSFiddle", "https://jsfiddle.net", "Editor", Color(0xFF38BDF8)),
    QuickBookmark("HTML5 Test", "https://html5test.co", "Testing", Color(0xFFF59E0B)),
    QuickBookmark("Localhost:3000", "http://localhost:3000", "Dev Server", Color(0xFF10B981)),
    QuickBookmark("Localhost:8080", "http://localhost:8080", "Dev Server", Color(0xFFEC4899))
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniBrowserScreen(
    viewModel: AppViewModel,
    onSaveAsApp: (title: String, htmlContent: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val savedUrl by viewModel.miniBrowserUrl.collectAsState()
    val browserHistory by viewModel.browserHistory.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentDisplayUrl by remember { mutableStateOf(if (savedUrl == "about:blank") "" else savedUrl) }
    var inputUrlText by remember { mutableStateOf(if (savedUrl == "about:blank") "" else savedUrl) }
    var pageTitle by remember { mutableStateOf("Mini Browser") }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var showStartPage by remember { mutableStateOf(savedUrl == "about:blank" || savedUrl.isBlank()) }
    var isSecure by remember { mutableStateOf(false) }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        if (trimmed.startsWith("localhost:") || trimmed.startsWith("127.0.0.1:")) {
            return "http://$trimmed"
        }
        // Fallback to Google search
        val query = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
        return "https://www.google.com/search?q=$query"
    }

    fun navigateToUrl(target: String) {
        val normalized = normalizeUrl(target)
        inputUrlText = normalized
        currentDisplayUrl = normalized
        showStartPage = (normalized == "about:blank")
        viewModel.setMiniBrowserUrl(normalized)
        webViewRef?.loadUrl(normalized)
    }

    // Android back navigation: go back in WebView if possible
    BackHandler(enabled = canGoBack && !showStartPage) {
        webViewRef?.let {
            if (it.canGoBack()) {
                it.goBack()
            } else {
                showStartPage = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BROWSER APP BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                // Address Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activePrimary = MaterialTheme.colorScheme.primary

                    // Back & Forward
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack && !showStartPage,
                        modifier = Modifier.size(36.dp).testTag("browser_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack && !showStartPage) activePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward && !showStartPage,
                        modifier = Modifier.size(36.dp).testTag("browser_forward_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward && !showStartPage) activePrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    // URL Input Box
                    OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("browser_url_input"),
                        placeholder = {
                            Text("Enter URL or search...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        leadingIcon = {
                            if (isSecure) {
                                Icon(Icons.Default.Lock, contentDescription = "Secure", tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Language, contentDescription = "Web", tint = activePrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        trailingIcon = {
                            if (inputUrlText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputUrlText = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { navigateToUrl(inputUrlText) }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Reload or Stop Button
                    IconButton(
                        onClick = {
                            if (isLoading) {
                                webViewRef?.stopLoading()
                            } else {
                                if (showStartPage && inputUrlText.isNotBlank()) {
                                    navigateToUrl(inputUrlText)
                                } else {
                                    webViewRef?.reload()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp).testTag("browser_reload_btn")
                    ) {
                        if (isLoading) {
                            Icon(Icons.Default.Close, contentDescription = "Stop", tint = RoseError)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = activePrimary)
                        }
                    }

                    // Home Button
                    IconButton(
                        onClick = {
                            showStartPage = true
                            inputUrlText = ""
                            viewModel.setMiniBrowserUrl("about:blank")
                        },
                        modifier = Modifier.size(36.dp).testTag("browser_home_btn")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home Page", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Sub-Toolbar with Tools & Status
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activePrimary = MaterialTheme.colorScheme.primary
                    // Page Title or Host
                    Text(
                        text = if (showStartPage) "⚡ Quick Start & Dev Tools" else pageTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Desktop / Mobile Mode Toggle
                        IconButton(
                            onClick = {
                                isDesktopMode = !isDesktopMode
                                webViewRef?.settings?.let { s ->
                                    if (isDesktopMode) {
                                        s.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                                    } else {
                                        s.userAgentString = null // default mobile
                                    }
                                }
                                webViewRef?.reload()
                                Toast.makeText(context, if (isDesktopMode) "Desktop Mode Enabled" else "Mobile Mode Enabled", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp).testTag("browser_desktop_toggle_btn")
                        ) {
                            Icon(
                                if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                                contentDescription = "Desktop Mode Toggle",
                                tint = if (isDesktopMode) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clone / Save Web Page to App Hub Local Workspace
                        IconButton(
                            onClick = {
                                if (showStartPage || webViewRef == null) {
                                    Toast.makeText(context, "Open a web page first to save as app", Toast.LENGTH_SHORT).show()
                                } else {
                                    webViewRef?.evaluateJavascript(
                                        "(function() { return document.documentElement.outerHTML; })();"
                                    ) { html ->
                                        if (!html.isNullOrBlank() && html != "null") {
                                            val cleaned = html.trim().let {
                                                if (it.startsWith("\"") && it.endsWith("\"")) {
                                                    // unescape JSON string
                                                    try {
                                                        org.json.JSONTokener(it).nextValue().toString()
                                                    } catch (e: Exception) {
                                                        it.removeSurrounding("\"")
                                                    }
                                                } else it
                                            }
                                            val appName = if (pageTitle.isNotBlank() && pageTitle != "Mini Browser") pageTitle else "Cloned Web App"
                                            onSaveAsApp(appName, cleaned)
                                            Toast.makeText(context, "Page captured and created as local app!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not extract page HTML", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("browser_save_app_btn")
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save as Local App",
                                tint = EmeraldGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // External Browser Launcher
                        IconButton(
                            onClick = {
                                if (currentDisplayUrl.isNotBlank() && currentDisplayUrl != "about:blank") {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentDisplayUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open external browser", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = "Open in Chrome/System Browser",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Progress bar
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = CyberCyan,
                    trackColor = Color.Transparent
                )
            }
        }

        // CONTENT AREA: START PAGE OR WEBVIEW
        Box(modifier = Modifier.fillMaxSize()) {
            if (showStartPage) {
                // START PAGE WITH BOOKMARKS AND HISTORY
                BrowserStartPage(
                    onSelectBookmark = { url ->
                        navigateToUrl(url)
                    },
                    history = browserHistory,
                    onClearHistory = { viewModel.clearBrowserHistory() }
                )
            } else {
                // REAL LIVE WEBVIEW
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowFileAccess = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let {
                                        currentDisplayUrl = it
                                        inputUrlText = it
                                        isSecure = it.startsWith("https://")
                                    }
                                    canGoBack = canGoBack()
                                    canGoForward = canGoForward()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    loadProgress = 1f
                                    url?.let {
                                        currentDisplayUrl = it
                                        inputUrlText = it
                                        viewModel.setMiniBrowserUrl(it)
                                        isSecure = it.startsWith("https://")
                                    }
                                    pageTitle = view?.title ?: "Web Page"
                                    canGoBack = canGoBack()
                                    canGoForward = canGoForward()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    val scheme = uri.scheme ?: return false
                                    if (scheme == "http" || scheme == "https") {
                                        return false
                                    }
                                    // Handle non-web schemes (mailto, tel, intent)
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                        true
                                    } catch (e: Exception) {
                                        true
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress / 100f
                                    if (newProgress >= 100) {
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank()) {
                                        pageTitle = title
                                    }
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    viewModel.pendingFileChooserCallback = filePathCallback
                                    return true
                                }
                            }

                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                try {
                                    val uri = Uri.parse(url)
                                    val req = android.app.DownloadManager.Request(uri).apply {
                                        setMimeType(mimetype)
                                        addRequestHeader("User-Agent", userAgent)
                                        setDescription("Downloading web file...")
                                        setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype))
                                        setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        setDestinationInExternalFilesDir(ctx, android.os.Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype))
                                    }
                                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                    dm.enqueue(req)
                                    Toast.makeText(ctx, "Download started...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Download: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            webViewRef = this
                            if (currentDisplayUrl.isNotBlank() && currentDisplayUrl != "about:blank") {
                                loadUrl(currentDisplayUrl)
                            }
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun BrowserStartPage(
    onSelectBookmark: (String) -> Unit,
    history: List<String>,
    onClearHistory: () -> Unit
) {
    val scrollState = rememberScrollState()

    val activePrimary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Hero Header Card
        NeomorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(activePrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = activePrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("AppHub Mini Browser", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Universal HTML Web Viewer & Inspector", fontSize = 12.sp, color = activePrimary)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Browse live websites, test localhost dev servers, inspect HTML5 apps, and clone online pages directly into your local App Hub workspace with 1 click!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Launch Bookmarks Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = activePrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("QUICK LAUNCH & DEVELOPER PORTALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activePrimary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bookmarks Grid (2 columns)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PRESET_BOOKMARKS.chunked(2).forEach { rowBookmarks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowBookmarks.forEach { item ->
                        NeomorphicCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectBookmark(item.url) },
                            shape = RoundedCornerShape(14.dp),
                            elevation = 3.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Box(
                                        modifier = Modifier
                                            .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(item.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = item.color)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    item.url,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (rowBookmarks.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent History
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = activePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RECENTLY VISITED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activePrimary)
                }
                TextButton(onClick = onClearHistory) {
                    Text("Clear", fontSize = 11.sp, color = RoseError)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            NeomorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    history.forEachIndexed { index, url ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBookmark(url) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = activePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                url,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < history.size - 1) {
                            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))
                        }
                    }
                }
            }
        }
    }
}
