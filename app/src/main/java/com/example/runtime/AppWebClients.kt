package com.example.runtime

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.db.ConsoleLogDao
import com.example.data.model.ConsoleLogEntity
import com.example.data.model.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppWebClient(
    private val appId: String,
    private val appName: String,
    private val resourceManager: AppResourceManager,
    private val consoleLogDao: ConsoleLogDao,
    private val onPageFinishedListener: (String) -> Unit = {},
    private val onPageErrorListener: (String) -> Unit = {},
    private val onRendererCrashListener: () -> Unit = {}
) : WebViewClient() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url ?: return null
        val response = resourceManager.handleInterceptedRequest(uri, appId)
        if (response != null && response.statusCode >= 400) {
            scope.launch {
                consoleLogDao.insertLog(
                    ConsoleLogEntity(
                        appId = appId,
                        severity = LogSeverity.NETWORK,
                        message = "Failed to load resource [${response.statusCode}]: ${uri.path}",
                        sourceId = uri.toString()
                    )
                )
            }
        }
        return response
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view != null && url != null) {
            // Inject non-destructive download observer so <a download> and blob: downloads automatically route through AppHub
            injectDownloadHelper(view)
            onPageFinishedListener(url)
        }
    }

    private fun injectDownloadHelper(view: WebView) {
        val script = """
            (function() {
                if (window.__apphub_download_hooked) return;
                window.__apphub_download_hooked = true;
                
                document.addEventListener('click', function(e) {
                    let target = e.target;
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    if (!target || !target.hasAttribute('download')) return;
                    
                    const href = target.getAttribute('href');
                    const filename = target.getAttribute('download') || 'download';
                    if (!href) return;
                    
                    if (href.startsWith('blob:') && window.AppHub) {
                        e.preventDefault();
                        fetch(href)
                            .then(res => res.blob())
                            .then(blob => {
                                const reader = new FileReader();
                                reader.onloadend = function() {
                                    window.AppHub.download(filename, reader.result, blob.type || 'application/octet-stream');
                                };
                                reader.readAsDataURL(blob);
                            })
                            .catch(err => console.error('[AppHub] Blob download extraction error:', err));
                    } else if (href.startsWith('data:') && window.AppHub) {
                        e.preventDefault();
                        window.AppHub.download(filename, href, 'application/octet-stream');
                    }
                }, true);
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: "Network/Page load error"
            onPageErrorListener(description)
        }
        scope.launch {
            consoleLogDao.insertLog(
                ConsoleLogEntity(
                    appId = appId,
                    severity = LogSeverity.ERROR,
                    message = "Resource error: ${error?.description} (${request?.url})",
                    sourceId = request?.url?.toString() ?: ""
                )
            )
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        scope.launch {
            consoleLogDao.insertLog(
                ConsoleLogEntity(
                    appId = appId,
                    severity = LogSeverity.NETWORK,
                    message = "HTTP ${errorResponse?.statusCode}: ${request?.url}",
                    sourceId = request?.url?.toString() ?: ""
                )
            )
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        onRendererCrashListener()
        return true // Prevent host app crash!
    }
}

class AppWebChromeClient(
    private val appId: String,
    private val consoleLogDao: ConsoleLogDao,
    private val onProgressChangedListener: (Int) -> Unit = {},
    private val onTitleReceivedListener: (String) -> Unit = {},
    private val onShowFileChooserCallback: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean = { _, _ -> false },
    private val onCustomViewShownListener: (View, WebChromeClient.CustomViewCallback) -> Unit = { _, _ -> },
    private val onCustomViewHiddenListener: () -> Unit = {}
) : WebChromeClient() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChangedListener(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            onTitleReceivedListener(title)
        }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            val severity = when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> LogSeverity.ERROR
                ConsoleMessage.MessageLevel.WARNING -> LogSeverity.WARN
                else -> LogSeverity.INFO
            }
            scope.launch {
                consoleLogDao.insertLog(
                    ConsoleLogEntity(
                        appId = appId,
                        severity = severity,
                        message = consoleMessage.message() ?: "",
                        sourceId = consoleMessage.sourceId() ?: "",
                        lineNumber = consoleMessage.lineNumber()
                    )
                )
            }
        }
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onShowFileChooserCallback(filePathCallback, fileChooserParams)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            onCustomViewShownListener(view, callback)
        } else {
            super.onShowCustomView(view, callback)
        }
    }

    override fun onHideCustomView() {
        onCustomViewHiddenListener()
        super.onHideCustomView()
    }
}
