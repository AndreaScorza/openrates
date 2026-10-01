package com.andrea.openrates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.andrea.openrates.data.RatesSnapshot
import com.andrea.openrates.ui.ConverterScreen
import com.andrea.openrates.ui.ConverterUiState
import com.andrea.openrates.ui.theme.OpenRatesTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the converter on a spread of screens — small phone to tablet, portrait and
 * landscape, default and enlarged text — so a layout change can be checked on all of
 * them without owning the devices.
 *
 * `./gradlew recordRoborazziDebug` writes the images to `app/screenshots/`;
 * `./gradlew verifyRoborazziDebug` fails if any of them changed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ScreenSizesTest {

    @get:Rule
    val compose = createComposeRule()

    // The Galaxy S25 Ultra the app is designed on: 1440x3120 px at 600 dpi.
    @Test fun phone() = capture("phone", PHONE)
    @Test fun phoneLongNumbers() = capture("phone_long_numbers", PHONE, LONG_NUMBERS)
    @Test fun phoneFont150() = capture("phone_font150", PHONE, fontScale = 1.5f)
    @Test fun phoneFont200() = capture("phone_font200", PHONE, fontScale = 2f)
    @Test fun phoneLandscape() = capture("phone_landscape", "w832dp-h384dp-land-night-600dpi")

    // The narrowest screen Android supports, also what "Display size: largest" gives.
    @Test fun smallPhone() = capture("small_phone", SMALL)
    @Test fun smallPhoneLongNumbers() = capture("small_phone_long_numbers", SMALL, LONG_NUMBERS)
    @Test fun smallPhoneFont200() = capture("small_phone_font200", SMALL, fontScale = 2f)

    @Test fun foldable() = capture("foldable", "w673dp-h841dp-port-night-420dpi")
    @Test fun tablet() = capture("tablet", "w800dp-h1280dp-port-night-xhdpi")
    @Test fun tabletLandscape() = capture("tablet_landscape", "w1280dp-h800dp-land-night-xhdpi")

    private fun capture(
        name: String,
        qualifiers: String,
        state: ConverterUiState = SAMPLE,
        fontScale: Float = 1f,
    ) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            OpenRatesTheme {
                ConverterScreen(
                    state = state,
                    onAmountChange = {},
                    onFromChange = {},
                    onToChange = {},
                    onSwap = {},
                    onRefresh = {},
                    onToggleWatch = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private companion object {
        const val PHONE = "w384dp-h832dp-port-night-600dpi"
        const val SMALL = "w320dp-h568dp-port-night-xhdpi"

        val SAMPLE = ConverterUiState(
            amountInput = "80",
            from = "HKD",
            to = "EUR",
            snapshot = RatesSnapshot(
                base = "EUR",
                rates = mapOf(
                    "HKD" to 8.9135, "USD" to 1.1355, "GBP" to 0.8562,
                    "JPY" to 178.72, "IDR" to 18520.0,
                ),
                dates = mapOf("HKD" to "2026-10-01", "USD" to "2026-10-01"),
                fetchedAtEpochMs = System.currentTimeMillis(),
            ),
            names = mapOf(
                "HKD" to "Hong Kong Dollar", "EUR" to "Euro",
                "USD" to "United States Dollar", "GBP" to "British Pound",
                "JPY" to "Japanese Yen", "IDR" to "Indonesian Rupiah",
            ),
            watchlist = listOf("USD", "GBP", "JPY"),
        )

        // The worst realistic case for width: a big amount into a currency with large units.
        val LONG_NUMBERS = SAMPLE.copy(
            amountInput = "1250000",
            from = "EUR",
            to = "IDR",
            watchlist = listOf("USD", "JPY", "IDR"),
        )
    }
}
