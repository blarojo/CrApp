package com.crapp.ui.foodcatalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Food
import com.crapp.data.repository.DeleteFoodResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Fields for the Food Catalog's own "Add new food" flow -- docs/backlog.md spec 12's manual-entry addition. Null [name] means the dialog is closed. */
data class AddFoodUiState(
    val name: String = "",
    val brand: String = "",
    val ingredients: String = "",
    /** This food's usual portion, e.g. "1" + "tin (400g)" -- see [Food.usualAmountValue]. */
    val usualAmountValueText: String = "",
    val usualAmountUnit: String? = null
)

/** Backs the Food Catalog screen (docs/development-plan.md Phase 8): add/view/edit foods and their ingredients, delete old entries. */
class FoodCatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).foodRepository

    val foods: StateFlow<List<Food>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editingFood = MutableStateFlow<Food?>(null)
    val editingFood: StateFlow<Food?> = _editingFood.asStateFlow()

    private val _addFoodState = MutableStateFlow<AddFoodUiState?>(null)
    val addFoodState: StateFlow<AddFoodUiState?> = _addFoodState.asStateFlow()

    private val _pendingDelete = MutableStateFlow<Food?>(null)
    val pendingDelete: StateFlow<Food?> = _pendingDelete.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun startEditing(food: Food) {
        _editingFood.value = food
    }

    fun cancelEditing() {
        _editingFood.value = null
    }

    /**
     * Saves the edit-food dialog's fields: ingredients (unchanged from before) plus
     * this food's usual amount, parsed the same way the food-logging screen parses
     * its own structured-amount fields (blank value/no unit selected -> both null,
     * i.e. "no usual amount set").
     */
    fun saveFoodEdits(ingredients: String, usualAmountValueText: String, usualAmountUnit: String?) {
        val food = _editingFood.value ?: return
        viewModelScope.launch {
            repository.updateFood(
                food.copy(
                    ingredients = ingredients.ifBlank { null },
                    usualAmountValue = usualAmountValueText.toDoubleOrNull(),
                    usualAmountUnit = usualAmountUnit
                )
            )
            _editingFood.value = null
        }
    }

    fun startAddingNewFood() {
        _addFoodState.value = AddFoodUiState()
    }

    fun cancelAddingNewFood() {
        _addFoodState.value = null
    }

    fun onNewFoodNameChange(name: String) {
        _addFoodState.update { it?.copy(name = name) }
    }

    fun onNewFoodBrandChange(brand: String) {
        _addFoodState.update { it?.copy(brand = brand) }
    }

    fun onNewFoodIngredientsChange(ingredients: String) {
        _addFoodState.update { it?.copy(ingredients = ingredients) }
    }

    fun onNewFoodUsualAmountValueTextChange(text: String) {
        _addFoodState.update { it?.copy(usualAmountValueText = text) }
    }

    fun onNewFoodUsualAmountUnitChange(unit: String) {
        _addFoodState.update { it?.copy(usualAmountUnit = if (it.usualAmountUnit == unit) null else unit) }
    }

    fun confirmAddNewFood() {
        val state = _addFoodState.value ?: return
        val name = state.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addOrUpdateFood(
                name = name,
                brand = state.brand.trim().ifBlank { null },
                ingredients = state.ingredients.trim().ifBlank { null },
                usualAmountValue = state.usualAmountValueText.toDoubleOrNull(),
                usualAmountUnit = state.usualAmountUnit
            )
            _addFoodState.value = null
            _message.value = "Added \"$name\"."
        }
    }

    fun requestDelete(food: Food) {
        _pendingDelete.value = food
    }

    fun cancelDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val food = _pendingDelete.value ?: return
        viewModelScope.launch {
            when (val result = repository.deleteFood(food)) {
                is DeleteFoodResult.Success -> _message.value = "Deleted \"${food.name}\"."
                is DeleteFoodResult.InUse -> _message.value =
                    "Can't delete \"${food.name}\" -- ${result.entryCount} logged food " +
                        "${if (result.entryCount == 1) "entry" else "entries"} still reference it. " +
                        "Delete those from History first."
            }
            _pendingDelete.value = null
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
