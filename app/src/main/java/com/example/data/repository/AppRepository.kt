package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDao
import com.example.data.model.AppEntity
import com.example.data.model.FileCategory
import com.example.data.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AppRepository(
    private val context: Context,
    private val appDao: AppDao
) {

    val allAppsFlow: Flow<List<AppEntity>> = appDao.getAllAppsFlow()

    fun getAppFlow(appId: String): Flow<AppEntity?> = appDao.getAppFlow(appId)

    suspend fun getApp(appId: String): AppEntity? = appDao.getAppById(appId)

    suspend fun createNewAppFromTemplate(
        name: String,
        templateType: String,
        description: String = ""
    ): AppEntity = withContext(Dispatchers.IO) {
        val appId = "app_" + System.currentTimeMillis()
        val dirName = appId
        val appDir = File(File(context.filesDir, "apps"), dirName).apply { mkdirs() }

        val (icon, entry) = when (templateType) {
            "canvas" -> {
                File(appDir, "index.html").writeText(
                    """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$name</title>
  <style>
    body{margin:0;background:#111;color:#fff;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;padding:20px;}
    canvas{background:#222;border:2px solid #00e5ff;border-radius:12px;touch-action:none;}
    button{background:#00e5ff;color:#000;border:none;border-radius:8px;padding:10px 18px;font-weight:bold;margin-top:16px;cursor:pointer;}
  </style>
</head>
<body>
  <h2>$name</h2>
  <canvas id="c" width="320" height="320"></canvas>
  <button onclick="clearCanvas()">Clear Canvas</button>
  <script>
    const c=document.getElementById('c'), ctx=c.getContext('2d');
    let down=false;
    ctx.strokeStyle='#00e5ff'; ctx.lineWidth=4; ctx.lineCap='round';
    function draw(e){
      if(!down)return;
      const rect=c.getBoundingClientRect();
      const x=(e.clientX||(e.touches&&e.touches[0].clientX))-rect.left;
      const y=(e.clientY||(e.touches&&e.touches[0].clientY))-rect.top;
      ctx.lineTo(x,y); ctx.stroke(); ctx.beginPath(); ctx.moveTo(x,y);
    }
    c.onmousedown=c.ontouchstart=(e)=>{down=true;ctx.beginPath();draw(e);};
    window.onmouseup=window.ontouchend=()=>{down=false;};
    c.onmousemove=c.ontouchmove=draw;
    function clearCanvas(){ctx.clearRect(0,0,c.width,c.height);}
  </script>
</body>
</html>"""
                )
                Pair("palette", "index.html")
            }
            "react" -> {
                File(appDir, "index.html").writeText(
                    """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$name</title>
  <script src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
  <style>
    body{background:#0f172a;color:#f8fafc;font-family:sans-serif;margin:0;padding:24px;display:flex;justify-content:center;}
    .card{background:#1e293b;border:1px solid #334155;border-radius:16px;padding:24px;max-width:400px;width:100%;text-align:center;}
    button{background:#38bdf8;color:#0f172a;border:none;border-radius:8px;padding:10px 20px;font-weight:bold;cursor:pointer;font-size:16px;}
  </style>
</head>
<body>
  <div id="root"></div>
  <script type="text/babel">
    function App() {
      const [count, setCount] = React.useState(0);
      return (
        <div className="card">
          <h1>⚡ $name</h1>
          <p>Running React 18 & Babel via CDN in App Hub Pro!</p>
          <div style={{margin: '20px 0', fontSize: '36px', fontWeight: 'bold', color: '#38bdf8'}}>{count}</div>
          <button onClick={() => setCount(c => c + 1)}>Increment Counter</button>
        </div>
      );
    }
    ReactDOM.createRoot(document.getElementById('root')).render(<App />);
  </script>
</body>
</html>"""
                )
                Pair("code", "index.html")
            }
            else -> { // Blank HTML + CSS + JS
                File(appDir, "index.html").writeText(
                    """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$name</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <div class="container">
    <h1>🚀 $name</h1>
    <p>Your universal web application is ready.</p>
    <button id="btnClick">Test Interactive JS</button>
    <div id="output" style="margin-top: 16px; color: #38bdf8;"></div>
  </div>
  <script src="app.js"></script>
</body>
</html>"""
                )
                File(appDir, "styles.css").writeText(
                    """body {
  background: #090d16;
  color: #f8fafc;
  font-family: -apple-system, system-ui, sans-serif;
  margin: 0;
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}
.container {
  background: #111827;
  border: 1px solid #1f2937;
  border-radius: 20px;
  padding: 30px;
  max-width: 440px;
  width: 100%;
  text-align: center;
  box-shadow: 0 10px 25px rgba(0,0,0,0.5);
}
h1 { color: #00e5ff; font-size: 24px; margin-bottom: 8px; }
p { color: #94a3b8; font-size: 14px; margin-bottom: 24px; }
button {
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 10px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}"""
                )
                File(appDir, "app.js").writeText(
                    """document.getElementById('btnClick').addEventListener('click', () => {
  const time = new Date().toLocaleTimeString();
  document.getElementById('output').innerText = 'Action triggered at: ' + time;
  if (window.AppHub) {
    window.AppHub.showToast('AppHub bridge working smoothly!');
  }
});"""
                )
                Pair("web", "index.html")
            }
        }

        val app = AppEntity(
            id = appId,
            name = name.trim().ifEmpty { "New Web App" },
            description = description.trim(),
            iconName = icon,
            entryPoint = entry,
            projectDirName = dirName,
            storageSizeBytes = calculateDirSize(appDir)
        )
        appDao.insertApp(app)
        app
    }

    suspend fun importSingleHtml(
        fileName: String,
        content: String
    ): AppEntity = withContext(Dispatchers.IO) {
        val sanitizedContent = com.example.runtime.HtmlSanitizer.extractExecutableHtml(content)
        val extractedTitle = com.example.runtime.HtmlSanitizer.extractAppTitle(sanitizedContent)

        val appId = "app_" + System.currentTimeMillis()
        val appDir = File(File(context.filesDir, "apps"), appId).apply { mkdirs() }
        val cleanName = if (fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true)) fileName else "$fileName.html"
        val entryFile = File(appDir, cleanName)
        entryFile.writeText(sanitizedContent)

        val appName = extractedTitle ?: cleanName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            .replaceFirstChar { it.uppercase() }

        val app = AppEntity(
            id = appId,
            name = appName,
            description = "Single HTML standalone application",
            iconName = "html",
            entryPoint = cleanName,
            projectDirName = appId,
            storageSizeBytes = entryFile.length()
        )
        appDao.insertApp(app)
        app
    }

    suspend fun importFromZipStream(
        zipInputStream: InputStream,
        suggestedName: String
    ): Pair<AppEntity, List<String>> = withContext(Dispatchers.IO) {
        val appId = "app_" + System.currentTimeMillis()
        val appDir = File(File(context.filesDir, "apps"), appId).apply { mkdirs() }
        val canonicalDest = appDir.canonicalFile

        val candidateHtmlFiles = mutableListOf<String>()

        ZipInputStream(zipInputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryPath = entry.name.replace("\\", "/")
                // Skip Mac __MACOSX metadata
                if (!entryPath.contains("__MACOSX") && !entryPath.startsWith(".")) {
                    val destFile = File(canonicalDest, entryPath)
                    val canonicalDestFile = destFile.canonicalFile
                    if (!canonicalDestFile.path.startsWith(canonicalDest.path)) {
                        // Directory traversal attempt inside zip! Skip it safely
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }
                    if (entry.isDirectory) {
                        canonicalDestFile.mkdirs()
                    } else {
                        canonicalDestFile.parentFile?.mkdirs()
                        FileOutputStream(canonicalDestFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        if (canonicalDestFile.name.endsWith(".html", ignoreCase = true) || canonicalDestFile.name.endsWith(".htm", ignoreCase = true)) {
                            candidateHtmlFiles.add(canonicalDestFile.relativeTo(canonicalDest).path)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Smart entry point selection
        val selectedEntry = candidateHtmlFiles.firstOrNull { it.equals("index.html", ignoreCase = true) }
            ?: candidateHtmlFiles.firstOrNull { it.endsWith("index.html", ignoreCase = true) }
            ?: candidateHtmlFiles.firstOrNull { it.equals("main.html", ignoreCase = true) }
            ?: candidateHtmlFiles.firstOrNull { it.equals("app.html", ignoreCase = true) }
            ?: candidateHtmlFiles.firstOrNull()
            ?: "index.html"

        val app = AppEntity(
            id = appId,
            name = suggestedName.removeSuffix(".zip").replace("_", " ").replace("-", " "),
            description = "Imported ZIP project (${candidateHtmlFiles.size} HTML files)",
            iconName = "folder_zip",
            entryPoint = selectedEntry,
            projectDirName = appId,
            storageSizeBytes = calculateDirSize(appDir)
        )
        appDao.insertApp(app)
        Pair(app, candidateHtmlFiles)
    }

    suspend fun exportAppAsZip(app: AppEntity, targetOutputStream: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        if (!appDir.exists()) return@withContext

        ZipOutputStream(targetOutputStream).use { zos ->
            appDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relPath = file.relativeTo(appDir).path.replace("\\", "/")
                    val entry = ZipEntry(relPath)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    suspend fun duplicateApp(app: AppEntity): AppEntity = withContext(Dispatchers.IO) {
        val newId = "app_" + System.currentTimeMillis()
        val srcDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val destDir = File(File(context.filesDir, "apps"), newId).apply { mkdirs() }
        
        if (srcDir.exists()) {
            srcDir.copyRecursively(destDir, overwrite = true)
        }

        val copy = app.copy(
            id = newId,
            name = "${app.name} (Copy)",
            projectDirName = newId,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis(),
            isSampleApp = false,
            storageSizeBytes = calculateDirSize(destDir)
        )
        appDao.insertApp(copy)
        copy
    }

    suspend fun deleteApp(app: AppEntity) = withContext(Dispatchers.IO) {
        appDao.deleteApp(app)
        val dir = File(File(context.filesDir, "apps"), app.projectDirName)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun updateApp(app: AppEntity) = withContext(Dispatchers.IO) {
        appDao.updateApp(app)
    }

    suspend fun renameApp(appId: String, newName: String, newDescription: String? = null) = withContext(Dispatchers.IO) {
        if (newDescription != null) {
            appDao.updateAppDetails(appId, newName.trim(), newDescription.trim())
        } else {
            appDao.updateAppName(appId, newName.trim())
        }
    }

    suspend fun togglePinApp(appId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        appDao.updatePinStatus(appId, isPinned)
    }

    suspend fun updateEntryPoint(appId: String, newEntry: String) = withContext(Dispatchers.IO) {
        appDao.updateEntryPoint(appId, newEntry)
    }

    fun getProjectFiles(app: AppEntity, subDirectoryPath: String = ""): List<ProjectFile> {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val targetDir = if (subDirectoryPath.isBlank()) appDir else File(appDir, subDirectoryPath)
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        return targetDir.listFiles()?.map { file ->
            val relPath = file.relativeTo(appDir).path.replace("\\", "/")
            val category = if (file.isDirectory) {
                FileCategory.DIRECTORY
            } else {
                categorizeFile(file.name)
            }
            ProjectFile(
                name = file.name,
                relativePath = relPath,
                absolutePath = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) calculateDirSize(file) else file.length(),
                lastModified = file.lastModified(),
                category = category
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    fun readFileContent(app: AppEntity, relativePath: String): String {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val file = File(appDir, relativePath)
        if (!file.canonicalPath.startsWith(appDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt detected")
        }
        return if (file.exists() && file.isFile) file.readText(Charsets.UTF_8) else ""
    }

    suspend fun saveFileContent(app: AppEntity, relativePath: String, content: String) = withContext(Dispatchers.IO) {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val file = File(appDir, relativePath)
        if (!file.canonicalPath.startsWith(appDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt detected")
        }
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
        appDao.updateStorageSize(app.id, calculateDirSize(appDir))
    }

    suspend fun createNewFile(app: AppEntity, parentRelativePath: String, fileName: String, isFolder: Boolean) = withContext(Dispatchers.IO) {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val parentDir = if (parentRelativePath.isBlank()) appDir else File(appDir, parentRelativePath)
        val target = File(parentDir, fileName)
        if (!target.canonicalPath.startsWith(appDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt detected")
        }
        if (isFolder) {
            target.mkdirs()
        } else {
            target.parentFile?.mkdirs()
            if (!target.exists()) target.createNewFile()
        }
        appDao.updateStorageSize(app.id, calculateDirSize(appDir))
    }

    suspend fun deleteProjectFile(app: AppEntity, relativePath: String) = withContext(Dispatchers.IO) {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val file = File(appDir, relativePath)
        if (!file.canonicalPath.startsWith(appDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt detected")
        }
        if (file.isDirectory) file.deleteRecursively() else file.delete()
        appDao.updateStorageSize(app.id, calculateDirSize(appDir))
    }

    suspend fun renameProjectFile(app: AppEntity, relativePath: String, newName: String) = withContext(Dispatchers.IO) {
        val appDir = File(File(context.filesDir, "apps"), app.projectDirName)
        val file = File(appDir, relativePath)
        val target = File(file.parentFile, newName)
        if (!target.canonicalPath.startsWith(appDir.canonicalPath)) {
            throw SecurityException("Path traversal attempt detected")
        }
        file.renameTo(target)
    }

    fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun categorizeFile(name: String): FileCategory {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".html") || lower.endsWith(".htm") -> FileCategory.HTML
            lower.endsWith(".css") -> FileCategory.CSS
            lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".ts") -> FileCategory.JAVASCRIPT
            lower.endsWith(".json") -> FileCategory.JSON
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg") || lower.endsWith(".ico") -> FileCategory.IMAGE
            lower.endsWith(".ttf") || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".otf") -> FileCategory.FONT
            lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".ogg") -> FileCategory.MEDIA
            else -> FileCategory.OTHER
        }
    }
}
