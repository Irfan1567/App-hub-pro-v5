package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDao
import com.example.data.model.AppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
            val apps = appDao.getAllAppsFlow()
            // We can read first value of flow
            var appCount = 0

            ZipOutputStream(outputStream).use { zos ->
                // Collect apps list
                val appList = mutableListOf<AppEntity>()
                val cursor = context.filesDir
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
            val appsDir = File(context.filesDir, "apps").apply { mkdirs() }
            var restoredApps = 0

            ZipInputStream(inputStream).use { zis ->
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
            Result.success(restoredApps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
