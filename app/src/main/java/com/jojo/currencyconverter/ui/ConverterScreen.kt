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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private const val CompactWindowHeightThresholdDp = 780f

internal fun shouldUseCompactWindowLayout(heightDp: Float): Boolean =
    heightDp < CompactWindowHeightThresholdDp

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
        val compactWindow = shouldUseCompactWindowLayout(maxHeight.value)

        if (compactWindow) {
            val scrollState = rememberScrollState()
            val conversionHeight = (maxHeight * 0.62f).coerceIn(270.dp, 340.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState),
            ) {
                ConversionPanel(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(conversionHeight),
                    compact = true,
                    onOpenCurrencyPicker = onOpenCurrencyPicker,
                    onSwapCurrencies = onSwapCurrencies,
                )
                CalculatorPad(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    onDigit = onDigit,
                    onDecimal = onDecimal,
                    onOperator = onOperator,
                    onBackspace = onBackspace,
                    onClear = onClear,
                    onPercent = onPercent,
                    onEquals = onEquals,
                )
            }
        } else {
            val conversionHeight = (maxHeight * 0.47f).coerceIn(390.dp, 438.dp)
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
                    onOpenCurrencyPicker = onOpenCurrencyPicker,
                    onSwapCurrencies = onSwapCurrencies,
                )
                CalculatorPad(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
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
}

