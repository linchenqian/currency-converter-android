package com.jojo.currencyconverter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = LightCurrencyColors.primaryText,
    onPrimary = Color.White,
    secondary = LightCurrencyColors.accent,
    background = LightCurrencyColors.appBackground,
    surface = LightCurrencyColors.consoleSurface,
    onBackground = LightCurrencyColors.primaryText,
    onSurface = LightCurrencyColors.primaryText,
)

private val DarkScheme = darkColorScheme(
    primary = DarkCurrencyColors.primaryText,
    onPrimary = DarkCurrencyColors.appBackground,
    secondary = DarkCurrencyColors.accent,
    background = DarkCurrencyColors.appBackground,
    surface = DarkCurrencyColors.consoleSurface,
    onBackground = DarkCurrencyColors.primaryText,
    onSurface = DarkCurrencyColors.primaryText,
)

@Composable
fun CurrencyConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkCurrencyColors else LightCurrencyColors
    CompositionLocalProvider(LocalCurrencyColors provides appColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}
