package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet

@Composable
fun CreateAppDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, templateType: String, description: String) -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("standard") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Create New Web App", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    placeholder = { Text("e.g. My Dashboard") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    placeholder = { Text("e.g. Interactive financial calculator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "STARTER TEMPLATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                TemplateOptionCard(
                    title = "Standard Web App",
                    subtitle = "Modular index.html, styles.css, app.js",
                    icon = Icons.Default.Web,
                    isSelected = selectedTemplate == "standard",
                    onClick = { selectedTemplate = "standard" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TemplateOptionCard(
                    title = "Interactive Canvas Studio",
                    subtitle = "HTML5 Canvas drawing with touch support",
                    icon = Icons.Default.Palette,
                    isSelected = selectedTemplate == "canvas",
                    onClick = { selectedTemplate = "canvas" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TemplateOptionCard(
                    title = "React 18 SPA (CDN)",
                    subtitle = "Modern declarative UI with Babel standalone",
                    icon = Icons.Default.Code,
                    isSelected = selectedTemplate == "react",
                    onClick = { selectedTemplate = "react" }
                )
            }
        },
        confirmButton = {
            val activePrimary = MaterialTheme.colorScheme.primary
            Button(
                onClick = {
                    if (appName.isNotBlank()) {
                        onCreate(appName.trim(), selectedTemplate, description.trim())
                        onDismiss()
                    }
                },
                enabled = appName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activePrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create App", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TemplateOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activePrimary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isSelected) activePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activePrimary else ElectricViolet,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) activePrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
