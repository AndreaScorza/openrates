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

    companion object {
        fun create(context: Context): RatesRepository =
            RatesRepository(FrankfurterApi(), RatesCache(context.applicationContext))
    }
}
