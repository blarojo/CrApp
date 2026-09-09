package com.crapp.ui.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crapp.data.model.BowelMovement
import com.crapp.export.BowelMovementPhotoStore
import com.crapp.ui.common.PhotoThumbnail

/**
 * A 2-column grid of every bowel-movement photo taken so far, newest first
 * (docs/backlog.md spec 14) -- browsing "what did this look like a few weeks ago"
 * as a scroll instead of a hunt through History for a 📷 marker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val photos by viewModel.photos.collectAsState()
    var enlarged by remember { mutableStateOf<BowelMovement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { innerPadding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No photos yet — attach one next time you log a bowel movement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(photos, key = { it.id }) { movement ->
                    GalleryCard(movement = movement, onClick = { enlarged = movement })
                }
            }
        }
    }

    enlarged?.let { movement ->
        PhotoViewerDialog(movement = movement, onDismiss = { enlarged = null })
    }
}

@Composable
private fun GalleryCard(movement: BowelMovement, onClick: () -> Unit) {
    val caption = remember(movement) { buildGalleryCaption(movement) }
    val photoUri = movement.photoUri ?: return // observeAllWithPhoto() guarantees non-null, but keeps this composable safe on its own.

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            PhotoThumbnail(
                photoUri = photoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                GalleryCaptionText(caption)
            }
        }
    }
}

@Composable
private fun GalleryCaptionText(caption: GalleryCaption) {
    Text(caption.dateTime, style = MaterialTheme.typography.labelMedium)
    Text(caption.consistencyAndAmount, style = MaterialTheme.typography.bodyMedium)
    caption.locationAndNight?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The full-size photo viewer -- a `Dialog`, not a new screen/route. `Dialog`'s own
 * default `dismissOnClickOutside = true` already gives "tap outside to close" for
 * free (the literal ask); the ✕ button is a deliberate addition beyond that, since
 * tap-outside-to-dismiss isn't self-evident to every user and a modal with no
 * visible way out is a common real complaint.
 */
@Composable
private fun PhotoViewerDialog(movement: BowelMovement, onDismiss: () -> Unit) {
    val caption = remember(movement) { buildGalleryCaption(movement) }
    val photoUri = movement.photoUri

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column {
                Box {
                    if (photoUri != null) {
                        FullSizePhoto(photoUri = photoUri)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GalleryCaptionText(caption)
                }
            }
        }
    }
}

private sealed interface FullSizeLoadState {
    data object Loading : FullSizeLoadState
    data class Loaded(val bitmap: Bitmap) : FullSizeLoadState
    data object Failed : FullSizeLoadState
}

/**
 * Undownsampled decode -- unlike [PhotoThumbnail], this is the one place a
 * full-resolution image is worth it. Distinguishes still-loading from failed-to-load
 * (same reasoning as [PhotoThumbnail]) so a missing photo shows a real message
 * instead of a spinner stuck forever.
 */
@Composable
private fun FullSizePhoto(photoUri: String) {
    val context = LocalContext.current
    val state by produceState<FullSizeLoadState>(initialValue = FullSizeLoadState.Loading, key1 = photoUri) {
        value = BowelMovementPhotoStore(context).loadFullSize(photoUri)
            ?.let { FullSizeLoadState.Loaded(it) }
            ?: FullSizeLoadState.Failed
    }

    when (val current = state) {
        is FullSizeLoadState.Loaded -> Image(
            bitmap = current.bitmap.asImageBitmap(),
            contentDescription = "Bowel movement photo, full size",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
        FullSizeLoadState.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        FullSizeLoadState.Failed -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "📷 Photo not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
