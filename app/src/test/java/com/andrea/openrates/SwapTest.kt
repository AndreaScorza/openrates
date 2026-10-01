package com.andrea.openrates

import com.andrea.openrates.data.RatesSnapshot
import com.andrea.openrates.ui.ConverterUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SwapTest {

    // Frankfurter's EUR rates for 2026-10-01, as the app receives them.
    private val eurRates = mapOf("HKD" to 8.9136, "GBP" to 0.85569, "JPY" to 178.63)

    private val state = ConverterUiState(
        amountInput = "35",
        from = "EUR",
        to = "HKD",
        watchlist = listOf("GBP", "JPY", "HKD"),
        snapshot = RatesSnapshot(base = "EUR", rates = eurRates, fetchedAtEpochMs = 0),
    )

    @Test
    fun `swap flips the direction and keeps the typed number`() {
        val swapped = state.swapped()

        assertEquals("HKD", swapped.from)
        assertEquals("EUR", swapped.to)
        assertEquals("35", swapped.amountInput)
        assertEquals(35 / 8.9136, swapped.converted!!, 1e-12)
    }

    @Test
    fun `swapping back returns to the same conversion`() {
        val back = state.swapped().swapped()

        assertEquals(state, back)
        assertEquals(35 * 8.9136, back.converted!!, 1e-12)
    }

    @Test
    fun `pairs are crossed from EUR rates`() {
        val fromHkd = state.swapped()
        assertEquals(0.85569 / 8.9136, fromHkd.rateFor("GBP")!!, 1e-12)
    }

    @Test
    fun `the amount's own currency stays in the watchlist at face value`() {
        val fromHkd = state.swapped()
        assertEquals(1.0, fromHkd.rateFor("HKD")!!, 0.0)
    }
}
