package com.crapp.data.insights

import android.content.Context

/**
 * Persists the raw JSON of the last-uploaded insights report via plain
 * SharedPreferences (same reasoning as `ThemePreferences`: one small value, no
 * DataStore dependency needed), so the Insights screen still shows something after
 * the app is closed and reopened without re-uploading.
 */
class InsightsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readLastReportJson(): String? = prefs.getString(KEY_LAST_REPORT, null)

    fun saveReportJson(json: String) {
        prefs.edit().putString(KEY_LAST_REPORT, json).apply()
    }

    private companion object {
        const val PREFS_NAME = "crapp_insights"
        const val KEY_LAST_REPORT = "last_report_json"
    }
}
