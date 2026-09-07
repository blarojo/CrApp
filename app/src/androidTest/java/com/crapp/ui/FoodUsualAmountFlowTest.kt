package com.crapp.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.MainActivity
import com.crapp.data.db.AppDatabase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end (BDD-style) Compose UI test of a food's own "usual amount" -- a
 * follow-up to docs/backlog.md spec 14 (see [FoodLogViewModel.onFoodSelected]'s
 * KDoc): selecting a food that has one set on the Food Catalog screen pre-fills the
 * food-logging screen's amount fields, still fully overridable. Driven through the
 * real `MainActivity`/navigation graph, same style/tooling as
 * [BowelMovementLoggingFlowTest].
 *
 * Runs against an in-memory database ([AppDatabase.useInMemoryDatabaseForTesting]),
 * never the real on-device `crapp.db`.
 */
@RunWith(AndroidJUnit4::class)
class FoodUsualAmountFlowTest {

    companion object {
        init {
            // Must run before the rule below launches MainActivity/CrAppApplication.
            AppDatabase.useInMemoryDatabaseForTesting = true
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        AppDatabase.useInMemoryDatabaseForTesting = false
    }

    /** Adds a new food via the Food Catalog's own dialog with a usual amount set, then returns to Home. */
    private fun addFoodWithUsualAmount(name: String, unit: String) {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Manage Foods & Ingredients").performClick()
        composeTestRule.onNodeWithContentDescription("Add food").performClick()

        composeTestRule.onNodeWithText("Name").performTextInput(name)
        composeTestRule.onNodeWithText("Value").performTextInput("1")
        composeTestRule.onNodeWithText(unit).performClick()
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("← Back").performClick() // Food Catalog -> Settings
        composeTestRule.onNodeWithContentDescription("← Back").performClick() // Settings -> Home
    }

    @Test
    fun selectingAFoodWithAUsualAmount_prefillsTheAmountFields_andRoundTripsToHistory() {
        // Given a food ("HA Wet") with a usual amount of "1 tin (400g)" already set
        // on it via the Food Catalog.
        addFoodWithUsualAmount("HA Wet", "tin (400g)")

        // When the user logs a food entry and selects that food from the dropdown --
        // without touching the amount fields at all.
        composeTestRule.onNodeWithContentDescription("Add entry").performClick()
        composeTestRule.onNodeWithText("🍗 Food").performClick()
        composeTestRule.onNodeWithText("Select a food").performClick()
        composeTestRule.onNodeWithText("HA Wet").performClick()

        // Then the amount is already filled in, and saving round-trips it correctly.
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Meal · 1 tin (400g)").assertExists()
    }

    @Test
    fun theUsualAmountPrefillStaysOverridable() {
        // Given the same food with a usual amount of "1 cup".
        addFoodWithUsualAmount("Kibble", "cup")

        // When the user selects it, then overrides the pre-filled amount by tapping
        // a different quick-amount shortcut before saving.
        composeTestRule.onNodeWithContentDescription("Add entry").performClick()
        composeTestRule.onNodeWithText("🍗 Food").performClick()
        composeTestRule.onNodeWithText("Select a food").performClick()
        composeTestRule.onNodeWithText("Kibble").performClick()
        composeTestRule.onNodeWithText("1 tin (400g)").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Then the override is what's saved, not the food's usual amount.
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Meal · 1 tin (400g)").assertExists()
        composeTestRule.onNodeWithText("Meal · 1 cup").assertDoesNotExist()
    }
}
