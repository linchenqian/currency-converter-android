package com.jojo.currencyconverter.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowLayoutPolicyTest {
    @Test
    fun shortSplitScreenUsesCompactScrollableLayout() {
        assertTrue(shouldUseCompactWindowLayout(420f))
        assertTrue(shouldUseCompactWindowLayout(779.9f))
    }

    @Test
    fun regularPhoneHeightKeepsOriginalLayout() {
        assertFalse(shouldUseCompactWindowLayout(780f))
        assertFalse(shouldUseCompactWindowLayout(840f))
    }
}
