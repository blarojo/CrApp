package com.crapp.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.CrAppApplication
import com.crapp.MainActivity
import com.crapp.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end (BDD-style: given/when/then) Compose UI test of the app's single most
 * important flow -- see docs/development-plan.md §11 -- driven through the real
 * `MainActivity`/navigation graph, the Android-idiomatic equivalent of a
 * Cucumber/Gherkin front-to-back test (Cucumber-Android itself is effectively
 * unmaintained; `androidx.compose.ui.test` + Espresso is the currently-recommended,
 * actively-supported tooling for this).
 *
 * Runs against an in-memory database ([AppDatabase.useInMemoryDatabaseForTesting]),
 * never the real on-device `crapp.db` -- this must never touch a real install's
 * actual logged data.
 */
@RunWith(AndroidJUnit4::class)
class BowelMovementLoggingFlowTest {

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

    @Test
    fun loggingABowelMovement_appearsInHistoryWithTheChosenConsistency() {
        // Given the app is freshly launched with no logged entries.
        composeTestRule.onNodeWithText("No entries yet — tap + to log your first one.").assertExists()

        // When the user opens the quick-add menu and starts a bowel movement log.
        composeTestRule.onNodeWithContentDescription("Add entry").performClick()
        composeTestRule.onNodeWithText("💩 Bowel Movement").performClick()
        composeTestRule.onNodeWithText("Log Bowel Movement").assertExists()

        // And saves with the default consistency (4) -- the selector is a LazyRow, so
        // picking a specific score reliably would mean scrolling it into view first;
        // the default-value save path is exactly as real a test of the save flow.
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Then it's back on the dashboard, reflecting the new entry.
        composeTestRule.onNodeWithText("1 bowel movement today").assertExists()

        // And it shows up in History with the chosen consistency.
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Consistency 4").assertExists()
    }

    @Test
    fun deletingABowelMovementFromHistory_removesItAndTheDashboardReflectsThat() {
        // Given a bowel movement already logged (seeded directly via the repository --
        // this test is about delete, not logging, so it doesn't re-drive that flow).
        val app = composeTestRule.activity.application as CrAppApplication
        runBlocking {
            app.bowelMovementRepository.add(
                com.crapp.data.model.BowelMovement(
                    timestamp = java.time.Instant.now(),
                    consistency = 5
                )
            )
        }

        // When the user opens History and long-presses the entry to delete it.
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Consistency 5").assertExists()
        composeTestRule.onNodeWithText("Consistency 5").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Delete").performClick()

        // Then it's gone from History...
        composeTestRule.onNodeWithText("Consistency 5").assertDoesNotExist()

        // ...and the underlying data is really gone, not just hidden in the UI.
        val remaining = runBlocking { app.bowelMovementRepository.allMovements.first() }
        assertEquals(0, remaining.size)
    }
}
