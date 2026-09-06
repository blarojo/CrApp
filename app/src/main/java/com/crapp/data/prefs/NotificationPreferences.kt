package com.crapp.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** "No movement logged in over N hours" reminder settings -- docs/future-features.md spec 6 (reminders). */
data class NotificationSettings(val enabled: Boolean = false, val thresholdHours: Int = 24)

/**
 * Persists the reminder toggle + threshold via plain SharedPreferences, same
 * reasoning as [ThemePreferences] -- two small values don't need a DataStore
 * dependency.
 */
class NotificationPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readStored())
    val settings: StateFlow<NotificationSettings> = _settings

    fun setSettings(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_THRESHOLD_HOURS, settings.thresholdHours.coerceIn(1, 168))
            .apply()
        _settings.value = settings
    }

    private fun readStored(): NotificationSettings = NotificationSettings(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        thresholdHours = prefs.getInt(KEY_THRESHOLD_HOURS, 24)
    )

    private companion object {
        const val PREFS_NAME = "crapp_prefs"
        const val KEY_ENABLED = "reminders_enabled"
        const val KEY_THRESHOLD_HOURS = "reminders_threshold_hours"
    }
}
