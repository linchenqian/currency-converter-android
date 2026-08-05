package com.jojo.currencyconverter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jojo.currencyconverter.ConverterUiState
import com.jojo.currencyconverter.ConverterViewModel
import com.jojo.currencyconverter.CurrencySlot
import com.jojo.currencyconverter.data.CurrencyCatalog
import com.jojo.currencyconverter.data.CurrencyInfo
import com.jojo.currencyconverter.data.ExpressionEvaluator
import com.jojo.currencyconverter.data.RateStatus
import com.jojo.currencyconverter.ui.theme.LocalCurrencyColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HalfScreenHeightThresholdDp = 520f
private const val CompactWindowHeightThresholdDp = 780f

internal enum class ConverterWindowMode {
    Regular,
    Compact,
    HalfScreen,
}

internal fun converterWindowMode(heightDp: Float): ConverterWindowMode = when {
    heightDp < HalfScreenHeightThresholdDp -> ConverterWindowMode.HalfScreen
    heightDp < CompactWindowHeightThresholdDp -> ConverterWindowMode.Compact
    else -> ConverterWindowMode.Regular
}

@Composable
fun ConverterRoute(
    viewModel: ConverterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currencySlot by remember { mutableStateOf<CurrencySlot?>(null) }

    ConverterScreen(
        state = state,
        onOpenCurrencyPicker = { currencySlot = it },
        onSwapCurrencies = viewModel::swapCurrencies,
        onDigit = viewModel::appendDigit,
        onDecimal = viewModel::appendDecimal,
        onOperator = viewModel::appendOperator,
        onBackspace = viewModel::backspace,
        onClear = viewModel::clear,
        onPercent = viewModel::applyPercent,
        onEquals = viewModel::commitExpression,
    )

    currencySlot?.let { slot ->
        CurrencyPickerSheet(
            slot = slot,
            selectedCode = if (slot == CurrencySlot.From) state.fromCode else state.toCode,
            onDismiss = { currencySlot = null },
            onSelect = { currency ->
                viewModel.selectCurrency(slot, currency)
                currencySlot = null
            },
        )
    }
}

@Composable
private fun ConverterScreen(
    state: ConverterUiState,
    onOpenCurrencyPicker: (CurrencySlot) -> Unit,
    onSwapCurrencies: () -> Unit,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
) {
    val colors = LocalCurrencyColors.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground),
    ) {
        val windowMode = converterWindowMode(maxHeight.value)
        val conversionHeight = when (windowMode) {
            ConverterWindowMode.HalfScreen -> (maxHeight * 0.38f).coerceIn(122.dp, 168.dp)
            ConverterWindowMode.Compact -> (maxHeight * 0.43f).coerceIn(220.dp, 340.dp)
            ConverterWindowMode.Regular -> (maxHeight * 0.47f).coerceIn(390.dp, 438.dp)
        }
        val calculatorPadding = when (windowMode) {
            ConverterWindowMode.HalfScreen -> 5.dp
            ConverterWindowMode.Compact -> 9.dp
            ConverterWindowMode.Regular -> 14.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            ConversionPanel(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(conversionHeight),
                windowMode = windowMode,
                onOpenCurrencyPicker = onOpenCurrencyPicker,
                onSwapCurrencies = onSwapCurrencies,
            )
            CalculatorPad(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = calculatorPadding, vertical = calculatorPadding),
                windowMode = windowMode,
                onDigit = onDigit,
                onDecimal = onDecimal,
                onOperator = onOperator,
                onBackspace = onBackspace,
                onClear = onClear,
                onPercent = onPercent,
                onEquals = onEquals,
            )
        }
    }
}

