package com.crapp.ocr

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Captures a food-label photo and runs on-device text recognition on it, for the
 * Food Catalog's "Scan label" button (docs/backlog.md spec 12). Deliberately not the
 * same pattern as [com.crapp.export.BowelMovementPhotoStore]: that photo is a kept,
 * user-visible record (`MediaStore`/`Pictures/CrApp`); this one is disposable source
 * material for OCR only, so it lives in the app's own cache dir and is deleted right
 * after each scan -- see [deleteScan].
 *
 * Uses `com.google.mlkit:text-recognition` (the fully-bundled model, not the
 * Play-Services-downloaded variant) specifically so this never makes a network call,
 * matching the app's "no backend" constraint -- see the Gradle dependency's own
 * comment in `gradle/libs.versions.toml`.
 */
class LabelScanStore(private val context: Context) {

    private val authority = "${context.packageName}.fileprovider"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Creates a fresh temp file in `cacheDir/ocr_scans/` and returns a `content://` URI a camera intent can write into. */
    fun createScanCaptureTarget(): Uri {
        val dir = File(context.cacheDir, "ocr_scans").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Runs on-device text recognition on the photo at [uri], returning the
     * recognized text (possibly blank if nothing was found) or a failure if the
     * image couldn't be read/processed. Never throws.
     */
    suspend fun recognizeText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text -> continuation.resume(text.text) }
                    .addOnFailureListener { error -> continuation.resumeWithException(error) }
            }
        }
    }

    /** Deletes the temp scan file -- call this once the scan is done (success, failure, or cancelled), it's never kept. */
    fun deleteScan(uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
