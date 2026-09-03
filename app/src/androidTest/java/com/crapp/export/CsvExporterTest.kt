package com.crapp.export

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.data.model.BowelMovement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/** Needs a real Context (cache dir, FileProvider), hence instrumented rather than JVM. */
@RunWith(AndroidJUnit4::class)
class CsvExporterTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun export_writesFiveCsvFilesAndReturnsAShareIntentForAllOfThem() {
        val exporter = CsvExporter(context)
        val movements = listOf(
            BowelMovement(id = 1, timestamp = Instant.parse("2026-09-01T08:00:00Z"), consistency = 6)
        )

        val intent = exporter.export(movements, emptyList(), emptyMap(), emptyList())

        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        assertEquals("text/csv", intent.type)
        val uris = intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        assertEquals(5, uris?.size)

        val exportsDir = File(context.cacheDir, "exports")
        assertTrue(File(exportsDir, "bowel_movements.csv").readText().contains("6"))
        assertTrue(File(exportsDir, "food_entries.csv").exists())
        assertTrue(File(exportsDir, "medication_entries.csv").exists())
        assertTrue(File(exportsDir, "energy_entries.csv").exists())
        assertTrue(File(exportsDir, "walk_entries.csv").exists())
    }

    @Test
    fun export_clearsStaleFilesFromAPreviousExport() {
        val exporter = CsvExporter(context)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(exportsDir, "leftover_from_before.csv").writeText("stale")

        exporter.export(emptyList(), emptyList(), emptyMap(), emptyList())

        assertTrue(!File(exportsDir, "leftover_from_before.csv").exists())
    }
}
