package com.crapp.data.prefs

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** SharedPreferences-backed, so this needs a real Context -- hence instrumented, not JVM. */
@RunWith(AndroidJUnit4::class)
class ThemePreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun defaultMode_isSystem_whenNothingStoredYet() {
        context.getSharedPreferences("crapp_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()

        val prefs = ThemePreferences(context)

        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.value)
    }

    @Test
    fun setThemeMode_updatesTheFlowImmediately() {
        val prefs = ThemePreferences(context)

        prefs.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, prefs.themeMode.value)
    }

    @Test
    fun setThemeMode_persistsAcrossNewInstances() {
        ThemePreferences(context).setThemeMode(ThemeMode.LIGHT)

        // A fresh instance re-reads from SharedPreferences rather than sharing state
        // in memory -- this is what actually proves persistence, not just the flow.
        val secondInstance = ThemePreferences(context)

        assertEquals(ThemeMode.LIGHT, secondInstance.themeMode.value)
    }

    @Test
    fun themeMode_isCollectibleAsAFlow() = runBlocking {
        val prefs = ThemePreferences(context)
        prefs.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, prefs.themeMode.first())
    }
}
