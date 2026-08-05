package com.jojo.currencyconverter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    val tagName: String,
    val versionName: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkSha256: String?,
)

class AppUpdateRepository {
    suspend fun latestRelease(): AppRelease? = withContext(Dispatchers.IO) {
        latestFromApi() ?: latestFromRedirect()
    }

    private fun latestFromApi(): AppRelease? {
        val connection = (URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Currency-Android")
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parseRelease(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun latestFromRedirect(): AppRelease? {
        val connection = (URL(LATEST_RELEASE_WEB_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            instanceFollowRedirects = false
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "Currency-Android")
        }
        return try {
            if (connection.responseCode !in 300..399) return null
            releaseFromLatestRedirect(connection.getHeaderField("Location").orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(response: String): AppRelease? {
        val release = JSONObject(response)
        val tagName = release.optString("tag_name").trim()
        val versionName = tagName.removePrefix("v").removePrefix("V")
        if (versionName.isBlank()) return null

        val assets = release.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length())
            .asSequence()
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { asset ->
                asset.optString("name").endsWith(".apk", ignoreCase = true) &&
                    asset.optString("state") == "uploaded"
            }
            ?: return null

        val apkUrl = apk.optString("browser_download_url")
        if (!apkUrl.startsWith("https://")) return null
        val digest = apk.optString("digest")
            .removePrefix("sha256:")
            .takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
            ?.lowercase()

        return AppRelease(
            tagName = tagName,
            versionName = versionName,
            title = release.optString("name").ifBlank { tagName },
            notes = release.optString("body"),
            apkUrl = apkUrl,
            apkSha256 = digest,
        )
    }

    private companion object {
        const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/linchenqian/currency-converter-android/releases/latest"
        const val LATEST_RELEASE_WEB_URL =
            "https://github.com/linchenqian/currency-converter-android/releases/latest"
    }
}

internal fun releaseFromLatestRedirect(location: String): AppRelease? {
    val prefix = "https://github.com/linchenqian/currency-converter-android/releases/tag/"
    if (!location.startsWith(prefix)) return null
    val tagName = location.removePrefix(prefix)
    if (!tagName.matches(Regex("[A-Za-z0-9._-]+"))) return null
    val versionName = tagName.removePrefix("v").removePrefix("V")
    if (versionName.isBlank()) return null
    return AppRelease(
        tagName = tagName,
        versionName = versionName,
        title = tagName,
        notes = "",
        apkUrl = "https://github.com/linchenqian/currency-converter-android/" +
            "releases/download/$tagName/Currency-$versionName-debug.apk",
        apkSha256 = null,
    )
}

internal fun isNewerVersion(latest: String, current: String): Boolean {
    val latestVersion = ParsedVersion.parse(latest) ?: return false
    val currentVersion = ParsedVersion.parse(current) ?: return false
    return latestVersion > currentVersion
}

private data class ParsedVersion(
    val numbers: List<Int>,
    val preRelease: String?,
) : Comparable<ParsedVersion> {
    override fun compareTo(other: ParsedVersion): Int {
        val componentCount = maxOf(numbers.size, other.numbers.size)
        for (index in 0 until componentCount) {
            val comparison = (numbers.getOrNull(index) ?: 0)
                .compareTo(other.numbers.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return when {
            preRelease == null && other.preRelease != null -> 1
            preRelease != null && other.preRelease == null -> -1
            else -> (preRelease ?: "").compareTo(other.preRelease ?: "")
        }
    }

    companion object {
        fun parse(raw: String): ParsedVersion? {
            val normalized = raw.trim().removePrefix("v").removePrefix("V")
            val versionParts = normalized.split('-', limit = 2)
            val numbers = versionParts.first()
                .split('.')
                .map { component -> component.toIntOrNull() ?: return null }
            if (numbers.isEmpty()) return null
            return ParsedVersion(
                numbers = numbers,
                preRelease = versionParts.getOrNull(1)?.takeIf { it.isNotBlank() },
            )
        }
    }
}
