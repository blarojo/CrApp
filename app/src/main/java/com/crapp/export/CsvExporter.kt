package com.crapp.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MedicationEntry
import com.crapp.data.model.WalkEntry
import java.io.File
import java.time.ZoneId

/**
 * Writes exported CSV data to an app-scoped cache directory and builds a share
 * [Intent] backed by [FileProvider] content URIs -- avoids requesting the broad
 * storage permissions `ACTION_SEND` via a plain `file://` URI would need (see
 * docs/development-plan.md §8).
 */
class CsvExporter(private val context: Context) {

    private val authority = "${context.packageName}.fileprovider"

    /** Writes every CSV and returns a share-sheet [Intent] for all of them. */
    fun export(
        movements: List<BowelMovement>,
        foodEntries: List<FoodEntry>,
        foodsById: Map<Long, Food>,
        medications: List<MedicationEntry>,
        energyEntries: List<EnergyEntry> = emptyList(),
        walkEntries: List<WalkEntry> = emptyList(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Clear stale files from a previous export so the share sheet never offers
        // out-of-date data alongside the fresh files.
        dir.listFiles()?.forEach { it.delete() }

        val files = listOf(
            "bowel_movements.csv" to CsvWriter.bowelMovementsCsv(movements, zone),
            "food_entries.csv" to CsvWriter.foodEntriesCsv(foodEntries, foodsById, zone),
            "medication_entries.csv" to CsvWriter.medicationEntriesCsv(medications, zone),
            "energy_entries.csv" to CsvWriter.energyEntriesCsv(energyEntries, zone),
            "walk_entries.csv" to CsvWriter.walkEntriesCsv(walkEntries, zone)
        ).map { (fileName, content) -> File(dir, fileName).apply { writeText(content) } }

        val uris = ArrayList(files.map { file -> FileProvider.getUriForFile(context, authority, file) })

        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
