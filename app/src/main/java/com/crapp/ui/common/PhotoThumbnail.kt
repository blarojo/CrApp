package com.crapp.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crapp.export.BowelMovementPhotoStore

private sealed interface PhotoLoadState {
    data object Loading : PhotoLoadState
    data class Loaded(val bitmap: Bitmap) : PhotoLoadState
    data object Failed : PhotoLoadState
}

/**
 * Decodes and shows a bowel-movement photo from its `MediaStore` URI (no image-
 * loading library dependency needed for this small a use -- see
 * [BowelMovementPhotoStore.loadThumbnail]).
 *
 * [modifier] controls the drawn size -- callers own sizing (the log/edit screen's
 * fixed 80dp square via the default, the Gallery grid's `fillMaxWidth().aspectRatio(1f)`
 * cell), rather than this composable forcing its own size internally. Distinguishes
 * a still-loading photo from one that failed to decode (e.g. deleted from outside
 * the app, or a restored backup pointing at a URI that doesn't exist on this device)
 * so a broken photo shows a real placeholder instead of spinning forever.
 */
@Composable
fun PhotoThumbnail(
    photoUri: String,
    modifier: Modifier = Modifier.size(80.dp),
    contentScale: ContentScale = ContentScale.Fit,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val state by produceState<PhotoLoadState>(initialValue = PhotoLoadState.Loading, key1 = photoUri) {
        value = BowelMovementPhotoStore(context).loadThumbnail(photoUri)
            ?.let { PhotoLoadState.Loaded(it) }
            ?: PhotoLoadState.Failed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        when (val current = state) {
            is PhotoLoadState.Loaded -> Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = "Bowel movement photo",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
            PhotoLoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            PhotoLoadState.Failed -> Text(
                "📷 Photo not found",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
