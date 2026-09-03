package com.crapp.ui.medicationcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.data.model.Medication

/**
 * Admin screen for the medication catalog -- add or delete entries so the
 * medication-log screen's dropdown stays curated (mirrors `FoodCatalogScreen`,
 * minus ingredients since medications don't have those). Deleting is never
 * blocked -- see [com.crapp.data.model.MedicationEntry]'s KDoc on why, unlike
 * deleting a food.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationCatalogScreen(
    onBack: () -> Unit,
    viewModel: MedicationCatalogViewModel = viewModel()
) {
    val medications by viewModel.medications.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medication Catalog") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Filled.Add, contentDescription = "Add medication")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { innerPadding ->
        if (medications.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    "No medications yet -- tap + to add one, or they'll show up here " +
                        "automatically once logged.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(medications, key = { it.id }) { medication ->
                    MedicationCatalogRow(
                        medication = medication,
                        onDeleteClick = { viewModel.requestDelete(medication) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::cancelAddDialog,
            title = { Text("Add medication") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addMedication(name) }, enabled = name.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelAddDialog) { Text("Cancel") } }
        )
    }

    pendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete \"${medication.name}\"?") },
            text = {
                Text(
                    "Existing logged entries keep their recorded name and dose -- they just " +
                        "won't be linked to this catalog entry anymore."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MedicationCatalogRow(medication: Medication, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                medication.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
                    .padding(16.dp)
            )
            IconButton(onClick = onDeleteClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${medication.name}")
            }
        }
    }
}
