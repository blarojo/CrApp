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

/** Fixed unit choices for [FoodLogUiState.amountUnit] -- docs/backlog.md's dose/amount spec. */
val FOOD_AMOUNT_UNITS = listOf("cup", "tbsp", "g", "tin (400g)")

/**
 * A one-tap shortcut that fills both the free-text [FoodLogUiState.amount] and the
 * structured [FoodLogUiState.amountValueText]/[FoodLogUiState.amountUnit] fields at once,
 * for the most common portions -- docs/backlog.md spec 14. A small list literal rather than
 * a catalog/DB table, since it's just two fixed, food-independent shortcuts for now.
 */
data class QuickAmount(val label: String, val value: Double, val unit: String)

val FOOD_QUICK_AMOUNTS = listOf(
    QuickAmount(label = "1 cup", value = 1.0, unit = "cup"),
    QuickAmount(label = "1 tin (400g)", value = 1.0, unit = "tin (400g)")
)

/** Renders a whole-number [Double] without a trailing ".0" so a quick-amount tap fills the value field with "1", not "1.0". */
private fun Double.toAmountValueText(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

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

    /**
     * Alphabetical (docs/backlog.md spec 12's ordering request), same source
     * (`repository.allFoods`) as the Food Catalog admin screen -- was previously
     * most-recently-used-first ([FoodRepository.foodsByRecentUse], still used
     * elsewhere e.g. backup/export where display order doesn't matter).
     */
    val foods: StateFlow<List<Food>> = repository.allFoods
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

    /**
     * Fills the free-text and structured amount fields from a [QuickAmount] shortcut in one
     * tap. The fields stay directly editable afterwards -- this is just a starting point, not
     * a locked value; tapping a different shortcut (or the same one again) simply overwrites
     * whatever's there, same as picking a different amount unit chip does today.
     */
    fun onQuickAmountSelected(quickAmount: QuickAmount) {
        _uiState.update {
            it.copy(
                amount = quickAmount.label,
                amountValueText = quickAmount.value.toAmountValueText(),
                amountUnit = quickAmount.unit
            )
        }
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
