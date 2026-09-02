package com.crapp.ui.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

data class FoodLogUiState(
    val timestamp: Instant = Instant.now(),
    val selectedFood: Food? = null,
    val newFoodName: String = "",
    val amount: String = "",
    val mealType: MealType = MealType.MEAL,
    val showAddNewDialog: Boolean = false,
    val saved: Boolean = false
)

class FoodLogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).foodRepository

    val foodsByRecentUse: StateFlow<List<Food>> = repository.foodsByRecentUse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(FoodLogUiState())
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()

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
            repository.logFoodEntry(
                FoodEntry(
                    timestamp = state.timestamp,
                    foodId = food.id,
                    amount = state.amount.ifBlank { null },
                    mealType = state.mealType
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
