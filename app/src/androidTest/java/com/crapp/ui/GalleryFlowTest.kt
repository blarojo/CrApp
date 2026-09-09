package com.crapp.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crapp.CrAppApplication
import com.crapp.MainActivity
import com.crapp.data.db.AppDatabase
import com.crapp.data.model.BowelMovement
import com.crapp.data.model.Location
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * End-to-end (BDD-style) Compose UI test of the Gallery screen (docs/backlog.md
 * spec 14). Real camera capture can't be automated on the test device (same
 * limitation already documented for the photo-attachment and label-scan features),
 * so this seeds movements directly via the repository, bypassing the camera -- the
 * grid/dialog logic itself doesn't need a real photo capture to exercise.
 *
 * Runs against an in-memory database ([AppDatabase.useInMemoryDatabaseForTesting]),
 * never the real on-device `crapp.db`.
 */
@RunWith(AndroidJUnit4::class)
class GalleryFlowTest {

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
    fun galleryShowsOnlyMovementsWithAPhoto_captionedCorrectly_andOpensTheFullSizeViewer() {
        // Given one movement with a photo (garden, daytime) and one without any photo.
        val app = composeTestRule.activity.application as CrAppApplication
        runBlocking {
            app.bowelMovementRepository.add(
                BowelMovement(
                    timestamp = Instant.now(),
                    consistency = 5,
                    location = Location.GARDEN,
                    photoUri = "content://media/external/images/media/999999"
                )
            )
            app.bowelMovementRepository.add(
                BowelMovement(timestamp = Instant.now(), consistency = 3) // no photo -- must not appear
            )
        }

        // When the user opens Gallery from Home.
        composeTestRule.onNodeWithText("Gallery").performClick()

        // Then only the photographed movement's card appears, captioned with its
        // consistency and location -- and the photo-less movement is nowhere in it.
        composeTestRule.onNodeWithText("Consistency 5").assertExists()
        composeTestRule.onNodeWithText("Garden").assertExists()
        composeTestRule.onNodeWithText("Consistency 3").assertDoesNotExist()

        // And tapping the card opens the full-size viewer with a visible close button
        // (not just tap-outside-to-dismiss).
        composeTestRule.onNodeWithText("Consistency 5").performClick()
        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun galleryWithNoPhotos_showsTheEmptyStateMessage() {
        // Given no entries at all.

        // When the user opens Gallery from Home.
        composeTestRule.onNodeWithText("Gallery").performClick()

        // Then the empty state explains what to do next, not a blank grid.
        composeTestRule.onNodeWithText("No photos yet — attach one next time you log a bowel movement.").assertExists()
    }
}
