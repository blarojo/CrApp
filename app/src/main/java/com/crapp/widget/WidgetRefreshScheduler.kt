package com.crapp.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

/**
 * Schedules [WidgetRefreshWorker]'s hourly fallback refresh (docs/backlog.md spec 15)
 * -- deliberately not the legacy `android:updatePeriodMillis` widget-provider metadata
 * field (see `res/xml/crapp_widget_info.xml`, set to 0), to stay consistent with how
 * [com.crapp.reminders.ReminderScheduler]'s periodic job is already built. Always
 * scheduled at app startup regardless of whether the widget is actually placed
 * anywhere (unlike reminders, there's no user-facing toggle for this) -- a no-op
 * hourly wakeup is cheap, and [WidgetRefreshWorker] itself no-ops if no widget
 * instance exists.
 */
object WidgetRefreshScheduler {
    private const val WORK_NAME = "widget_refresh"
    private val CHECK_INTERVAL = Duration.ofHours(1)

    /** Called once at app startup (see [com.crapp.CrAppApplication.onCreate]). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(CHECK_INTERVAL).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
