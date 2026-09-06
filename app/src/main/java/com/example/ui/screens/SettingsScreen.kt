package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import com.example.ui.ThemeMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError
import com.example.ui.theme.NeomorphicCard
import com.example.ui.theme.NeomorphicButton
import com.example.ui.theme.isDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val webViewInfo = remember {
        try {
            WebView.getCurrentWebViewPackage()?.let {
                "${it.packageName} v${it.versionName}"
            } ?: "Android System WebView"
        } catch (e: Exception) {
            "Android System WebView"
        }
    }

    // Restore File Picker Launcher
    val restorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val res = viewModel.backupManager.restoreWorkspaceBackup(stream)
                        if (res.isSuccess) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "Restored ${res.getOrNull()} apps successfully!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "Restore failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        val activeAccent = MaterialTheme.colorScheme.primary
        val onAccentColor = MaterialTheme.colorScheme.onPrimary

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppNavDestination.HOME) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Settings & Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Workspace configuration & tools", fontSize = 12.sp, color = activeAccent)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Appearance & Day/Night Mode Section
        val currentThemeMode by viewModel.themeMode.collectAsState()
        Text("APPEARANCE & THEME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activeAccent)
        Spacer(modifier = Modifier.height(8.dp))

        NeomorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Display Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Switch between Light, Dark, or System Day/Night mode", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    ThemeMode.SYSTEM to "System Default (Auto Day/Night)",
                    ThemeMode.LIGHT to "Light Mode (Soft Neomorphic Day)",
                    ThemeMode.DARK to "Dark Mode (Obsidian Clay Night)"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentThemeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (currentThemeMode == mode) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentThemeMode == mode) activeAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Workspace Backup & Restore Section
        Text("BACKUP & EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activeAccent)
        Spacer(modifier = Modifier.height(8.dp))

        NeomorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = activeAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Workspace Full Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Export all apps, scripts, and assets into an offline backup package.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val backupFile = File(context.cacheDir, "apphub_workspace_backup_${System.currentTimeMillis()}.apphubbackup")
                                FileOutputStream(backupFile).use { fos ->
                                    viewModel.backupManager.createWorkspaceBackup(fos)
                                }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Workspace Backup"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccent, contentColor = onAccentColor)
                    ) {
                        Text("Export Backup", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            restorePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Web Sandbox & Security Architecture Card
        Text("SECURITY & RUNTIME ISOLATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
        Spacer(modifier = Modifier.height(8.dp))

        NeomorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Per-App Isolated Virtual Origins", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Each app runs on its own virtual host origin (https://app-<id>.apphub.local/). LocalStorage, IndexedDB, cookies, and cache are completely isolated to prevent cross-app data leakage.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Direct file:// access is blocked to shield private device files from scripts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Specs Card
        Text("DIAGNOSTIC ENVIRONMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
        Spacer(modifier = Modifier.height(8.dp))

        NeomorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Engine Specifications", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("App Hub Pro: Version 1.0.0 (Release)", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("WebView Provider: $webViewInfo", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
