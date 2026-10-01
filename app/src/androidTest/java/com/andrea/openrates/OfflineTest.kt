package com.andrea.openrates

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.andrea.openrates.data.Conversion
import com.andrea.openrates.data.FrankfurterApi
import com.andrea.openrates.data.RatesCache
import com.andrea.openrates.data.RatesRepository
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The offline promise, exercised on the device: once rates have been downloaded
 * they survive a restart with no reachable network, and every pair still converts.
 */
@RunWith(AndroidJUnit4::class)
class OfflineTest {

    private lateinit var directory: File

    /** A port nothing listens on, so requests fail the way a dead network does. */
    private val unreachable = "http://127.0.0.1:1/".toHttpUrl()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        directory = File(context.cacheDir, "offline-test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test
    fun cachedRatesSurviveWithNoNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val cache = RatesCache(context, directory)

        // 1. One successful download against the real API.
        val online = RatesRepository(FrankfurterApi(), cache)
        val downloaded = online.refresh("EUR")
        assertTrue("live refresh failed: ${downloaded.exceptionOrNull()}", downloaded.isSuccess)

        // 2. A fresh repository that cannot reach the network at all.
        val offline = RatesRepository(FrankfurterApi(baseUrl = unreachable), RatesCache(context, directory))
        assertTrue("refresh should fail while offline", offline.refresh("EUR").isFailure)

        // 3. The cached snapshot still answers, for pairs that do not involve the base.
        val snapshot = offline.cachedSnapshot()
        assertNotNull("cached snapshot must outlive the network", snapshot)
        val usd = Conversion.convert(snapshot!!, 100.0, "EUR", "USD")
        assertNotNull("EUR->USD must convert offline", usd)
        assertTrue("implausible EUR->USD result: $usd", usd!! > 10.0 && usd < 1000.0)

        val crossRate = Conversion.rate(snapshot, "USD", "GBP")
        assertNotNull("cross rates must be derivable offline", crossRate)
        assertTrue("snapshot should cover the whole currency list", snapshot.currencies.size > 100)
    }

    @Test
    fun failedRefreshNeverDestroysTheLastGoodSnapshot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val cache = RatesCache(context, directory)
        assertTrue(RatesRepository(FrankfurterApi(), cache).refresh("EUR").isSuccess)
        val before = cache.readSnapshot()!!

        repeat(3) {
            RatesRepository(FrankfurterApi(baseUrl = unreachable), cache).refresh("EUR")
        }

        val after = cache.readSnapshot()
        assertNotNull(after)
        assertEquals(before.rates, after!!.rates)
        assertEquals(before.fetchedAtEpochMs, after.fetchedAtEpochMs)
    }
}
