package com.example.runtime

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLConnection

class AppResourceManager(private val context: Context) {

    companion object {
        const val VIRTUAL_HOST_SUFFIX = ".apphub.local"
        const val VIRTUAL_SCHEME = "https"

        fun getAppOrigin(appId: String): String {
            // Safe alphanumeric host
            val sanitized = appId.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
            return "$VIRTUAL_SCHEME://app-$sanitized$VIRTUAL_HOST_SUFFIX"
        }

        fun getAppBaseUrl(appId: String, entryPoint: String = "index.html"): String {
            val origin = getAppOrigin(appId)
            val cleanEntry = entryPoint.removePrefix("/")
            return "$origin/$cleanEntry"
        }
    }

    fun isAppVirtualUrl(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return host.endsWith(VIRTUAL_HOST_SUFFIX) || host == "apphub.local"
    }

    fun resolveAppIdFromUri(uri: Uri): String? {
        val host = uri.host ?: return null
        if (host.endsWith(VIRTUAL_HOST_SUFFIX)) {
            val prefix = host.removeSuffix(VIRTUAL_HOST_SUFFIX)
            return prefix.removePrefix("app-")
        }
        if (host == "apphub.local") {
            val segments = uri.pathSegments
            if (segments.isNotEmpty()) {
                return segments[0]
            }
        }
        return null
    }

    fun getAppDirectory(appId: String, customDirName: String? = null): File {
        val appsRoot = File(context.filesDir, "apps")
        if (!appsRoot.exists()) appsRoot.mkdirs()
        
        val dirName = customDirName ?: run {
            // Find existing directory matching app id
            appsRoot.listFiles()?.firstOrNull { it.isDirectory && (it.name == appId || it.name == "app_$appId" || it.name == "app_sample_$appId") }?.name
                ?: "app_$appId"
        }
        val appDir = File(appsRoot, dirName)
        if (!appDir.exists()) appDir.mkdirs()
        return appDir
    }

    fun handleInterceptedRequest(uri: Uri, currentAppId: String, projectDirName: String? = null): WebResourceResponse? {
        if (!isAppVirtualUrl(uri)) {
            // Return null so standard Android WebView handles public HTTPS / CDN calls naturally!
            return null
        }

        val appId = resolveAppIdFromUri(uri) ?: currentAppId
        val appDir = getAppDirectory(appId, projectDirName)
        val canonicalRootDir = appDir.canonicalFile

        var relativePath = uri.path ?: "/index.html"
        if (uri.host == "apphub.local" && uri.pathSegments.isNotEmpty()) {
            // Drop appId segment if present
            val segments = uri.pathSegments.drop(1)
            relativePath = "/" + segments.joinToString("/")
        }

        if (relativePath == "/" || relativePath.isEmpty()) {
            relativePath = "/index.html"
        }

        val decodedPath = Uri.decode(relativePath).removePrefix("/")
        var targetFile = File(canonicalRootDir, decodedPath)

        // Strict directory traversal prevention
        val canonicalTarget = targetFile.canonicalFile
        if (!canonicalTarget.path.startsWith(canonicalRootDir.path)) {
            return createErrorResponse(403, "Access Denied: Path Traversal Detected")
        }

        // Case-insensitive & Directory fallback
        if (!targetFile.exists()) {
            // Check case-insensitive match on Linux filesystem
            val parts = decodedPath.split('/', '\\').filter { it.isNotEmpty() }
            var curr = canonicalRootDir
            var matched = true
            for (part in parts) {
                val match = curr.listFiles()?.firstOrNull { it.name.equals(part, ignoreCase = true) }
                if (match != null) {
                    curr = match
                } else {
                    matched = false
                    break
                }
            }
            if (matched && curr.exists()) {
                targetFile = curr
            }
        }

        // Directory index.html resolution
        if (targetFile.exists() && targetFile.isDirectory) {
            val indexInside = File(targetFile, "index.html")
            if (indexInside.exists() && indexInside.isFile) {
                targetFile = indexInside
            }
        }

        if (!targetFile.exists() || targetFile.isDirectory) {
            // Fallback: If requesting root or index.html, search for any html in the project root or subfolder
            if (decodedPath == "index.html" || decodedPath.isEmpty() || decodedPath == "/") {
                val anyHtml = canonicalRootDir.walkTopDown().firstOrNull { it.isFile && (it.extension.equals("html", ignoreCase = true) || it.extension.equals("htm", ignoreCase = true)) }
                if (anyHtml != null) {
                    val mimeType = detectMimeType(anyHtml.name)
                    return serveFileResponse(anyHtml, mimeType, canonicalRootDir)
                }
            }
            return createErrorResponse(404, "File Not Found: $decodedPath")
        }

        val mimeType = detectMimeType(targetFile.name)
        return serveFileResponse(targetFile, mimeType, canonicalRootDir)
    }

