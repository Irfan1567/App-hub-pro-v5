package com.example.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AppRuntimeBridge(
    private val context: Context,
    private val appId: String,
    private val appName: String,
    private val downloadPipeline: DownloadPipeline,
    private val onFullscreenRequested: (Boolean) -> Unit,
    private val onPickFileRequested: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    @JavascriptInterface
    fun showToast(message: String?) {
        if (message.isNullOrBlank()) return
        scope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun download(fileName: String?, dataOrUrl: String?, mimeType: String?) {
        if (fileName.isNullOrBlank() || dataOrUrl.isNullOrBlank()) return
        val safeName = fileName.trim()
        val safeMime = mimeType?.trim() ?: "application/octet-stream"

        scope.launch(Dispatchers.IO) {
            try {
                if (dataOrUrl.startsWith("data:")) {
                    downloadPipeline.handleDownload(
                        appId = appId,
                        appName = appName,
                        url = dataOrUrl,
                        mimeType = safeMime,
                        suggestedFileName = safeName
                    )
                } else if (dataOrUrl.startsWith("http://") || dataOrUrl.startsWith("https://")) {
                    downloadPipeline.handleDownload(
                        appId = appId,
                        appName = appName,
                        url = dataOrUrl,
                        mimeType = safeMime,
                        suggestedFileName = safeName
                    )
                } else {
                    // Raw Base64 or plain string
                    downloadPipeline.saveBase64Download(
                        appId = appId,
                        appName = appName,
                        fileName = safeName,
                        base64Data = dataOrUrl,
                        mimeType = safeMime
                    )
                }
            } catch (e: Exception) {
                // Bridge methods must never crash
            }
        }
    }

    @JavascriptInterface
    fun shareFile(textOrUrl: String?) {
        if (textOrUrl.isNullOrBlank()) return
        scope.launch {
            try {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, textOrUrl)
                    type = "text/plain"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                // Catch safely
            }
        }
    }

    @JavascriptInterface
    fun openExternal(url: String?) {
        if (url.isNullOrBlank()) return
        scope.launch {
            try {
                val uri = Uri.parse(url)
                if (uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "mailto" || uri.scheme == "tel") {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                // Catch safely
            }
        }
    }

    @JavascriptInterface
    fun fullscreen() {
        scope.launch {
            onFullscreenRequested(true)
        }
    }

    @JavascriptInterface
    fun exitFullscreen() {
        scope.launch {
            onFullscreenRequested(false)
        }
    }

    @JavascriptInterface
    fun pickFile() {
        scope.launch {
            onPickFileRequested()
        }
    }

    @JavascriptInterface
    fun getAppInfo(): String {
        return JSONObject().apply {
            put("appId", appId)
            put("appName", appName)
            put("platform", "Android")
            put("runtime", "AppHubPro")
            put("version", "1.0.0")
        }.toString()
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return JSONObject().apply {
            put("osVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
        }.toString()
    }
}
