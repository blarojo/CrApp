package com.crapp.export

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MedicationEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Hand-rolled CSV serialization for the three loggable entity types (see
 * docs/development-plan.md §8) -- the schema is small and stable enough that a
 * library would be overkill. Produces one CSV per table rather than a single mixed
 * file, since the row shapes don't align into sensible shared columns.
 *
 * Fields are escaped per RFC 4180 (quote if the value contains a comma, quote, or
 * line break; double up embedded quotes) since free-text `notes`/`amount` fields can
 * contain any of those. Rows are terminated with CRLF per the RFC.
 */
object CsvWriter {

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun bowelMovementsCsv(
        movements: List<BowelMovement>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val header = listOf("id", "timestamp", "consistency", "color", "has_blood", "has_mucus", "notes")
        val rows = movements.map { m ->
            listOf(
                m.id.toString(),
                formatTimestamp(m.timestamp, zone),
                m.consistency.toString(),
                m.color.orEmpty(),
                m.hasBlood.toString(),
                m.hasMucus.toString(),
                m.notes.orEmpty()
            )
        }
        return toCsv(header, rows)
    }

    /**
     * [foodsById] resolves each entry's [FoodEntry.foodId] to its catalog
     * name/brand/ingredients so the export is human-readable without needing to
     * cross-reference a second file, and so ingredient-level correlation is
     * possible from the CSV alone (see the `crapp-insights` Claude skill).
     */
    fun foodEntriesCsv(
        entries: List<FoodEntry>,
        foodsById: Map<Long, Food>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val header = listOf("id", "timestamp", "food", "brand", "amount", "meal_type", "ingredients")
        val rows = entries.map { e ->
            val food = foodsById[e.foodId]
            listOf(
                e.id.toString(),
                formatTimestamp(e.timestamp, zone),
                food?.name.orEmpty(),
                food?.brand.orEmpty(),
                e.amount.orEmpty(),
                e.mealType.name,
                food?.ingredients.orEmpty()
            )
        }
        return toCsv(header, rows)
    }

    fun medicationEntriesCsv(
        entries: List<MedicationEntry>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val header = listOf("id", "timestamp", "name", "dose", "notes")
        val rows = entries.map { e ->
            listOf(e.id.toString(), formatTimestamp(e.timestamp, zone), e.name, e.dose.orEmpty(), e.notes.orEmpty())
        }
        return toCsv(header, rows)
    }

    private fun formatTimestamp(instant: Instant, zone: ZoneId): String =
        timestampFormatter.format(instant.atZone(zone))

    private fun toCsv(header: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { escape(it) }).append("\r\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { escape(it) }).append("\r\n")
        }
        return sb.toString()
    }

    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
