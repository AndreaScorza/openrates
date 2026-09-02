package com.andrea.openrates

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.andrea.openrates.data.RatesSnapshot
import com.andrea.openrates.ui.ConverterScreen
import com.andrea.openrates.ui.ConverterUiState
import com.andrea.openrates.ui.Tags
import com.andrea.openrates.ui.theme.OpenRatesTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** What the user sees when the last refresh failed: rates, plus why they are old. */
@RunWith(AndroidJUnit4::class)
class OfflineUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val staleSnapshot = RatesSnapshot(
        base = "EUR",
        rates = mapOf("USD" to 1.1603, "GBP" to 0.85686),
        dates = mapOf("USD" to "2026-09-02", "GBP" to "2026-09-02"),
        fetchedAtEpochMs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(30),
    )

    @Test
    fun offlineStillConvertsAndSaysSo() {
        rule.setContent {
            OpenRatesTheme(dynamicColor = false) {
                ConverterScreen(
                    state = ConverterUiState(
                        amountInput = "100",
                        from = "EUR",
                        to = "USD",
                        snapshot = staleSnapshot,
                        isOffline = true,
                        watchlist = listOf("GBP"),
                    ),
                    onAmountChange = {},
                    onFromChange = {},
                    onToChange = {},
                    onSwap = {},
                    onRefresh = {},
                    onToggleWatch = {},
                )
            }
        }

        rule.onNodeWithTag(Tags.RESULT).assertIsDisplayed()
        val result = rule.onNodeWithTag(Tags.RESULT).textValue()
        assertTrue("cached rates must still convert: '$result'", result.any { it.isDigit() })

        val status = rule.onNodeWithTag(Tags.STATUS).textValue()
        assertTrue("status must explain the staleness: '$status'", status.contains("offline"))
        assertTrue("status must date the rates: '$status'", status.contains("yesterday") || status.contains("ago"))

        // The watchlist keeps working from the same cached snapshot.
        rule.onNodeWithTag(Tags.watchRow("GBP")).assertIsDisplayed()
    }
}
