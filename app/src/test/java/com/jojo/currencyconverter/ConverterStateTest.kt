package com.jojo.currencyconverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConverterStateTest {
    @Test
    fun equalsReplacesExpressionWithCalculatedResult() {
        val initial = ConverterUiState(
            expression = "991÷89",
            rates = emptyMap(),
        )

        val committed = commitCalculation(initial)

        assertEquals("11.1348314607", committed.expression)
        assertTrue(committed.committed)
        assertEquals(11.1348314607, committed.sourceValue!!, 0.0000000001)
    }

    @Test
    fun equalsLeavesInvalidExpressionAvailableForEditing() {
        val initial = ConverterUiState(
            expression = "8÷0",
            rates = emptyMap(),
        )

        val committed = commitCalculation(initial)

        assertSame(initial, committed)
        assertFalse(committed.committed)
    }
}
