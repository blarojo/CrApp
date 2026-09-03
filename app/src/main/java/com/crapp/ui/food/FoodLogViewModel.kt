package com.crapp.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Food
import com.crapp.data.model.FoodEntry
import com.crapp.data.model.MealType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Fixed unit choices for [FoodLogUiState.amountUnit] -- docs/future-features.md's dose/amount spec. */
val FOOD_AMOUNT_UNITS = listOf("cup", "tbsp", "g")

data class FoodLogUiState(
    val timestamp: Instant = Instant.now(),
    val selectedFood: Food? = null,
    val newFoodName: String = "",
    val amount: String = "",
    val mealType: MealType = MealType.MEAL,
    val showAddNewDialog: Boolean = false,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
    /** Structured amount, additive to the free-text [amount] above -- raw text, parsed to Double on save. */
    val amountValueText: String = "",
    val amountUnit: String? = null
)

class FoodLogViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).foodRepository
    private val editingId: Long = savedStateHandle.get<Long>("id") ?: -1L

    val foodsByRecentUse: StateFlow<List<Food>> = repository.foodsByRecentUse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(FoodLogUiState(isEditing = editingId != -1L))
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()

    init {
        if (editingId != -1L) {
            viewModelScope.launch {
                repository.getFoodEntryById(editingId)?.let { entry ->
                    val food = repository.getFoodById(entry.foodId)
                    _uiState.update {
                        it.copy(
                            timestamp = entry.timestamp,
                            selectedFood = food,
                            amount = entry.amount ?: "",
                            mealType = entry.mealType,
                            amountValueText = entry.amountValue?.toString() ?: "",
                            amountUnit = entry.amountUnit
                        )
                    }
                }
            }
        }
    }

    fun onTimestampChange(timestamp: Instant) {
        _uiState.update { it.copy(timestamp = timestamp) }
    }

    fun onFoodSelected(food: Food) {
        _uiState.update { it.copy(selectedFood = food) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onMealTypeChange(mealType: MealType) {
        _uiState.update { it.copy(mealType = mealType) }
    }

    fun onAmountValueTextChange(text: String) {
        _uiState.update { it.copy(amountValueText = text) }
    }

    fun onAmountUnitChange(unit: String) {
        _uiState.update { it.copy(amountUnit = if (it.amountUnit == unit) null else unit) }
    }

    fun onShowAddNewDialog(show: Boolean) {
        _uiState.update { it.copy(showAddNewDialog = show, newFoodName = "") }
    }

    fun onNewFoodNameChange(name: String) {
        _uiState.update { it.copy(newFoodName = name) }
    }

    fun confirmAddNewFood() {
        val name = _uiState.value.newFoodName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.getOrCreateFood(name)
            _uiState.update {
                it.copy(
                    selectedFood = Food(id = id, name = name),
                    showAddNewDialog = false,
                    newFoodName = ""
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val food = state.selectedFood ?: return
        viewModelScope.launch {
            val entry = FoodEntry(
                id = if (editingId != -1L) editingId else 0,
                timestamp = state.timestamp,
                foodId = food.id,
                amount = state.amount.ifBlank { null },
                mealType = state.mealType,
                amountValue = state.amountValueText.toDoubleOrNull(),
                amountUnit = state.amountUnit
            )
            if (editingId != -1L) repository.updateFoodEntry(entry) else repository.logFoodEntry(entry)
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (editingId == -1L) return
        viewModelScope.launch {
            repository.getFoodEntryById(editingId)?.let { repository.deleteFoodEntry(it) }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
