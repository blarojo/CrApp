package com.crapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Hourly fallback refresh (docs/backlog.md spec 15's "Refresh strategy") so the
 * widget's "today" summary still resets correctly across a local-midnight rollover
 * even if it sits untouched. The reactive collector in
 * [com.crapp.CrAppApplication.onCreate] already handles every *data-change* case
 * near-instantly (same pattern as this app's existing Wear OS today-count sync) --
 * this covers the one case that isn't a data change at all. A no-op (cheap) if no
 * widget instance is currently placed on any home screen.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        CrAppWidget().updateAll(applicationContext)
        return Result.success()
    }
}
