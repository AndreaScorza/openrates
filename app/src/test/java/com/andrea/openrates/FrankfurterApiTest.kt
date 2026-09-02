package com.andrea.openrates

import com.andrea.openrates.data.FrankfurterApi
import com.andrea.openrates.data.FrankfurterException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Parsing and request shaping checked against recorded Frankfurter v2 payloads. */
class FrankfurterApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: FrankfurterApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = FrankfurterApi(baseUrl = server.url("/"))
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `single pair endpoint returns an object`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"date":"2026-09-02","base":"EUR","quote":"USD","rate":1.1603}""")
        )
        val rate = api.rate("EUR", "USD")

        assertEquals("/v2/rate/EUR/USD", server.takeRequest().path)
        assertEquals("USD", rate.quote)
        assertEquals(1.1603, rate.rate, 1e-9)
    }

    @Test
    fun `multi quote endpoint returns a flat array`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[{"date":"2026-09-02","base":"EUR","quote":"GBP","rate":0.85686},
                    {"date":"2026-09-01","base":"EUR","quote":"JPY","rate":185.65}]"""
            )
        )
        val rates = api.rates("EUR", listOf("GBP", "JPY"))

        assertEquals("/v2/rates?base=EUR&quotes=GBP%2CJPY", server.takeRequest().path)
        assertEquals(2, rates.size)
        assertEquals(185.65, rates.first { it.quote == "JPY" }.rate, 1e-9)
    }

    @Test
    fun `snapshot folds every quote into one offline blob`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[{"date":"2026-09-02","base":"EUR","quote":"USD","rate":1.1603},
                    {"date":"2026-09-01","base":"EUR","quote":"ALL","rate":92.13}]"""
            )
        )
        val snapshot = api.snapshot("EUR")

        assertEquals("/v2/rates?base=EUR", server.takeRequest().path)
        assertEquals(setOf("USD", "ALL"), snapshot.rates.keys)
        // Per-quote dates are preserved: illiquid currencies lag by a day.
        assertEquals("2026-09-01", snapshot.dates["ALL"])
        assertEquals("2026-09-02", snapshot.latestDate)
        assertTrue(snapshot.fetchedAtEpochMs > 0)
    }

    @Test
    fun `unknown currency surfaces the api message`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422)
                .setBody("""{"status":422,"message":"invalid currency: XYZ"}""")
        )
        try {
            api.rate("EUR", "XYZ")
            fail("expected FrankfurterException")
        } catch (e: FrankfurterException) {
            assertEquals(422, e.status)
            assertEquals("invalid currency: XYZ", e.message)
        }
    }

    @Test
    fun `currency names are mapped by iso code`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[{"iso_code":"USD","iso_numeric":"840","name":"United States Dollar","symbol":"${'$'}"}]"""
            )
        )
        val names = api.currencies()

        assertEquals("/v2/currencies", server.takeRequest().path)
        assertEquals("United States Dollar", names.names["USD"])
        assertEquals("${'$'}", names.symbols["USD"])
    }
}
