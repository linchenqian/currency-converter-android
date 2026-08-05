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

    @Test
    fun percentUsesExactDecimalForTrailingOperand() {
        val initial = ConverterUiState(
            expression = "1475×668.55",
            rates = emptyMap(),
        )

        val percent = applyPercentCalculation(initial)

        assertEquals("1475×6.6855", percent.expression)
        assertEquals(9861.1125, percent.sourceValue!!, 0.0000001)
    }

    @Test
    fun percentPreservesSmallDecimalValues() {
        val initial = ConverterUiState(
            expression = "0.1",
            rates = emptyMap(),
        )

        val percent = applyPercentCalculation(initial)

        assertEquals("0.001", percent.expression)
    }
}
