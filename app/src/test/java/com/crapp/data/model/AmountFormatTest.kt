package com.crapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatTest {

    @Test
    fun toAmountText_wholeNumber_dropsTrailingDotZero() {
        assertEquals("1", 1.0.toAmountText())
        assertEquals("50", 50.0.toAmountText())
    }

    @Test
    fun toAmountText_fractional_keepsTheDecimal() {
        assertEquals("1.5", 1.5.toAmountText())
    }
}
