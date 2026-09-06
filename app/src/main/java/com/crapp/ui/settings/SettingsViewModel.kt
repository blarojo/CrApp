package com.crapp.ui.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.prefs.NotificationSettings
import com.crapp.data.prefs.ThemeMode
import com.crapp.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BackupStatus { IDLE, WORKING }

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val status: BackupStatus = BackupStatus.IDLE,
    val message: String? = null,
    val notificationSettings: NotificationSettings = NotificationSettings()
)

/**
 * Backs the Settings screen (docs/development-plan.md Phase 8): theme choice +
 * backup/restore. The backup/restore file I/O runs in [viewModelScope] rather than
 * a Composable-scoped `rememberCoroutineScope()` -- it needs to survive the
 * round-trip through the external system file-picker Activity. Outcomes also show
 * as a [Toast] (not just the in-app [SettingsUiState.message] text): some devices
 * recreate the hosting Activity across that round-trip in a way that can drop the
 * Compose-observed state update, but a Toast doesn't depend on that Activity's
 * Compose tree surviving.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication
    private val themePreferences = app.themePreferences
    private val backupRepository = app.backupRepository
    private val notificationPreferences = app.notificationPreferences

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = themePreferences.themeMode.value,
            notificationSettings = notificationPreferences.settings.value
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            notificationPreferences.settings.collect { settings ->
                _uiState.update { it.copy(notificationSettings = settings) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    /**
     * [permissionGranted] is only meaningful when turning reminders *on* on Android
     * 13+ -- the caller (SettingsScreen) requests `POST_NOTIFICATIONS` first and
     * passes the result here, since a Worker can't request runtime permissions
     * itself. Turning reminders off never needs it.
     */
    fun setRemindersEnabled(enabled: Boolean, permissionGranted: Boolean = true) {
        if (enabled && !permissionGranted) return
        val updated = notificationPreferences.settings.value.copy(enabled = enabled)
        notificationPreferences.setSettings(updated)
        if (enabled) ReminderScheduler.schedule(getApplication()) else ReminderScheduler.cancel(getApplication())
    }

    fun setReminderThresholdHours(hours: Int) {
        val updated = notificationPreferences.settings.value.copy(thresholdHours = hours.coerceIn(1, 168))
        notificationPreferences.setSettings(updated)
    }

    /** Writes a fresh backup to [uri] (from a `CreateDocument` picker result). */
    fun backupTo(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupStatus.WORKING, message = null) }
            val success = try {
                val json = backupRepository.exportToJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("no output stream for $uri")
                }
                true
            } catch (e: Exception) {
                false
            }
            finish(if (success) "Backup saved." else "Backup wasn't saved -- try again.")
        }
    }

    /** Restores from [uri] (from an `OpenDocument` picker result), replacing all current data. */
    fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupStatus.WORKING, message = null) }
            val message = try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("couldn't open $uri")
                }
                backupRepository.restoreFromJson(json)
                "Data restored from backup."
            } catch (e: Exception) {
                "Restore failed: ${e.message ?: "invalid backup file"}"
            }
            finish(message)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = BackupStatus.WORKING, message = null) }
            val message = try {
                backupRepository.clearAllData()
                "All data cleared."
            } catch (e: Exception) {
                "Couldn't clear data: ${e.message}"
            }
            finish(message)
        }
    }

    private fun finish(message: String) {
        _uiState.update { it.copy(status = BackupStatus.IDLE, message = message) }
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }
}
