package com.jojo.currencyconverter.data

import java.util.Currency
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flagCode: String,
)

object CurrencyCatalog {
    private val supportedCodes = listOf(
        "USD", "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN",
        "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD",
        "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLF", "CLP", "CNH", "CNY",
        "COP", "CRC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN",
        "ETB", "EUR", "FJD", "FKP", "FOK", "GBP", "GEL", "GGP", "GHS", "GIP", "GMD",
        "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "IMP",
        "INR", "IQD", "IRR", "ISK", "JEP", "JMD", "JOD", "JPY", "KES", "KGS", "KHR",
        "KID", "KMF", "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL",
        "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR",
        "MWK", "MXN", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR",
        "PAB", "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB",
        "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SLL", "SOS",
        "SRD", "SSP", "STN", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY",
        "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "UYU", "UZS", "VES", "VND", "VUV",
        "WST", "XAF", "XCD", "XCG", "XDR", "XOF", "XPF", "YER", "ZAR", "ZMW", "ZWG",
        "ZWL",
    )

    private val popularCodes = listOf(
        "CNY", "USD", "JPY", "EUR", "GBP", "HKD", "KRW", "AUD", "CAD", "SGD", "TWD",
    )

    private val preferredNames = mapOf(
        "CNY" to "人民币",
        "USD" to "美元",
        "JPY" to "日元",
        "EUR" to "欧元",
        "GBP" to "英镑",
        "HKD" to "港币",
        "KRW" to "韩元",
        "TWD" to "新台币",
        "MOP" to "澳门元",
        "CNH" to "离岸人民币",
    )

    private val preferredSymbols = mapOf(
        "CNY" to "¥",
        "CNH" to "¥",
        "USD" to "$",
        "JPY" to "¥",
        "EUR" to "€",
        "GBP" to "£",
        "HKD" to "HK$",
        "KRW" to "₩",
        "TWD" to "NT$",
        "MOP" to "MOP$",
    )

    private val flagOverrides = mapOf(
        "CNH" to "cn",
        "EUR" to "eu",
        "XAF" to "cf",
        "XCD" to "ag",
        "XCG" to "cw",
        "XDR" to "un",
        "XOF" to "sn",
        "XPF" to "pf",
    )

    val all: List<CurrencyInfo> = (popularCodes + supportedCodes)
        .distinct()
        .map(::currencyInfo)

    private val byCode = all.associateBy(CurrencyInfo::code)

    fun find(code: String): CurrencyInfo = byCode[code] ?: all.first()

    fun search(query: String): List<CurrencyInfo> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return all
        return all.filter {
            it.code.contains(normalized, ignoreCase = true) ||
                it.name.contains(normalized, ignoreCase = true)
        }
    }

    private fun currencyInfo(code: String): CurrencyInfo {
        val javaCurrency = runCatching { Currency.getInstance(code) }.getOrNull()
        val name = preferredNames[code]
            ?: javaCurrency?.getDisplayName(Locale.SIMPLIFIED_CHINESE)
            ?: code
        val symbol = preferredSymbols[code]
            ?: javaCurrency?.getSymbol(Locale.SIMPLIFIED_CHINESE)
                ?.takeUnless { it.equals(code, ignoreCase = true) }
            ?: code
        return CurrencyInfo(
            code = code,
            name = name,
            symbol = symbol,
            flagCode = flagOverrides[code] ?: code.take(2).lowercase(Locale.ROOT),
        )
    }
}
