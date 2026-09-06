package com.crapp.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crapp.AppConfig
import com.crapp.CrAppApplication
import com.crapp.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

/**
 * "No movement logged in over N hours" reminder (docs/future-features.md spec 6) --
 * a periodic [androidx.work.WorkManager] job (survives process death/reboot, unlike
 * a plain in-memory timer) that checks the most recent [com.crapp.data.model.BowelMovement]
 * timestamp and posts a local notification if it's older than the configured
 * threshold.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CrAppApplication
        val settings = app.notificationPreferences.settings.value
        if (!settings.enabled) return Result.success()

        val movements = app.bowelMovementRepository.allMovements.first()
        val lastTimestamp = movements.maxOfOrNull { it.timestamp } ?: return Result.success()

        val hoursSince = Duration.between(lastTimestamp, Instant.now()).toHours()
        if (hoursSince >= settings.thresholdHours) {
            postNotification(hoursSince)
        }
        return Result.success()
    }

    private fun postNotification(hoursSince: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Android 13+ requires the runtime POST_NOTIFICATIONS grant (requested from
            // SettingsScreen when the toggle is turned on); without it, skip rather than crash.
            return
        }

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_LOG_BOWEL_MOVEMENT, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("No movement logged in $hoursSince h")
            .setContentText("Tap to log ${AppConfig.DOG_NAME}'s next bowel movement.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 1
    }
}
