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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

            // -- Today ------------------------------------------------------------
            SectionHeader(title = "Today")
            TodaySummaryCard(uiState)

            if (uiState.hasAnyEntries) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // -- History --------------------------------------------------------
                SectionHeader(title = "History")
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
                    ConsistencyTrendChart(
                        points = uiState.consistencyTrend,
                        windowStart = uiState.dailyFrequency.first().date,
                        windowDays = uiState.dailyFrequency.size,
                        walkTicks = uiState.walkTicksInWindow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                DashboardCard(title = "Movements per day") {
                    FrequencyBarChart(days = uiState.dailyFrequency, modifier = Modifier.fillMaxWidth())
                }

                Text(
                    "Where and when, per day",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(emoji = "🚶", label = "Walk", value = uiState.walkMovementsInWindow, modifier = Modifier.weight(1f))
                    StatTile(emoji = "🌙", label = "Night", value = uiState.nightMovementsInWindow, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(emoji = "🏠", label = "Inside home", value = uiState.insideHomeMovementsInWindow, modifier = Modifier.weight(1f))
                    StatTile(emoji = "🌳", label = "Garden", value = uiState.gardenMovementsInWindow, modifier = Modifier.weight(1f))
                }

                DashboardCard(title = "🚶 Movements during walks per day") {
                    CategoryFrequencyOrEmpty(uiState.dailyWalk, "No walk-tagged movements in this window.")
                }
                DashboardCard(title = "🌙 Night movements per day") {
                    CategoryFrequencyOrEmpty(uiState.dailyNight, "No night-time movements in this window.")
                }
                DashboardCard(title = "🏠 Inside home per day") {
                    CategoryFrequencyOrEmpty(uiState.dailyInsideHome, "No movements tagged \"Inside home\" in this window.")
                }
                DashboardCard(title = "🌳 Garden per day") {
                    CategoryFrequencyOrEmpty(uiState.dailyGarden, "No movements tagged \"Garden\" in this window.")
                }
            }

            // Keeps the last card clear of the floating action button.
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun CategoryFrequencyOrEmpty(days: List<DailyCount>, emptyMessage: String) {
    if (days.all { it.count == 0 }) {
        Text(emptyMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        FrequencyBarChart(days = days, modifier = Modifier.fillMaxWidth())
    }
}

/** A bold, slightly letter-spaced label marking a scroll section ("Today" vs "History"). */
@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DashboardCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** A single stat number for the selected window -- spec 3's location/night movement rollups. */
@Composable
private fun StatTile(emoji: String, label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("$emoji $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The "Today" hero card: every category logged today at a glance, in a filled,
 * higher-contrast card so it reads as the dashboard's headline rather than just
 * another tile among equals.
 */
@Composable
private fun TodaySummaryCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!uiState.hasAnyEntries) {
                Text(
                    "No entries yet — tap + to log your first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        uiState.bowelMovementsToday.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.bowelMovementsToday == 1) "bowel movement today" else "bowel movements today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                uiState.lastLoggedAt?.let { lastLoggedAt ->
                    val relativeTime = DateUtils.getRelativeTimeSpanString(
                        lastLoggedAt.toEpochMilli(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    Text(
                        "Last logged $relativeTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                val extras = buildList {
                    if (uiState.foodEntriesToday > 0) add("🍗 ${uiState.foodEntriesToday} food")
                    if (uiState.medicationEntriesToday > 0) add("💊 ${uiState.medicationEntriesToday} medication")
                    if (uiState.energyEntriesToday > 0) add("⚡ ${uiState.energyEntriesToday} energy")
                    if (uiState.walkEntriesToday > 0) add("🚶 ${uiState.walkEntriesToday} walk report")
                }
                if (extras.isNotEmpty()) {
                    Text(
                        "Also today: ${extras.joinToString("   ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
