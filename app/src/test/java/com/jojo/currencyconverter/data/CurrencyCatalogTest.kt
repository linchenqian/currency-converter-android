package com.jojo.currencyconverter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyCatalogTest {
    @Test
    fun catalogContainsAFullUniqueCurrencySet() {
        assertTrue(CurrencyCatalog.all.size >= 160)
        assertEquals(
            CurrencyCatalog.all.size,
            CurrencyCatalog.all.map { it.code }.distinct().size,
        )
    }

    @Test
    fun popularCurrenciesAppearFirst() {
        assertEquals(
            listOf("CNY", "USD", "JPY", "EUR", "GBP"),
            CurrencyCatalog.all.take(5).map { it.code },
        )
    }

    @Test
    fun searchMatchesCodeAndChineseName() {
        assertEquals("JPY", CurrencyCatalog.search("JPY").first().code)
        assertEquals("CNY", CurrencyCatalog.search("人民币").first().code)
    }
}
