package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDao
import com.example.data.model.AppEntity
import com.example.runtime.HtmlSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreManager(
    private val context: Context,
    private val appDao: AppDao
) {
    companion object {
        const val BACKUP_VERSION = 1
        const val MANIFEST_ENTRY = "apphub_backup_manifest.json"
    }

    suspend fun createWorkspaceBackup(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val appsDir = File(context.filesDir, "apps")
            var appCount = 0

            ZipOutputStream(outputStream).use { zos ->
                val appsList = appsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

                val manifestJson = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("createdAt", System.currentTimeMillis())
                    put("device", android.os.Build.MODEL)
                    val appsArray = JSONArray()
                    appsList.forEach { dir ->
                        val appObj = JSONObject().apply {
                            put("dirName", dir.name)
                        }
                        appsArray.put(appObj)
                    }
                    put("apps", appsArray)
                }

                // Write manifest
                val manifestEntry = ZipEntry(MANIFEST_ENTRY)
                zos.putNextEntry(manifestEntry)
                zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write all app folders
                appsList.forEach { dir ->
                    appCount++
                    dir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val rel = "apps/${dir.name}/${file.relativeTo(dir).path.replace('\\', '/')}"
                            val entry = ZipEntry(rel)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            Result.success(appCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreWorkspaceBackup(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.readBytes()
            if (bytes.isEmpty()) {
                return@withContext Result.failure(Exception("File is empty"))
            }

            // Check if input is a ZIP archive: ZIP files begin with 'PK\x03\x04' (0x50, 0x4B, 0x03, 0x04)
            val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

            if (isZip) {
                restoreZipBackup(ByteArrayInputStream(bytes))
            } else {
                // Must be a JSON full backup (from Web/HTML App Hub, OmniChat, or custom JSON format)
                val jsonString = String(bytes, Charsets.UTF_8)
                restoreJsonWorkspaceBackup(jsonString)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreZipBackup(zipStream: InputStream): Result<Int> {
        val appsDir = File(context.filesDir, "apps").apply { mkdirs() }
        var restoredApps = 0

        ZipInputStream(zipStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val path = entry.name.replace('\\', '/')
                if (path.startsWith("apps/") && !path.contains("__MACOSX") && !entry.isDirectory) {
                    val subPath = path.removePrefix("apps/")
                    val destFile = File(appsDir, subPath)
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { zis.copyTo(it) }

                    // If it's a new directory root, count it
                    val rootDirName = subPath.substringBefore('/')
                    val rootDir = File(appsDir, rootDirName)
                    if (appDao.getAppById(rootDirName) == null) {
                        val candidateEntry = if (File(rootDir, "index.html").exists()) "index.html" else "main.html"
                        appDao.insertApp(
                            AppEntity(
                                id = rootDirName,
                                name = rootDirName.removePrefix("app_").replace('_', ' ').replaceFirstChar { it.uppercase() },
                                description = "Restored from backup package",
                                iconName = "restore",
                                entryPoint = candidateEntry,
                                projectDirName = rootDirName,
                                storageSizeBytes = rootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                            )
                        )
                        restoredApps++
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return Result.success(restoredApps)
    }

    /**
     * Comprehensively parses and restores applications and storage from any HTML-based App Hub JSON export.
     * Supports:
     * - {"apps": [ ... ], "data": { ... }}
     * - {"apps": { "appId": { ... } }}
     * - [ { "name": "...", "html": "..." } ]
     * - {"localStorage": { "apphub_apps": "...", "data": "..." }}
     * - Single app export {"name": "...", "html": "..."}
     * - OmniChat / SPA backups with chats, providers, sites
     */
    suspend fun restoreJsonWorkspaceBackup(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val appsDir = File(context.filesDir, "apps").apply { mkdirs() }
            var restoredApps = 0
            val trimmed = jsonString.trim()

            if (trimmed.startsWith("[")) {
                // Array of apps directly at root
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    if (restoreSingleAppFromJson(item, appsDir, i, null, null)) {
                        restoredApps++
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

                // Extract root-level collections
                val rootFiles = root.optJSONObject("files")
                val rootLs = root.optJSONObject("ls") ?: root.optJSONObject("localStorage") ?: root.optJSONObject("storage")
                val pinnedArray = root.optJSONArray("pinned")
                val pinnedSet = mutableSetOf<String>()
                if (pinnedArray != null) {
                    for (p in 0 until pinnedArray.length()) {
                        pinnedSet.add(pinnedArray.opt(p)?.toString() ?: "")
                    }
                }

                // Check 1: "apps" array or map
                if (root.has("apps")) {
                    val appsVal = root.get("apps")
                    if (appsVal is JSONArray) {
                        for (i in 0 until appsVal.length()) {
                            val item = appsVal.optJSONObject(i) ?: continue
                            if (restoreSingleAppFromJson(item, appsDir, i, rootFiles, rootLs, pinnedSet)) {
                                restoredApps++
                            }
                        }
                    } else if (appsVal is JSONObject) {
                        var index = 0
                        val keys = appsVal.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val item = appsVal.optJSONObject(key) ?: continue
                            if (!item.has("id")) item.put("id", key)
                            if (!item.has("name")) item.put("name", key)
                            if (restoreSingleAppFromJson(item, appsDir, index++, rootFiles, rootLs, pinnedSet)) {
                                restoredApps++
                            }
                        }
                    }
                }

                // Check 2: If no apps were restored via "apps", but "files" contains apps mapped by ID
                if (restoredApps == 0 && rootFiles != null && rootFiles.length() > 0) {
                    val fileKeys = rootFiles.keys()
                    var fIndex = 0
                    while (fileKeys.hasNext()) {
                        val fileKey = fileKeys.next()
                        val fileVal = rootFiles.get(fileKey)
                        val appId = if (fileKey.matches(Regex("^[0-9]+$"))) "app_$fileKey" else fileKey
                        val appDir = File(appsDir, appId).apply { mkdirs() }
                        var entryHtml = ""

                        if (fileVal is String) {
                            entryHtml = HtmlSanitizer.extractExecutableHtml(fileVal)
                            File(appDir, "index.html").writeText(entryHtml)
                        } else if (fileVal is JSONObject) {
                            val subKeys = fileVal.keys()
                            while (subKeys.hasNext()) {
                                val subKey = subKeys.next()
                                val content = fileVal.optString(subKey)
                                val f = File(appDir, subKey)
                                f.parentFile?.mkdirs()
                                val clean = if (subKey.endsWith(".html", ignoreCase = true)) HtmlSanitizer.extractExecutableHtml(content) else content
                                f.writeText(clean)
                                if (entryHtml.isEmpty() && subKey.endsWith(".html", ignoreCase = true)) {
                                    entryHtml = clean
                                }
                            }
                        }

                        if (rootLs != null) {
                            try {
                                File(appDir, "localstorage.json").writeText(rootLs.toString())
                            } catch (_: Exception) {}
                        }

                        val title = HtmlSanitizer.extractAppTitle(entryHtml) ?: "Imported App ${fIndex + 1}"
                        appDao.insertApp(
                            AppEntity(
                                id = appId,
                                name = title,
                                description = "Imported from App Hub backup",
                                iconName = "code",
                                entryPoint = "index.html",
                                projectDirName = appId,
                                isPinned = pinnedSet.contains(fileKey) || pinnedSet.contains(appId),
                                storageSizeBytes = appDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                            )
                        )
                        restoredApps++
                        fIndex++
                    }
                }

                // Check 3: "projects" or "items" or "workspaces"
                for (candidateListKey in listOf("projects", "items", "workspaces", "sites")) {
                    if (restoredApps == 0 && root.has(candidateListKey)) {
                        val arr = root.optJSONArray(candidateListKey)
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                if (restoreSingleAppFromJson(item, appsDir, i, rootFiles, rootLs, pinnedSet)) {
                                    restoredApps++
                                }
                            }
                        }
                    }
                }

                // Check 4: Single app JSON (has "html", "content", "code", "entry", etc.)
                if (restoredApps == 0) {
                    val hasHtml = root.has("html") || root.has("content") || root.has("code") || root.has("source")
                    if (hasHtml || root.has("name") || root.has("title") || root.has("app")) {
                        if (restoreSingleAppFromJson(root, appsDir, 0, rootFiles, rootLs, pinnedSet)) {
                            restoredApps++
                        }
                    }
                }

                // Check 5: OmniChat Pro backup format or specific app with chats/providers
                if (restoredApps == 0 && (root.has("providers") || root.has("chats") || root.optString("app").contains("OmniChat", ignoreCase = true))) {
                    val omniName = root.optString("app", "OmniChat Pro")
                    val appId = "app_omnichat_restored"
                    val appDir = File(appsDir, appId).apply { mkdirs() }

                    if (rootLs != null) {
                        File(appDir, "localstorage.json").writeText(rootLs.toString())
                    } else {
                        File(appDir, "localstorage.json").writeText(jsonString)
                    }

                    val runnerHtml = """<!DOCTYPE html><html><head><meta charset="UTF-8"><title>$omniName</title><style>body{background:#0b0d14;color:#eef1f8;font-family:sans-serif;padding:24px;text-align:center;}button{background:#7c6cff;color:#fff;border:none;padding:12px 24px;border-radius:12px;font-weight:bold;cursor:pointer;margin-top:16px;}</style></head><body><h1>⚡ $omniName Data Restored</h1><p>Chats, providers and models imported from backup.</p><div id="info" style="font-family:monospace;margin:16px 0;opacity:0.8;">Data loaded successfully</div><script>console.log('OmniChat backup data restored');</script></body></html>"""
                    File(appDir, "index.html").writeText(runnerHtml)

                    appDao.insertApp(
                        AppEntity(
                            id = appId,
                            name = omniName,
                            description = "Restored from OmniChat Pro full backup",
                            iconName = "chat",
                            entryPoint = "index.html",
                            projectDirName = appId,
                            storageSizeBytes = appDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        )
                    )
                    restoredApps++
                }

                // Preserve global data / localStorage file in workspace
                try {
                    val workspaceDataFile = File(context.filesDir, "restored_workspace_data.json")
                    workspaceDataFile.writeText(jsonString)
                } catch (_: Exception) {}
            }

            Result.success(restoredApps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreSingleAppFromJson(
        item: JSONObject,
        appsDir: File,
        index: Int,
        rootFiles: JSONObject? = null,
        rootLs: JSONObject? = null,
        pinnedSet: Set<String> = emptySet()
    ): Boolean {
        val rawId = item.opt("id")?.toString() ?: ""
        val appId = if (rawId.isNotBlank()) {
            if (rawId.matches(Regex("^[0-9]+$"))) "app_$rawId" else rawId
        } else {
            "app_" + System.currentTimeMillis() + "_" + index
        }

        var rawHtml = item.optString("html").ifBlank {
            item.optString("content").ifBlank {
                item.optString("code").ifBlank {
                    item.optString("source").ifBlank {
                        item.optString("rawHtml").ifBlank {
                            item.optString("entry", "")
                        }
                    }
                }
            }
        }

        // Check if rootFiles has the code for this app using rawId or raw numeric id
        var filesObj = item.optJSONObject("files")
        val filesArr = item.optJSONArray("files")

        if (rawHtml.isBlank() && filesObj == null && rootFiles != null) {
            // Check by exact raw ID (e.g. "1712345678901")
            val fromRoot = if (rawId.isNotBlank()) rootFiles.opt(rawId) else null
            val fromStripped = if (rawId.startsWith("app_")) rootFiles.opt(rawId.removePrefix("app_")) else null
            val fileCandidate = fromRoot ?: fromStripped ?: run {
                // If only 1 file in rootFiles, use it
                if (rootFiles.length() == 1) {
                    val k = rootFiles.keys().next()
                    rootFiles.opt(k)
                } else null
            }

            if (fileCandidate is String) {
                rawHtml = fileCandidate
            } else if (fileCandidate is JSONObject) {
                filesObj = fileCandidate
            }
        }

        if (rawHtml.isBlank() && filesObj == null && filesArr == null) {
            // Check if any string field in item contains HTML
            val keys = item.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val str = item.optString(k, "")
                if (str.length > 60 && (str.contains("<!doctype", ignoreCase = true) || str.contains("<html", ignoreCase = true) || str.contains("&lt;!DOCTYPE", ignoreCase = true))) {
                    rawHtml = str
                    break
                }
            }
        }

        // Even if no code found, if this is an app with name and type="html", create an entry with a fallback or title
        val hasName = item.has("name") || item.has("title")
        if (rawHtml.isBlank() && filesObj == null && filesArr == null && !hasName) {
            return false
        }

        val sanitizedHtml = if (rawHtml.isNotBlank()) HtmlSanitizer.extractExecutableHtml(rawHtml) else ""
        val extractedTitle = if (sanitizedHtml.isNotBlank()) HtmlSanitizer.extractAppTitle(sanitizedHtml) else null

        val name = item.optString("name").ifBlank {
            item.optString("title").ifBlank {
                extractedTitle ?: "App ${index + 1}"
            }
        }

        val desc = item.optString("description").ifBlank {
            item.optString("category").ifBlank {
                item.optString("desc", "Restored from App Hub backup")
            }
        }

        val icon = item.optString("icon").ifBlank {
            item.optString("iconName", "code")
        }

        val appDir = File(appsDir, appId).apply { mkdirs() }
        var entryPoint = item.optString("entryPoint", "index.html")

        // Write files if map exists
        if (filesObj != null) {
            val fKeys = filesObj.keys()
            while (fKeys.hasNext()) {
                val fName = fKeys.next()
                val fContent = filesObj.optString(fName)
                val targetFile = File(appDir, fName)
                targetFile.parentFile?.mkdirs()
                val cleanContent = if (fName.endsWith(".html", ignoreCase = true)) HtmlSanitizer.extractExecutableHtml(fContent) else fContent
                targetFile.writeText(cleanContent)
            }
        } else if (filesArr != null) {
            for (fIdx in 0 until filesArr.length()) {
                val fObj = filesArr.optJSONObject(fIdx) ?: continue
                val fName = fObj.optString("name", "file_$fIdx")
                val fContent = fObj.optString("content", "")
                val targetFile = File(appDir, fName)
                targetFile.parentFile?.mkdirs()
                val cleanContent = if (fName.endsWith(".html", ignoreCase = true)) HtmlSanitizer.extractExecutableHtml(fContent) else fContent
                targetFile.writeText(cleanContent)
            }
        }

        // Ensure entry point exists
        val entryFile = File(appDir, entryPoint)
        if (!entryFile.exists() && sanitizedHtml.isNotBlank()) {
            entryFile.writeText(sanitizedHtml)
        } else if (!entryFile.exists()) {
            val firstHtml = appDir.walkTopDown().firstOrNull { it.isFile && it.extension.equals("html", ignoreCase = true) }
            if (firstHtml != null) {
                entryPoint = firstHtml.relativeTo(appDir).path.replace('\\', '/')
            } else {
                // Generate a placeholder page if code was in an external URL or missing
                val url = item.optString("url")
                val placeholder = if (url.isNotBlank() && url != "null") {
                    """<!DOCTYPE html><html><head><meta charset="UTF-8"><title>$name</title><meta http-equiv="refresh" content="0; url=$url"></head><body>Redirecting to $url...</body></html>"""
                } else {
                    """<!DOCTYPE html><html><head><meta charset="UTF-8"><title>$name</title><style>body{background:#0b0d14;color:#eef1f8;font-family:sans-serif;padding:24px;text-align:center;}</style></head><body><h1>$name</h1><p>Restored from App Hub</p></body></html>"""
                }
                entryFile.writeText(placeholder)
            }
        }

        // Save app data / localStorage
        val dataObj = item.optJSONObject("data") ?: item.optJSONObject("storage") ?: item.optJSONObject("localStorage") ?: rootLs
        if (dataObj != null) {
            try {
                File(appDir, "localstorage.json").writeText(dataObj.toString())
            } catch (_: Exception) {}
        }

        val isPinned = pinnedSet.contains(rawId) || pinnedSet.contains(appId) || item.optBoolean("isPinned", false)

        val entity = AppEntity(
            id = appId,
            name = name,
            description = desc,
            iconName = icon,
            entryPoint = entryPoint,
            projectDirName = appId,
            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            lastUsedAt = item.optLong("lastOpened", System.currentTimeMillis()),
            isPinned = isPinned,
            storageSizeBytes = appDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        )
        appDao.insertApp(entity)
        return true
    }
}
