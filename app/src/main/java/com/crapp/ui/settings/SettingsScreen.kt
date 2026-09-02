package com.crapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.AppConfig
import com.crapp.data.prefs.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageFoodCatalog: () -> Unit,
    onViewInsights: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::backupTo) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::restoreFrom) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = "Appearance") {
                Column {
                    ThemeOptionRow("System default", ThemeMode.SYSTEM, uiState.themeMode, viewModel::setThemeMode)
                    ThemeOptionRow("Light", ThemeMode.LIGHT, uiState.themeMode, viewModel::setThemeMode)
                    ThemeOptionRow("Dark", ThemeMode.DARK, uiState.themeMode, viewModel::setThemeMode)
                }
            }

            SettingsSection(title = "Food Catalog") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Add or edit ingredients for foods you've logged, so they can be " +
                            "cross-checked against ${AppConfig.DOG_NAME}'s bowel movements for " +
                            "possible triggers.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = onManageFoodCatalog, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage Foods & Ingredients")
                    }
                }
            }

            SettingsSection(title = "Insights") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "View an analysis report generated from an exported CSV (see Export) -- " +
                            "trends, frequency, and possible correlations, surfaced outside the " +
                            "app and uploaded back in as a file.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = onViewInsights, modifier = Modifier.fillMaxWidth()) {
                        Text("View Insights")
                    }
                }
            }

            SettingsSection(title = "Backup & Restore") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Back up all of ${AppConfig.DOG_NAME}'s logged data to a file you save " +
                            "yourself (Drive, Files, email, etc.), so an uninstall, a phone reset, " +
                            "or a new phone doesn't lose it. A normal app update alone never loses " +
                            "data -- this is only needed for those bigger changes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { backupLauncher.launch(defaultBackupFileName()) },
                        enabled = uiState.status == BackupStatus.IDLE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.status == BackupStatus.WORKING) "Working…" else "Back Up Data")
                    }
                    OutlinedButton(
                        onClick = { showRestoreConfirm = true },
                        enabled = uiState.status == BackupStatus.IDLE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore from Backup")
                    }
                }
            }

            SettingsSection(title = "Danger Zone") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Permanently deletes every bowel movement, food entry, medication " +
                            "entry, and the food catalog itself -- back up first if you want a " +
                            "way back.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { showClearDataConfirm = true },
                        enabled = uiState.status == BackupStatus.IDLE,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Data")
                    }
                }
            }

            uiState.message?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from backup?") },
            text = {
                Text(
                    "This replaces everything currently logged on this phone with the contents " +
                        "of the backup file you choose next. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    restoreLauncher.launch(arrayOf("application/json"))
                }) { Text("Choose File") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear all data?") },
            text = {
                Text(
                    "This permanently deletes every bowel movement, food entry, medication " +
                        "entry, and the food catalog. This can't be undone -- back up first " +
                        "(above) if you want a way back."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataConfirm = false
                    viewModel.clearAllData()
                }) { Text("Clear Everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(vertical = 2.dp)
    ) {
        RadioButton(selected = selectedMode == mode, onClick = { onSelect(mode) })
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun defaultBackupFileName(): String =
    "crapp_backup_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
