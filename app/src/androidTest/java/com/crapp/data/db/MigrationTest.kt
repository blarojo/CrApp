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
}
