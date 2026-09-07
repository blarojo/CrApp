package com.crapp.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.MainActivity
import com.crapp.data.db.AppDatabase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end (BDD-style) regression test for a real bug: logging a dog-walker-
 * reported walk didn't move the "Today" hero card's bowel-movement count, even
 * though "Movements per day" further down the same dashboard already correctly
 * folded walk reports in. Fixed via the shared `countBowelMovementsToday` (see
 * docs/app-functionality.md SS7/SS14) -- this test exercises the real save -> Home
 * refresh path, not just the pure function.
 *
 * Runs against an in-memory database ([AppDatabase.useInMemoryDatabaseForTesting]),
 * never the real on-device `crapp.db`.
 */
@RunWith(AndroidJUnit4::class)
class WalkBowelMovementCountFlowTest {

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
    fun loggingAWalkReport_addsItsCountToTodaysBowelMovementTotal() {
        // Given a freshly launched app with no logged entries.
        composeTestRule.onNodeWithText("No entries yet — tap + to log your first one.").assertExists()

        // When the user logs a dog-walker-reported walk with 3 bowel movements.
        composeTestRule.onNodeWithContentDescription("Add entry").performClick()
        composeTestRule.onNodeWithText("🚶 Walk").performClick()
        composeTestRule.onNodeWithText("Log Walk").assertExists()
        repeat(3) { composeTestRule.onNodeWithText("+", substring = false).performClick() }
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Then the Today card's headline bowel-movement count reflects it, exactly
        // like it already does for individually-logged movements -- not "0 bowel
        // movements today" despite a walk with real movements having been logged.
        composeTestRule.onNodeWithText("3 bowel movements today").assertExists()
    }
}
