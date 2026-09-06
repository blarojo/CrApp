package com.crapp.data.backup

import com.crapp.data.model.Amount
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.EnergyLevel
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.Location
import com.crapp.data.model.MealType
import com.crapp.data.model.Medication
import com.crapp.data.model.MedicationEntry
import com.crapp.data.model.WalkEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Full snapshot of every table, for backup/restore (docs/development-plan.md Phase
 * 8). Note that [BackupRepository.restoreFromJson][com.crapp.data.repository.BackupRepository.restoreFromJson]
 * deliberately does *not* apply [foods]/[foodEntries]/[medications] back onto the
 * database -- see that function's KDoc. They're still captured here so the backup
 * file itself remains a complete snapshot (e.g. for manual recovery), even though
 * the normal restore flow skips them.
 */
data class BackupData(
    val bowelMovements: List<BowelMovement>,
    val foods: List<Food>,
    val foodEntries: List<FoodEntry>,
    val medicationEntries: List<MedicationEntry>,
    val energyEntries: List<EnergyEntry> = emptyList(),
    val walkEntries: List<WalkEntry> = emptyList(),
    val medications: List<Medication> = emptyList()
)

/**
 * Hand-rolled JSON (de)serialization for the full local database, via `org.json`
 * (built into Android -- no new dependency). Distinct from the CSV export
 * (development-plan.md §8): CSV is a vet-facing, lossy, per-table format; this
 * preserves every row's id and foreign keys exactly, so it restores byte-for-byte.
 *
 * `energyEntries`/`walkEntries` and the new `bowel_movement`/`food_entry`/
 * `medication_entry` fields (docs/future-features.md specs 1/3/4/5 and the
 * dose/amount spec) are read with `opt*` so an *older* backup file (from before
 * those fields existed) still restores cleanly -- missing means "not recorded,"
 * same as a fresh nullable column after the schema migration.
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
                put("amount", m.amount?.name)
                put("location", m.location?.name)
                put("locationOther", m.locationOther)
                put("isNightTime", m.isNightTime)
                put("photoUri", m.photoUri)
            }
        }))

        root.put("foods", JSONArray(data.foods.map { f ->
            JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("brand", f.brand)
                put("ingredients", f.ingredients)
            }
        }))

        root.put("foodEntries", JSONArray(data.foodEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("timestampEpochMillis", e.timestamp.toEpochMilli())
                put("foodId", e.foodId)
                put("amount", e.amount)
                put("mealType", e.mealType.name)
                put("amountValue", e.amountValue)
                put("amountUnit", e.amountUnit)
            }
        }))

        root.put("medicationEntries", JSONArray(data.medicationEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("timestampEpochMillis", e.timestamp.toEpochMilli())
                put("name", e.name)
                put("dose", e.dose)
                put("notes", e.notes)
                put("doseValue", e.doseValue)
                put("doseUnit", e.doseUnit)
                put("medicationId", e.medicationId)
            }
        }))

        root.put("medications", JSONArray(data.medications.map { m ->
            JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("notes", m.notes)
            }
        }))

        root.put("energyEntries", JSONArray(data.energyEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("timestampEpochMillis", e.timestamp.toEpochMilli())
                put("level", e.level.name)
                put("notes", e.notes)
            }
        }))

        root.put("walkEntries", JSONArray(data.walkEntries.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("timestampEpochMillis", e.timestamp.toEpochMilli())
                put("bowelMovementCount", e.bowelMovementCount)
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
                    notes = o.optStringOrNull("notes"),
                    amount = o.optStringOrNull("amount")?.let { runCatching { Amount.valueOf(it) }.getOrNull() },
                    location = o.optStringOrNull("location")?.let { runCatching { Location.valueOf(it) }.getOrNull() },
                    locationOther = o.optStringOrNull("locationOther"),
                    isNightTime = o.optBoolean("isNightTime", false),
                    photoUri = o.optStringOrNull("photoUri")
                )
            }
        }.orEmpty()

        val foods = root.optJSONArray("foods")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Food(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    brand = o.optStringOrNull("brand"),
                    ingredients = o.optStringOrNull("ingredients")
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
                    mealType = runCatching { MealType.valueOf(o.getString("mealType")) }.getOrDefault(MealType.MEAL),
                    amountValue = o.optDoubleOrNull("amountValue"),
                    amountUnit = o.optStringOrNull("amountUnit")
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
                    notes = o.optStringOrNull("notes"),
                    doseValue = o.optDoubleOrNull("doseValue"),
                    doseUnit = o.optStringOrNull("doseUnit"),
                    medicationId = o.optLongOrNull("medicationId")
                )
            }
        }.orEmpty()

        val medications = root.optJSONArray("medications")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Medication(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    notes = o.optStringOrNull("notes")
                )
            }
        }.orEmpty()

        val energyEntries = root.optJSONArray("energyEntries")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                EnergyEntry(
                    id = o.getLong("id"),
                    timestamp = Instant.ofEpochMilli(o.getLong("timestampEpochMillis")),
                    level = runCatching { EnergyLevel.valueOf(o.getString("level")) }.getOrDefault(EnergyLevel.NORMAL),
                    notes = o.optStringOrNull("notes")
                )
            }
        }.orEmpty()

        val walkEntries = root.optJSONArray("walkEntries")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                WalkEntry(
                    id = o.getLong("id"),
                    timestamp = Instant.ofEpochMilli(o.getLong("timestampEpochMillis")),
                    bowelMovementCount = o.getInt("bowelMovementCount"),
                    notes = o.optStringOrNull("notes")
                )
            }
        }.orEmpty()

        return BackupData(bowelMovements, foods, foodEntries, medicationEntries, energyEntries, walkEntries, medications)
    }

    /** [JSONObject.optString] returns "" for a missing/null value rather than null -- this doesn't. */
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key, null)

    /** [JSONObject.optDouble] returns `NaN` for a missing/null value rather than null -- this doesn't. */
    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

    /** [JSONObject.optLong] returns `0` for a missing/null value rather than null -- this doesn't. */
    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)
}
