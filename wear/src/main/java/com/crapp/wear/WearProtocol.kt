package com.crapp.wear

/**
 * Wearable Data Layer API paths shared with the phone app's
 * `com.crapp.wear.PhoneWearableListenerService` (docs/future-features.md spec 5).
 * Duplicated rather than shared via a Gradle module dependency -- not worth a
 * third `:shared` module just for these few constants. Keep in sync with the
 * phone app's copy (app/src/main/java/com/crapp/wear/WearProtocol.kt) if either
 * changes.
 */
object WearProtocol {
    /** Watch -> phone: "log a bowel movement now." No payload; the phone timestamps it on receipt. */
    const val PATH_LOG_MOVEMENT = "/crapp/log_movement"

    /** Phone -> watch: today's bowel-movement count, pushed on every change. */
    const val PATH_TODAY_COUNT = "/crapp/today_count"
    const val KEY_COUNT = "count"
    const val KEY_DATE = "date"
}
