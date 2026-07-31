package com.jojo.currencyconverter.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object ExpressionEvaluator {
    private val operators = setOf('+', '-', '×', '÷')
    private val trailingOperators = Regex("[+\\-×÷]+$")
    private val numericToken = Regex("""\d*\.?\d+""")
    private val groupedNumber = DecimalFormat(
        "#,##0.################",
        DecimalFormatSymbols(Locale.US),
    )
    private val amountNumber = DecimalFormat(
        "#,##0.##",
        DecimalFormatSymbols(Locale.US),
    )
    private val scientificNumber = DecimalFormat(
        "0.####E0",
        DecimalFormatSymbols(Locale.US),
    )

    fun evaluate(rawExpression: String): Double? {
        val expression = rawExpression.replace(trailingOperators, "")
        if (expression.isBlank()) return 0.0

        var index = 0

        fun parseNumber(allowLeadingMinus: Boolean): Double? {
            val start = index
            if (allowLeadingMinus && expression.getOrNull(index) == '-') index += 1
            var hasDigit = false
            var hasDecimal = false
            while (index < expression.length) {
                val character = expression[index]
                when {
                    character.isDigit() -> {
                        hasDigit = true
                        index += 1
                    }
                    character == '.' && !hasDecimal -> {
                        hasDecimal = true
                        index += 1
                    }
                    else -> break
                }
            }
            if (!hasDigit) return null
            return expression.substring(start, index).toDoubleOrNull()
        }

        var currentTerm = parseNumber(allowLeadingMinus = true) ?: return null
        var total = 0.0
        var additiveSign = 1.0

        while (index < expression.length) {
            val operator = expression[index]
            if (operator !in operators) return null
            index += 1
            val nextValue = parseNumber(allowLeadingMinus = false) ?: return null

            when (operator) {
                '×' -> currentTerm *= nextValue
                '÷' -> {
                    if (nextValue == 0.0) return null
                    currentTerm /= nextValue
                }
                '+' -> {
                    total += additiveSign * currentTerm
                    currentTerm = nextValue
                    additiveSign = 1.0
                }
                '-' -> {
                    total += additiveSign * currentTerm
                    currentTerm = nextValue
                    additiveSign = -1.0
                }
            }
        }

        return total + additiveSign * currentTerm
    }

    fun formatExpression(expression: String): String {
        if (expression.isBlank()) return "0"
        return numericToken.replace(expression) { match ->
            val token = match.value
            if (token.endsWith(".")) {
                "${groupedNumber.format(token.dropLast(1).toDoubleOrNull() ?: 0.0)}."
            } else {
                groupedNumber.format(token.toDoubleOrNull() ?: 0.0)
            }
        }.replace("+", " + ")
            .replace("-", " − ")
            .replace("×", " × ")
            .replace("÷", " ÷ ")
            .trim()
    }

    fun formatAmount(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        if (abs(value) >= 1e18) return scientificNumber.format(value)
        val normalized = if (abs(value) < 0.005) 0.0 else value
        return amountNumber.format(normalized)
    }

    fun formatRate(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return BigDecimal.valueOf(value)
            .round(MathContext(6, RoundingMode.HALF_UP))
            .stripTrailingZeros()
            .toPlainString()
    }

    fun plainNumber(value: Double): String = BigDecimal.valueOf(value)
        .stripTrailingZeros()
        .toPlainString()
}
