package com.crapp.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

private data class HistoryRowContent(
    val badgeText: String,
    val badgeColor: Color,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onEditBowelMovement: (Long) -> Unit,
    onEditFood: (Long) -> Unit,
    onEditMedication: (Long) -> Unit,
    onExport: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val zone = ZoneId.systemDefault()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } },
                actions = { TextButton(onClick = onExport) { Text("Export") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = HistoryEntryType.BOWEL_MOVEMENT in uiState.filter.types,
                    onClick = { viewModel.toggleType(HistoryEntryType.BOWEL_MOVEMENT) },
                    label = { Text("Bowel") }
                )
                FilterChip(
                    selected = HistoryEntryType.FOOD in uiState.filter.types,
                    onClick = { viewModel.toggleType(HistoryEntryType.FOOD) },
                    label = { Text("Food") }
                )
                FilterChip(
                    selected = HistoryEntryType.MEDICATION in uiState.filter.types,
                    onClick = { viewModel.toggleType(HistoryEntryType.MEDICATION) },
                    label = { Text("Medication") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showStartPicker = true }) {
                    Text(uiState.filter.startDate?.format(dateFormatter) ?: "From")
                }
                OutlinedButton(onClick = { showEndPicker = true }) {
                    Text(uiState.filter.endDate?.format(dateFormatter) ?: "To")
                }
                if (uiState.filter.startDate != null || uiState.filter.endDate != null) {
                    TextButton(onClick = viewModel::clearDateRange) { Text("Clear") }
                }
            }

            if (uiState.entries.isEmpty()) {
                Text("No entries yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.entries, key = { "${it.type}_${it.id}" }) { entry ->
                        HistoryRow(
                            entry = entry,
                            zone = zone,
                            onClick = {
                                when (entry) {
                                    is HistoryEntry.BowelMovementEntry -> onEditBowelMovement(entry.id)
                                    is HistoryEntry.FoodLogEntry -> onEditFood(entry.id)
                                    is HistoryEntry.MedicationLogEntry -> onEditMedication(entry.id)
                                }
                            },
                            onLongClick = { viewModel.requestDelete(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.filter.startDate ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.setStartDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.filter.endDate ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.setEndDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    uiState.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    zone: ZoneId,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val content = when (entry) {
        is HistoryEntry.BowelMovementEntry -> {
            val m = entry.movement
            val flags = buildList {
                if (m.hasBlood) add("blood")
                if (m.hasMucus) add("mucus")
            }
            HistoryRowContent(
                badgeText = "BOWEL",
                badgeColor = Color(0xFF6D4C41),
                title = "Consistency ${m.consistency}" + if (flags.isNotEmpty()) " (${flags.joinToString()})" else "",
                subtitle = m.notes ?: ""
            )
        }
        is HistoryEntry.FoodLogEntry -> {
            val e = entry.entry
            HistoryRowContent(
                badgeText = "FOOD",
                badgeColor = Color(0xFF2E7D32),
                title = entry.food?.name ?: "Unknown food",
                subtitle = e.mealType.name.lowercase().replaceFirstChar { it.uppercase() } +
                    (e.amount?.let { " · $it" } ?: "")
            )
        }
        is HistoryEntry.MedicationLogEntry -> {
            val e = entry.entry
            HistoryRowContent(
                badgeText = "MED",
                badgeColor = Color(0xFF1565C0),
                title = e.name,
                subtitle = e.dose ?: ""
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(content.badgeText, color = content.badgeColor, style = MaterialTheme.typography.labelLarge)
                Text(dateTimeFormatter.format(entry.timestamp.atZone(zone)), style = MaterialTheme.typography.labelMedium)
            }
            Text(content.title, style = MaterialTheme.typography.bodyLarge)
            if (content.subtitle.isNotBlank()) {
                Text(content.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
