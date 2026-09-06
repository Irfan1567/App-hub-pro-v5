package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppEntity
import com.example.data.model.ConsoleLogEntity
import com.example.data.model.LogSeverity
import com.example.runtime.AppResourceManager
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsConsoleScreen(
    viewModel: AppViewModel,
    app: AppEntity
) {
    val context = LocalContext.current
    val logs by viewModel.consoleLogs.collectAsState()
    var selectedFilter by remember { mutableStateOf<LogSeverity?>(null) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs
        else logs.filter { it.severity == selectedFilter }
    }

    val webViewPackage = remember {
        try {
            WebView.getCurrentWebViewPackage()?.versionName ?: "Standard Android WebView"
        } catch (e: Exception) {
            "Android System WebView"
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
                IconButton(onClick = { viewModel.navigateTo(AppNavDestination.PLAYER) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("DevConsole & Diagnostics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(app.name, fontSize = 12.sp, color = CyberCyan)
                }
            }

            Row {
                IconButton(onClick = {
                    val fullReport = buildDiagnosticsText(app, webViewPackage, logs)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("AppHub Diagnostics", fullReport))
                    Toast.makeText(context, "Diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report")
                }
                IconButton(onClick = {
                    val fullReport = buildDiagnosticsText(app, webViewPackage, logs)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, fullReport)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Diagnostics"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = { viewModel.clearDevConsole(app.id) }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = RoseError)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System Diagnostic Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("RUNTIME ENVIRONMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Spacer(modifier = Modifier.height(4.dp))
                Text("WebView: $webViewPackage", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Origin: ${AppResourceManager.getAppOrigin(app.id)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Severity Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = (selectedFilter == null),
                onClick = { selectedFilter = null },
                label = { Text("All (${logs.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyberCyan.copy(alpha = 0.2f), selectedLabelColor = CyberCyan)
            )
            FilterChip(
                selected = (selectedFilter == LogSeverity.ERROR),
                onClick = { selectedFilter = LogSeverity.ERROR },
                label = { Text("Errors (${logs.count { it.severity == LogSeverity.ERROR }})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoseError.copy(alpha = 0.2f), selectedLabelColor = RoseError)
            )
            FilterChip(
                selected = (selectedFilter == LogSeverity.WARN),
                onClick = { selectedFilter = LogSeverity.WARN },
                label = { Text("Warnings", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AmberAlert.copy(alpha = 0.2f), selectedLabelColor = AmberAlert)
            )
            FilterChip(
                selected = (selectedFilter == LogSeverity.NETWORK),
                onClick = { selectedFilter = LogSeverity.NETWORK },
                label = { Text("Network", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldGlow.copy(alpha = 0.2f), selectedLabelColor = EmeraldGlow)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logs Output Area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030712), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentPadding = PaddingValues(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Console is clean. No errors recorded.",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    ConsoleLogEntryRow(log = log)
                }
            }
        }
    }
}

@Composable
fun ConsoleLogEntryRow(log: ConsoleLogEntity) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val (badgeColor, badgeText) = when (log.severity) {
        LogSeverity.ERROR -> Pair(RoseError, "ERR")
        LogSeverity.WARN -> Pair(AmberAlert, "WRN")
        LogSeverity.NETWORK -> Pair(EmeraldGlow, "NET")
        LogSeverity.INFO -> Pair(CyberCyan, "LOG")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeFormat.format(Date(log.timestamp)),
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (log.sourceId.isNotEmpty() || log.lineNumber > 0) {
                    Text(
                        text = "${log.sourceId.substringAfterLast("/")}:${log.lineNumber}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = when (log.severity) {
                    LogSeverity.ERROR -> Color(0xFFFCA5A5)
                    LogSeverity.WARN -> Color(0xFFFDE68A)
                    else -> Color(0xFFE2E8F0)
                }
            )
        }
    }
}

fun buildDiagnosticsText(app: AppEntity, webViewVersion: String, logs: List<ConsoleLogEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("=== APP HUB PRO DIAGNOSTICS REPORT ===")
    sb.appendLine("App Name: ${app.name} (${app.id})")
    sb.appendLine("Entry Point: ${app.entryPoint}")
    sb.appendLine("Origin: ${AppResourceManager.getAppOrigin(app.id)}")
    sb.appendLine("WebView: $webViewVersion")
    sb.appendLine("Generated At: ${Date()}")
    sb.appendLine("---------------------------------------")
    sb.appendLine("LOG ENTRIES (${logs.size}):")
    logs.forEach { log ->
        sb.appendLine("[${log.severity}] ${log.message} (Source: ${log.sourceId}:${log.lineNumber})")
    }
    sb.appendLine("=======================================")
    return sb.toString()
}
