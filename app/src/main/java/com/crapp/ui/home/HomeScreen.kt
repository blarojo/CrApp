package com.crapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onLogBowelMovement: () -> Unit,
    onLogFood: () -> Unit,
    onLogMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
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

            Button(onClick = onLogBowelMovement, modifier = Modifier.fillMaxWidth()) {
                Text("Log Bowel Movement")
            }
            Button(onClick = onLogFood, modifier = Modifier.fillMaxWidth()) {
                Text("Log Food")
            }
            Button(onClick = onLogMedication, modifier = Modifier.fillMaxWidth()) {
                Text("Log Medication")
            }
        }
    }
}
