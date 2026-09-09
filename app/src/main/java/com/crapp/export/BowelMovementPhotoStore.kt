package com.crapp.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores bowel-movement photos in the shared, user-visible `Pictures/CrApp` album via
 * `MediaStore` -- docs/backlog.md's photo-attachment spec. Deliberately not
 * app-private storage (`context.filesDir` / `getExternalFilesDir()`): those are wiped
 * on uninstall, which would defeat the point of a photo log. A `MediaStore` row is
 * owned by this app's package name, so both the file and this app's access to it
 * survive an uninstall + reinstall (the same events that used to wipe app-private
 * storage), and the album is browsable in any gallery/file-manager app -- not
 * something buried in app-internal storage.
 *
 * On Android 10+ (`RELATIVE_PATH`, scoped storage) this needs no extra permission.
 * Below that (API 26-28), `RELATIVE_PATH` is ignored and the file lands in the
 * device's general Pictures collection instead of a `CrApp` subfolder -- untested on
 * a real device that old; the primary target is a modern (10+) phone.
 */
class BowelMovementPhotoStore(private val context: Context) {

    /** Inserts a new empty `MediaStore` row and returns its `content://` URI, ready for a camera intent to fill. */
    fun createNewPhotoUri(): Uri? {
        val fileName = "crapp_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CrApp")
            }
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /** Deletes the underlying `MediaStore` row -- e.g. when a photo is removed/replaced. Silently no-ops if already gone. */
    fun delete(uriString: String) {
        runCatching { context.contentResolver.delete(Uri.parse(uriString), null, null) }
    }

    /**
     * Decodes [uriString] to a [Bitmap] off the main thread, downsampled so neither
     * dimension exceeds [maxDimensionPx], or null if it can't be read (e.g. deleted
     * from outside the app). Used for every small/grid preview -- see
     * [com.crapp.ui.common.PhotoThumbnail] and the Gallery grid
     * (docs/backlog.md spec 14) -- so a screen decoding several photos at once
     * (previously: several *full camera-resolution* photos at once) doesn't spend
     * memory/time it doesn't need. For the full, undownsampled image, use
     * [loadFullSize].
     */
    suspend fun loadThumbnail(uriString: String, maxDimensionPx: Int = DEFAULT_THUMBNAIL_MAX_DIMENSION_PX): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching { decodeBounded(Uri.parse(uriString), maxDimensionPx) }.getOrNull()
        }

    /** Decodes [uriString] to a [Bitmap] off the main thread at full resolution -- for the Gallery full-size viewer. */
    suspend fun loadFullSize(uriString: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    /**
     * Two-pass decode: read bounds only, then decode at the smallest power-of-two
     * subsample that still covers [maxDimensionPx]. Note `BitmapFactory.decodeStream`
     * always returns null in `inJustDecodeBounds` mode -- that's how bounds-only
     * decoding reports back, not a failure -- so success/failure of the first pass
     * has to be read from `bounds.outWidth`/`outHeight` (each -1 if the stream
     * couldn't be read/decoded), not from decodeStream's own return value.
     */
    private fun decodeBounded(uri: Uri, maxDimensionPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= maxDimensionPx || h / 2 >= maxDimensionPx) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }

    private companion object {
        /** Generous enough for both the 80dp log-screen thumbnail and a 2-column Gallery cell on any real phone density. */
        const val DEFAULT_THUMBNAIL_MAX_DIMENSION_PX = 480
    }
}
