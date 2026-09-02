package com.crapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.crapp.data.prefs.ThemeMode
import com.crapp.ui.nav.CrAppNavHost
import com.crapp.ui.theme.CrAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themePreferences = (application as CrAppApplication).themePreferences
        setContent {
            val themeMode by themePreferences.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CrAppTheme(darkTheme = darkTheme) {
                CrAppNavHost()
            }
        }
    }
}
