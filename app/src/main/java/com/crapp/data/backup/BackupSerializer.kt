package com.crapp.data.backup

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import com.crapp.data.model.MedicationEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Full snapshot of every table, for backup/restore (docs/development-plan.md Phase 8). */
data class BackupData(
    val bowelMovements: List<BowelMovement>,
    val foods: List<Food>,
    val foodEntries: List<FoodEntry>,
    val medicationEntries: List<MedicationEntry>
)

/**
 * Hand-rolled JSON (de)serialization for the full local database, via `org.json`
 * (built into Android -- no new dependency). Distinct from the CSV export
 * (development-plan.md §8): CSV is a vet-facing, lossy, per-table format; this
 * preserves every row's id and foreign keys exactly, so it restores byte-for-byte.
 */
object BackupSerializer {
    private const val FORMAT_VERSION = 1

    fun serialize(data: BackupData): String {
        val root = JSONObject()
        root.put("backupFormatVersion", FORMAT_VERSION)
        root.put("exportedAtEpochMillis", Instant.now().toEpochMilli())

        root.put("bowelMovements", JSONArray(data.bowelMovements.map { m ->
            JSONObject().apply {
                put("id", m.id)
                put("timestampEpochMillis", m.timestamp.toEpochMilli())
                put("consistency", m.consistency)
                put("color", m.color)
                put("hasBlood", m.hasBlood)
                put("hasMucus", m.hasMucus)
                put("notes", m.notes)
            }
        }))

        root.put("foods", JSONArray(data.foods.map { f ->
            JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("brand", f.brand)
            }
        }))

        root.put("foodEntries", JSONArray(data.foodEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("timestampEpochMillis", e.timestamp.toEpochMilli())
                put("foodId", e.foodId)
                put("amount", e.amount)
                put("mealType", e.mealType.name)
            }
        }))

        root.put("medicationEntries", JSONArray(data.medicationEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("timestampEpochMillis", e.timestamp.toEpochMilli())
                put("name", e.name)
                put("dose", e.dose)
                put("notes", e.notes)
            }
        }))

        return root.toString(2)
    }

    /** @throws IllegalArgumentException if [json] isn't a recognizable CrApp backup. */
    fun deserialize(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("That doesn't look like a CrApp backup file (not valid JSON).", e)
        }
        if (!root.has("backupFormatVersion")) {
            throw IllegalArgumentException("That doesn't look like a CrApp backup file (missing format marker).")
        }

        val bowelMovements = root.optJSONArray("bowelMovements")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                BowelMovement(
                    id = o.getLong("id"),
                    timestamp = Instant.ofEpochMilli(o.getLong("timestampEpochMillis")),
                    consistency = o.getInt("consistency"),
                    color = o.optStringOrNull("color"),
                    hasBlood = o.optBoolean("hasBlood", false),
                    hasMucus = o.optBoolean("hasMucus", false),
                    notes = o.optStringOrNull("notes")
                )
            }
        }.orEmpty()

        val foods = root.optJSONArray("foods")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Food(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    brand = o.optStringOrNull("brand")
                )
            }
        }.orEmpty()

        val foodEntries = root.optJSONArray("foodEntries")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                FoodEntry(
                    id = o.getLong("id"),
                    timestamp = Instant.ofEpochMilli(o.getLong("timestampEpochMillis")),
                    foodId = o.getLong("foodId"),
                    amount = o.optStringOrNull("amount"),
                    mealType = runCatching { MealType.valueOf(o.getString("mealType")) }.getOrDefault(MealType.MEAL)
                )
            }
        }.orEmpty()

        val medicationEntries = root.optJSONArray("medicationEntries")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                MedicationEntry(
                    id = o.getLong("id"),
                    timestamp = Instant.ofEpochMilli(o.getLong("timestampEpochMillis")),
                    name = o.getString("name"),
                    dose = o.optStringOrNull("dose"),
                    notes = o.optStringOrNull("notes")
                )
            }
        }.orEmpty()

        return BackupData(bowelMovements, foods, foodEntries, medicationEntries)
    }

    /** [JSONObject.optString] returns "" for a missing/null value rather than null -- this doesn't. */
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key, null)
}
