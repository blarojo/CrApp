package com.crapp.export

import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import com.crapp.data.model.MedicationEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class CsvWriterTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun allThreeWriters_omittedZoneArgument_usesSystemDefaultWithoutThrowing() {
        // Exercises the default-parameter (zone = ZoneId.systemDefault()) overload
        // specifically -- every other test passes zone explicitly.
        val movement = BowelMovement(id = 1, timestamp = Instant.parse("2026-09-01T08:30:00Z"), consistency = 6)
        val entry = FoodEntry(id = 1, timestamp = Instant.parse("2026-09-01T12:00:00Z"), foodId = 1, mealType = MealType.MEAL)
        val medication = MedicationEntry(id = 1, timestamp = Instant.parse("2026-09-01T09:00:00Z"), name = "Metronidazole")

        assertEquals(2, CsvWriter.bowelMovementsCsv(listOf(movement)).split("\r\n").size - 1)
        assertEquals(2, CsvWriter.foodEntriesCsv(listOf(entry), emptyMap()).split("\r\n").size - 1)
        assertEquals(2, CsvWriter.medicationEntriesCsv(listOf(medication)).split("\r\n").size - 1)
    }

    @Test
    fun bowelMovementsCsv_headerAndRows() {
        val movements = listOf(
            BowelMovement(
                id = 1,
                timestamp = Instant.parse("2026-09-01T08:30:00Z"),
                consistency = 6,
                color = "brown",
                hasBlood = true,
                hasMucus = false,
                notes = "watery, right after breakfast"
            )
        )

        val csv = CsvWriter.bowelMovementsCsv(movements, zone)

        val lines = csv.split("\r\n")
        assertEquals(
            "id,timestamp,consistency,color,has_blood,has_mucus,notes,amount,location,location_other,is_night_time,photo_uri",
            lines[0]
        )
        assertEquals(
            "1,2026-09-01 08:30:00,6,brown,true,false,\"watery, right after breakfast\",,,,false,",
            lines[1]
        )
    }

    @Test
    fun bowelMovementsCsv_emptyList_producesHeaderOnly() {
        val csv = CsvWriter.bowelMovementsCsv(emptyList(), zone)

        assertEquals(
            "id,timestamp,consistency,color,has_blood,has_mucus,notes,amount,location,location_other,is_night_time,photo_uri\r\n",
            csv
        )
    }

    @Test
    fun foodEntriesCsv_resolvesFoodNameBrandAndIngredients() {
        val food = Food(id = 10, name = "Hill's I/D", brand = "Hill's", ingredients = "Chicken, rice, oat fiber")
        val entries = listOf(
            FoodEntry(id = 1, timestamp = Instant.parse("2026-09-01T12:00:00Z"), foodId = 10, amount = "1/2 cup", mealType = MealType.MEAL)
        )

        val csv = CsvWriter.foodEntriesCsv(entries, mapOf(10L to food), zone)

        val lines = csv.split("\r\n")
        assertEquals("id,timestamp,food,brand,amount,meal_type,ingredients,amount_value,amount_unit", lines[0])
        assertEquals(
            "1,2026-09-01 12:00:00,Hill's I/D,Hill's,1/2 cup,MEAL,\"Chicken, rice, oat fiber\",,",
            lines[1]
        )
    }

    @Test
    fun foodEntriesCsv_missingFood_leavesNameBrandAndIngredientsBlank() {
        val entries = listOf(
            FoodEntry(id = 1, timestamp = Instant.parse("2026-09-01T12:00:00Z"), foodId = 999, mealType = MealType.TREAT)
        )

        val csv = CsvWriter.foodEntriesCsv(entries, emptyMap(), zone)

        val lines = csv.split("\r\n")
        assertEquals("1,2026-09-01 12:00:00,,,,TREAT,,,", lines[1])
    }

    @Test
    fun medicationEntriesCsv_headerAndRows() {
        val entries = listOf(
            MedicationEntry(id = 1, timestamp = Instant.parse("2026-09-01T09:00:00Z"), name = "Metronidazole", dose = "250mg", notes = null)
        )

        val csv = CsvWriter.medicationEntriesCsv(entries, zone)

        val lines = csv.split("\r\n")
        assertEquals("id,timestamp,name,dose,notes,dose_value,dose_unit", lines[0])
        assertEquals("1,2026-09-01 09:00:00,Metronidazole,250mg,,,", lines[1])
    }

    @Test
    fun escaping_quotesFieldsContainingCommaQuoteOrNewline() {
        val entries = listOf(
            MedicationEntry(
                id = 1,
                timestamp = Instant.parse("2026-09-01T09:00:00Z"),
                name = "Med, \"special\"",
                dose = null,
                notes = "line one\nline two"
            )
        )

        val csv = CsvWriter.medicationEntriesCsv(entries, zone)

        val lines = csv.split("\r\n")
        // Embedded quotes are doubled and the whole field is wrapped in quotes;
        // the embedded \n stays inside the quoted field rather than starting a new row.
        assertEquals(
            "1,2026-09-01 09:00:00,\"Med, \"\"special\"\"\",,\"line one\nline two\",,",
            lines[1]
        )
    }
}
