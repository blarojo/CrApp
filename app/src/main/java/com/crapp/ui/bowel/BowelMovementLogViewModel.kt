package com.crapp.ui.bowel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Amount
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Location
import com.crapp.export.BowelMovementPhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class BowelMovementLogUiState(
    val timestamp: Instant = Instant.now(),
    val consistency: Int = 4,
    val hasBlood: Boolean = false,
    val hasMucus: Boolean = false,
    val notes: String = "",
    val amount: Amount? = null,
    val location: Location? = null,
    val locationOther: String = "",
    val photoUri: String? = null,
    val isEditing: Boolean = false,
    val saved: Boolean = false
)

class BowelMovementLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val app = application as CrAppApplication
    private val repository = app.bowelMovementRepository
    private val nightWindowPreferences = app.nightWindowPreferences
    private val photoStore = BowelMovementPhotoStore(application)
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    private var previousPhotoUri: String? = null

    private val _uiState = MutableStateFlow(BowelMovementLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<BowelMovementLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getById(editingId)?.let { movement ->
                    previousPhotoUri = movement.photoUri
                    _uiState.update {
                        it.copy(
                            timestamp = movement.timestamp,
                            consistency = movement.consistency,
                            hasBlood = movement.hasBlood,
                            hasMucus = movement.hasMucus,
                            notes = movement.notes ?: "",
                            amount = movement.amount,
                            location = movement.location,
                            locationOther = movement.locationOther ?: "",
                            photoUri = movement.photoUri
                        )
                    }
                }
            }
        }
    }

    fun onTimestampChange(timestamp: Instant) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun onConsistencyChange(consistency: Int) {
        _uiState.update { it.copy(consistency = consistency) }
    }

    fun onHasBloodChange(hasBlood: Boolean) {
        _uiState.update { it.copy(hasBlood = hasBlood) }
    }

    fun onHasMucusChange(hasMucus: Boolean) {
        _uiState.update { it.copy(hasMucus = hasMucus) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onAmountChange(amount: Amount) {
        _uiState.update { it.copy(amount = if (it.amount == amount) null else amount) }
    }

    fun onLocationChange(location: Location) {
        _uiState.update {
            val newLocation = if (it.location == location) null else location
            it.copy(location = newLocation, locationOther = if (newLocation == Location.OTHER) it.locationOther else "")
        }
    }

    fun onLocationOtherChange(text: String) {
        _uiState.update { it.copy(locationOther = text) }
    }

    /** Creates a fresh `MediaStore` target for the camera to write into; returns null if the store rejected it. */
    fun createPhotoCaptureTarget(): Uri? = photoStore.createNewPhotoUri()

    /** Called once the camera activity result confirms [uri] was written to. */
    fun onPhotoCaptured(uri: Uri) {
        _uiState.update { it.copy(photoUri = uri.toString()) }
    }

    fun onRemovePhoto() {
        _uiState.value.photoUri?.let(photoStore::delete)
        _uiState.update { it.copy(photoUri = null) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val nightWindow = nightWindowPreferences.nightWindow.value
            val movement = BowelMovement(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                consistency = state.consistency,
                hasBlood = state.hasBlood,
                hasMucus = state.hasMucus,
                notes = state.notes.ifBlank { null },
                amount = state.amount,
                location = state.location,
                locationOther = state.locationOther.ifBlank { null }.takeIf { state.location == Location.OTHER },
                isNightTime = nightWindow.isNightTime(state.timestamp),
                photoUri = state.photoUri
            )
            if (editingId != -1L) repository.update(movement) else repository.add(movement)
            // A photo replaced mid-edit leaves its old MediaStore row orphaned otherwise.
            if (previousPhotoUri != null && previousPhotoUri != state.photoUri) {
                photoStore.delete(previousPhotoUri!!)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (editingId == -1L) return
        viewModelScope.launch {
            repository.getById(editingId)?.let { movement ->
                movement.photoUri?.let(photoStore::delete)
                repository.delete(movement)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
