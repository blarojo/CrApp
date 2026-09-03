package com.crapp.wear

import com.crapp.CrAppApplication
import com.crapp.data.model.BowelMovement
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Phone side of the Wear OS companion app sync (docs/future-features.md spec 5):
 * receives the watch's "log a movement now" message and inserts a real
 * [BowelMovement] row -- the watch itself never touches the database directly, so
 * there's only ever one source of truth (see the `:wear` module's KDoc).
 *
 * The watch UI intentionally can't set [BowelMovement.consistency] (only the phone
 * app can, per the original requirement). This is the "trickiest modeling
 * question" flagged in the spec: rather than making the column nullable (a bigger
 * ripple through every screen/chart/export that assumes a real 1-7 score) or an
 * arbitrary sentinel that would corrupt the consistency trend chart, a
 * watch-originated row gets the same neutral default the phone's own new-entry
 * form starts on ([DEFAULT_CONSISTENCY]) plus a [FOLLOW_UP_NOTE] marker so it reads
 * as "needs a real consistency score" in History rather than a silently wrong one.
 */
class PhoneWearableListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearProtocol.PATH_LOG_MOVEMENT) return

        val app = applicationContext as CrAppApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.bowelMovementRepository.add(
                BowelMovement(
                    timestamp = Instant.now(),
                    consistency = DEFAULT_CONSISTENCY,
                    notes = FOLLOW_UP_NOTE
                )
            )
            // The count push on the repository's own collector (see
            // CrAppApplication.onCreate) also fires from this insert, but pushing
            // again immediately here means the watch UI updates without waiting for
            // that Flow to re-emit through its debounce/dispatch.
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val todayCount = app.bowelMovementRepository.allMovements.first()
                .count { it.timestamp.atZone(zone).toLocalDate() == today }
            WearSyncPublisher.pushTodayCount(applicationContext, todayCount)
        }
    }

    private companion object {
        const val DEFAULT_CONSISTENCY = 4
        const val FOLLOW_UP_NOTE = "Logged from Wear OS watch -- edit to set a real consistency score."
    }
}
