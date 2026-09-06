package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.dialogs.CreateAppDialog
import com.example.ui.dialogs.ImportWizardDialog
import com.example.ui.screens.AppFileManagerScreen
import com.example.ui.screens.AppPlayerScreen
import com.example.ui.screens.CodeEditorScreen
import com.example.ui.screens.DiagnosticsConsoleScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MiniBrowserScreen
import com.example.ui.screens.MultiAppSwitcherSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageInspectorScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                val currentDest by viewModel.currentDestination.collectAsState()
                val selectedApp by viewModel.selectedApp.collectAsState()
                val activeSessions by viewModel.activeSessions.collectAsState()
                val allApps by viewModel.allApps.collectAsState()

                var showCreateDialog by remember { mutableStateOf(false) }
                var showImportDialog by remember { mutableStateOf(false) }
                var showSwitcherSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                // File Chooser launcher for HTML <input type="file">
                val fileChooserLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    val cb = viewModel.pendingFileChooserCallback
                    if (cb != null) {
                        cb.onReceiveValue(if (!uris.isNullOrEmpty()) uris.toTypedArray() else null)
                        viewModel.pendingFileChooserCallback = null
                    }
                }

                // Check if file chooser needs to open
                LaunchedEffect(viewModel.pendingFileChooserCallback) {
                    if (viewModel.pendingFileChooserCallback != null) {
                        fileChooserLauncher.launch(arrayOf("*/*"))
                    }
                }

                val showBottomBar = currentDest in listOf(
                    AppNavDestination.HOME,
                    AppNavDestination.MINI_BROWSER,
                    AppNavDestination.DOWNLOADS,
                    AppNavDestination.SETTINGS
                )

                // Edge-to-edge: status bar merges seamlessly on Home screen and throughout the app
                val scaffoldInsets = WindowInsets(0, 0, 0, 0)

                val activePrimary = MaterialTheme.colorScheme.primary

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = scaffoldInsets,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                NavigationBarItem(
                                    selected = currentDest == AppNavDestination.HOME,
                                    onClick = { viewModel.navigateTo(AppNavDestination.HOME) },
                                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                                    label = {
                                        Text(
                                            "Apps",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentDest == AppNavDestination.HOME) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = activePrimary,
                                        selectedTextColor = activePrimary,
                                        indicatorColor = activePrimary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_apps")
                                )
                                NavigationBarItem(
                                    selected = currentDest == AppNavDestination.MINI_BROWSER,
                                    onClick = { viewModel.navigateTo(AppNavDestination.MINI_BROWSER) },
                                    icon = { Icon(Icons.Default.Language, contentDescription = "Mini Browser") },
                                    label = {
                                        Text(
                                            "Mini Browser",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentDest == AppNavDestination.MINI_BROWSER) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = activePrimary,
                                        selectedTextColor = activePrimary,
                                        indicatorColor = activePrimary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_browser")
                                )
                                NavigationBarItem(
                                    selected = currentDest == AppNavDestination.DOWNLOADS,
                                    onClick = { viewModel.navigateTo(AppNavDestination.DOWNLOADS) },
                                    icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                                    label = {
                                        Text(
                                            "Downloads",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentDest == AppNavDestination.DOWNLOADS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = activePrimary,
                                        selectedTextColor = activePrimary,
                                        indicatorColor = activePrimary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_downloads")
                                )
                                NavigationBarItem(
                                    selected = currentDest == AppNavDestination.SETTINGS,
                                    onClick = { viewModel.navigateTo(AppNavDestination.SETTINGS) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = {
                                        Text(
                                            "Settings",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentDest == AppNavDestination.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = activePrimary,
                                        selectedTextColor = activePrimary,
                                        indicatorColor = activePrimary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        when (currentDest) {
                            AppNavDestination.HOME -> {
                                HomeDashboardScreen(
                                    viewModel = viewModel,
                                    onOpenImportDialog = { showImportDialog = true },
                                    onOpenCreateDialog = { showCreateDialog = true },
                                    onOpenSwitcherSheet = { showSwitcherSheet = true }
                                )
                            }
                            AppNavDestination.MINI_BROWSER -> {
                                MiniBrowserScreen(
                                    viewModel = viewModel,
                                    onSaveAsApp = { title, htmlContent ->
                                        viewModel.importSingleHtml(title, htmlContent) { newApp ->
                                            viewModel.launchApp(newApp)
                                        }
                                    }
                                )
                            }
                            AppNavDestination.PLAYER -> {
                                if (selectedApp != null) {
                                    androidx.compose.runtime.key(selectedApp!!.id) {
                                        AppPlayerScreen(
                                            viewModel = viewModel,
                                            app = selectedApp!!,
                                            onOpenSwitcherSheet = { showSwitcherSheet = true }
                                        )
                                    }
                                } else {
                                    viewModel.navigateTo(AppNavDestination.HOME)
                                }
                            }
                            AppNavDestination.FILE_MANAGER -> {
                                if (selectedApp != null) {
                                    AppFileManagerScreen(
                                        viewModel = viewModel,
                                        app = selectedApp!!
                                    )
                                } else {
                                    viewModel.navigateTo(AppNavDestination.HOME)
                                }
                            }
                            AppNavDestination.CODE_EDITOR -> {
                                if (selectedApp != null) {
                                    CodeEditorScreen(
                                        viewModel = viewModel,
                                        app = selectedApp!!
                                    )
                                } else {
                                    viewModel.navigateTo(AppNavDestination.HOME)
                                }
                            }
                            AppNavDestination.DEV_CONSOLE -> {
                                if (selectedApp != null) {
                                    DiagnosticsConsoleScreen(
                                        viewModel = viewModel,
                                        app = selectedApp!!
                                    )
                                } else {
                                    viewModel.navigateTo(AppNavDestination.HOME)
                                }
                            }
                            AppNavDestination.STORAGE_INSPECTOR -> {
                                if (selectedApp != null) {
                                    StorageInspectorScreen(
                                        viewModel = viewModel,
                                        app = selectedApp!!
                                    )
                                } else {
                                    viewModel.navigateTo(AppNavDestination.HOME)
                                }
                            }
                            AppNavDestination.DOWNLOADS -> {
                                DownloadsScreen(viewModel = viewModel)
                            }
                            AppNavDestination.SETTINGS -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                        }

                        // Multi-App Switcher Modal Sheet
                        if (showSwitcherSheet) {
                            MultiAppSwitcherSheet(
                                activeSessions = activeSessions,
                                allApps = allApps,
                                currentActiveAppId = selectedApp?.id,
                                sheetState = sheetState,
                                onDismiss = { showSwitcherSheet = false },
                                onSwitchApp = { appId ->
                                    viewModel.switchToRunningApp(appId)
                                    showSwitcherSheet = false
                                },
                                onCloseSession = { appId ->
                                    viewModel.closeRunningSession(appId)
                                },
                                onCloseAll = {
                                    viewModel.sessionManager.destroyAll()
                                    viewModel.navigateTo(AppNavDestination.HOME)
                                    showSwitcherSheet = false
                                }
                            )
                        }

                        // Create App Dialog
                        if (showCreateDialog) {
                            CreateAppDialog(
                                onDismiss = { showCreateDialog = false },
                                onCreate = { name, template, desc ->
                                    viewModel.createNewApp(name, template, desc) { app ->
                                        viewModel.launchApp(app)
                                    }
                                }
                            )
                        }

                        // Import Wizard Dialog
                        if (showImportDialog) {
                            ImportWizardDialog(
                                onDismiss = { showImportDialog = false },
                                onImportZipUri = { uri, name ->
                                    try {
                                        contentResolver.openInputStream(uri)?.use { stream ->
                                            if (name.endsWith(".json", ignoreCase = true)) {
                                                viewModel.restoreWorkspaceBackup(stream) { result ->
                                                    result.onSuccess { count ->
                                                        Toast.makeText(this@MainActivity, "Restored $count apps & data!", Toast.LENGTH_LONG).show()
                                                    }.onFailure { err ->
                                                        Toast.makeText(this@MainActivity, "Restore error: ${err.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                viewModel.importZip(stream, name) { app, _ ->
                                                    viewModel.launchApp(app)
                                                    Toast.makeText(this@MainActivity, "Imported ${app.name}!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Failed to import ZIP: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onImportHtmlFileUri = { uri, name ->
                                    try {
                                        contentResolver.openInputStream(uri)?.use { stream ->
                                            if (name.endsWith(".json", ignoreCase = true)) {
                                                viewModel.restoreWorkspaceBackup(stream) { result ->
                                                    result.onSuccess { count ->
                                                        Toast.makeText(this@MainActivity, "Restored $count apps & data!", Toast.LENGTH_LONG).show()
                                                    }.onFailure { err ->
                                                        Toast.makeText(this@MainActivity, "Restore error: ${err.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                val content = stream.bufferedReader().readText()
                                                viewModel.importSingleHtml(name, content) { app ->
                                                    viewModel.launchApp(app)
                                                    Toast.makeText(this@MainActivity, "Imported ${app.name}!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Failed to import file: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onImportJsonBackupUri = { uri, name ->
                                    try {
                                        contentResolver.openInputStream(uri)?.use { stream ->
                                            viewModel.restoreWorkspaceBackup(stream) { result ->
                                                result.onSuccess { count ->
                                                    Toast.makeText(this@MainActivity, "Restored $count apps and all data from backup!", Toast.LENGTH_LONG).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(this@MainActivity, "Failed to restore backup: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Failed to open backup: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onImportRawHtml = { name, content ->
                                    val trimmed = content.trim()
                                    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                                        viewModel.restoreJsonBackup(content) { result ->
                                            result.onSuccess { count ->
                                                Toast.makeText(this@MainActivity, "Restored $count apps & data from JSON!", Toast.LENGTH_LONG).show()
                                            }.onFailure { err ->
                                                Toast.makeText(this@MainActivity, "Restore error: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        viewModel.importSingleHtml(name, content) { app ->
                                            viewModel.launchApp(app)
                                            Toast.makeText(this@MainActivity, "Saved and launched ${app.name}!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

