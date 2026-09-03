package com.crapp.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.crapp.CrAppApplication
import java.time.Duration

/**
 * Schedules/cancels the periodic [ReminderWorker] job based on the saved
 * [com.crapp.data.prefs.NotificationPreferences] -- docs/future-features.md spec 6.
 * Checks every 6 hours (a daily check would leave up to 24h between an overdue
 * movement and the reminder actually firing) rather than a shorter interval;
 * WorkManager's minimum periodic interval is 15 minutes but polling that often for
 * a many-hours threshold would just waste battery for no earlier a notification.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "reminders"
    private const val WORK_NAME = "no_movement_reminder"
    private val CHECK_INTERVAL = Duration.ofHours(6)

    /** Called once at app startup (see [CrAppApplication.onCreate]) to (re)apply whatever was last saved. */
    fun applySavedPreference(context: Context) {
        ensureNotificationChannel(context)
        val settings = (context.applicationContext as CrAppApplication).notificationPreferences.settings.value
        if (settings.enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(CHECK_INTERVAL).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Movement reminders", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you when no bowel movement has been logged in a while."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
