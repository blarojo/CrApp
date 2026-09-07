package com.crapp.ui.foodcatalog

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.data.model.Food
import com.crapp.ocr.IngredientsTextExtractor
import com.crapp.ocr.LabelScanStore
import kotlinx.coroutines.launch

/**
 * Lists the food catalog with its ingredients (docs/development-plan.md Phase 8) --
 * tap a food to add or edit its ingredient list, either pasted from a label or typed
 * manually. Foods pre-seeded on first install already have ingredients filled in.
 *
 * Also supports deleting a food (the "delete old ones" admin flow) -- blocked with
 * an explanatory message if any logged food entry still references it, rather than
 * silently orphaning history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCatalogScreen(
    onBack: () -> Unit,
    viewModel: FoodCatalogViewModel = viewModel()
) {
    val foods by viewModel.foods.collectAsState()
    val editingFood by viewModel.editingFood.collectAsState()
    val addFoodState by viewModel.addFoodState.collectAsState()
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
                title = { Text("Food Catalog") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::startAddingNewFood) {
                Icon(Icons.Filled.Add, contentDescription = "Add food")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { innerPadding ->
        if (foods.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    "No foods yet -- tap + to add one (name, brand, ingredients), or log a " +
                        "food entry and it'll show up here automatically.",
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
                    FoodCatalogRow(
                        food = food,
                        onClick = { viewModel.startEditing(food) },
                        onDeleteClick = { viewModel.requestDelete(food) }
                    )
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

    addFoodState?.let { state ->
        AddFoodDialog(
            state = state,
            onDismiss = viewModel::cancelAddingNewFood,
            onNameChange = viewModel::onNewFoodNameChange,
            onBrandChange = viewModel::onNewFoodBrandChange,
            onIngredientsChange = viewModel::onNewFoodIngredientsChange,
            onSave = viewModel::confirmAddNewFood
        )
    }

    pendingDelete?.let { food ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete \"${food.name}\"?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FoodCatalogRow(food: Food, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
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
            IconButton(onClick = onDeleteClick, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${food.name}")
            }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Ingredients") },
                    placeholder = { Text("Paste from the label, type manually, or scan below") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                ScanLabelButton(onTextRecognized = { text = it })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * The Food Catalog's own "Add new food" dialog (docs/backlog.md spec 12's
 * manual-entry addition) -- name + optional brand + optional ingredients, all in one
 * place, without needing to log a food entry first (the only way to create a new
 * catalog row before this). Shares the same "Scan label" OCR button as the
 * ingredients-edit dialog above.
 */
@Composable
private fun AddFoodDialog(
    state: AddFoodUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onIngredientsChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add new food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = onBrandChange,
                    label = { Text("Brand (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.ingredients,
                    onValueChange = onIngredientsChange,
                    label = { Text("Ingredients (optional)") },
                    placeholder = { Text("Paste from the label, type manually, or scan below") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                ScanLabelButton(onTextRecognized = onIngredientsChange)
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = state.name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * A "📷 Scan label" button, shared by [IngredientsEditDialog] and [AddFoodDialog]:
 * takes a photo (via the same disposable-`FileProvider`-cache-file pattern as
 * [com.crapp.ocr.LabelScanStore]'s own KDoc explains), runs on-device text
 * recognition on it, then runs [IngredientsTextExtractor] to isolate just the
 * ingredients/composition section from the rest of the label (nutritional info,
 * weight, address, etc.) before calling [onTextRecognized] -- the caller decides how
 * to fold that into its own ingredients field (both callers here simply replace it,
 * matching docs/backlog.md spec 12: "pre-fills ... for the user to review/edit
 * before saving", never auto-saved unreviewed). Falls back to the full recognized
 * text, with a heads-up toast, if no ingredients/composition heading is found.
 */
@Composable
private fun ScanLabelButton(onTextRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val labelScanStore = remember { LabelScanStore(context) }
    var pendingScanUri by remember { mutableStateOf<Uri?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingScanUri
        pendingScanUri = null
        if (success && uri != null) {
            isScanning = true
            scope.launch {
                val result = labelScanStore.recognizeText(uri)
                labelScanStore.deleteScan(uri)
                isScanning = false
                result.fold(
                    onSuccess = { text ->
                        if (text.isBlank()) {
                            Toast.makeText(
                                context,
                                "No text found in that photo -- try again with better lighting/focus.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val ingredientsOnly = IngredientsTextExtractor.extract(text)
                            if (ingredientsOnly != null) {
                                onTextRecognized(ingredientsOnly)
                            } else {
                                onTextRecognized(text)
                                Toast.makeText(
                                    context,
                                    "Couldn't spot an \"Ingredients\"/\"Composition\" heading -- " +
                                        "filled in the full scanned text instead, trim as needed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onFailure = {
                        Toast.makeText(context, "Couldn't read that photo -- try again.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        } else if (uri != null) {
            // Camera was cancelled -- the temp file (if the camera app created one) is disposable anyway.
            labelScanStore.deleteScan(uri)
        }
    }

    OutlinedButton(
        onClick = {
            val uri = labelScanStore.createScanCaptureTarget()
            pendingScanUri = uri
            takePictureLauncher.launch(uri)
        },
        enabled = !isScanning
    ) {
        if (isScanning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Reading label…")
            }
        } else {
            Text("📷 Scan label")
        }
    }
}
