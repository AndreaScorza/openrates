package com.andrea.openrates

import com.andrea.openrates.data.Conversion
import com.andrea.openrates.data.RatesSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionTest {

    private val snapshot = RatesSnapshot(
        base = "EUR",
        rates = mapOf("USD" to 1.1603, "GBP" to 0.85686, "JPY" to 185.65),
        dates = mapOf("USD" to "2026-09-02", "GBP" to "2026-09-02", "JPY" to "2026-09-01"),
        fetchedAtEpochMs = 0L,
    )

    @Test
    fun `rate from the snapshot base is used directly`() {
        assertEquals(1.1603, Conversion.rate(snapshot, "EUR", "USD")!!, 1e-9)
    }

    @Test
    fun `rate to the snapshot base is the inverse`() {
        assertEquals(1 / 1.1603, Conversion.rate(snapshot, "USD", "EUR")!!, 1e-9)
    }

    @Test
    fun `cross rate is triangulated through the base`() {
        // 1 USD -> EUR -> GBP
        assertEquals(0.85686 / 1.1603, Conversion.rate(snapshot, "USD", "GBP")!!, 1e-9)
    }

    @Test
    fun `same currency is always one to one`() {
        assertEquals(1.0, Conversion.rate(snapshot, "JPY", "JPY")!!, 1e-12)
    }

    @Test
    fun `unknown currency yields no rate rather than a wrong one`() {
        assertNull(Conversion.rate(snapshot, "EUR", "XYZ"))
        assertNull(Conversion.rate(snapshot, "XYZ", "EUR"))
    }

    @Test
    fun `conversion scales with the amount`() {
        assertEquals(116.03, Conversion.convert(snapshot, 100.0, "EUR", "USD")!!, 1e-9)
    }

    @Test
    fun `small results keep enough decimals to stay meaningful`() {
        // 1 JPY in EUR is 0.00539 - two decimals would round it to 0.01.
        val value = Conversion.convert(snapshot, 1.0, "JPY", "EUR")!!
        assertEquals(6, Conversion.decimalsFor(value))
        assertEquals(4, Conversion.decimalsFor(0.5))
        assertEquals(2, Conversion.decimalsFor(116.03))
    }

    @Test
    fun `snapshot exposes its currencies including the base`() {
        assertEquals(listOf("EUR", "GBP", "JPY", "USD"), snapshot.currencies)
    }

    @Test
    fun `pair date reports the older of the two quotes`() {
        assertEquals("2026-09-01", snapshot.dateFor("EUR", "JPY"))
        assertEquals("2026-09-02", snapshot.dateFor("EUR", "USD"))
    }
}