@Composable
private fun ConversionPanel(
    state: ConverterUiState,
    modifier: Modifier,
    onOpenCurrencyPicker: (CurrencySlot) -> Unit,
    onSwapCurrencies: () -> Unit,
    compact: Boolean = false,
) {
    val colors = LocalCurrencyColors.current
    Surface(
        modifier = modifier,
        color = colors.displaySurface,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = if (compact) 18.dp else 28.dp,
                    end = if (compact) 18.dp else 28.dp,
                    top = if (compact) 4.dp else 12.dp,
                    bottom = if (compact) 8.dp else 16.dp,
                ),
        ) {
            ConversionBand(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                currency = state.fromCurrency,
                expression = ExpressionEvaluator.formatExpression(state.expression),
                amount = state.sourceValue,
                amountMaxSize = if (compact) 40 else 54,
                amountMinSize = if (compact) 15 else 18,
                compact = compact,
                onCurrencyClick = { onOpenCurrencyPicker(CurrencySlot.From) },
            )
            SwapDivider(onSwapCurrencies, compact)
            ConversionBand(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                currency = state.toCurrency,
                expression = "约合",
                amount = state.convertedValue,
                amountMaxSize = if (compact) 38 else 48,
                amountMinSize = if (compact) 14 else 17,
                compact = compact,
                converted = true,
                onCurrencyClick = { onOpenCurrencyPicker(CurrencySlot.To) },
            )
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
    compact: Boolean = false,
) {
    val colors = LocalCurrencyColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencySelector(
            currency = currency,
            compact = compact,
            onClick = onCurrencyClick,
        )
        Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = if (converted) 6.dp else 2.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
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
    compact: Boolean = false,
) {
    val colors = LocalCurrencyColors.current
    Column(
        modifier = Modifier
            .width(if (compact) 66.dp else 84.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "选择${currency.name}",
                onClick = onClick,
            )
            .padding(vertical = if (compact) 1.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SvgFlag(
            flagCode = currency.flagCode,
            modifier = Modifier.size(if (compact) 32.dp else 42.dp),
        )
        Spacer(Modifier.height(if (compact) 3.dp else 7.dp))
        Text(
            text = currency.name,
            color = colors.primaryText,
            fontSize = if (compact) 15.sp else 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(Modifier.height(if (compact) 0.dp else 2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currency.code,
                color = colors.secondaryText,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(if (compact) 16.dp else 19.dp),
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
    compact: Boolean = false,
) {
    val colors = LocalCurrencyColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 34.dp else 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(color = colors.divider)
        Surface(
            onClick = onClick,
            modifier = Modifier.size(if (compact) 38.dp else 48.dp),
            shape = CircleShape,
            color = colors.swapSurface,
            contentColor = colors.primaryText,
            border = BorderStroke(1.dp, colors.swapBorder),
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Height,
                    contentDescription = "交换原始币种和目标币种",
                    modifier = Modifier.size(if (compact) 23.dp else 29.dp),
                )
            }
        }
    }
}

@Composable
private fun CalculatorPad(
    modifier: Modifier,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPercent: () -> Unit,
    onEquals: () -> Unit,
) {
    val colors = LocalCurrencyColors.current
    Surface(
        modifier = modifier.heightIn(min = 328.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.consoleSurface,
        border = BorderStroke(1.dp, colors.surfaceBorder),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CalculatorRow {
                    CalculatorKey("C", "清除", onClear, Modifier.weight(1f), utility = true)
                    CalculatorIconKey(
                        icon = Icons.AutoMirrored.Rounded.Backspace,
                        label = "退格",
                        onClick = onBackspace,
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorKey("%", "百分比", onPercent, Modifier.weight(1f), utility = true)
                }
                CalculatorRow {
                    CalculatorKey("7", "7", { onDigit("7") }, Modifier.weight(1f))
                    CalculatorKey("8", "8", { onDigit("8") }, Modifier.weight(1f))
                    CalculatorKey("9", "9", { onDigit("9") }, Modifier.weight(1f))
                }
                CalculatorRow {
                    CalculatorKey("4", "4", { onDigit("4") }, Modifier.weight(1f))
                    CalculatorKey("5", "5", { onDigit("5") }, Modifier.weight(1f))
                    CalculatorKey("6", "6", { onDigit("6") }, Modifier.weight(1f))
                }
                CalculatorRow {
                    CalculatorKey("1", "1", { onDigit("1") }, Modifier.weight(1f))
                    CalculatorKey("2", "2", { onDigit("2") }, Modifier.weight(1f))
                    CalculatorKey("3", "3", { onDigit("3") }, Modifier.weight(1f))
                }
                CalculatorRow {
                    CalculatorKey("00", "00", { onDigit("00") }, Modifier.weight(1f))
                    CalculatorKey("0", "0", { onDigit("0") }, Modifier.weight(1f))
                    CalculatorKey(".", "小数点", onDecimal, Modifier.weight(1f))
                }
            }

            OperatorRail(
                modifier = Modifier
                    .widthIn(min = 70.dp, max = 78.dp)
                    .fillMaxHeight(),
                onOperator = onOperator,
                onEquals = onEquals,
            )
        }
    }
}

@Composable
private fun ColumnScope.CalculatorRow(
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun CalculatorKey(
    text: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    utility: Boolean = false,
) {
    val colors = LocalCurrencyColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(22.dp),
        color = if (utility) colors.keySurfaceStrong else colors.keySurface,
        contentColor = colors.primaryText,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = if (utility) 25.sp else 27.sp,
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
    modifier: Modifier = Modifier,
) {
    val colors = LocalCurrencyColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(22.dp),
        color = colors.keySurfaceStrong,
        contentColor = colors.primaryText,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(27.dp),
            )
        }
    }
}

@Composable
private fun OperatorRail(
    modifier: Modifier,
    onOperator: (Char) -> Unit,
    onEquals: () -> Unit,
) {
    val colors = LocalCurrencyColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = colors.operatorSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OperatorKey("÷", "除", { onOperator('÷') }, showDivider = true)
            OperatorKey("×", "乘", { onOperator('×') }, showDivider = true)
            OperatorKey("−", "减", { onOperator('-') }, showDivider = true)
            OperatorKey("+", "加", { onOperator('+') }, showDivider = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    onClick = onEquals,
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = colors.accent,
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "=",
                            fontSize = 31.sp,
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
    showDivider: Boolean,
) {
    val colors = LocalCurrencyColors.current
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
                fontSize = 29.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp),
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
