package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppHubDatabase
import com.example.data.model.ActiveAppSession
import com.example.data.model.AppEntity
import com.example.data.model.ConsoleLogEntity
import com.example.data.model.DownloadEntity
import com.example.data.model.ProjectFile
import com.example.data.repository.AppRepository
import com.example.data.repository.BackupRestoreManager
import com.example.runtime.LiveAppSession
import com.example.runtime.MultiAppSessionManager
import com.example.runtime.SampleAppsSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

enum class AppNavDestination {
    HOME,
    MINI_BROWSER,
    PLAYER,
    FILE_MANAGER,
    CODE_EDITOR,
    DEV_CONSOLE,
    STORAGE_INSPECTOR,
    DOWNLOADS,
    SETTINGS
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("apphub_pro_preferences", Context.MODE_PRIVATE)

    // Mini Browser URL & History
    private val _miniBrowserUrl = MutableStateFlow(prefs.getString("browser_last_url", "about:blank") ?: "about:blank")
    val miniBrowserUrl: StateFlow<String> = _miniBrowserUrl.asStateFlow()

    private val _browserHistory = MutableStateFlow<List<String>>(
        prefs.getStringSet("browser_history", emptySet())?.toList()?.reversed() ?: emptyList()
    )
    val browserHistory: StateFlow<List<String>> = _browserHistory.asStateFlow()

    fun setMiniBrowserUrl(url: String) {
        _miniBrowserUrl.value = url
        prefs.edit().putString("browser_last_url", url).apply()
        if (url.isNotBlank() && url != "about:blank" && !url.startsWith("data:")) {
            val updated = (listOf(url) + _browserHistory.value.filter { it != url }).take(20)
            _browserHistory.value = updated
            prefs.edit().putStringSet("browser_history", updated.toSet()).apply()
        }
    }

    fun clearBrowserHistory() {
        _browserHistory.value = emptyList()
        prefs.edit().remove("browser_history").apply()
    }

    // Theme Mode
    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // Scratchpad Quick Notes
    private val _scratchpadNotes = MutableStateFlow(prefs.getString("scratchpad_notes", "") ?: "")
    val scratchpadNotes: StateFlow<String> = _scratchpadNotes.asStateFlow()

    fun saveScratchpadNotes(notes: String) {
        _scratchpadNotes.value = notes
        prefs.edit().putString("scratchpad_notes", notes).apply()
    }

    private val db = AppHubDatabase.getInstance(application)
    val appRepository = AppRepository(application, db.appDao())
    val backupManager = BackupRestoreManager(application, db.appDao())
    val sessionManager = MultiAppSessionManager(
        application,
        db.appDao(),
        db.downloadDao(),
        db.consoleLogDao()
    )

    // Reactive Data
    val allApps: StateFlow<List<AppEntity>> = appRepository.allAppsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDownloads: StateFlow<List<DownloadEntity>> = db.downloadDao().getAllDownloadsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessions: StateFlow<List<ActiveAppSession>> = sessionManager.activeSessionsFlow

    // Current Navigation Destination
    private val _currentDestination = MutableStateFlow(AppNavDestination.HOME)
    val currentDestination: StateFlow<AppNavDestination> = _currentDestination.asStateFlow()

    // Currently Selected App
    private val _selectedApp = MutableStateFlow<AppEntity?>(null)
    val selectedApp: StateFlow<AppEntity?> = _selectedApp.asStateFlow()

    // File Manager State
    private val _currentFolderPath = MutableStateFlow("")
    val currentFolderPath: StateFlow<String> = _currentFolderPath.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val projectFiles: StateFlow<List<ProjectFile>> = _projectFiles.asStateFlow()

    // Code Editor State
    private val _editingFilePath = MutableStateFlow<String?>(null)
    val editingFilePath: StateFlow<String?> = _editingFilePath.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _editorIsDirty = MutableStateFlow(false)
    val editorIsDirty: StateFlow<Boolean> = _editorIsDirty.asStateFlow()

    // DevConsole State
    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntity>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntity>> = _consoleLogs.asStateFlow()

    // File Chooser Callback for HTML input type=file
    var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null

