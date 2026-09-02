package com.crapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MedicationEntry

/**
 * An app *update* (installing a new version over an existing install, e.g. `adb
 * install -r` or a Play Store update) never touches this database file -- it's
 * already preserved without anything special. The one way to lose that guarantee is
 * a future schema change (adding a `Migration`-less version bump, or worse, calling
 * `fallbackToDestructiveMigration()` on the builder below) -- don't add either
 * without writing a real `Migration`. See docs/development-plan.md Phase 8 for the
 * separate backup/restore feature (BackupRepository) that covers the cases this
 * doesn't: uninstalling, losing the phone, or a factory reset.
 */
@Database(
    entities = [BowelMovement::class, Food::class, FoodEntry::class, MedicationEntry::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bowelMovementDao(): BowelMovementDao
    abstract fun foodDao(): FoodDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun medicationEntryDao(): MedicationEntryDao

    companion object {
        private const val DATABASE_NAME = "crapp.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SeedDataCallback)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * Pre-populates the food catalog on a brand-new install only -- `onCreate` fires
 * exactly once, when the database file doesn't exist yet, so an existing install
 * upgrading (via [MIGRATION_1_2] instead) never gets these inserted alongside its
 * real data. See [SEED_FOODS].
 */
private object SeedDataCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        SEED_FOODS.forEach { seed ->
            db.execSQL(
                "INSERT INTO food (name, brand, ingredients) VALUES (?, ?, ?)",
                arrayOf(seed.name, seed.brand, seed.ingredients)
            )
        }
    }
}
