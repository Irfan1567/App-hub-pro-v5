package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow

@Composable
fun ImportWizardDialog(
    onDismiss: () -> Unit,
    onImportZipUri: (Uri, String) -> Unit,
    onImportHtmlFileUri: (Uri, String) -> Unit,
    onImportJsonBackupUri: (Uri, String) -> Unit,
    onImportRawHtml: (name: String, content: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Files, 1: Paste Code

    // SAF Launchers
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "imported_app.zip"
            if (fileName.endsWith(".json", ignoreCase = true)) {
                onImportJsonBackupUri(uri, fileName)
            } else {
                onImportZipUri(uri, fileName)
            }
            onDismiss()
        }
    }

    val htmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "standalone.html"
            if (fileName.endsWith(".json", ignoreCase = true)) {
                onImportJsonBackupUri(uri, fileName)
            } else {
                onImportHtmlFileUri(uri, fileName)
            }
            onDismiss()
        }
    }

    val jsonBackupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "apphub_backup.json"
            onImportJsonBackupUri(uri, fileName)
            onDismiss()
        }
    }

    var rawCodeName by remember { mutableStateOf("") }
    var rawCodeContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Import Projects & Backups", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Device File / Backup", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Paste Code / JSON", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val activePrimary = MaterialTheme.colorScheme.primary
                if (selectedTab == 0) {
                    // Option 1: App Hub JSON Backup (Apps + Data)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                jsonBackupPickerLauncher.launch(
                                    arrayOf("application/json", "text/json", "text/plain", "application/octet-stream", "*/*")
                                )
                            }
                            .border(1.dp, EmeraldGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DataObject, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Import JSON Backup (Apps + Data)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Restores full backup exported from HTML App Hub with all apps & storage.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 2: ZIP Package
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) }
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Import ZIP Package", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Unpacks folders, nested HTML, CSS, and JS assets automatically.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 3: Standalone Single HTML File
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { htmlPickerLauncher.launch(arrayOf("text/html", "application/xhtml+xml", "*/*")) }
                            .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Import Single .html File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Self-contained HTML file (auto-unwraps any embedded or exported code).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    // Paste Code / JSON
                    val isJsonDetected = rawCodeContent.trim().startsWith("{") || rawCodeContent.trim().startsWith("[")

                    OutlinedTextField(
                        value = rawCodeName,
                        onValueChange = { rawCodeName = it },
                        label = { Text(if (isJsonDetected) "Backup Label (Optional)" else "App Name") },
                        placeholder = { Text(if (isJsonDetected) "e.g. My App Hub Backup" else "e.g. OmniChat Pro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawCodeContent,
                        onValueChange = { rawCodeContent = it },
                        label = { Text(if (isJsonDetected) "JSON Backup Data (Detected!)" else "HTML Source Code") },
                        placeholder = { Text("Paste <!DOCTYPE html>... or JSON backup...") },
                        maxLines = 8,
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (rawCodeContent.isNotBlank()) {
                                onImportRawHtml(rawCodeName.trim(), rawCodeContent)
                                onDismiss()
                            }
                        },
                        enabled = rawCodeContent.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activePrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            if (isJsonDetected) "Restore Apps & Data from JSON" else "Save & Run App",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name ?: uri.lastPathSegment
}