@Composable
private fun ConversionPanel(
    state: ConverterUiState,
    modifier: Modifier,
    onOpenCurrencyPicker: (CurrencySlot) -> Unit,
    onSwapCurrencies: () -> Unit,
    windowMode: ConverterWindowMode,
) {
    val colors = LocalCurrencyColors.current
    val compact = windowMode != ConverterWindowMode.Regular
    val halfScreen = windowMode == ConverterWindowMode.HalfScreen
    Surface(
        modifier = modifier,
        color = colors.displaySurface,
        shape = RoundedCornerShape(
            bottomStart = if (halfScreen) 22.dp else 30.dp,
            bottomEnd = if (halfScreen) 22.dp else 30.dp,
        ),
        shadowElevation = if (halfScreen) 6.dp else 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = when (windowMode) {
                        ConverterWindowMode.HalfScreen -> 12.dp
                        ConverterWindowMode.Compact -> 18.dp
                        ConverterWindowMode.Regular -> 28.dp
                    },
                    end = when (windowMode) {
                        ConverterWindowMode.HalfScreen -> 12.dp
                        ConverterWindowMode.Compact -> 18.dp
                        ConverterWindowMode.Regular -> 28.dp
                    },
                    top = when (windowMode) {
                        ConverterWindowMode.HalfScreen -> 1.dp
                        ConverterWindowMode.Compact -> 4.dp
                        ConverterWindowMode.Regular -> 12.dp
                    },
                    bottom = when (windowMode) {
                        ConverterWindowMode.HalfScreen -> 3.dp
                        ConverterWindowMode.Compact -> 8.dp
                        ConverterWindowMode.Regular -> 16.dp
                    },
                ),
        ) {
            ConversionBand(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                currency = state.fromCurrency,
                expression = ExpressionEvaluator.formatExpression(state.expression),
                amount = state.sourceValue,
                amountMaxSize = when (windowMode) {
                    ConverterWindowMode.HalfScreen -> 31
                    ConverterWindowMode.Compact -> 40
                    ConverterWindowMode.Regular -> 54
                },
                amountMinSize = if (halfScreen) 13 else if (compact) 15 else 18,
                windowMode = windowMode,
                onCurrencyClick = { onOpenCurrencyPicker(CurrencySlot.From) },
            )
            SwapDivider(onSwapCurrencies, windowMode)
            ConversionBand(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                currency = state.toCurrency,
                expression = "约合",
                amount = state.convertedValue,
                amountMaxSize = when (windowMode) {
                    ConverterWindowMode.HalfScreen -> 30
                    ConverterWindowMode.Compact -> 38
                    ConverterWindowMode.Regular -> 48
                },
                amountMinSize = if (halfScreen) 12 else if (compact) 14 else 17,
                windowMode = windowMode,
                converted = true,
                onCurrencyClick = { onOpenCurrencyPicker(CurrencySlot.To) },
            )
            if (!halfScreen) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = buildRateText(state),
                    color = colors.secondaryText,
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ConversionBand(
    currency: CurrencyInfo,
    expression: String,
    amount: Double?,
    amountMaxSize: Int,
    amountMinSize: Int,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    converted: Boolean = false,
    windowMode: ConverterWindowMode,
) {
    val colors = LocalCurrencyColors.current
    val compact = windowMode != ConverterWindowMode.Regular
    val halfScreen = windowMode == ConverterWindowMode.HalfScreen
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencySelector(
            currency = currency,
            windowMode = windowMode,
            onClick = onCurrencyClick,
        )
        Spacer(Modifier.width(if (halfScreen) 4.dp else if (compact) 6.dp else 10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = if (halfScreen) 0.dp else if (converted) 6.dp else 2.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!halfScreen) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = expression,
                    color = colors.primaryText,
                    fontSize = when {
                        compact && converted -> 14.sp
                        compact -> 13.sp
                        converted -> 18.sp
                        else -> 16.sp
                    },
                    fontWeight = if (converted) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(
                    Modifier.height(
                        when {
                            compact -> 5.dp
                            converted -> 14.dp
                            else -> 16.dp
                        },
                    ),
                )
            }
            AutoSizeAmount(
                currency = currency,
                amount = amount,
                maxSizeSp = amountMaxSize,
                minSizeSp = amountMinSize,
            )
        }
    }
}

