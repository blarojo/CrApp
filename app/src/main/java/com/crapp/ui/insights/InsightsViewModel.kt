package com.crapp.ui.insights

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.insights.InsightsParser
import com.crapp.data.insights.InsightsReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InsightsUiState(
    val report: InsightsReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Backs the Insights screen (docs/development-plan.md Phase 8). */
class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CrAppApplication
    private val insightsPreferences = app.insightsPreferences

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        insightsPreferences.readLastReportJson()?.let { json ->
            runCatching { InsightsParser.parse(json) }
                .onSuccess { report -> _uiState.update { it.copy(report = report) } }
        }
    }

    /** Loads and parses a report from [uri] (an `OpenDocument` picker result), persisting it. */
    fun loadFrom(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("couldn't open $uri")
                }
                val report = InsightsParser.parse(json)
                insightsPreferences.saveReportJson(json)
                _uiState.update { it.copy(report = report, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Couldn't load that file.")
                }
            }
        }
    }
}
