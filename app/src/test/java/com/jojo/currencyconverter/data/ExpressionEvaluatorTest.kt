package com.jojo.currencyconverter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpressionEvaluatorTest {
    @Test
    fun multiplicationAndDivisionTakePrecedence() {
        assertEquals(30.0, ExpressionEvaluator.evaluate("10+5×4") ?: Double.NaN, 0.000001)
        assertEquals(12.0, ExpressionEvaluator.evaluate("20-16÷2") ?: Double.NaN, 0.000001)
    }

    @Test
    fun trailingOperatorIsIgnoredWhileTyping() {
        assertEquals(42.0, ExpressionEvaluator.evaluate("42+") ?: Double.NaN, 0.000001)
    }

    @Test
    fun divisionByZeroReturnsNoResult() {
        assertNull(ExpressionEvaluator.evaluate("8÷0"))
    }

    @Test
    fun amountFormattingKeepsCalculatorFriendlyPrecision() {
        assertEquals("12,345.68", ExpressionEvaluator.formatAmount(12345.678))
        assertEquals("0", ExpressionEvaluator.formatAmount(0.001))
    }
}
