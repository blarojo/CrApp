package com.crapp.ui.bowel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.data.model.Amount
import com.crapp.data.model.Location
import com.crapp.export.BowelMovementPhotoStore
import com.crapp.ui.common.ConsistencySelector
import com.crapp.ui.common.DateTimeField
import com.crapp.ui.common.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BowelMovementLogScreen(
    onDone: () -> Unit,
    viewModel: BowelMovementLogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val photoStore = remember { BowelMovementPhotoStore(context) }
    var pendingPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            viewModel.onPhotoCaptured(uri)
        } else if (uri != null) {
            // Camera was cancelled -- the empty MediaStore row created for it would
            // otherwise sit as an orphan in the CrApp album forever.
            photoStore.delete(uri.toString())
        }
        pendingPhotoUri = null
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Bowel Movement" else "Log Bowel Movement") },
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

            Text(text = "Consistency (Purina scale)", style = MaterialTheme.typography.titleMedium)
            ConsistencySelector(value = uiState.consistency, onValueChange = viewModel::onConsistencyChange)

            Text(text = "Amount", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Amount.entries.forEach { amount ->
                    FilterChip(
                        selected = uiState.amount == amount,
                        onClick = { viewModel.onAmountChange(amount) },
                        label = { Text(amount.displayName) }
                    )
                }
            }

            Text(text = "When / Where", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // "Night" is an independent toggle (can combine with Walk/Home/Garden
                // below), pre-filled from the configured night window but user-overridable
                // -- see BowelMovementLogViewModel.onNightTimeChange.
                FilterChip(
                    selected = uiState.isNightTime,
                    onClick = { viewModel.onNightTimeChange(!uiState.isNightTime) },
                    label = { Text("Night") }
                )
                FilterChip(
                    selected = uiState.location == Location.WALK,
                    onClick = { viewModel.onLocationChange(Location.WALK) },
                    label = { Text(Location.WALK.displayName) }
                )
                FilterChip(
                    selected = uiState.location == Location.HOME,
                    onClick = { viewModel.onLocationChange(Location.HOME) },
                    label = { Text(Location.HOME.displayName) }
                )
                FilterChip(
                    selected = uiState.location == Location.GARDEN,
                    onClick = { viewModel.onLocationChange(Location.GARDEN) },
                    label = { Text(Location.GARDEN.displayName) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.hasBlood, onCheckedChange = viewModel::onHasBloodChange)
                Text("Blood present")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.hasMucus, onCheckedChange = viewModel::onHasMucusChange)
                Text("Mucus present")
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Photo", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val photoUri = uiState.photoUri
                if (photoUri != null) {
                    PhotoThumbnail(photoUri = photoUri)
                    TextButton(onClick = viewModel::onRemovePhoto) { Text("Remove") }
                } else {
                    OutlinedButton(onClick = {
                        val uri = viewModel.createPhotoCaptureTarget()
                        if (uri != null) {
                            pendingPhotoUri = uri
                            takePictureLauncher.launch(uri)
                        }
                    }) {
                        Text("Take Photo")
                    }
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
