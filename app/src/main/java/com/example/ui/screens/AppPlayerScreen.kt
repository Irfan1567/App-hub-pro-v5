package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AppEntity
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError

@Composable
fun AppPlayerScreen(
    viewModel: AppViewModel,
    app: AppEntity,
    onOpenSwitcherSheet: () -> Unit
) {
    val session = remember(app.id) {
        viewModel.sessionManager.getOrCreateSession(app)
    }

    var isHudVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val activeSessions by viewModel.activeSessions.collectAsState()

    val currentSessionState = activeSessions.firstOrNull { it.appId == app.id }
    val errorCount = currentSessionState?.errorCount ?: 0

    // Back handling: web history -> exit fullscreen -> exit player
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else if (session.webView.canGoBack()) {
            session.webView.goBack()
        } else {
            viewModel.navigateTo(AppNavDestination.HOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main WebView Container
        AndroidView(
            factory = { context ->
                session.webView.parent?.let { parent ->
                    (parent as? ViewGroup)?.removeView(session.webView)
                }
                session.webView.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                session.webView
            },
            update = { view ->
                // Keep view attached
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Progress Bar
        if (session.isLoading && session.progress < 100) {
            LinearProgressIndicator(
                progress = { session.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = CyberCyan,
                trackColor = Color.Transparent
            )
        }

        // Render error overlay if main frame failed
        if (session.loadError != null) {
            ErrorRecoveryOverlay(
                appName = app.name,
                errorMessage = session.loadError ?: "Unknown load failure",
                onReload = { viewModel.sessionManager.reloadSession(app.id, hardReload = false) },
                onHardReload = { viewModel.sessionManager.reloadSession(app.id, hardReload = true) },
                onOpenDiagnostics = { viewModel.openDevConsole(app) },
                onClearCache = { viewModel.sessionManager.clearAppData(app.id) },
                onBack = { viewModel.navigateTo(AppNavDestination.HOME) }
            )
        }

        // Smart Adaptive Overlay HUD (Collapsible)
        AnimatedVisibility(
            visible = isHudVisible && !isFullscreen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    // Top Row: Title, Back, Switcher, Collapse
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = { viewModel.navigateTo(AppNavDestination.HOME) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home", tint = CyberCyan)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = session.currentTitle.ifEmpty { app.name },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (app.isPinned) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = AmberAlert,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (session.isDesktopMode) "Desktop Mode • ${session.zoomPercent}%" else "Mobile Mode • ${session.zoomPercent}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right actions: DevConsole badge, Tab Switcher, Collapse HUD
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // DevConsole button with error badge
                            Box {
                                IconButton(
                                    onClick = { viewModel.openDevConsole(app) },
                                    modifier = Modifier.size(34.dp).testTag("hud_dev_console")
                                ) {
                                    Icon(
                                        Icons.Default.BugReport,
                                        contentDescription = "Diagnostics",
                                        tint = if (errorCount > 0) RoseError else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (errorCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(RoseError),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$errorCount",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Switcher button
                            IconButton(
                                onClick = onOpenSwitcherSheet,
                                modifier = Modifier.size(34.dp).testTag("hud_app_switcher")
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = "Switcher", tint = EmeraldGlow)
                            }

                            // Collapse HUD button
                            IconButton(
                                onClick = { isHudVisible = false },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Hide HUD")
                            }
                        }
                    }

                    // Bottom Control Tools Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // History back / forward
                        Row {
                            IconButton(
                                onClick = { if (session.webView.canGoBack()) session.webView.goBack() },
                                enabled = session.webView.canGoBack(),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { if (session.webView.canGoForward()) session.webView.goForward() },
                                enabled = session.webView.canGoForward(),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Reload & Hard Reload
                        Row {
                            IconButton(
                                onClick = { viewModel.sessionManager.reloadSession(app.id, hardReload = false) },
                                modifier = Modifier.size(32.dp).testTag("hud_reload")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.sessionManager.toggleDesktopMode(app.id) },
                                modifier = Modifier.size(32.dp).testTag("hud_desktop_toggle")
                            ) {
                                Icon(
                                    if (session.isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                                    contentDescription = "Toggle Desktop",
                                    tint = if (session.isDesktopMode) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Zoom In & Out
                        Row {
                            IconButton(
                                onClick = { viewModel.sessionManager.setZoom(app.id, session.zoomPercent - 20) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.sessionManager.setZoom(app.id, session.zoomPercent + 20) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Shortcuts (Files, Editor, Fullscreen, More)
                        Row {
                            IconButton(
                                onClick = { viewModel.openFileManager(app) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "Files", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.openCodeEditor(app, app.entryPoint) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Code", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { isFullscreen = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", modifier = Modifier.size(16.dp))
                            }

                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (app.isPinned) "Unpin from Top" else "Pin to Top") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = if (app.isPinned) AmberAlert else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.togglePinApp(app)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename App") },
                                        leadingIcon = {
                                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = CyberCyan)
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            showRenameDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hard Reload (Clear Cache)") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.sessionManager.reloadSession(app.id, hardReload = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Storage & Site Data") },
                                        leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.openStorageInspector(app)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Reset Zoom (100%)") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.sessionManager.setZoom(app.id, 100)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // HUD Expand Pill when collapsed
        if (!isHudVisible && !isFullscreen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clickable { isHudVisible = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CyberCyan))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HUD CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }
        }

        // Exit Fullscreen Floating Pill
        if (isFullscreen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clickable { isFullscreen = false },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Default.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    modifier = Modifier.padding(12.dp),
                    tint = Color.White
                )
            }
        }

        if (showRenameDialog) {
            var editName by remember { mutableStateOf(app.name) }
            var editDesc by remember { mutableStateOf(app.description) }
            val activePrimary = MaterialTheme.colorScheme.primary

            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = activePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Rename App", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            "Update the application display name and optional description.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("App Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editDesc,
                            onValueChange = { editDesc = it },
                            label = { Text("Description (optional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                viewModel.renameApp(app.id, editName.trim(), editDesc.trim())
                                showRenameDialog = false
                            }
                        },
                        enabled = editName.isNotBlank()
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ErrorRecoveryOverlay(
    appName: String,
    errorMessage: String,
    onReload: () -> Unit,
    onHardReload: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE090D16))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Application Runtime Fault",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = RoseError
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Failed to load: $appName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReload,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                    ) {
                        Text("Reload")
                    }
                    Button(
                        onClick = onHardReload,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White)
                    ) {
                        Text("Hard Reload")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.weight(1f)) {
                        Text("Diagnostics")
                    }
                    OutlinedButton(onClick = onClearCache, modifier = Modifier.weight(1f)) {
                        Text("Clear Cache")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onBack) {
                    Text("Return to Home")
                }
            }
        }
    }
}
