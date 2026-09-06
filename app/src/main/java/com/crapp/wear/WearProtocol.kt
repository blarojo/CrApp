package com.crapp.wear

/**
 * Wearable Data Layer API paths shared between this phone-side listener and the
 * `:wear` module's watch app (docs/future-features.md spec 5, Wear OS companion
 * app). The two modules can't share a Gradle dependency without a third `:shared`
 * module just for these few constants, so the literal path strings are duplicated
 * on both sides -- keep them in sync if either changes; see `wear/.../WearProtocol.kt`.
 */
object WearProtocol {
    /** Watch -> phone: "log a bowel movement now." No payload; the phone timestamps it on receipt. */
    const val PATH_LOG_MOVEMENT = "/crapp/log_movement"

    /** Phone -> watch: today's bowel-movement count, pushed on every change. */
    const val PATH_TODAY_COUNT = "/crapp/today_count"
    const val KEY_COUNT = "count"
    const val KEY_DATE = "date"
}
