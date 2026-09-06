package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.AppEntity
import com.example.data.model.FileCategory
import com.example.data.model.ProjectFile
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun AppFileManagerScreen(
    viewModel: AppViewModel,
    app: AppEntity
) {
    val context = LocalContext.current
    val currentSubPath by viewModel.currentFolderPath.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameAppDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<ProjectFile?>(null) }
    var fileToDelete by remember { mutableStateOf<ProjectFile?>(null) }

    BackHandler {
        if (currentSubPath.isNotEmpty()) {
            val parent = currentSubPath.substringBeforeLast("/", "").ifEmpty { "" }
            viewModel.navigateFolder(parent)
        } else {
            viewModel.navigateTo(AppNavDestination.HOME)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentSubPath.isNotEmpty()) {
                        val parent = currentSubPath.substringBeforeLast("/", "").ifEmpty { "" }
                        viewModel.navigateFolder(parent)
                    } else {
                        viewModel.navigateTo(AppNavDestination.HOME)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
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
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showRenameAppDialog = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("rename_app_btn")
                        ) {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                contentDescription = "Rename App",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.togglePinApp(app) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("pin_app_btn")
                        ) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = if (app.isPinned) "Unpin App" else "Pin App",
                                tint = if (app.isPinned) AmberAlert else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Text(
                        text = if (currentSubPath.isEmpty()) "Root Directory" else "/$currentSubPath",
                        fontSize = 12.sp,
                        color = CyberCyan
                    )
                }
            }

            Row {
                IconButton(onClick = { showCreateFileDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add File", tint = CyberCyan)
                }
                IconButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder", tint = ElectricViolet)
                }
                IconButton(onClick = {
                    // Export Project as ZIP
                    CoroutineScope(Dispatchers.IO).launch {
                        val cacheZip = File(context.cacheDir, "${app.name.replace(" ", "_")}_project.zip")
                        FileOutputStream(cacheZip).use { fos ->
                            viewModel.appRepository.exportAppAsZip(app, fos)
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheZip)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Project ZIP"))
                    }
                }) {
                    Icon(Icons.Default.FolderZip, contentDescription = "Export ZIP")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Files List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (projectFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Folder is empty", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Create a new HTML, CSS, or JS file using the '+' button above.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(projectFiles, key = { it.relativePath }) { file ->
                    val isEntryPoint = (file.relativePath == app.entryPoint)
                    ProjectFileRow(
                        file = file,
                        isEntryPoint = isEntryPoint,
                        onOpen = {
                            if (file.isDirectory) {
                                viewModel.navigateFolder(file.relativePath)
                            } else {
                                viewModel.openCodeEditor(app, file.relativePath)
                            }
                        },
                        onSetEntryPoint = {
                            viewModel.updateEntryPoint(app.id, file.relativePath)
                        },
                        onRename = { fileToRename = file },
                        onDelete = { fileToDelete = file }
                    )
                }
            }
        }
    }

    // Dialog: Rename App
    if (showRenameAppDialog) {
        var editName by remember { mutableStateOf(app.name) }
        var editDesc by remember { mutableStateOf(app.description) }
        val activePrimary = MaterialTheme.colorScheme.primary

        AlertDialog(
            onDismissRequest = { showRenameAppDialog = false },
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
                            showRenameAppDialog = false
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Create File
    if (showCreateFileDialog) {
        var newFileName by remember { mutableStateOf("script.js") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create New File") },
            text = {
                Column {
                    Text("Enter filename with extension (.html, .css, .js, .json):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.appRepository.createNewFile(app, currentSubPath, newFileName.trim(), false)
                            viewModel.refreshProjectFiles(app, currentSubPath)
                        }
                        showCreateFileDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Create Folder
    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("assets") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    placeholder = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.appRepository.createNewFile(app, currentSubPath, newFolderName.trim(), true)
                            viewModel.refreshProjectFiles(app, currentSubPath)
                        }
                        showCreateFolderDialog = false
                    }
                }) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Rename
    fileToRename?.let { file ->
        var renameInput by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.appRepository.renameProjectFile(app, file.relativePath, renameInput.trim())
                            viewModel.refreshProjectFiles(app, currentSubPath)
                        }
                        fileToRename = null
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Delete
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete ${if (file.isDirectory) "Folder" else "File"}?") },
            text = { Text("Are you sure you want to delete '${file.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.appRepository.deleteProjectFile(app, file.relativePath)
                            viewModel.refreshProjectFiles(app, currentSubPath)
                        }
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProjectFileRow(
    file: ProjectFile,
    isEntryPoint: Boolean,
    onOpen: () -> Unit,
    onSetEntryPoint: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .border(
                1.dp,
                if (isEntryPoint) CyberCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getFileCategoryBg(file.category)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFileCategoryIcon(file.category),
                        contentDescription = null,
                        tint = getFileCategoryTint(file.category),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = file.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isEntryPoint) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyberCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ENTRY POINT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (file.isDirectory) "Directory" else "${formatBytes(file.sizeBytes)} • ${file.relativePath}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!file.isDirectory && (file.name.endsWith(".html") || file.name.endsWith(".htm"))) {
                        DropdownMenuItem(
                            text = { Text("Set as Entry Point") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = CyberCyan) },
                            onClick = { menuOpen = false; onSetEntryPoint() }
                        )
                    }
                    if (!file.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Open in Editor") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onOpen() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = RoseError) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

fun getFileCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.DIRECTORY -> Icons.Default.Folder
        FileCategory.HTML -> Icons.Default.Description
        FileCategory.CSS -> Icons.Default.Code
        FileCategory.JAVASCRIPT -> Icons.Default.Code
        FileCategory.JSON -> Icons.Default.Code
        FileCategory.IMAGE -> Icons.Default.Image
        FileCategory.FONT -> Icons.Default.TextFields
        FileCategory.MEDIA -> Icons.Default.MusicNote
        else -> Icons.Default.Description
    }
}

fun getFileCategoryBg(category: FileCategory): Color {
    return when (category) {
        FileCategory.DIRECTORY -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        FileCategory.HTML -> Color(0xFFEF4444).copy(alpha = 0.15f)
        FileCategory.CSS -> Color(0xFF3B82F6).copy(alpha = 0.15f)
        FileCategory.JAVASCRIPT -> Color(0xFFEAB308).copy(alpha = 0.15f)
        FileCategory.JSON -> Color(0xFF10B981).copy(alpha = 0.15f)
        FileCategory.IMAGE -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
        else -> Color(0xFF64748B).copy(alpha = 0.15f)
    }
}

fun getFileCategoryTint(category: FileCategory): Color {
    return when (category) {
        FileCategory.DIRECTORY -> Color(0xFFF59E0B)
        FileCategory.HTML -> Color(0xFFEF4444)
        FileCategory.CSS -> Color(0xFF3B82F6)
        FileCategory.JAVASCRIPT -> Color(0xFFEAB308)
        FileCategory.JSON -> Color(0xFF10B981)
        FileCategory.IMAGE -> Color(0xFF8B5CF6)
        else -> Color(0xFF94A3B8)
    }
}
