package com.crapp.ui.walk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.ui.common.DateTimeField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkLogScreen(
    onDone: () -> Unit,
    viewModel: WalkLogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Walk" else "Log Walk") },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "For the dog walker's report only -- just a time and a count, no " +
                        "detail per movement. Only log this if you didn't already tag " +
                        "individual movements as \"Walk\" for this outing, or it'll be " +
                        "counted twice on the dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            DateTimeField(value = uiState.timestamp, onValueChange = viewModel::onTimestampChange)

            Text(text = "Bowel movements", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { viewModel.onCountChange(uiState.bowelMovementCount - 1) }) {
                    Text("−", style = MaterialTheme.typography.headlineMedium)
                }
                Text(uiState.bowelMovementCount.toString(), style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { viewModel.onCountChange(uiState.bowelMovementCount + 1) }) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
