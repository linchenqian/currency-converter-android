package com.jojo.currencyconverter.data

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class RateStatus {
    Loading,
    Live,
    Cached,
    Offline,
}

data class RateSnapshot(
    val rates: Map<String, Double>,
    val updatedAt: Long,
    val status: RateStatus,
)

class RatesRepository(context: Context) {
    private val preferences = context.getSharedPreferences(CacheName, Context.MODE_PRIVATE)

    fun initialSnapshot(): RateSnapshot {
        val cached = readCache()
        return cached ?: RateSnapshot(
            rates = FallbackRates,
            updatedAt = 0L,
            status = RateStatus.Offline,
        )
    }

    suspend fun refresh(): RateSnapshot = withContext(Dispatchers.IO) {
        val connection = (URL(Endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CurrencyConverter-Android/1.0")
        }

        try {
            check(connection.responseCode in 200..299) {
                "Rate request failed: ${connection.responseCode}"
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(payload)
            check(root.optString("result") == "success") {
                "Rate response was incomplete"
            }
            val ratesJson = root.getJSONObject("rates")
            val rates = buildMap {
                val keys = ratesJson.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val value = ratesJson.optDouble(code, Double.NaN)
                    if (value.isFinite() && value > 0) put(code, value)
                }
            }
            check(rates.size >= 100) {
                "Rate response did not include the full currency list"
            }
            val updatedAt = root.optLong("time_last_update_unix", System.currentTimeMillis() / 1000) *
                1000
            writeCache(rates, updatedAt)
            RateSnapshot(rates, updatedAt, RateStatus.Live)
        } finally {
            connection.disconnect()
        }
    }

    private fun readCache(): RateSnapshot? = runCatching {
        val raw = preferences.getString(CacheKey, null) ?: return null
        val root = JSONObject(raw)
        val ratesJson = root.getJSONObject("rates")
        val rates = buildMap {
            val keys = ratesJson.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                val value = ratesJson.optDouble(code, Double.NaN)
                if (value.isFinite() && value > 0) put(code, value)
            }
        }
        if (rates.size < 100) return null
        RateSnapshot(
            rates = rates,
            updatedAt = root.optLong("updatedAt", 0L),
            status = RateStatus.Cached,
        )
    }.getOrNull()

    private fun writeCache(rates: Map<String, Double>, updatedAt: Long) {
        val ratesJson = JSONObject()
        rates.forEach { (code, value) -> ratesJson.put(code, value) }
        val root = JSONObject()
            .put("rates", ratesJson)
            .put("updatedAt", updatedAt)
        preferences.edit().putString(CacheKey, root.toString()).apply()
    }

    companion object {
        private const val Endpoint = "https://open.er-api.com/v6/latest/USD"
        private const val CacheName = "currency_rates"
        private const val CacheKey = "usd_rates_v1"

        val FallbackRates = mapOf(
            "USD" to 1.0,
            "CNY" to 6.765736,
            "JPY" to 160.57454,
            "EUR" to 0.868755,
            "GBP" to 0.74463,
            "HKD" to 7.843276,
            "KRW" to 1429.51398,
        )
    }
}
