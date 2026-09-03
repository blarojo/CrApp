package com.crapp.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId

/** The hours (24h clock, device-local time) treated as "night" for [isNightTime] -- docs/future-features.md spec 3. */
data class NightWindow(val startHour: Int = 22, val endHour: Int = 6) {
    /** Whether [instant] falls inside this window, handling the midnight wraparound (e.g. 22 -> 6). */
    fun isNightTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val hour = instant.atZone(zone).hour
        return if (startHour <= endHour) {
            hour in startHour until endHour
        } else {
            // Wraps past midnight, e.g. 22..23 or 0..5 for a 22 -> 6 window.
            hour >= startHour || hour < endHour
        }
    }
}

/**
 * Persists the configurable night-window setting via plain SharedPreferences --
 * same reasoning as [ThemePreferences]: two ints don't need a DataStore dependency.
 */
class NightWindowPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _nightWindow = MutableStateFlow(readStored())
    val nightWindow: StateFlow<NightWindow> = _nightWindow

    fun setNightWindow(window: NightWindow) {
        prefs.edit()
            .putInt(KEY_START_HOUR, window.startHour.coerceIn(0, 23))
            .putInt(KEY_END_HOUR, window.endHour.coerceIn(0, 23))
            .apply()
        _nightWindow.value = window
    }

    private fun readStored(): NightWindow {
        val default = NightWindow()
        return NightWindow(
            startHour = prefs.getInt(KEY_START_HOUR, default.startHour),
            endHour = prefs.getInt(KEY_END_HOUR, default.endHour)
        )
    }

    private companion object {
        const val PREFS_NAME = "crapp_prefs"
        const val KEY_START_HOUR = "night_window_start_hour"
        const val KEY_END_HOUR = "night_window_end_hour"
    }
}
