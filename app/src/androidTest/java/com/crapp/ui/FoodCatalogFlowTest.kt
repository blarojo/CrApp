package com.crapp.ui

import androidx.compose.ui.test.assertIsNotEnabled
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
 * End-to-end (BDD-style) Compose UI test of the Food Catalog's own "Add new food"
 * flow -- docs/backlog.md spec 12's manual-entry addition -- driven through the real
 * `MainActivity`/navigation graph, same style/tooling as [BowelMovementLoggingFlowTest].
 *
 * Doesn't cover the "Scan label" OCR button: like the bowel-movement photo capture
 * flow (see docs/app-functionality.md's Testing status), a real camera capture can't
 * be driven via synthetic taps in an instrumented test on this hardware -- that part
 * needs a manual on-device pass.
 *
 * Runs against an in-memory database ([AppDatabase.useInMemoryDatabaseForTesting]),
 * never the real on-device `crapp.db`.
 */
@RunWith(AndroidJUnit4::class)
class FoodCatalogFlowTest {

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

    private fun openFoodCatalog() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Manage Foods & Ingredients").performClick()
        composeTestRule.onNodeWithText("Food Catalog").assertExists()
    }

    @Test
    fun addingANewFoodManually_appearsInTheCatalogWithAllThreeFields() {
        // Given the Food Catalog is empty and the user opens the add-food dialog.
        openFoodCatalog()
        composeTestRule.onNodeWithText(
            "No foods yet -- tap + to add one (name, brand, ingredients), or log a food " +
                "entry and it'll show up here automatically."
        ).assertExists()

        composeTestRule.onNodeWithContentDescription("Add food").performClick()
        composeTestRule.onNodeWithText("Add new food").assertExists()

        // "Add" is disabled until a name is entered -- no way to save a nameless food.
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()

        // When the user fills in all three fields, entirely by hand (no scan).
        composeTestRule.onNodeWithText("Name").performTextInput("Boiled Chicken")
        composeTestRule.onNodeWithText("Brand (optional)").performTextInput("Homemade")
        composeTestRule.onNodeWithText("Ingredients (optional)").performTextInput("Chicken breast, water")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()

        // Then it shows up in the catalog with exactly those three fields, without
        // ever having been logged as a food entry first.
        composeTestRule.onNodeWithText("Boiled Chicken").assertExists()
        composeTestRule.onNodeWithText("Homemade").assertExists()
        composeTestRule.onNodeWithText("Chicken breast, water").assertExists()
    }

    @Test
    fun addingAFoodWithANameThatAlreadyExists_updatesThatRowInstead() {
        // Given a food that was already auto-created via the food-logging screen's
        // inline "Add new" (name only, no ingredients yet) -- see FoodLoggingFlowTest.
        composeTestRule.onNodeWithContentDescription("Add entry").performClick()
        composeTestRule.onNodeWithText("🍗 Food").performClick()
        composeTestRule.onNodeWithText("Select a food").performClick()
        composeTestRule.onNodeWithText("+ Add new food").performClick()
        composeTestRule.onNodeWithText("Food name").performTextInput("Rice")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // When the user later fills in that same food's ingredients from the Food
        // Catalog's own add dialog, typing the exact same name rather than tapping
        // "Rice" in the (still-empty-ingredients) list to edit it directly.
        openFoodCatalog()
        composeTestRule.onNodeWithContentDescription("Add food").performClick()
        composeTestRule.onNodeWithText("Name").performTextInput("Rice")
        composeTestRule.onNodeWithText("Ingredients (optional)").performTextInput("White rice")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()

        // Then there's still only one "Rice" row -- the ingredients were added to the
        // existing catalog entry, not duplicated into a second one.
        composeTestRule.onNodeWithText("Rice").assertExists()
        composeTestRule.onNodeWithText("White rice").assertExists()
    }
}