    private fun serveFileResponse(file: File, initialMimeType: String, appRootDir: File): WebResourceResponse {
        var mimeType = initialMimeType
        val headers = mutableMapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
            "Access-Control-Allow-Headers" to "*",
            "Cache-Control" to "no-cache, no-store, must-revalidate",
            "Pragma" to "no-cache",
            "Expires" to "0"
        )

        // Check if file is HTML despite extension (e.g. .txt or no extension)
        if (mimeType != "text/html" && file.length() < 5 * 1024 * 1024) {
            try {
                val headerBytes = ByteArray(1024)
                val readLen = FileInputStream(file).use { it.read(headerBytes) }
                if (readLen > 0) {
                    val headSample = String(headerBytes, 0, readLen, Charsets.UTF_8).lowercase()
                    if (headSample.contains("<!doctype html") || headSample.contains("<html") || headSample.contains("<body") || headSample.contains("<script")) {
                        mimeType = "text/html"
                    }
                }
            } catch (_: Exception) {}
        }

        val encoding = if (mimeType.startsWith("text/") || mimeType == "application/javascript" || mimeType == "application/json" || mimeType == "image/svg+xml") {
            "UTF-8"
        } else {
            null // Binary stream for WASM, fonts, images, audio, video
        }

        // Auto-detect if HTML file is wrapped in document/code block tags (<pre>&lt;!DOCTYPE...)
        // and inject restored localStorage if present
        if (mimeType == "text/html" && file.exists()) {
            try {
                val rawContent = file.readText(Charsets.UTF_8)
                val executable = HtmlSanitizer.extractExecutableHtml(rawContent)
                if (executable != rawContent) {
                    try {
                        file.writeText(executable, Charsets.UTF_8)
                    } catch (_: Exception) {}
                }

                // Check for localstorage.json to inject restored state
                val lsFile = File(appRootDir, "localstorage.json")
                val finalHtml = if (lsFile.exists()) {
                    try {
                        val lsJson = lsFile.readText(Charsets.UTF_8).trim()
                        if (lsJson.startsWith("{") && lsJson.endsWith("}")) {
                            val injectScript = "<script>(function(){try{var _hub_ls=$lsJson;for(var _k in _hub_ls){if(localStorage.getItem(_k)===null){localStorage.setItem(_k,typeof _hub_ls[_k]==='string'?_hub_ls[_k]:JSON.stringify(_hub_ls[_k]));}}}catch(e){}})();</script>"
                            if (executable.contains("<head", ignoreCase = true)) {
                                executable.replaceFirst(Regex("(<head[^>]*>)", RegexOption.IGNORE_CASE), "$1$injectScript")
                            } else {
                                "$injectScript$executable"
                            }
                        } else {
                            executable
                        }
                    } catch (_: Exception) {
                        executable
                    }
                } else {
                    executable
                }

                return WebResourceResponse(
                    "text/html",
                    "UTF-8",
                    200,
                    "OK",
                    headers,
                    finalHtml.byteInputStream(Charsets.UTF_8)
                )
            } catch (_: Exception) {}
        }

        val inputStream: InputStream = FileInputStream(file)
        return WebResourceResponse(
            mimeType,
            encoding,
            200,
            "OK",
            headers,
            inputStream
        )
    }

    private fun createErrorResponse(statusCode: Int, message: String): WebResourceResponse {
        val html = """<!DOCTYPE html><html><head><title>Error $statusCode</title><style>body{background:#090d16;color:#ef4444;font-family:sans-serif;padding:32px;text-align:center;}h1{font-size:24px;}p{color:#94a3b8;font-size:14px;}</style></head><body><h1>[$statusCode] Resource Error</h1><p>$message</p></body></html>"""
        val headers = mapOf("Access-Control-Allow-Origin" to "*")
        return WebResourceResponse(
            "text/html",
            "UTF-8",
            statusCode,
            message,
            headers,
            html.byteInputStream()
        )
    }

    fun detectMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".html") || lower.endsWith(".htm") -> "text/html"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".js") || lower.endsWith(".mjs") -> "application/javascript"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".ico") -> "image/x-icon"
            lower.endsWith(".woff2") -> "font/woff2"
            lower.endsWith(".woff") -> "font/woff"
            lower.endsWith(".ttf") -> "font/ttf"
            lower.endsWith(".otf") -> "font/otf"
            lower.endsWith(".wasm") -> "application/wasm"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".ogg") -> "audio/ogg"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".xml") -> "application/xml"
            lower.endsWith(".zip") -> "application/zip"
            else -> URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
        }
    }
}
