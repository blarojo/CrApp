package com.crapp

import android.app.Application
import com.crapp.data.db.AppDatabase
import com.crapp.data.insights.InsightsPreferences
import com.crapp.data.prefs.ThemePreferences
import com.crapp.data.repository.BackupRepository
import com.crapp.data.repository.BowelMovementRepository
import com.crapp.data.repository.FoodRepository
import com.crapp.data.repository.MedicationRepository

/**
 * Simple manual dependency provision -- no DI framework needed at this scale.
 * ViewModels can reach these via (application as CrAppApplication).
 */
class CrAppApplication : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val bowelMovementRepository by lazy { BowelMovementRepository(database.bowelMovementDao()) }
    val foodRepository by lazy { FoodRepository(database.foodDao(), database.foodEntryDao()) }
    val medicationRepository by lazy { MedicationRepository(database.medicationEntryDao()) }
    val backupRepository by lazy {
        BackupRepository(database, bowelMovementRepository, foodRepository, medicationRepository)
    }
    val themePreferences by lazy { ThemePreferences(this) }
    val insightsPreferences by lazy { InsightsPreferences(this) }
}
