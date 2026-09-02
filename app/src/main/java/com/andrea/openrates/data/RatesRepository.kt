package com.andrea.openrates.data

import android.content.Context

/**
 * Single source of truth for rates: serves whatever was cached last, and layers
 * fresh network data on top when it is available.
 *
 * Nothing here throws for a missing network — callers get a [Result] and keep
 * showing the cached snapshot, which is the whole point of the app working on a
 * plane or a bad train connection.
 */
class RatesRepository(
    private val api: FrankfurterApi,
    private val cache: RatesCache,
) {

    /** Snapshot persisted by the last successful refresh, if any. */
    suspend fun cachedSnapshot(): RatesSnapshot? = cache.readSnapshot()

    /** Currency display names persisted by the last successful refresh, if any. */
    suspend fun cachedNames(): CurrencyNames? = cache.readNames()

    /**
     * Downloads every quote for [base] (`/v2/rates?base=`) and persists it.
     * Currency names are only re-fetched when they are missing, since they
     * change on the order of never.
     */
    suspend fun refresh(base: String): Result<RatesSnapshot> = runCatching {
        val snapshot = api.snapshot(base)
        cache.writeSnapshot(snapshot)
        if (cache.readNames() == null) {
            runCatching { api.currencies() }.onSuccess { cache.writeNames(it) }
        }
        snapshot
    }

    /**
     * Freshest value for one pair via `/v2/rate/{base}/{quote}`, used to confirm
     * the headline conversion rather than relying on the triangulated snapshot.
     */
    suspend fun livePair(from: String, to: String): Result<RateDto> = runCatching {
        api.rate(from, to)
    }

    /** Several quotes in one call via `/v2/rates?base=&quotes=`, for the watchlist. */
    suspend fun liveQuotes(base: String, quotes: List<String>): Result<List<RateDto>> = runCatching {
        if (quotes.isEmpty()) emptyList() else api.rates(base, quotes)
    }

    companion object {
        fun create(context: Context): RatesRepository =
            RatesRepository(FrankfurterApi(), RatesCache(context.applicationContext))
    }
}
