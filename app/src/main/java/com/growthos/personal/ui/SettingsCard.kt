package com.growthos.personal.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growthos.personal.data.BackupTools
import com.growthos.personal.data.SettingsStore
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsCard() {
    val c = Growth.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val text = BackupTools.buildBackup(context)
                        val stream = context.contentResolver.openOutputStream(uri, "wt")
                        if (stream == null) {
                            "Backup failed. Please try again."
                        } else {
                            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                            "Backup saved."
                        }
                    } catch (e: Exception) {
                        "Backup failed. Please try again."
                    }
                }
                message = result
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val text = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                        if (text == null || text.length > 20000000) {
                            "Could not read this file."
                        } else {
                            BackupTools.restoreBackup(context, text)
                        }
                    } catch (e: Exception) {
                        "Could not read this file."
                    }
                }
                message = result
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Settings", color = c.textPrimary, fontSize = 18.sp)
            Text(text = "Appearance", color = c.textPrimary, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { index, label ->
                    LogChip(
                        label = label,
                        selected = ThemePrefs.mode == index,
                        onClick = {
                            ThemePrefs.mode = index
                            SettingsStore(context).setThemeMode(index)
                        }
                    )
                }
            }
            Text(text = "Backup", color = c.textPrimary, fontSize = 15.sp)
            Text(
                text = "Your data is stored only on this phone and the app has no internet access. " +
                    "Save a backup file now and then (for example into Google Drive from the Files screen).",
                color = c.textSecondary,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        message = null
                        exportLauncher.launch("growth-os-backup-" + LocalDate.now().toString() + ".json")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Text("Export backup")
                }
                TextButton(onClick = { confirmRestore = true }) {
                    Text("Restore", color = c.accent)
                }
            }
            val note = message
            if (note != null) {
                Text(text = note, color = c.textPrimary, fontSize = 14.sp)
            }
            Text(
                text = "Reminders and the wake alarm are managed in the Plan tab.",
                color = c.textSecondary,
                fontSize = 12.sp
            )
            if (version.isNotEmpty()) {
                Text(text = "Growth OS version $version", color = c.textSecondary, fontSize = 12.sp)
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore backup?") },
            text = {
                Text("This replaces ALL current data on this phone with the data in the backup file. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            }
        )
    }
}
