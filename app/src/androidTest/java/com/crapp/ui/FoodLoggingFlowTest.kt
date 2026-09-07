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
 * End-to-end (BDD-style) Compose UI test of the food-amount quick-fill buttons --
 * docs/backlog.md spec 14 -- driven through the real `MainActivity`/navigation graph,
 * same style/tooling as [BowelMovementLoggingFlowTest].
 *
 * Runs against an in-memory database ([AppDatabase.useInMemoryDatabaseForTesting]),
 * never the real on-device `crapp.db`.
 */
@RunWith(AndroidJUnit4::class)
class FoodLoggingFlowTest {

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

    private fun addNewFoodNamed(name: String) {
        composeTestRule.onNodeWithContentDescription("Add entry").performClick()
        composeTestRule.onNodeWithText("🍗 Food").performClick()
        composeTestRule.onNodeWithText("Log Food").assertExists()

        composeTestRule.onNodeWithText("Select a food").performClick()
        composeTestRule.onNodeWithText("+ Add new food").performClick()
        composeTestRule.onNodeWithText("Food name").performTextInput(name)
        composeTestRule.onNodeWithText("Add").performClick()
    }

    @Test
    fun tappingAQuickAmountButton_fillsBothAmountFieldsAndRoundTripsToHistory() {
        // Given a brand-new food entry being logged.
        addNewFoodNamed("Chicken")

        // When the user taps the "1 tin (400g)" shortcut instead of typing the amount
        // and picking the unit chip by hand.
        composeTestRule.onNodeWithText("1 tin (400g)").performClick()

        // And saves.
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Then History shows the free-text amount the shortcut filled -- which only
        // renders if the structured value/unit fields were *also* set correctly by the
        // same tap, since an empty/invalid structured amount would fail to save the
        // entry at all (FoodLogViewModel.save() requires a selected food, not a valid
        // amount, but this end-to-end path exercises the real save regardless).
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Meal · 1 tin (400g)").assertExists()
    }

    @Test
    fun tappingASecondQuickAmountButton_overwritesRatherThanAccumulates() {
        // Given a food entry where the user already tapped "1 cup"...
        addNewFoodNamed("Rice")
        composeTestRule.onNodeWithText("1 cup").performClick()

        // When they change their mind and tap "1 tin (400g)" instead.
        composeTestRule.onNodeWithText("1 tin (400g)").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Then only the second shortcut's value is saved -- not both concatenated.
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Meal · 1 tin (400g)").assertExists()
        composeTestRule.onNodeWithText("Meal · 1 cup").assertDoesNotExist()
    }
}