@Composable
private fun CurrencySelector(
    currency: CurrencyInfo,
    onClick: () -> Unit,
    windowMode: ConverterWindowMode,
) {
    val colors = LocalCurrencyColors.current
    val compact = windowMode != ConverterWindowMode.Regular
    val halfScreen = windowMode == ConverterWindowMode.HalfScreen
    Column(
        modifier = Modifier
            .width(if (halfScreen) 54.dp else if (compact) 66.dp else 84.dp)
            .clip(RoundedCornerShape(if (halfScreen) 12.dp else 18.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "选择${currency.name}",
                onClick = onClick,
            )
            .padding(vertical = if (halfScreen) 0.dp else if (compact) 1.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SvgFlag(
            flagCode = currency.flagCode,
            modifier = Modifier.size(if (halfScreen) 25.dp else if (compact) 32.dp else 42.dp),
        )
        Spacer(Modifier.height(if (halfScreen) 1.dp else if (compact) 3.dp else 7.dp))
        if (!halfScreen) {
            Text(
                text = currency.name,
                color = colors.primaryText,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(if (compact) 0.dp else 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currency.code,
                color = colors.secondaryText,
                fontSize = if (halfScreen) 10.sp else if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(if (halfScreen) 12.dp else if (compact) 16.dp else 19.dp),
            )
        }
    }
}

@Composable
private fun AutoSizeAmount(
    currency: CurrencyInfo,
    amount: Double?,
    maxSizeSp: Int,
    minSizeSp: Int,
) {
    val colors = LocalCurrencyColors.current
    val numberText = ExpressionEvaluator.formatAmount(amount)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val selectedSize = remember(
            currency.symbol,
            numberText,
            maxWidthPx,
            maxSizeSp,
            minSizeSp,
        ) {
            var low = minSizeSp.toFloat()
            var high = maxSizeSp.toFloat()
            repeat(9) {
                val candidate = (low + high) / 2f
                val measured = textMeasurer.measure(
                    text = amountAnnotatedText(currency.symbol, numberText, candidate),
                    style = androidx.compose.ui.text.TextStyle(
                        color = colors.primaryText,
                        fontSize = candidate.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.045).em,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
                if (measured.size.width <= maxWidthPx) {
                    low = candidate
                } else {
                    high = candidate
                }
            }
            low
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = amountAnnotatedText(currency.symbol, numberText, selectedSize),
            color = colors.primaryText,
            fontSize = selectedSize.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.045).em,
            lineHeight = selectedSize.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun amountAnnotatedText(
    symbol: String,
    number: String,
    sizeSp: Float,
): AnnotatedString = buildAnnotatedString {
    withStyle(
        SpanStyle(
            fontSize = (sizeSp * 0.52f).sp,
            fontWeight = FontWeight.Medium,
            baselineShift = BaselineShift(0.12f),
            letterSpacing = (-0.01).em,
        ),
    ) {
        append(symbol)
        append(" ")
    }
    append(number)
}

@Composable
private fun SwapDivider(
    onClick: () -> Unit,
    windowMode: ConverterWindowMode,
) {
    val colors = LocalCurrencyColors.current
    val compact = windowMode != ConverterWindowMode.Regular
    val halfScreen = windowMode == ConverterWindowMode.HalfScreen
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (halfScreen) 24.dp else if (compact) 34.dp else 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(color = colors.divider)
        Surface(
            onClick = onClick,
            modifier = Modifier.size(if (halfScreen) 28.dp else if (compact) 38.dp else 48.dp),
            shape = CircleShape,
            color = colors.swapSurface,
            contentColor = colors.primaryText,
            border = BorderStroke(1.dp, colors.swapBorder),
            shadowElevation = if (halfScreen) 2.dp else 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Height,
                    contentDescription = "交换原始币种和目标币种",
                    modifier = Modifier.size(if (halfScreen) 18.dp else if (compact) 23.dp else 29.dp),
                )
            }
        }
    }
}

@Composable
private fun CalculatorPad(
    modifier: Modifier,
    windowMode: ConverterWindowMode,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
) {
    val colors = LocalCurrencyColors.current
    val minimumHeight = if (windowMode == ConverterWindowMode.Regular) 328.dp else 0.dp
    val surfaceRadius = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 18.dp
        ConverterWindowMode.Compact -> 24.dp
        ConverterWindowMode.Regular -> 28.dp
    }
    val innerPadding = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 5.dp
        ConverterWindowMode.Compact -> 10.dp
        ConverterWindowMode.Regular -> 16.dp
    }
    val horizontalSpacing = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 5.dp
        ConverterWindowMode.Compact -> 8.dp
        ConverterWindowMode.Regular -> 12.dp
    }
    val verticalSpacing = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 4.dp
        ConverterWindowMode.Compact -> 7.dp
        ConverterWindowMode.Regular -> 10.dp
    }
    val operatorMinWidth = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 52.dp
        ConverterWindowMode.Compact -> 62.dp
        ConverterWindowMode.Regular -> 70.dp
    }
    val operatorMaxWidth = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 58.dp
        ConverterWindowMode.Compact -> 70.dp
        ConverterWindowMode.Regular -> 78.dp
    }
    Surface(
        modifier = modifier.heightIn(min = minimumHeight),
        shape = RoundedCornerShape(surfaceRadius),
        color = colors.consoleSurface,
        border = BorderStroke(1.dp, colors.surfaceBorder),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        ) {
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                CalculatorRow(horizontalSpacing) {
                    CalculatorKey(
                        "C",
                        "清除",
                        onClear,
                        windowMode,
                        Modifier.weight(1f),
                        utility = true,
                    )
                    CalculatorIconKey(
                        icon = Icons.AutoMirrored.Rounded.Backspace,
                        label = "退格",
                        onClick = onBackspace,
                        windowMode = windowMode,
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorKey(
                        "%",
                        "百分比",
                        onPercent,
                        windowMode,
                        Modifier.weight(1f),
                        utility = true,
                    )
                }
                CalculatorRow(horizontalSpacing) {
                    CalculatorKey("7", "7", { onDigit("7") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("8", "8", { onDigit("8") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("9", "9", { onDigit("9") }, windowMode, Modifier.weight(1f))
                }
                CalculatorRow(horizontalSpacing) {
                    CalculatorKey("4", "4", { onDigit("4") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("5", "5", { onDigit("5") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("6", "6", { onDigit("6") }, windowMode, Modifier.weight(1f))
                }
                CalculatorRow(horizontalSpacing) {
                    CalculatorKey("1", "1", { onDigit("1") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("2", "2", { onDigit("2") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("3", "3", { onDigit("3") }, windowMode, Modifier.weight(1f))
                }
                CalculatorRow(horizontalSpacing) {
                    CalculatorKey("00", "00", { onDigit("00") }, windowMode, Modifier.weight(1f))
                    CalculatorKey("0", "0", { onDigit("0") }, windowMode, Modifier.weight(1f))
                    CalculatorKey(".", "小数点", onDecimal, windowMode, Modifier.weight(1f))
                }
            }

            OperatorRail(
                modifier = Modifier
                    .widthIn(min = operatorMinWidth, max = operatorMaxWidth)
                    .fillMaxHeight(),
                windowMode = windowMode,
                onOperator = onOperator,
                onEquals = onEquals,
            )
        }
    }
}

@Composable
private fun ColumnScope.CalculatorRow(
    horizontalSpacing: Dp,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        content = content,
    )
}

@Composable
private fun CalculatorKey(
    text: String,
    label: String,
    onClick: () -> Unit,
    windowMode: ConverterWindowMode,
    modifier: Modifier = Modifier,
    utility: Boolean = false,
) {
    val colors = LocalCurrencyColors.current
    val keyRadius = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 12.dp
        ConverterWindowMode.Compact -> 18.dp
        ConverterWindowMode.Regular -> 22.dp
    }
    val fontSize = when (windowMode) {
        ConverterWindowMode.HalfScreen -> if (utility) 18.sp else 20.sp
        ConverterWindowMode.Compact -> if (utility) 23.sp else 25.sp
        ConverterWindowMode.Regular -> if (utility) 25.sp else 27.sp
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(keyRadius),
        color = if (utility) colors.keySurfaceStrong else colors.keySurface,
        contentColor = colors.primaryText,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalculatorIconKey(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    windowMode: ConverterWindowMode,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCurrencyColors.current
    val keyRadius = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 12.dp
        ConverterWindowMode.Compact -> 18.dp
        ConverterWindowMode.Regular -> 22.dp
    }
    val iconSize = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 20.dp
        ConverterWindowMode.Compact -> 24.dp
        ConverterWindowMode.Regular -> 27.dp
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(keyRadius),
        color = colors.keySurfaceStrong,
        contentColor = colors.primaryText,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun OperatorRail(
    modifier: Modifier,
    windowMode: ConverterWindowMode,
    onOperator: (Char) -> Unit,
    onEquals: () -> Unit,
) {
    val colors = LocalCurrencyColors.current
    val railRadius = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 15.dp
        ConverterWindowMode.Compact -> 20.dp
        ConverterWindowMode.Regular -> 24.dp
    }
    val equalsSize = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 34.dp
        ConverterWindowMode.Compact -> 48.dp
        ConverterWindowMode.Regular -> 58.dp
    }
    val equalsFontSize = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 23.sp
        ConverterWindowMode.Compact -> 27.sp
        ConverterWindowMode.Regular -> 31.sp
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(railRadius),
        color = colors.operatorSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OperatorKey("÷", "除", { onOperator('÷') }, windowMode, showDivider = true)
            OperatorKey("×", "乘", { onOperator('×') }, windowMode, showDivider = true)
            OperatorKey("−", "减", { onOperator('-') }, windowMode, showDivider = true)
            OperatorKey("+", "加", { onOperator('+') }, windowMode, showDivider = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    onClick = onEquals,
                    modifier = Modifier.size(equalsSize),
                    shape = CircleShape,
                    color = colors.accent,
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "=",
                            fontSize = equalsFontSize,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.OperatorKey(
    text: String,
    label: String,
    onClick: () -> Unit,
    windowMode: ConverterWindowMode,
    showDivider: Boolean,
) {
    val colors = LocalCurrencyColors.current
    val operatorFontSize = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 22.sp
        ConverterWindowMode.Compact -> 26.sp
        ConverterWindowMode.Regular -> 29.sp
    }
    val dividerPadding = when (windowMode) {
        ConverterWindowMode.HalfScreen -> 8.dp
        ConverterWindowMode.Compact -> 10.dp
        ConverterWindowMode.Regular -> 14.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    role = Role.Button,
                    onClickLabel = label,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = colors.accent,
                fontSize = operatorFontSize,
                fontWeight = FontWeight.Medium,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = dividerPadding),
                color = colors.operatorDivider,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    slot: CurrencySlot,
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (CurrencyInfo) -> Unit,
) {
    val colors = LocalCurrencyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember(slot) { mutableStateOf("") }
    val currencies = remember(query) { CurrencyCatalog.search(query) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.consoleSurface,
        contentColor = colors.primaryText,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 11.dp)
                    .size(width = 38.dp, height = 4.dp),
                shape = CircleShape,
                color = colors.secondaryText.copy(alpha = 0.35f),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = if (slot == CurrencySlot.From) "选择原始币种" else "选择目标币种",
                color = colors.primaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "选择后立即换算 · 共 ${CurrencyCatalog.all.size} 种货币",
                color = colors.secondaryText,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                placeholder = {
                    Text("搜索币种名称或代码")
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.operatorSurface,
                    unfocusedContainerColor = colors.operatorSurface,
                    focusedTextColor = colors.primaryText,
                    unfocusedTextColor = colors.primaryText,
                    focusedBorderColor = colors.primaryText.copy(alpha = 0.32f),
                    unfocusedBorderColor = colors.surfaceBorder,
                    focusedLeadingIconColor = colors.secondaryText,
                    unfocusedLeadingIconColor = colors.secondaryText,
                    focusedPlaceholderColor = colors.tertiaryText,
                    unfocusedPlaceholderColor = colors.tertiaryText,
                    cursorColor = colors.primaryText,
                ),
            )
            Spacer(Modifier.height(10.dp))
            if (currencies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "没有找到相关币种",
                        color = colors.secondaryText,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(currencies, key = CurrencyInfo::code) { currency ->
                        CurrencyListItem(
                            currency = currency,
                            selected = currency.code == selectedCode,
                            onClick = { onSelect(currency) },
                        )
                    }
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp),
                            text = "汇率由 ExchangeRate-API 提供",
                            color = colors.tertiaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyListItem(
    currency: CurrencyInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalCurrencyColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.operatorSurface else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SvgFlag(
                flagCode = currency.flagCode,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currency.name,
                    color = colors.primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currency.code,
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = currency.symbol,
                color = colors.secondaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已选择",
                    tint = colors.primaryText,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

private fun buildRateText(state: ConverterUiState): String {
    val status = when (state.rateStatus) {
        RateStatus.Live -> {
            if (state.ratesUpdatedAt > 0) {
                val date = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    .format(Date(state.ratesUpdatedAt))
                "$date 更新"
            } else {
                "今日汇率"
            }
        }
        RateStatus.Cached -> "缓存汇率"
        RateStatus.Offline -> "离线参考"
        RateStatus.Loading -> "更新中"
    }
    return "1 ${state.fromCode} = ${ExpressionEvaluator.formatRate(state.conversionRate)} " +
        "${state.toCode} · $status"
}
