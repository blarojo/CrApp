package com.crapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * True if [this] falls on today's calendar date in [zone] -- the single shared
 * primitive behind every "today" count in the app (the dashboard's Today card,
 * [com.crapp.widget.CrAppWidget]'s summary), so they can never quietly drift apart
 * into disagreeing about what counts as "today".
 */
fun Instant.isToday(zone: ZoneId = ZoneId.systemDefault()): Boolean =
    atZone(zone).toLocalDate() == LocalDate.now(zone)
