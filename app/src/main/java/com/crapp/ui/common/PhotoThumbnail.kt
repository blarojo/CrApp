package com.crapp.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crapp.export.BowelMovementPhotoStore

/**
 * Decodes and shows a bowel-movement photo from its `MediaStore` URI (no image-
 * loading library dependency needed for this small a use -- see
 * [BowelMovementPhotoStore.loadThumbnail]).
 */
@Composable
fun PhotoThumbnail(
    photoUri: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = BowelMovementPhotoStore(context).loadThumbnail(photoUri)
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap
        if (current != null) {
            Image(bitmap = current.asImageBitmap(), contentDescription = "Bowel movement photo")
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
