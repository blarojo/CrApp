package com.crapp.data.backup

import com.crapp.data.model.Amount
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.EnergyEntry
import com.crapp.data.model.EnergyLevel
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.Location
import com.crapp.data.model.MealType
import com.crapp.data.model.MedicationEntry
import com.crapp.data.model.WalkEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class BackupSerializerTest {

    @Test
    fun roundTrip_preservesEveryFieldIncludingFoodIngredients() {
        // Regression test: a first pass at this serializer forgot to write/read
        // Food.ingredients at all, so a restored backup silently dropped every
        // food's ingredients -- caught by inspecting a real exported file on-device
        // rather than by a test, since nothing exercised this before.
        val data = BackupData(
            bowelMovements = listOf(
                BowelMovement(
                    id = 1,
                    timestamp = Instant.parse("2026-09-01T08:30:00Z"),
                    consistency = 6,
                    color = "brown",
                    hasBlood = true,
                    hasMucus = false,
                    notes = "watery",
                    amount = Amount.A_LOT,
                    location = Location.WALK,
                    locationOther = null,
                    isNightTime = true,
                    photoUri = "content://media/external/images/media/42"
                )
            ),
            foods = listOf(
                Food(id = 10, name = "Z/D", brand = "Hill's", ingredients = "Chicken, water"),
                Food(id = 11, name = "BoiledChicken", brand = null, ingredients = null)
            ),
            foodEntries = listOf(
                FoodEntry(
                    id = 1, timestamp = Instant.parse("2026-09-01T12:00:00Z"), foodId = 10, amount = "1/2 cup",
                    mealType = MealType.MEAL, amountValue = 0.5, amountUnit = "cup"
                )
            ),
            medicationEntries = listOf(
                MedicationEntry(
                    id = 1, timestamp = Instant.parse("2026-09-01T09:00:00Z"), name = "Metronidazole", dose = "250mg",
                    notes = null, doseValue = 250.0, doseUnit = "mg"
                )
            ),
            energyEntries = listOf(
                EnergyEntry(id = 1, timestamp = Instant.parse("2026-09-01T07:00:00Z"), level = EnergyLevel.A_BIT_PLAYFUL, notes = "zoomies")
            ),
            walkEntries = listOf(
                WalkEntry(id = 1, timestamp = Instant.parse("2026-09-01T16:00:00Z"), bowelMovementCount = 2, notes = "dog walker")
            )
        )

        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(data))

        assertEquals(data.bowelMovements, restored.bowelMovements)
        assertEquals(data.foods, restored.foods)
        assertEquals(data.foodEntries, restored.foodEntries)
        assertEquals(data.medicationEntries, restored.medicationEntries)
        assertEquals(data.energyEntries, restored.energyEntries)
        assertEquals(data.walkEntries, restored.walkEntries)

        // Explicitly on the field that regressed, since the list-equality check above
        // would also pass if both sides were wrong the same way.
        assertEquals("Chicken, water", restored.foods.first { it.id == 10L }.ingredients)
        assertNull(restored.foods.first { it.id == 11L }.ingredients)
    }

    @Test
    fun deserialize_rejectsJsonWithoutFormatMarker() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupSerializer.deserialize("""{"foo": "bar"}""")
        }
    }

    @Test
    fun deserialize_rejectsNonJson() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupSerializer.deserialize("not json at all")
        }
    }

    @Test
    fun serialize_emptyData_producesEmptyArrays() {
        val empty = BackupData(emptyList(), emptyList(), emptyList(), emptyList())

        val restored = BackupSerializer.deserialize(BackupSerializer.serialize(empty))

        assertEquals(emptyList<BowelMovement>(), restored.bowelMovements)
        assertEquals(emptyList<Food>(), restored.foods)
        assertEquals(emptyList<FoodEntry>(), restored.foodEntries)
        assertEquals(emptyList<MedicationEntry>(), restored.medicationEntries)
        assertEquals(emptyList<EnergyEntry>(), restored.energyEntries)
        assertEquals(emptyList<WalkEntry>(), restored.walkEntries)
    }

    @Test
    fun deserialize_olderBackupWithoutNewFields_stillRestores() {
        // A backup taken before specs 1/3/4/5/dose-amount existed has none of these
        // keys at all -- must restore as "not recorded" (null/false/empty), not throw.
        val legacyJson = """
            {
              "backupFormatVersion": 1,
              "bowelMovements": [
                {"id": 1, "timestampEpochMillis": 1756714200000, "consistency": 6, "color": null, "hasBlood": false, "hasMucus": false, "notes": null}
              ],
              "foods": [],
              "foodEntries": [],
              "medicationEntries": [
                {"id": 1, "timestampEpochMillis": 1756717800000, "name": "Metronidazole", "dose": "250mg", "notes": null}
              ]
            }
        """.trimIndent()

        val restored = BackupSerializer.deserialize(legacyJson)

        val movement = restored.bowelMovements.single()
        assertNull(movement.amount)
        assertNull(movement.location)
        assertEquals(false, movement.isNightTime)
        assertNull(movement.photoUri)

        val medication = restored.medicationEntries.single()
        assertNull(medication.doseValue)
        assertNull(medication.doseUnit)

        assertEquals(emptyList<EnergyEntry>(), restored.energyEntries)
        assertEquals(emptyList<WalkEntry>(), restored.walkEntries)
    }
}
