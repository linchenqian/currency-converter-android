package com.jojo.currencyconverter.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowLayoutPolicyTest {
    @Test
    fun halfScreenHeightUsesDenseLayout() {
        assertEquals(ConverterWindowMode.HalfScreen, converterWindowMode(360f))
        assertEquals(ConverterWindowMode.HalfScreen, converterWindowMode(519.9f))
    }

    @Test
    fun mediumHeightUsesCompactLayout() {
        assertEquals(ConverterWindowMode.Compact, converterWindowMode(520f))
        assertEquals(ConverterWindowMode.Compact, converterWindowMode(779.9f))
    }

    @Test
    fun regularPhoneHeightKeepsFullLayout() {
        assertEquals(ConverterWindowMode.Regular, converterWindowMode(780f))
        assertEquals(ConverterWindowMode.Regular, converterWindowMode(840f))
    }
}
