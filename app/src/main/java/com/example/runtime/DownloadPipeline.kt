package com.example.runtime

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.webkit.URLUtil
import android.widget.Toast
import com.example.data.db.DownloadDao
import com.example.data.model.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadPipeline(
    private val context: Context,
    private val downloadDao: DownloadDao
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun handleDownload(
        appId: String,
        appName: String,
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        suggestedFileName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            when {
                url.startsWith("data:") -> {
                    handleDataUrlDownload(appId, appName, url, mimeType, suggestedFileName)
                }
                url.startsWith("blob:") -> {
                    // Blob URLs are handled via JavaScript bridge extraction, or if received here:
                    Result.failure(IllegalArgumentException("Blob URLs require bridge extraction"))
                }
                else -> {
                    handleHttpDownload(appId, appName, url, contentDisposition, mimeType, suggestedFileName)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBase64Download(
        appId: String,
        appName: String,
        fileName: String,
        base64Data: String,
        mimeType: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            saveBinaryDownload(appId, appName, fileName, bytes, mimeType, "blob/bridge")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBinaryDownload(
        appId: String,
        appName: String,
        suggestedName: String,
        bytes: ByteArray,
        mimeType: String,
        sourceUrl: String = ""
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = getDownloadsDirectory()
            val safeFileName = resolveUniqueFileName(targetDir, sanitizeFileName(suggestedName))
            val targetFile = File(targetDir, safeFileName)
            
            FileOutputStream(targetFile).use { it.write(bytes) }

            val entity = DownloadEntity(
                id = UUID.randomUUID().toString(),
                appId = appId,
                appName = appName,
                fileName = safeFileName,
                mimeType = mimeType.ifEmpty { "application/octet-stream" },
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                timestamp = System.currentTimeMillis(),
                status = "COMPLETED",
                sourceUrl = sourceUrl
            )
            downloadDao.insertDownload(entity)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloaded: $safeFileName (${targetFile.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleDataUrlDownload(
        appId: String,
        appName: String,
        dataUrl: String,
        mimeType: String?,
        suggestedName: String?
    ): Result<File> {
        val metaEnd = dataUrl.indexOf(',')
        if (metaEnd == -1) return Result.failure(IllegalArgumentException("Malformed data URL"))

        val metadata = dataUrl.substring(5, metaEnd) // after "data:"
        val rawData = dataUrl.substring(metaEnd + 1)
        val isBase64 = metadata.contains(";base64", ignoreCase = true)
        val detectedMime = metadata.substringBefore(';').ifEmpty { mimeType ?: "application/octet-stream" }

        val bytes = if (isBase64) {
            Base64.decode(rawData, Base64.DEFAULT)
        } else {
            URLDecoder.decode(rawData, "UTF-8").toByteArray(Charsets.UTF_8)
        }

        val name = suggestedName ?: "download_${System.currentTimeMillis()}.${guessExtension(detectedMime)}"
        return saveBinaryDownload(appId, appName, name, bytes, detectedMime, "data-url")
    }

    private suspend fun handleHttpDownload(
        appId: String,
        appName: String,
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        suggestedName: String?
    ): Result<File> {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            return Result.failure(IllegalStateException("HTTP error ${response.code} downloading $url"))
        }

        val resolvedName = suggestedName 
            ?: URLUtil.guessFileName(url, contentDisposition, mimeType)
            ?: "download_${System.currentTimeMillis()}"

        val bytes = response.body?.bytes() ?: return Result.failure(IllegalStateException("Empty response body"))
        val finalMime = response.header("Content-Type") ?: mimeType ?: "application/octet-stream"

        return saveBinaryDownload(appId, appName, resolvedName, bytes, finalMime, url)
    }

    private fun getDownloadsDirectory(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sanitizeFileName(name: String): String {
        var clean = name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        if (clean.isBlank() || clean == "." || clean == "..") {
            clean = "download_${System.currentTimeMillis()}"
        }
        return clean
    }

    fun resolveUniqueFileName(directory: File, baseName: String): String {
        val file = File(directory, baseName)
        if (!file.exists()) return baseName

        val dotIdx = baseName.lastIndexOf('.')
        val prefix = if (dotIdx != -1) baseName.substring(0, dotIdx) else baseName
        val ext = if (dotIdx != -1) baseName.substring(dotIdx) else ""

        var counter = 1
        while (true) {
            val candidateName = "$prefix ($counter)$ext"
            if (!File(directory, candidateName).exists()) {
                return candidateName
            }
            counter++
        }
    }

    private fun guessExtension(mime: String): String {
        return when (mime.lowercase()) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            "image/svg+xml" -> "svg"
            "application/json" -> "json"
            "application/pdf" -> "pdf"
            "application/zip" -> "zip"
            "text/plain" -> "txt"
            "text/html" -> "html"
            "text/csv" -> "csv"
            else -> "bin"
        }
    }
}
