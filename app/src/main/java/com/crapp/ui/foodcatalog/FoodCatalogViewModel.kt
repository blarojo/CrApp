package com.crapp.ui.foodcatalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crapp.CrAppApplication
import com.crapp.data.model.Food
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Food Catalog screen (docs/development-plan.md Phase 8): view/edit ingredients. */
class FoodCatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CrAppApplication).foodRepository

    val foods: StateFlow<List<Food>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editingFood = MutableStateFlow<Food?>(null)
    val editingFood: StateFlow<Food?> = _editingFood.asStateFlow()

    fun startEditing(food: Food) {
        _editingFood.value = food
    }

    fun cancelEditing() {
        _editingFood.value = null
    }

    fun saveIngredients(ingredients: String) {
        val food = _editingFood.value ?: return
        viewModelScope.launch {
            repository.updateFood(food.copy(ingredients = ingredients.ifBlank { null }))
            _editingFood.value = null
        }
    }
}
