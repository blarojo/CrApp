package com.crapp

import android.app.Application
import com.crapp.data.db.AppDatabase
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
}
