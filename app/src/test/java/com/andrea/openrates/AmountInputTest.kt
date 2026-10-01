package com.andrea.openrates

import com.andrea.openrates.data.RatesSnapshot
import com.andrea.openrates.ui.ConverterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputTest {

    @Test
    fun `comma and dot separators are both accepted`() {
        assertEquals(1.5, ConverterUiState.parseAmount("1,5")!!, 1e-9)
        assertEquals(1.5, ConverterUiState.parseAmount("1.5")!!, 1e-9)
    }

    @Test
    fun `empty or partial input has no amount`() {
        assertNull(ConverterUiState.parseAmount(""))
        assertNull(ConverterUiState.parseAmount("."))
    }

    @Test
    fun `state converts with the snapshot rate`() {
        val snapshot = RatesSnapshot(base = "EUR", rates = mapOf("USD" to 1.2), fetchedAtEpochMs = 0)
        val state = ConverterUiState(amountInput = "10", from = "EUR", to = "USD", snapshot = snapshot)
        assertEquals(12.0, state.converted!!, 1e-9)
    }
}
