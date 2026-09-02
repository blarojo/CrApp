package com.crapp.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds `Food.ingredients` (docs/development-plan.md Phase 8: ingredient tracking). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food ADD COLUMN ingredients TEXT")
    }
}
