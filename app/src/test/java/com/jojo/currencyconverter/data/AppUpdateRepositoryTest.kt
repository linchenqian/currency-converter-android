package com.jojo.currencyconverter.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRepositoryTest {
    @Test
    fun newerSemanticVersionIsDetected() {
        assertTrue(isNewerVersion("1.0.6", "1.0.5"))
        assertTrue(isNewerVersion("v1.1.0", "1.0.99"))
        assertTrue(isNewerVersion("2.0", "1.9.9"))
    }

    @Test
    fun sameOrOlderVersionIsIgnored() {
        assertFalse(isNewerVersion("v1.0.5", "1.0.5"))
        assertFalse(isNewerVersion("1.0.4", "1.0.5"))
        assertFalse(isNewerVersion("1.0.5-beta", "1.0.5"))
    }

    @Test
    fun stableReleaseSupersedesMatchingPreRelease() {
        assertTrue(isNewerVersion("1.0.5", "1.0.5-beta"))
    }

    @Test
    fun githubRedirectBuildsPredictableFallbackAssetUrl() {
        val release = releaseFromLatestRedirect(
            "https://github.com/linchenqian/currency-converter-android/releases/tag/v1.0.6",
        )

        assertTrue(release != null)
        assertTrue(release!!.apkUrl.endsWith("/v1.0.6/Currency-1.0.6-debug.apk"))
    }

    @Test
    fun unrelatedRedirectIsRejected() {
        assertTrue(releaseFromLatestRedirect("https://example.com/releases/tag/v9.9.9") == null)
    }
}
