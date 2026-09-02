package com.crapp.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.data.model.MealType
import com.crapp.ui.common.DateTimeField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogScreen(
    onDone: () -> Unit,
    viewModel: FoodLogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val foods by viewModel.foodsByRecentUse.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Food" else "Log Food") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DateTimeField(value = uiState.timestamp, onValueChange = viewModel::onTimestampChange)

            Text(text = "Food", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedFood?.name ?: "Select a food",
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
                    foods.forEach { food ->
                        DropdownMenuItem(
                            text = { Text(food.name) },
                            onClick = {
                                viewModel.onFoodSelected(food)
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Add new food") },
                        onClick = {
                            expanded = false
                            viewModel.onShowAddNewDialog(true)
                        }
                    )
                }
            }

            Text(text = "Meal type", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.forEach { mealType ->
                    FilterChip(
                        selected = uiState.mealType == mealType,
                        onClick = { viewModel.onMealTypeChange(mealType) },
                        label = { Text(mealType.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                enabled = uiState.selectedFood != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    if (uiState.showAddNewDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowAddNewDialog(false) },
            title = { Text("Add new food") },
            text = {
                OutlinedTextField(
                    value = uiState.newFoodName,
                    onValueChange = viewModel::onNewFoodNameChange,
                    label = { Text("Food name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmAddNewFood) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowAddNewDialog(false) }) { Text("Cancel") }
            }
        )
    }
}
