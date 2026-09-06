package com.crapp.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds `Food.ingredients` (docs/development-plan.md Phase 8: ingredient tracking). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food ADD COLUMN ingredients TEXT")
    }
}

/**
 * Combines every docs/future-features.md schema change shipped together, rather
 * than one migration per spec, since several of them add columns to the same
 * `bowel_movement` table:
 *  - spec 1 (amount), spec 3 (location/night-time/photo): new nullable/defaulted
 *    columns on `bowel_movement` -- no backfill needed, existing rows just get null
 *    (or `0`/false for the non-nullable `isNightTime`).
 *  - spec 4 (energy) and spec 5 (walk summary): two new standalone tables.
 *  - spec 9 (structured ingredients): two new tables; populated by a one-time
 *    Kotlin-side backfill (see [com.crapp.data.repository.IngredientRepository]),
 *    not by this migration -- SQL isn't a sensible place to parse/canonicalize
 *    free-text ingredient labels.
 *  - dose/amount spec: new nullable structured columns on `medication_entry` and
 *    `food_entry`, additive to the existing free-text fields.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bowel_movement ADD COLUMN amount TEXT")
        db.execSQL("ALTER TABLE bowel_movement ADD COLUMN location TEXT")
        db.execSQL("ALTER TABLE bowel_movement ADD COLUMN locationOther TEXT")
        db.execSQL("ALTER TABLE bowel_movement ADD COLUMN isNightTime INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bowel_movement ADD COLUMN photoUri TEXT")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS energy_entry (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                level TEXT NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS walk_entry (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                bowelMovementCount INTEGER NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ingredient (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ingredient_name ON ingredient (name)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_ingredient (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                foodId INTEGER NOT NULL,
                ingredientId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY (foodId) REFERENCES food (id) ON DELETE CASCADE,
                FOREIGN KEY (ingredientId) REFERENCES ingredient (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_food_ingredient_foodId ON food_ingredient (foodId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_food_ingredient_ingredientId ON food_ingredient (ingredientId)")

        db.execSQL("ALTER TABLE medication_entry ADD COLUMN doseValue REAL")
        db.execSQL("ALTER TABLE medication_entry ADD COLUMN doseUnit TEXT")
        db.execSQL("ALTER TABLE food_entry ADD COLUMN amountValue REAL")
        db.execSQL("ALTER TABLE food_entry ADD COLUMN amountUnit TEXT")
    }
}

/**
 * Adds a `Medication` catalog, mirroring `Food`'s -- so the medication-logging
 * screen can offer a dropdown instead of retyping a name every time, and a
 * dedicated admin screen can delete stale entries. `medication_entry.medicationId`
 * is nullable and `ON DELETE SET NULL` (unlike `food_entry.foodId`'s `RESTRICT`):
 * `medication_entry.name` already carries the medication's name independently, so
 * deleting a catalog entry never orphans a log row's readability the way deleting
 * a food would.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medication (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_medication_name ON medication (name)")

        db.execSQL(
            "ALTER TABLE medication_entry ADD COLUMN medicationId INTEGER REFERENCES medication(id) ON DELETE SET NULL"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_medication_entry_medicationId ON medication_entry (medicationId)")
    }
}
