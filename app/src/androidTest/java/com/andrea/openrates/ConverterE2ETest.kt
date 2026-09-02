package com.andrea.openrates

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.andrea.openrates.ui.PICKER_SEARCH_TAG
import com.andrea.openrates.ui.Tags
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end on a real device against the live Frankfurter API: launch, download
 * rates, convert, swap, pick a currency, refresh.
 */
@RunWith(AndroidJUnit4::class)
class ConverterE2ETest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitRates() {
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithTagSafe(Tags.RATE).isNotEmpty()
        }
    }

    private fun textOf(tag: String): String = rule.onNodeWithTag(tag).textValue()

    @Test
    fun downloadsRatesAndConverts() {
        awaitRates()

        rule.onNodeWithTag(Tags.AMOUNT).assertIsDisplayed()
        rule.onNodeWithTag(Tags.RESULT).assertIsDisplayed()

        val result = textOf(Tags.RESULT)
        assertNotEquals("placeholder result", "—", result)
        assertTrue("expected a numeric result, got '$result'", result.any { it.isDigit() })

        val rate = textOf(Tags.RATE)
        assertTrue("rate line should name both currencies: '$rate'", rate.startsWith("1 "))
    }

    @Test
    fun changingTheAmountChangesTheResult() {
        awaitRates()
        val before = textOf(Tags.RESULT)

        rule.onNodeWithTag(Tags.AMOUNT).performTextClearance()
        rule.onNodeWithTag(Tags.AMOUNT).performTextInput("250")
        rule.waitForIdle()

        val after = textOf(Tags.RESULT)
        assertNotEquals("result should track the amount", before, after)
        assertTrue("expected a numeric result, got '$after'", after.any { it.isDigit() })
    }

    @Test
    fun swapExchangesTheTwoCurrencies() {
        awaitRates()
        val from = textOf(Tags.FROM)
        val to = textOf(Tags.TO)

        rule.onNodeWithTag(Tags.SWAP).performClick()
        rule.waitForIdle()

        assertTrue("from should now be the old to", textOf(Tags.FROM) == to)
        assertTrue("to should now be the old from", textOf(Tags.TO) == from)
    }

    @Test
    fun picksACurrencyFromTheSearchableList() {
        awaitRates()

        rule.onNodeWithTag(Tags.TO).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTagSafe(PICKER_SEARCH_TAG).isNotEmpty()
        }
        rule.onNodeWithTag(PICKER_SEARCH_TAG).performTextInput("CHF")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTagSafe(Tags.currency("CHF")).isNotEmpty()
        }
        rule.onNodeWithTag(Tags.currency("CHF")).performClick()
        rule.waitForIdle()

        assertTrue("picker selection should apply", textOf(Tags.TO) == "CHF")
    }

    @Test
    fun refreshUpdatesTheTimestampLine() {
        awaitRates()

        rule.onNodeWithTag(Tags.REFRESH).performClick()
        rule.waitForIdle()
        rule.waitUntil(timeoutMillis = 30_000) { textOf(Tags.STATUS).startsWith("Updated") }

        val status = textOf(Tags.STATUS)
        assertTrue("status should report freshness: '$status'", status.contains("Updated"))
        assertTrue("status should name the rate date: '$status'", status.contains("rates for"))
    }

    @Test
    fun watchlistAddsAndRemovesACurrency() {
        awaitRates()

        rule.onNodeWithTag(Tags.ADD_WATCH).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTagSafe(PICKER_SEARCH_TAG).isNotEmpty()
        }
        rule.onNodeWithTag(PICKER_SEARCH_TAG).performTextInput("SEK")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTagSafe(Tags.currency("SEK")).isNotEmpty()
        }
        rule.onNodeWithTag(Tags.currency("SEK")).performClick()
        Espresso.pressBack()
        rule.waitForIdle()

        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodesWithTagSafe(Tags.watchRow("SEK")).isNotEmpty()
        }
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onNodeWithTag(Tags.watchRow("SEK")).textValue().any { it.isDigit() }
        }

        rule.onNodeWithContentDescription("Remove SEK").performClick()
        rule.waitForIdle()
        assertTrue(
            "removed currency should leave the list",
            rule.onAllNodesWithTagSafe(Tags.watchRow("SEK")).isEmpty(),
        )
    }
}
