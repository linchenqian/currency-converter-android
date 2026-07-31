package com.jojo.currencyconverter.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CurrencyAppColors(
    val appBackground: Color,
    val displaySurface: Color,
    val consoleSurface: Color,
    val keySurface: Color,
    val keySurfaceStrong: Color,
    val operatorSurface: Color,
    val operatorDivider: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val divider: Color,
    val swapSurface: Color,
    val swapBorder: Color,
    val accent: Color,
    val accentPressed: Color,
    val surfaceBorder: Color,
    val panelShadow: Color,
)

val LightCurrencyColors = CurrencyAppColors(
    appBackground = Color(0xFFFBFAF7),
    displaySurface = Color(0xFFF0F6FC),
    consoleSurface = Color(0xFFFDFCF9),
    keySurface = Color(0x0A13355E),
    keySurfaceStrong = Color(0x0F13355E),
    operatorSurface = Color(0xFFEAF3FC),
    operatorDivider = Color(0x2E395B7E),
    primaryText = Color(0xFF08368D),
    secondaryText = Color(0xFF486684),
    tertiaryText = Color(0xFF6F8396),
    divider = Color(0xFFCFDEF0),
    swapSurface = Color(0xFFEDF5FD),
    swapBorder = Color(0xFFD2E0F1),
    accent = Color(0xFFFF7318),
    accentPressed = Color(0xFFE86008),
    surfaceBorder = Color(0x1F234568),
    panelShadow = Color(0x1A1B3A58),
)

val DarkCurrencyColors = CurrencyAppColors(
    appBackground = Color(0xFF090F18),
    displaySurface = Color(0xFF111C2A),
    consoleSurface = Color(0xFF0D1520),
    keySurface = Color(0xFF151F2B),
    keySurfaceStrong = Color(0xFF1A2735),
    operatorSurface = Color(0xFF142C3E),
    operatorDivider = Color(0x38AFC1D2),
    primaryText = Color(0xFFEDF4FA),
    secondaryText = Color(0xFFAFC1D2),
    tertiaryText = Color(0xFF8397AA),
    divider = Color(0xFF26384B),
    swapSurface = Color(0xFF132337),
    swapBorder = Color(0xFF2A4057),
    accent = Color(0xFFFF7A1C),
    accentPressed = Color(0xFFE9650D),
    surfaceBorder = Color(0xFF26384B),
    panelShadow = Color(0x4D000000),
)

val LocalCurrencyColors = staticCompositionLocalOf { LightCurrencyColors }
