package com.andrea.openrates.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One quote as returned by Frankfurter v2.
 *
 * `GET /v2/rate/{base}/{quote}` returns a single object of this shape, while
 * `GET /v2/rates?base=...` returns a flat JSON *array* of them. Note that the
 * `date` is per quote: illiquid currencies can lag the rest of the snapshot by
 * a day, so it is kept per currency rather than collapsed into one value.
 */
@Serializable
data class RateDto(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

/** An entry of `GET /v2/currencies`. */
@Serializable
data class CurrencyDto(
    @SerialName("iso_code") val isoCode: String,
    val name: String,
    val symbol: String? = null,
)

/** Error body returned with a 4xx, e.g. `{"status":422,"message":"invalid currency: XYZ"}`. */
@Serializable
data class ApiError(
    val status: Int,
    val message: String,
)

/**
 * Everything the app needs to convert any pair while offline.
 *
 * A single `GET /v2/rates?base=EUR` is ~10 KB and covers all 160+ currencies;
 * every other pair is derived locally (see [Conversion.convert]), so one cached
 * snapshot keeps the whole app usable without a network.
 */
@Serializable
data class RatesSnapshot(
    val base: String,
    /** quote ISO code -> rate from [base]. Does not contain [base] itself. */
    val rates: Map<String, Double>,
    /** quote ISO code -> the date that quote is effective for. */
    val dates: Map<String, String> = emptyMap(),
    /** Wall-clock time the snapshot was downloaded, used for the "updated" label. */
    val fetchedAtEpochMs: Long,
) {
    val currencies: List<String> get() = (rates.keys + base).sorted()

    /** Newest date across all quotes; the headline "rates as of" value. */
    val latestDate: String? get() = dates.values.maxOrNull()

    fun dateFor(vararg codes: String): String? =
        codes.filter { it != base }.mapNotNull { dates[it] }.minOrNull() ?: latestDate
}

/** Names for display, cached alongside the rates so the picker works offline too. */
@Serializable
data class CurrencyNames(
    val names: Map<String, String>,
    val symbols: Map<String, String> = emptyMap(),
)
