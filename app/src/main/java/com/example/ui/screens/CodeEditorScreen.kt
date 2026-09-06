package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppEntity
import com.example.ui.AppNavDestination
import com.example.ui.AppViewModel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldGlow

@Composable
fun CodeEditorScreen(
    viewModel: AppViewModel,
    app: AppEntity
) {
    val context = LocalContext.current
    val filePath by viewModel.editingFilePath.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val isDirty by viewModel.editorIsDirty.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }

    val linesCount by remember(editorContent) {
        derivedStateOf {
            editorContent.lines().size.coerceAtLeast(1)
        }
    }

    BackHandler {
        if (isDirty) {
            viewModel.saveEditorContent {
                viewModel.navigateTo(AppNavDestination.FILE_MANAGER)
            }
        } else {
            viewModel.navigateTo(AppNavDestination.FILE_MANAGER)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        // Editor Header Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(AppNavDestination.FILE_MANAGER) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = filePath?.substringAfterLast("/") ?: "Editor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isDirty) {
                                Text(
                                    text = " • unsaved",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${app.name} / ${filePath ?: ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Search toggle
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = if (showSearchBar) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Save Button
                    Button(
                        onClick = {
                            viewModel.saveEditorContent {
                                Toast.makeText(context, "Saved & Synced!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("editor_save_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDirty) CyberCyan else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDirty) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Hot Preview in Runtime Player
                    Button(
                        onClick = {
                            viewModel.saveEditorContent {
                                viewModel.launchApp(app)
                            }
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("editor_preview_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar (if visible)
        AnimatedVisibility(visible = showSearchBar) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search text in file...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { showSearchBar = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                }
            }
        }

        // Code Editor Gutter + Text Field Area
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // Line numbers gutter
            Column(
                modifier = Modifier
                    .background(Color(0xFF090D16))
                    .border(1.dp, Color(0xFF1F2937).copy(alpha = 0.5f))
                    .padding(vertical = 12.dp, horizontal = 10.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..linesCount) {
                    Text(
                        text = "$i",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )
                }
            }

            // Code input area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = editorContent,
                    onValueChange = { viewModel.updateEditorText(it) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFE2E8F0)
                    ),
                    cursorBrush = SolidColor(CyberCyan),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_text_field")
                )
            }
        }
    }
}
