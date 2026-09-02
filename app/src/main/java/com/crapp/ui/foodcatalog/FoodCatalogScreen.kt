package com.crapp.ui.foodcatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.data.model.Food

/**
 * Lists the food catalog with its ingredients (docs/development-plan.md Phase 8) --
 * tap a food to add or edit its ingredient list, either pasted from a label or typed
 * manually. Foods pre-seeded on first install already have ingredients filled in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCatalogScreen(
    onBack: () -> Unit,
    viewModel: FoodCatalogViewModel = viewModel()
) {
    val foods by viewModel.foods.collectAsState()
    val editingFood by viewModel.editingFood.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Catalog") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { innerPadding ->
        if (foods.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    "No foods logged yet -- foods you log will show up here so you can add " +
                        "their ingredients.",
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
                items(foods, key = { it.id }) { food ->
                    FoodCatalogRow(food = food, onClick = { viewModel.startEditing(food) })
                }
            }
        }
    }

    editingFood?.let { food ->
        IngredientsEditDialog(
            food = food,
            onDismiss = viewModel::cancelEditing,
            onSave = viewModel::saveIngredients
        )
    }
}

@Composable
private fun FoodCatalogRow(food: Food, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge)
            food.brand?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                food.ingredients?.takeIf { it.isNotBlank() } ?: "No ingredients recorded -- tap to add",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun IngredientsEditDialog(
    food: Food,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(food.id) { mutableStateOf(food.ingredients.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Ingredients") },
                placeholder = { Text("Paste from the label, or type manually") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