    // Search query on Home
    val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            SampleAppsSeeder.seedSampleAppsIfEmpty(getApplication(), db.appDao())
        }
    }

    fun navigateTo(destination: AppNavDestination) {
        _currentDestination.value = destination
    }

    fun launchApp(app: AppEntity) {
        _selectedApp.value = app
        sessionManager.getOrCreateSession(
            app = app,
            onFileChooser = { callback, _ ->
                pendingFileChooserCallback = callback
                true
            }
        )
        _currentDestination.value = AppNavDestination.PLAYER
    }

    fun switchToRunningApp(appId: String) {
        val app = allApps.value.firstOrNull { it.id == appId }
        if (app != null) {
            _selectedApp.value = app
            sessionManager.getOrCreateSession(
                app = app,
                onFileChooser = { callback, _ ->
                    pendingFileChooserCallback = callback
                    true
                }
            )
            sessionManager.switchToApp(appId)
        }
        _currentDestination.value = AppNavDestination.PLAYER
    }

    fun closeRunningSession(appId: String) {
        sessionManager.closeSession(appId)
        if (_selectedApp.value?.id == appId) {
            val remaining = sessionManager.activeSessionsFlow.value.firstOrNull()
            if (remaining != null) {
                _selectedApp.value = allApps.value.firstOrNull { it.id == remaining.appId }
            } else {
                _currentDestination.value = AppNavDestination.HOME
            }
        }
    }

    fun openFileManager(app: AppEntity) {
        _selectedApp.value = app
        _currentFolderPath.value = ""
        refreshProjectFiles(app, "")
        _currentDestination.value = AppNavDestination.FILE_MANAGER
    }

    fun navigateFolder(subPath: String) {
        val app = _selectedApp.value ?: return
        _currentFolderPath.value = subPath
        refreshProjectFiles(app, subPath)
    }

    fun refreshProjectFiles(app: AppEntity, subPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = appRepository.getProjectFiles(app, subPath)
            _projectFiles.value = files
        }
    }

    fun openCodeEditor(app: AppEntity, relativeFilePath: String) {
        _selectedApp.value = app
        _editingFilePath.value = relativeFilePath
        viewModelScope.launch(Dispatchers.IO) {
            val content = appRepository.readFileContent(app, relativeFilePath)
            withContext(Dispatchers.Main) {
                _editorContent.value = content
                _editorIsDirty.value = false
                _currentDestination.value = AppNavDestination.CODE_EDITOR
            }
        }
    }

    fun updateEditorText(newText: String) {
        _editorContent.value = newText
        _editorIsDirty.value = true
    }

    fun saveEditorContent(onSaved: () -> Unit = {}) {
        val app = _selectedApp.value ?: return
        val path = _editingFilePath.value ?: return
        val text = _editorContent.value
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.saveFileContent(app, path, text)
            // If this file is part of an active session, reload it
            sessionManager.reloadSession(app.id, hardReload = true)
            withContext(Dispatchers.Main) {
                _editorIsDirty.value = false
                onSaved()
            }
        }
    }

    fun openDevConsole(app: AppEntity) {
        _selectedApp.value = app
        viewModelScope.launch {
            db.consoleLogDao().getLogsForApp(app.id).collect { logs ->
                _consoleLogs.value = logs
            }
        }
        _currentDestination.value = AppNavDestination.DEV_CONSOLE
    }

    fun clearDevConsole(appId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.consoleLogDao().clearLogsForApp(appId)
        }
    }

    fun openStorageInspector(app: AppEntity) {
        _selectedApp.value = app
        _currentDestination.value = AppNavDestination.STORAGE_INSPECTOR
    }

    fun deleteApp(app: AppEntity) {
        viewModelScope.launch {
            sessionManager.closeSession(app.id)
            appRepository.deleteApp(app)
            if (_selectedApp.value?.id == app.id) {
                _selectedApp.value = null
                _currentDestination.value = AppNavDestination.HOME
            }
        }
    }

    fun duplicateApp(app: AppEntity) {
        viewModelScope.launch {
            appRepository.duplicateApp(app)
        }
    }

    fun renameApp(appId: String, newName: String, newDescription: String = "") {
        viewModelScope.launch {
            appRepository.renameApp(appId, newName, newDescription)
            if (_selectedApp.value?.id == appId) {
                _selectedApp.value = _selectedApp.value?.copy(name = newName, description = newDescription)
            }
        }
    }

    fun togglePinApp(app: AppEntity) {
        viewModelScope.launch {
            val newPin = !app.isPinned
            appRepository.togglePinApp(app.id, newPin)
            if (_selectedApp.value?.id == app.id) {
                _selectedApp.value = _selectedApp.value?.copy(isPinned = newPin)
            }
        }
    }

    fun updateEntryPoint(appId: String, entryPoint: String) {
        viewModelScope.launch {
            appRepository.updateEntryPoint(appId, entryPoint)
            sessionManager.reloadSession(appId, hardReload = true)
        }
    }

    fun importZip(inputStream: InputStream, name: String, onImported: (AppEntity, List<String>) -> Unit) {
        viewModelScope.launch {
            val (app, htmlList) = appRepository.importFromZipStream(inputStream, name)
            onImported(app, htmlList)
        }
    }

    fun importSingleHtml(name: String, content: String, onImported: (AppEntity) -> Unit) {
        viewModelScope.launch {
            val app = appRepository.importSingleHtml(name, content)
            onImported(app)
        }
    }

    fun restoreWorkspaceBackup(inputStream: InputStream, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreWorkspaceBackup(inputStream)
            onComplete(result)
        }
    }

    fun restoreJsonBackup(jsonText: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreJsonWorkspaceBackup(jsonText)
            onComplete(result)
        }
    }

    fun createNewApp(name: String, templateType: String, description: String, onCreated: (AppEntity) -> Unit) {
        viewModelScope.launch {
            val app = appRepository.createNewAppFromTemplate(name, templateType, description)
            onCreated(app)
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.downloadDao().deleteDownloadById(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.destroyAll()
    }
}
