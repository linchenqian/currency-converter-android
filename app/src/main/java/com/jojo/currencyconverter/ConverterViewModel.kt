package com.jojo.currencyconverter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jojo.currencyconverter.data.CurrencyCatalog
import com.jojo.currencyconverter.data.CurrencyInfo
import com.jojo.currencyconverter.data.ExpressionEvaluator
import com.jojo.currencyconverter.data.RateStatus
import com.jojo.currencyconverter.data.RatesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CurrencySlot {
    From,
    To,
}

data class ConverterUiState(
    val fromCode: String = "JPY",
    val toCode: String = "CNY",
    val expression: String = "12800×2+500",
    val committed: Boolean = false,
    val rates: Map<String, Double> = RatesRepository.FallbackRates,
    val rateStatus: RateStatus = RateStatus.Loading,
    val ratesUpdatedAt: Long = 0L,
) {
    val fromCurrency: CurrencyInfo get() = CurrencyCatalog.find(fromCode)
    val toCurrency: CurrencyInfo get() = CurrencyCatalog.find(toCode)
    val sourceValue: Double? get() = ExpressionEvaluator.evaluate(expression)
    val conversionRate: Double?
        get() {
            val fromRate = rates[fromCode] ?: return null
            val toRate = rates[toCode] ?: return null
            return toRate / fromRate
        }
    val convertedValue: Double?
        get() = sourceValue?.let { source -> conversionRate?.let(source::times) }
}

internal fun commitCalculation(current: ConverterUiState): ConverterUiState {
    val result = current.sourceValue?.takeIf { it.isFinite() } ?: return current
    return current.copy(
        expression = ExpressionEvaluator.resultNumber(result),
        committed = true,
    )
}

private val trailingNumber = Regex("""(\d*\.?\d+)$""")

internal fun applyPercentCalculation(current: ConverterUiState): ConverterUiState {
    val base = if (current.committed && current.sourceValue != null) {
        ExpressionEvaluator.resultNumber(current.sourceValue!!)
    } else {
        current.expression
    }
    val match = trailingNumber.find(base)
        ?: return current.copy(expression = base, committed = false)
    val replacement = ExpressionEvaluator.percentNumber(match.value)
        ?: return current.copy(expression = base, committed = false)
    return current.copy(
        expression = base.replaceRange(match.range, replacement),
        committed = false,
    )
}

class ConverterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RatesRepository(application)
    private val initialRates = repository.initialSnapshot()
    private val _uiState = MutableStateFlow(
        ConverterUiState(
            rates = initialRates.rates,
            rateStatus = if (initialRates.status == RateStatus.Offline) {
                RateStatus.Loading
            } else {
                initialRates.status
            },
            ratesUpdatedAt = initialRates.updatedAt,
        ),
    )
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    init {
        refreshRates()
    }

    fun refreshRates() {
        viewModelScope.launch {
            runCatching { repository.refresh() }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            rates = snapshot.rates,
                            rateStatus = snapshot.status,
                            ratesUpdatedAt = snapshot.updatedAt,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { current ->
                        current.copy(
                            rateStatus = if (initialRates.status == RateStatus.Cached) {
                                RateStatus.Cached
                            } else {
                                RateStatus.Offline
                            },
                        )
                    }
                }
        }
    }

    fun appendDigit(digit: String) {
        _uiState.update { current ->
            val next = when {
                current.committed -> digit
                current.expression == "0" && digit != "00" -> digit
                else -> current.expression + digit
            }
            current.copy(expression = next, committed = false)
        }
    }

    fun appendDecimal() {
        _uiState.update { current ->
            val expression = current.expression
            val next = when {
                current.committed || expression.isEmpty() -> "0."
                expression.substringAfterLastOperator().contains('.') -> expression
                expression.lastOrNull().isOperator() -> "${expression}0."
                else -> "$expression."
            }
            current.copy(expression = next, committed = false)
        }
    }

    fun appendOperator(operator: Char) {
        _uiState.update { current ->
            if (current.expression.isEmpty()) return@update current
            val next = when {
                current.committed && current.sourceValue != null ->
                    ExpressionEvaluator.plainNumber(current.sourceValue!!) + operator
                current.expression.lastOrNull().isOperator() ->
                    current.expression.dropLast(1) + operator
                else -> current.expression + operator
            }
            current.copy(expression = next, committed = false)
        }
    }

    fun backspace() {
        _uiState.update {
            it.copy(expression = it.expression.dropLast(1), committed = false)
        }
    }

    fun clear() {
        _uiState.update { it.copy(expression = "", committed = false) }
    }

    fun applyPercent() {
        _uiState.update(::applyPercentCalculation)
    }

    fun commitExpression() {
        _uiState.update(::commitCalculation)
    }

    fun swapCurrencies() {
        _uiState.update {
            it.copy(fromCode = it.toCode, toCode = it.fromCode)
        }
    }

    fun selectCurrency(slot: CurrencySlot, currency: CurrencyInfo) {
        _uiState.update { current ->
            when (slot) {
                CurrencySlot.From -> current.copy(
                    fromCode = currency.code,
                    toCode = if (currency.code == current.toCode) {
                        current.fromCode
                    } else {
                        current.toCode
                    },
                )
                CurrencySlot.To -> current.copy(
                    fromCode = if (currency.code == current.fromCode) {
                        current.toCode
                    } else {
                        current.fromCode
                    },
                    toCode = currency.code,
                )
            }
        }
    }

    private fun String.substringAfterLastOperator(): String =
        substringAfterLast('+')
            .substringAfterLast('-')
            .substringAfterLast('×')
            .substringAfterLast('÷')

    private fun Char?.isOperator(): Boolean = this == '+' || this == '-' || this == '×' || this == '÷'
}
