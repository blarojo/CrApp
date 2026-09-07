package com.crapp.data.model

/**
 * Renders a whole-number [Double] without a trailing ".0" (e.g. "1", not "1.0"; "1.5"
 * stays "1.5") -- shared by anything that pre-fills a free-text amount from a
 * structured value + unit, e.g. the food-logging screen's quick-amount buttons and a
 * food's own usual amount (see [Food.usualAmountValue]).
 */
fun Double.toAmountText(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
