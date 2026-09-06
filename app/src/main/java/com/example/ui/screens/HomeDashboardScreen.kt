package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveAppSession
import com.example.data.model.AppEntity
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.ThemeMode
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError
import com.example.ui.theme.NeomorphicCard
import com.example.ui.theme.NeomorphicInsetBox
import com.example.ui.theme.NeomorphicButton
import com.example.ui.theme.isDarkTheme

@Composable
fun HomeDashboardScreen(
    viewModel: AppViewModel,
    onOpenImportDialog: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onOpenSwitcherSheet: () -> Unit
) {
    val allApps by viewModel.allApps.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val downloads by viewModel.allDownloads.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalStorageBytes = remember(allApps) {
        allApps.sumOf { it.storageSizeBytes }
    }

    val themeMode by viewModel.themeMode.collectAsState()
    val scratchpadNotes by viewModel.scratchpadNotes.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Hero Header & Workspace Branding
        item {
            HeaderSection(
                activeCount = activeSessions.size,
                totalApps = allApps.size,
                totalStorageBytes = totalStorageBytes,
                themeMode = themeMode,
                onToggleTheme = {
                    val next = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                    viewModel.setThemeMode(next)
                },
                onOpenMiniBrowser = { viewModel.navigateTo(AppNavDestination.MINI_BROWSER) },
                onOpenSettings = { viewModel.navigateTo(AppNavDestination.SETTINGS) },
                onOpenDownloads = { viewModel.navigateTo(AppNavDestination.DOWNLOADS) },
                downloadsCount = downloads.size
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Action Bar (Create, Import, Switcher)
        item {
            QuickActionsBar(
                onCreateNew = onOpenCreateDialog,
                onImport = onOpenImportDialog,
                onOpenSwitcher = onOpenSwitcherSheet,
                activeCount = activeSessions.size
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Dedicated Quick Navigation Hub: Mini Browser & Settings Menu
        item {
            val isDark = MaterialTheme.isDarkTheme
            val activePrimary = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini Browser Card
                NeomorphicCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AppNavDestination.MINI_BROWSER) }
                        .testTag("dashboard_mini_browser_card"),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(activePrimary.copy(alpha = if (isDark) 0.2f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = activePrimary, modifier = Modifier.size(20.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activePrimary.copy(alpha = if (isDark) 0.2f else 0.1f)
                            ) {
                                Text(
                                    "LIVE WEB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Mini Browser", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Browse & test web URLs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Settings Menu Card
                NeomorphicCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AppNavDestination.SETTINGS) }
                        .testTag("dashboard_settings_menu_card"),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ElectricViolet.copy(alpha = if (isDark) 0.2f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricViolet.copy(alpha = if (isDark) 0.2f else 0.1f)
                            ) {
                                Text(
                                    "CONFIG",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Settings Menu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Theme, backup & tools", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Quick Notes Scratchpad Card
        item {
            QuickNotesScratchpadCard(
                notes = scratchpadNotes,
                onNotesChange = { viewModel.saveScratchpadNotes(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Running Sessions Bar (if any)
        if (activeSessions.isNotEmpty()) {
            item {
                Text(
                    text = "LIVE ACTIVE RUNTIMES (${activeSessions.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activeSessions, key = { it.appId }) { session ->
                        ActiveSessionCard(
                            session = session,
                            onClick = { viewModel.switchToRunningApp(session.appId) },
                            onClose = { viewModel.closeRunningSession(session.appId) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Search Bar with recessed Neomorphic Inset
        item {
            val activePrimary = MaterialTheme.colorScheme.primary
            NeomorphicInsetBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_apps_input"),
                    placeholder = { Text("Search installed HTML apps...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = activePrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Installed Apps Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "INSTALLED APPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    val pinnedCount = allApps.count { it.isPinned }
                    if (pinnedCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberAlert.copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = AmberAlert,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$pinnedCount Pinned",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAlert
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "${filteredApps.size} apps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Empty State or Apps List
        if (filteredApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Your App Hub is Ready" else "No matching apps found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Import an HTML app, ZIP project, or create one using a template." else "Try a different search keyword.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenCreateDialog,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First App", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredApps, key = { it.id }) { app ->
                val isRunning = activeSessions.any { it.appId == app.id }
                AppCardItem(
                    app = app,
                    isRunning = isRunning,
                    onLaunch = { viewModel.launchApp(app) },
                    onOpenFiles = { viewModel.openFileManager(app) },
                    onOpenEditor = { viewModel.openCodeEditor(app, app.entryPoint) },
                    onOpenConsole = { viewModel.openDevConsole(app) },
                    onOpenStorage = { viewModel.openStorageInspector(app) },
                    onTogglePin = { viewModel.togglePinApp(app) },
                    onRename = { newName, newDesc -> viewModel.renameApp(app.id, newName, newDesc) },
                    onDuplicate = { viewModel.duplicateApp(app) },
                    onDelete = { viewModel.deleteApp(app) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(
    activeCount: Int,
    totalApps: Int,
    totalStorageBytes: Long,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onOpenMiniBrowser: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    downloadsCount: Int
) {
    val isDark = MaterialTheme.isDarkTheme
    val activeAccent = MaterialTheme.colorScheme.primary

    NeomorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 5.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isDark) listOf(Color(0xFF0284C7), Color(0xFF6366F1))
                                    else listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "App Hub Pro",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeAccent.copy(alpha = if (isDark) 0.2f else 0.12f)
                            ) {
                                Text(
                                    text = "PRO WORKSPACE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Universal HTML Mobile Workspace",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val iconBg = if (isDark) Color(0xFF1E2838) else Color(0xFFE2E8F0)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBg)
                            .clickable(onClick = onToggleTheme)
                            .testTag("nav_theme_toggle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (themeMode == ThemeMode.LIGHT) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Toggle Day/Night Mode",
                            tint = activeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBg)
                            .clickable(onClick = onOpenMiniBrowser)
                            .testTag("nav_mini_browser_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Mini Browser", tint = activeAccent, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBg)
                            .clickable(onClick = onOpenDownloads)
                            .testTag("nav_downloads_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Downloads", tint = activeAccent, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBg)
                            .clickable(onClick = onOpenSettings)
                            .testTag("nav_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Workspace Metrics Recessed Gauge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Active Runtimes",
                    value = "$activeCount",
                    color = if (activeCount > 0) EmeraldGlow else MaterialTheme.colorScheme.onSurfaceVariant
                )
                MetricChip(
                    modifier = Modifier.weight(1f),
                    label = "Total Apps",
                    value = "$totalApps",
                    color = activeAccent
                )
                MetricChip(
                    modifier = Modifier.weight(1.1f),
                    label = "Storage",
                    value = formatBytes(totalStorageBytes),
                    color = ElectricViolet
                )
            }
        }
    }
}

@Composable
fun MetricChip(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    NeomorphicInsetBox(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun QuickActionsBar(
    onCreateNew: () -> Unit,
    onImport: () -> Unit,
    onOpenSwitcher: () -> Unit,
    activeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NeomorphicButton(
            onClick = onCreateNew,
            isPrimary = true,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("action_new_app"),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New App", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }

        NeomorphicButton(
            onClick = onImport,
            isPrimary = false,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("action_import_project"),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricViolet)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        NeomorphicButton(
            onClick = onOpenSwitcher,
            isPrimary = false,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .testTag("action_app_switcher"),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldGlow)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tabs ($activeCount)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    session: ActiveAppSession,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    NeomorphicCard(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldGlow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGlow
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = session.appName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${session.memoryEstimateMb.toInt()} MB • ${if (session.isDesktopMode) "Desktop" else "Mobile"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppCardItem(
    app: AppEntity,
    isRunning: Boolean,
    onLaunch: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenStorage: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val activePrimary = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    NeomorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(getAppIconBackground(app.iconName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAppIconVector(app.iconName),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = app.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (app.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AmberAlert.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = AmberAlert,
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "PINNED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberAlert
                                        )
                                    }
                                }
                            }
                            if (isRunning) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGlow)
                                )
                            }
                            if (app.isSampleApp) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricViolet.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "DEMO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricViolet,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (app.description.isNotEmpty()) app.description else "Entry: ${app.entryPoint}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: ${formatBytes(app.storageSizeBytes)} • ${app.entryPoint}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pin_button_${app.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (app.isPinned) "Unpin App" else "Pin App",
                            tint = if (app.isPinned) AmberAlert else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("menu_button_${app.id}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
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
                                    menuExpanded = false
                                    onTogglePin()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename App") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DriveFileRenameOutline,
                                        contentDescription = null,
                                        tint = activePrimary
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Code Editor") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenEditor() }
                            )
                            DropdownMenuItem(
                                text = { Text("Project Files") },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenFiles() }
                            )
                            DropdownMenuItem(
                                text = { Text("DevConsole Logs") },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenConsole() }
                            )
                            DropdownMenuItem(
                                text = { Text("Storage Inspector") },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                                onClick = { menuExpanded = false; onOpenStorage() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate App") },
                                leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                                onClick = { menuExpanded = false; onDuplicate() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete App", color = RoseError) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError) },
                                onClick = { menuExpanded = false; showDeleteConfirm = true }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLaunch,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) EmeraldGlow else activePrimary,
                        contentColor = if (isRunning) Color.Black else onPrimaryColor
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "Switch Tab" else "Run App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenFiles,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Files", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Files", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenEditor,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }
            }
        }
    }

    if (showRenameDialog) {
        var editName by remember { mutableStateOf(app.name) }
        var editDesc by remember { mutableStateOf(app.description) }

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
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onRename(editName.trim(), editDesc.trim())
                            showRenameDialog = false
                        }
                    },
                    enabled = editName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activePrimary,
                        contentColor = onPrimaryColor
                    )
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete App?") },
            text = { Text("Are you sure you want to delete '${app.name}' and all its files? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getAppIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "calculate" -> Icons.Default.Calculate
        "edit_note" -> Icons.Default.EditNote
        "palette" -> Icons.Default.Palette
        "bug_report", "code" -> Icons.Default.Code
        "html" -> Icons.Default.Description
        "folder_zip" -> Icons.Default.FolderZip
        else -> Icons.Default.Web
    }
}

fun getAppIconBackground(iconName: String): Brush {
    return when (iconName) {
        "calculate" -> Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))
        "edit_note" -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
        "palette" -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
        "bug_report", "code" -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
        "folder_zip" -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
        else -> Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

@Composable
fun QuickNotesScratchpadCard(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    var text by remember(notes) { mutableStateOf(notes) }
    var isSaved by remember { mutableStateOf(false) }
    val activePrimary = MaterialTheme.colorScheme.primary

    NeomorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = activePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEV SCRATCHPAD & NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = activePrimary, letterSpacing = 1.sp)
                }
                if (isSaved) {
                    Text("Auto-saved", fontSize = 11.sp, color = EmeraldGlow, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            NeomorphicInsetBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onNotesChange(it)
                        isSaved = true
                    },
                    placeholder = { Text("Jot down quick code snippets, URLs, notes... auto-saved locally", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

