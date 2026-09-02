package com.crapp.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onLogBowelMovement: () -> Unit,
    onLogFood: () -> Unit,
    onLogMedication: () -> Unit,
    onViewHistory: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "CrApp", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Bowel movement, food & medication tracker")

            TodaySummaryCard(uiState)

            Button(onClick = onLogBowelMovement, modifier = Modifier.fillMaxWidth()) {
                Text("Log Bowel Movement")
            }
            Button(onClick = onLogFood, modifier = Modifier.fillMaxWidth()) {
                Text("Log Food")
            }
            Button(onClick = onLogMedication, modifier = Modifier.fillMaxWidth()) {
                Text("Log Medication")
            }
            Button(onClick = onViewHistory, modifier = Modifier.fillMaxWidth()) {
                Text("View History")
            }
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export to CSV")
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(uiState: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!uiState.hasAnyEntries) {
                Text("No entries yet — log your first one below.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val movementLabel = when (uiState.bowelMovementsToday) {
                    0 -> "No bowel movements logged today"
                    1 -> "1 bowel movement today"
                    else -> "${uiState.bowelMovementsToday} bowel movements today"
                }
                Text(movementLabel, style = MaterialTheme.typography.titleMedium)

                uiState.lastLoggedAt?.let { lastLoggedAt ->
                    val relativeTime = DateUtils.getRelativeTimeSpanString(
                        lastLoggedAt.toEpochMilli(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    Text("Last logged $relativeTime", style = MaterialTheme.typography.bodyMedium)
                }

                val extras = buildList {
                    if (uiState.foodEntriesToday > 0) add("${uiState.foodEntriesToday} food")
                    if (uiState.medicationEntriesToday > 0) add("${uiState.medicationEntriesToday} medication")
                }
                if (extras.isNotEmpty()) {
                    Text("Also today: ${extras.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
