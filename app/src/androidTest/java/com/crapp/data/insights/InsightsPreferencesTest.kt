package com.crapp.data.insights

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InsightsPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun readLastReportJson_nothingSavedYet_returnsNull() {
        context.getSharedPreferences("crapp_insights", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()

        assertNull(InsightsPreferences(context).readLastReportJson())
    }

    @Test
    fun saveReportJson_thenRead_returnsIt() {
        val prefs = InsightsPreferences(context)
        val json = """{"schemaVersion": 1}"""

        prefs.saveReportJson(json)

        assertEquals(json, prefs.readLastReportJson())
    }

    @Test
    fun saveReportJson_overwritesThePreviousOne() {
        val prefs = InsightsPreferences(context)
        prefs.saveReportJson("""{"schemaVersion": 1, "summary": "first"}""")

        prefs.saveReportJson("""{"schemaVersion": 1, "summary": "second"}""")

        assertEquals("""{"schemaVersion": 1, "summary": "second"}""", prefs.readLastReportJson())
    }
}
