package com.crapp.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.AppConfig
import com.crapp.ui.common.AddEntryFab
import com.crapp.ui.common.AddEntryOption
import com.crapp.ui.common.ConsistencyTrendChart
import com.crapp.ui.common.FrequencyBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogBowelMovement: () -> Unit,
    onLogFood: () -> Unit,
    onLogMedication: () -> Unit,
    onLogEnergy: () -> Unit,
    onLogWalk: () -> Unit,
    onViewHistory: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("💩 CrApp") },
                actions = {
                    TextButton(onClick = onViewHistory) { Text("History") }
                    TextButton(onClick = onExport) { Text("Export") }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            AddEntryFab(
                options = listOf(
                    AddEntryOption("💩 Bowel Movement", onLogBowelMovement),
                    AddEntryOption("🍗 Food", onLogFood),
                    AddEntryOption("💊 Medication", onLogMedication),
                    AddEntryOption("⚡ Energy", onLogEnergy),
                    AddEntryOption("🚶 Walk", onLogWalk)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Tracking ${AppConfig.DOG_NAME}'s bowel movements, food & medication 🐾",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TodaySummaryCard(uiState)

            if (uiState.hasAnyEntries) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrendWindow.entries.forEach { window ->
                        FilterChip(
                            selected = uiState.window == window,
                            onClick = { viewModel.onWindowChange(window) },
                            label = { Text(window.label) }
                        )
                    }
                }

                DashboardCard(title = "Consistency trend") {
                    ConsistencyTrendChart(points = uiState.consistencyTrend, modifier = Modifier.fillMaxWidth())
                }
                DashboardCard(title = "Movements per day") {
                    FrequencyBarChart(days = uiState.dailyFrequency, modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(label = "Night movements", value = uiState.nightMovementsInWindow, modifier = Modifier.weight(1f))
                    StatTile(label = "Walk movements", value = uiState.walkMovementsInWindow, modifier = Modifier.weight(1f))
                }
            }

            // Keeps the last card clear of the floating action button.
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun DashboardCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** A single stat number for the selected window -- spec 3's night/walk movement rollups. */
@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodaySummaryCard(uiState: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!uiState.hasAnyEntries) {
                Text("No entries yet — tap + to log your first one.", style = MaterialTheme.typography.bodyMedium)
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
