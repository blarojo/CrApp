package com.crapp

import android.app.Application
import com.crapp.data.db.AppDatabase
import com.crapp.data.insights.InsightsPreferences
import com.crapp.data.prefs.NightWindowPreferences
import com.crapp.data.prefs.NotificationPreferences
import com.crapp.data.prefs.ThemePreferences
import com.crapp.data.repository.BackupRepository
import com.crapp.data.repository.BowelMovementRepository
import com.crapp.data.repository.EnergyRepository
import com.crapp.data.repository.FoodRepository
import com.crapp.data.repository.IngredientRepository
import com.crapp.data.repository.MedicationRepository
import com.crapp.data.repository.WalkRepository
import com.crapp.reminders.ReminderScheduler
import com.crapp.wear.WearSyncPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Simple manual dependency provision -- no DI framework needed at this scale.
 * ViewModels can reach these via (application as CrAppApplication).
 */
class CrAppApplication : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    /** Process-lifetime scope for one-off startup work (e.g. the ingredient backfill below). */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val bowelMovementRepository by lazy { BowelMovementRepository(database.bowelMovementDao()) }
    val foodRepository by lazy { FoodRepository(database.foodDao(), database.foodEntryDao()) }
    val medicationRepository by lazy { MedicationRepository(database.medicationEntryDao(), database.medicationDao()) }
    val energyRepository by lazy { EnergyRepository(database.energyEntryDao()) }
    val walkRepository by lazy { WalkRepository(database.walkEntryDao()) }
    val ingredientRepository by lazy { IngredientRepository(database.ingredientDao(), database.foodDao()) }
    val backupRepository by lazy {
        BackupRepository(
            database, bowelMovementRepository, foodRepository, medicationRepository,
            energyRepository, walkRepository
        )
    }
    val themePreferences by lazy { ThemePreferences(this) }
    val insightsPreferences by lazy { InsightsPreferences(this) }
    val notificationPreferences by lazy { NotificationPreferences(this) }
    val nightWindowPreferences by lazy { NightWindowPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        // One-time backfill (docs/future-features.md spec 9): parses every Food's
        // free-text ingredients into structured rows. Cheap no-op once already
        // structured, so safe to fire on every app start rather than only once ever.
        applicationScope.launch { ingredientRepository.backfillIfNeeded() }

        ReminderScheduler.applySavedPreference(this)

        // Keeps a paired Wear OS watch's today-count display current (spec 5) on
        // every change made from the phone side too, not just a watch-triggered
        // insert (which pushes its own immediate update from
        // PhoneWearableListenerService). No-op if no watch is paired -- putDataItem
        // just queues the update for whenever one next syncs.
        applicationScope.launch {
            val zone = ZoneId.systemDefault()
            bowelMovementRepository.allMovements
                .map { movements -> movements.count { it.timestamp.atZone(zone).toLocalDate() == LocalDate.now(zone) } }
                .distinctUntilChanged()
                .collect { count -> WearSyncPublisher.pushTodayCount(this@CrAppApplication, count) }
        }
    }
}
