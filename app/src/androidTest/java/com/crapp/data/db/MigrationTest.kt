package com.crapp.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies [MIGRATION_1_2] against a real version-1 database built from the exported
 * schema (app/schemas/com.crapp.data.db.AppDatabase/1.json) -- the persistence
 * guarantee documented on [AppDatabase] ("an app update never touches this database
 * file... unless a future schema change" skips writing a real Migration) is only as
 * good as this actually being exercised.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_addsIngredientsColumn_asNullForExistingRows() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO food (id, name, brand) VALUES (1, 'Z/D', 'Hill''s')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = migratedDb.query("SELECT name, brand, ingredients FROM food WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Z/D", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals("Hill's", cursor.getString(cursor.getColumnIndexOrThrow("brand")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("ingredients")))
        cursor.close()
    }

    @Test
    fun migrate1To2_preservesOtherTablesUntouched() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO bowel_movement (id, timestamp, consistency, hasBlood, hasMucus) " +
                    "VALUES (1, 1788384877804, 6, 1, 0)"
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        val cursor = migratedDb.query("SELECT consistency FROM bowel_movement WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(6, cursor.getInt(cursor.getColumnIndexOrThrow("consistency")))
        cursor.close()
    }

    /**
     * Verifies [MIGRATION_2_3] (docs/future-features.md specs 1/3/4/5/9 and the
     * dose/amount spec, all shipped in one version bump) against a real version-2
     * database: existing rows survive with the new columns null/false-defaulted,
     * and the new tables exist and are usable.
     */
    @Test
    fun migrate2To3_addsNewColumnsAsNullOrFalse_forExistingRows() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO bowel_movement (id, timestamp, consistency, hasBlood, hasMucus) " +
                    "VALUES (1, 1788384877804, 6, 1, 0)"
            )
            execSQL("INSERT INTO medication_entry (id, timestamp, name) VALUES (1, 1788384877804, 'Metronidazole')")
            execSQL(
                "INSERT INTO food (id, name, brand, ingredients) VALUES (1, 'Z/D', 'Hill''s', 'Chicken, water')"
            )
            execSQL(
                "INSERT INTO food_entry (id, timestamp, foodId, mealType) VALUES (1, 1788384877804, 1, 'MEAL')"
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        val bowelCursor = migratedDb.query(
            "SELECT amount, location, locationOther, isNightTime, photoUri FROM bowel_movement WHERE id = 1"
        )
        assertTrue(bowelCursor.moveToFirst())
        assertTrue(bowelCursor.isNull(bowelCursor.getColumnIndexOrThrow("amount")))
        assertTrue(bowelCursor.isNull(bowelCursor.getColumnIndexOrThrow("location")))
        assertTrue(bowelCursor.isNull(bowelCursor.getColumnIndexOrThrow("locationOther")))
        assertEquals(0, bowelCursor.getInt(bowelCursor.getColumnIndexOrThrow("isNightTime")))
        assertTrue(bowelCursor.isNull(bowelCursor.getColumnIndexOrThrow("photoUri")))
        bowelCursor.close()

        val medicationCursor = migratedDb.query("SELECT doseValue, doseUnit FROM medication_entry WHERE id = 1")
        assertTrue(medicationCursor.moveToFirst())
        assertTrue(medicationCursor.isNull(medicationCursor.getColumnIndexOrThrow("doseValue")))
        medicationCursor.close()

        val foodEntryCursor = migratedDb.query("SELECT amountValue, amountUnit FROM food_entry WHERE id = 1")
        assertTrue(foodEntryCursor.moveToFirst())
        assertTrue(foodEntryCursor.isNull(foodEntryCursor.getColumnIndexOrThrow("amountValue")))
        foodEntryCursor.close()

        // New standalone tables exist and accept inserts.
        migratedDb.execSQL("INSERT INTO energy_entry (id, timestamp, level) VALUES (1, 1788384877804, 'NORMAL')")
        migratedDb.execSQL("INSERT INTO walk_entry (id, timestamp, bowelMovementCount) VALUES (1, 1788384877804, 2)")
        migratedDb.execSQL("INSERT INTO ingredient (id, name) VALUES (1, 'chicken')")
        migratedDb.execSQL("INSERT INTO food_ingredient (id, foodId, ingredientId, position) VALUES (1, 1, 1, 0)")

        val ingredientJoinCursor = migratedDb.query(
            "SELECT ingredient.name FROM food_ingredient " +
                "INNER JOIN ingredient ON ingredient.id = food_ingredient.ingredientId " +
                "WHERE food_ingredient.foodId = 1"
        )
        assertTrue(ingredientJoinCursor.moveToFirst())
        assertEquals("chicken", ingredientJoinCursor.getString(ingredientJoinCursor.getColumnIndexOrThrow("name")))
        ingredientJoinCursor.close()
    }

    /**
     * Verifies [MIGRATION_3_4] (the Medication catalog): an existing free-text-only
     * medication_entry row survives with `medicationId` null, and the new
     * `medication` table exists, accepts inserts, and its `ON DELETE SET NULL` FK
     * actually unlinks (rather than blocking or cascading) when a linked
     * medication is deleted.
     */
    @Test
    fun migrate3To4_addsMedicationCatalog_existingEntriesUnlinkedButIntact() {
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO medication_entry (id, timestamp, name, dose) VALUES (1, 1788384877804, 'Metronidazole', '250mg')"
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        val cursor = migratedDb.query("SELECT name, dose, medicationId FROM medication_entry WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Metronidazole", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("medicationId")))
        cursor.close()

        migratedDb.execSQL("INSERT INTO medication (id, name) VALUES (1, 'Metronidazole')")
        migratedDb.execSQL("UPDATE medication_entry SET medicationId = 1 WHERE id = 1")
        migratedDb.execSQL("DELETE FROM medication WHERE id = 1")

        // SET_NULL, not RESTRICT/CASCADE: the entry survives, just unlinked --
        // see MedicationEntry's KDoc on why this differs from FoodEntry.foodId.
        val afterDeleteCursor = migratedDb.query("SELECT name, medicationId FROM medication_entry WHERE id = 1")
        assertTrue(afterDeleteCursor.moveToFirst())
        assertEquals("Metronidazole", afterDeleteCursor.getString(afterDeleteCursor.getColumnIndexOrThrow("name")))
        assertTrue(afterDeleteCursor.isNull(afterDeleteCursor.getColumnIndexOrThrow("medicationId")))
        afterDeleteCursor.close()
    }
}
