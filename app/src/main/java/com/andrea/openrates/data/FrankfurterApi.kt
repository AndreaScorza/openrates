package com.andrea.openrates.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A 4xx/5xx from the API, carrying the `message` field Frankfurter returns. */
class FrankfurterException(val status: Int, override val message: String) : IOException(message)

/**
 * Thin client for the Frankfurter v2 API (https://api.frankfurter.dev).
 *
 * Endpoints used:
 *  - `GET /v2/rate/{base}/{quote}`          single pair, freshest value for the converter
 *  - `GET /v2/rates?base=X&quotes=A,B,C`    several quotes at once, for the watchlist
 *  - `GET /v2/rates?base=X`                 every quote, cached as the offline snapshot
 *  - `GET /v2/currencies`                   ISO code -> display name
 */
class FrankfurterApi(
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** `GET /v2/rate/{base}/{quote}` -> a single [RateDto]. */
    suspend fun rate(base: String, quote: String): RateDto {
        val url = baseUrl.newBuilder()
            .addPathSegments("v2/rate/$base/$quote")
            .build()
        return json.decodeFromString(RateDto.serializer(), get(url))
    }

    /**
     * `GET /v2/rates?base=..&quotes=A,B,C` -> a flat array of [RateDto].
     * Passing an empty [quotes] asks for every currency.
     */
    suspend fun rates(base: String, quotes: List<String> = emptyList()): List<RateDto> {
        val url = baseUrl.newBuilder()
            .addPathSegments("v2/rates")
            .addQueryParameter("base", base)
            .apply {
                if (quotes.isNotEmpty()) addQueryParameter("quotes", quotes.joinToString(","))
            }
            .build()
        return json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(RateDto.serializer()), get(url))
    }

    /** Every quote for [base], folded into the offline snapshot. */
    suspend fun snapshot(base: String): RatesSnapshot {
        val quotes = rates(base)
        return RatesSnapshot(
            base = base,
            rates = quotes.associate { it.quote to it.rate },
            dates = quotes.associate { it.quote to it.date },
            fetchedAtEpochMs = System.currentTimeMillis(),
        )
    }

    /** `GET /v2/currencies` -> display names, cached so the picker reads well offline. */
    suspend fun currencies(): CurrencyNames {
        val url = baseUrl.newBuilder().addPathSegments("v2/currencies").build()
        val body = get(url)
        val list = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(CurrencyDto.serializer()),
            body,
        )
        return CurrencyNames(
            names = list.associate { it.isoCode to it.name },
            symbols = list.mapNotNull { c -> c.symbol?.let { c.isoCode to it } }.toMap(),
        )
    }

    private suspend fun get(url: HttpUrl): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    json.decodeFromString(ApiError.serializer(), body).message
                }.getOrElse { "HTTP ${response.code}" }
                throw FrankfurterException(response.code, message)
            }
            body
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.frankfurter.dev/"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
