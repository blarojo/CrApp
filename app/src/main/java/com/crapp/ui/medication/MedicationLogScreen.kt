package com.crapp.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.ui.common.DateTimeField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationLogScreen(
    onDone: () -> Unit,
    viewModel: MedicationLogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medications by viewModel.medicationsByRecentUse.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Medication" else "Log Medication") },
                actions = {
                    if (uiState.isEditing) {
                        TextButton(onClick = viewModel::delete) { Text("Delete") }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DateTimeField(value = uiState.timestamp, onValueChange = viewModel::onTimestampChange)

            Text(text = "Medication", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedMedication?.name ?: "Select a medication",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (medications.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No medications logged yet", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {},
                            enabled = false
                        )
                    }
                    medications.forEach { medication ->
                        DropdownMenuItem(
                            text = { Text(medication.name) },
                            onClick = {
                                viewModel.onMedicationSelected(medication)
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Add new medication") },
                        onClick = {
                            expanded = false
                            viewModel.onShowAddNewDialog(true)
                        }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.dose,
                onValueChange = viewModel::onDoseChange,
                label = { Text("Dose (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Structured dose (optional)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.doseValueText,
                onValueChange = viewModel::onDoseValueTextChange,
                label = { Text("Value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MEDICATION_DOSE_UNITS.forEach { unit ->
                    FilterChip(
                        selected = uiState.doseUnit == unit,
                        onClick = { viewModel.onDoseUnitChange(unit) },
                        label = { Text(unit) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                enabled = uiState.selectedMedication != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    if (uiState.showAddNewDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowAddNewDialog(false) },
            title = { Text("Add new medication") },
            text = {
                OutlinedTextField(
                    value = uiState.newMedicationName,
                    onValueChange = viewModel::onNewMedicationNameChange,
                    label = { Text("Medication name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmAddNewMedication,
                    enabled = uiState.newMedicationName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowAddNewDialog(false) }) { Text("Cancel") }
            }
        )
    }
}
